package com.saber.myapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "products.db"
        private const val DATABASE_VERSION = 3

        private const val TABLE_PRODUCTS = "products"
        private const val TABLE_CATEGORIES = "categories"

        private const val COL_ID = "id"
        private const val COL_BARCODE = "barcode"
        private const val COL_NAME = "name"
        private const val COL_EXPIRY = "expiryDate"
        private const val COL_IMAGE = "imagePath"
        private const val COL_CATEGORY = "category"

        private const val COL_CATEGORY_ID = "id"
        private const val COL_CATEGORY_NAME = "name"
    }

    // =========================
    // إنشاء قاعدة البيانات
    // =========================
    override fun onCreate(db: SQLiteDatabase) {

        // جدول المنتجات
        val createProductsTable = """
            CREATE TABLE $TABLE_PRODUCTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BARCODE TEXT,
                $COL_NAME TEXT,
                $COL_EXPIRY TEXT,
                $COL_IMAGE TEXT,
                $COL_CATEGORY TEXT DEFAULT ''
            )
        """.trimIndent()

        db.execSQL(createProductsTable)

        // جدول التصنيفات
        val createCategoriesTable = """
            CREATE TABLE $TABLE_CATEGORIES (
                $COL_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CATEGORY_NAME TEXT UNIQUE NOT NULL
            )
        """.trimIndent()

        db.execSQL(createCategoriesTable)

        // إضافة التصنيفات الافتراضية
        insertDefaultCategories(db)
    }

    // =========================
    // التصنيفات الافتراضية
    // =========================
    private fun insertDefaultCategories(db: SQLiteDatabase) {

        val defaultCategories = listOf(
            "عصائر",
            "مشروبات غازية",
            "خضار معلبة ومخللات",
            "أسماك معلبة",
            "كيك وبسكويت",
            "آيسكريم ومثلجات"
        )

        for (category in defaultCategories) {

            val values = ContentValues().apply {
                put(COL_CATEGORY_NAME, category)
            }

            db.insertWithOnConflict(
                TABLE_CATEGORIES,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }
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

        if (oldVersion < 3) {

            // إنشاء جدول التصنيفات
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIES (
                    $COL_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_CATEGORY_NAME TEXT UNIQUE NOT NULL
                )
                """.trimIndent()
            )

            // إضافة التصنيفات الافتراضية
            insertDefaultCategories(db)

            // نقل التصنيفات الموجودة مسبقاً في المنتجات
            db.execSQL(
                """
                INSERT OR IGNORE INTO $TABLE_CATEGORIES ($COL_CATEGORY_NAME)
                SELECT DISTINCT $COL_CATEGORY
                FROM $TABLE_PRODUCTS
                WHERE $COL_CATEGORY IS NOT NULL
                AND $COL_CATEGORY != ''
                AND $COL_CATEGORY != 'الكل'
                """.trimIndent()
            )
        }
    }

    // =========================
    // إضافة تصنيف جديد
    // =========================
    fun addCategory(category: String): Boolean {

        val cleanCategory = category.trim()

        if (cleanCategory.isEmpty() || cleanCategory == "الكل") {
            return false
        }

        val db = writableDatabase

        val values = ContentValues().apply {
            put(COL_CATEGORY_NAME, cleanCategory)
        }

        val result = db.insertWithOnConflict(
            TABLE_CATEGORIES,
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )

        db.close()

        return result != -1L
    }

    // =========================
    // جلب جميع التصنيفات
    // =========================
    fun getAllCategories(): List<String> {

        val categories = mutableListOf<String>()

        val db = readableDatabase

        val cursor = db.query(
            TABLE_CATEGORIES,
            arrayOf(COL_CATEGORY_NAME),
            null,
            null,
            null,
            null,
            "$COL_CATEGORY_ID ASC"
        )

        if (cursor.moveToFirst()) {

            do {

                val category = cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_CATEGORY_NAME)
                )

                categories.add(category)

            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return categories
    }

    // =========================
    // إضافة المنتج
    // =========================
    fun addProduct(product: Product): Boolean {

        val db = writableDatabase

        // التأكد من وجود التصنيف في جدول التصنيفات
        if (product.category.isNotBlank()) {

            val categoryValues = ContentValues().apply {
                put(COL_CATEGORY_NAME, product.category.trim())
            }

            db.insertWithOnConflict(
                TABLE_CATEGORIES,
                null,
                categoryValues,
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }

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

        // التأكد من حفظ التصنيف
        if (product.category.isNotBlank()) {

            val categoryValues = ContentValues().apply {
                put(COL_CATEGORY_NAME, product.category.trim())
            }

            db.insertWithOnConflict(
                TABLE_CATEGORIES,
                null,
                categoryValues,
                SQLiteDatabase.CONFLICT_IGNORE
            )
        }

        val values = ContentValues().apply {

            put(COL_BARCODE, product.barcode)
            put(COL_NAME, product.name)
            put(COL_EXPIRY, product.expiryDate)
            put(COL_IMAGE, product.imagePath)
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
    }
