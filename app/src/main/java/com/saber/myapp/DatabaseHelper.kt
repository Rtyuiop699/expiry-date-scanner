package com.saber.myapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "products.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_PRODUCTS = "products"

        private const val COL_ID = "id"
        private const val COL_BARCODE = "barcode"
        private const val COL_NAME = "name"
        private const val COL_EXPIRY = "expiryDate"
        private const val COL_IMAGE = "imagePath"
        private const val COL_CATEGORY = "category"
    }

    // =========================
    // إنشاء قاعدة البيانات
    // =========================
    override fun onCreate(db: SQLiteDatabase) {

        val createTable = """
            CREATE TABLE $TABLE_PRODUCTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BARCODE TEXT,
                $COL_NAME TEXT,
                $COL_EXPIRY TEXT,
                $COL_IMAGE TEXT,
                $COL_CATEGORY TEXT DEFAULT ''
            )
        """.trimIndent()

        db.execSQL(createTable)
    }

    // =========================
    // ترقية قاعدة البيانات
    // =========================
    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {

        if (oldVersion < 2) {

            db.execSQL(
                "ALTER TABLE $TABLE_PRODUCTS " +
                "ADD COLUMN $COL_CATEGORY TEXT DEFAULT ''"
            )
        }
    }

    // =========================
    // منع التكرار + إضافة المنتج
    // =========================
    fun addProduct(product: Product): Boolean {

        val db = writableDatabase

        // التحقق من وجود الباركود مسبقاً
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE_PRODUCTS WHERE $COL_BARCODE = ?",
            arrayOf(product.barcode)
        )

        if (cursor.moveToFirst()) {
            cursor.close()
            db.close()
            return false
        }

        cursor.close()

        val values = ContentValues().apply {

            put(COL_BARCODE, product.barcode)
            put(COL_NAME, product.name)
            put(COL_EXPIRY, product.expiryDate)
            put(COL_IMAGE, product.imagePath)

            // التصنيف
            put(COL_CATEGORY, product.category)
        }

        val result = db.insert(
            TABLE_PRODUCTS,
            null,
            values
        )

        db.close()

        return result != -1L
    }

    // =========================
    // جلب جميع المنتجات
    // =========================
    fun getAllProducts(): List<Product> {

        val products = mutableListOf<Product>()

        val db = readableDatabase

        val cursor = db.query(
            TABLE_PRODUCTS,
            null,
            null,
            null,
            null,
            null,
            "$COL_ID DESC"
        )

        if (cursor.moveToFirst()) {

            do {

                products.add(
                    Product(
                        id = cursor.getInt(
                            cursor.getColumnIndexOrThrow(COL_ID)
                        ),

                        barcode = cursor.getString(
                            cursor.getColumnIndexOrThrow(COL_BARCODE)
                        ),

                        name = cursor.getString(
                            cursor.getColumnIndexOrThrow(COL_NAME)
                        ),

                        expiryDate = cursor.getString(
                            cursor.getColumnIndexOrThrow(COL_EXPIRY)
                        ),

                        imagePath = cursor.getString(
                            cursor.getColumnIndexOrThrow(COL_IMAGE)
                        ),

                        category = cursor.getString(
                            cursor.getColumnIndexOrThrow(COL_CATEGORY)
                        )
                    )
                )

            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return products
    }

    // =========================
    // جلب منتج بواسطة الباركود
    // =========================
    fun getProductByBarcode(barcode: String): Product? {

        val db = readableDatabase

        val cursor = db.query(
            TABLE_PRODUCTS,
            null,
            "$COL_BARCODE = ?",
            arrayOf(barcode),
            null,
            null,
            null
        )

        val product = if (cursor.moveToFirst()) {

            Product(

                id = cursor.getInt(
                    cursor.getColumnIndexOrThrow(COL_ID)
                ),

                barcode = cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_BARCODE)
                ),

                name = cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_NAME)
                ),

                expiryDate = cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_EXPIRY)
                ),

                imagePath = cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_IMAGE)
                ),

                category = cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_CATEGORY)
                )
            )

        } else {
            null
        }

        cursor.close()
        db.close()

        return product
    }

    // =========================
    // تحديث المنتج
    // =========================
    fun updateProduct(product: Product): Int {

        val db = writableDatabase

        val values = ContentValues().apply {

            put(COL_BARCODE, product.barcode)
            put(COL_NAME, product.name)
            put(COL_EXPIRY, product.expiryDate)
            put(COL_IMAGE, product.imagePath)

            // تحديث التصنيف أيضاً
            put(COL_CATEGORY, product.category)
        }

        val result = db.update(
            TABLE_PRODUCTS,
            values,
            "$COL_BARCODE = ?",
            arrayOf(product.barcode)
        )

        db.close()

        return result
    }

    // =========================
    // حذف المنتج
    // =========================
    fun deleteProduct(barcode: String): Int {

        val db = writableDatabase

        val result = db.delete(
            TABLE_PRODUCTS,
            "$COL_BARCODE = ?",
            arrayOf(barcode)
        )

        db.close()

        return result
    }

    // =========================
    // جلب أسماء التصنيفات
    // =========================
    fun getAllCategories(): List<String> {

        val categories = mutableListOf<String>()

        val db = readableDatabase

        val cursor = db.rawQuery(
            """
            SELECT DISTINCT $COL_CATEGORY
            FROM $TABLE_PRODUCTS
            WHERE $COL_CATEGORY IS NOT NULL
            AND $COL_CATEGORY != ''
            AND $COL_CATEGORY != 'الكل'
            ORDER BY $COL_CATEGORY ASC
            """.trimIndent(),
            null
        )

        if (cursor.moveToFirst()) {

            do {

                val category = cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_CATEGORY)
                )

                categories.add(category)

            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return categories
    }
    }
