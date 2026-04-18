package com.saber.myapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.appbar.MaterialToolbar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    private val REQUEST_CAMERA_PERMISSION = 100

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var fab: FloatingActionButton

    private val productList = mutableListOf<Product>()
    private var currentDialog: AddProductDialog? = null

    // ✅ الباركود
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "تم إلغاء المسح", Toast.LENGTH_SHORT).show()
        } else {
            val barcodeValue = result.contents

            val existingProduct = databaseHelper.getProductByBarcode(barcodeValue)

            if (existingProduct != null) {
                Toast.makeText(
                    this,
                    "⚠️ المنتج موجود مسبقاً: ${existingProduct.name}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                openManualAddDialog(barcodeValue)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.toolbar_menu)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.btnSearch -> {
                    Toast.makeText(this, "بحث", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.btnHelp -> {
                    Toast.makeText(this, "مساعدة", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.btnSettings -> {
                    Toast.makeText(this, "إعدادات", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        databaseHelper = DatabaseHelper(this)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // ✅ عند الضغط على المنتج يتم فتح نافذة الإدخال مع بياناته
        adapter = ProductAdapter(productList) { product ->
            val dialog = AddProductDialog(
                this,
                product.barcode,
                product.name,
                product.expiryDate,
                product.imagePath
            ) { name, expiryDate, imagePath ->

                val updatedProduct = Product(
                    barcode = product.barcode,
                    name = name,
                    expiryDate = expiryDate,
                    imagePath = imagePath
                )

                databaseHelper.updateProduct(updatedProduct)
                loadProductsFromDatabase()

                Toast.makeText(this, "تم تحديث المنتج", Toast.LENGTH_SHORT).show()
            }

            currentDialog = dialog
            dialog.show()
        }

        recyclerView.adapter = adapter

        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            checkCameraPermissionAndOpenScanner()
        }

        loadProductsFromDatabase()

        // ✅ إضافة السحب لليمين مع تأكيد الحذف
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val product = productList[position]

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("تأكيد الحذف")
                    .setMessage("هل تريد حذف هذا المنتج؟")
                    .setPositiveButton("تأكيد") { _, _ ->
                        databaseHelper.deleteProduct(product.barcode)
                        productList.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        Toast.makeText(this@MainActivity, "🗑️ تم حذف المنتج", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("إلغاء") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                    .setCancelable(false)
                    .show()
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = android.graphics.Paint()
                paint.color = android.graphics.Color.RED
                c.drawRect(
                    itemView.left.toFloat(),
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat(),
                    paint
                )

                paint.color = android.graphics.Color.WHITE
                paint.textSize = 40f
                c.drawText("حذف", itemView.left + 50f, itemView.top + (itemView.height / 2f), paint)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun checkCameraPermissionAndOpenScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            openScanner()
        }
    }

    private fun openScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt("قرّب الباركود من الكاميرا")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(false)
        options.setOrientationLocked(true)
        options.setCaptureActivity(PortraitScanActivity::class.java)

        barcodeLauncher.launch(options)
    }

    private fun openManualAddDialog(barcodeValue: String) {
        val dialog = AddProductDialog(
            this,
            barcodeValue,
            "",
            "",
            null
        ) { name, expiryDate, imagePath ->

            val newProduct = Product(
                barcode = barcodeValue,
                name = name,
                expiryDate = expiryDate,
                imagePath = imagePath
            )

            databaseHelper.addProduct(newProduct)
            loadProductsFromDatabase()

            Toast.makeText(this, "تم حفظ المنتج", Toast.LENGTH_SHORT).show()
        }

        currentDialog = dialog
        dialog.show()
    }

    private fun loadProductsFromDatabase() {
        productList.clear()
        productList.addAll(databaseHelper.getAllProducts())
        adapter.notifyDataSetChanged()
    }

    // ✅ تمرير النتيجة إلى الـ Dialog ليظهر الصورة
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        currentDialog?.handleActivityResult(requestCode, resultCode, data)
    }
}
