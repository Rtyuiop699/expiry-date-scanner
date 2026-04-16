package com.saber.myapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "products.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_PRODUCTS = "products"

        private const val COL_ID = "id"
        private const val COL_BARCODE = "barcode"
        private const val COL_NAME = "name"
        private const val COL_EXPIRY = "expiryDate"
        private const val COL_IMAGE = "imagePath"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_PRODUCTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BARCODE TEXT,
                $COL_NAME TEXT,
                $COL_EXPIRY TEXT,
                $COL_IMAGE TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        onCreate(db)
    }

    fun addProduct(product: Product) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_BARCODE, product.barcode)
            put(COL_NAME, product.name)
            put(COL_EXPIRY, product.expiryDate)
            put(COL_IMAGE, product.imagePath)
        }
        db.insert(TABLE_PRODUCTS, null, values)
        db.close()
    }

    fun getAllProducts(): List<Product> {
        val products = mutableListOf<Product>()
        val db = readableDatabase
        val cursor = db.query(TABLE_PRODUCTS, null, null, null, null, null, "$COL_ID DESC")

        if (cursor.moveToFirst()) {
            do {
                products.add(
                    Product(
                        barcode = cursor.getString(cursor.getColumnIndexOrThrow(COL_BARCODE)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                        expiryDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPIRY)),
                        imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE))
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return products
    }

    fun getProductByBarcode(barcode: String): Product? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PRODUCTS,
            null,
            "$COL_BARCODE = ?",
            arrayOf(barcode),
            null, null, null
        )

        val product = if (cursor.moveToFirst()) {
            Product(
                barcode = cursor.getString(cursor.getColumnIndexOrThrow(COL_BARCODE)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                expiryDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPIRY)),
                imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE))
            )
        } else null

        cursor.close()
        db.close()
        return product
    }

    // =========================
    // ✔️ دالة التحديث المضافة
    // =========================
    fun updateProduct(product: Product): Int {
        val db = writableDatabase

        val values = ContentValues().apply {
            put(COL_BARCODE, product.barcode)
            put(COL_NAME, product.name)
            put(COL_EXPIRY, product.expiryDate)
            put(COL_IMAGE, product.imagePath)
        }

        return db.update(
            TABLE_PRODUCTS,
            values,
            "$COL_BARCODE = ?",
            arrayOf(product.barcode)
        )
    }
}
