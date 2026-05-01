package com.saber.myapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    // الملفات المساعدة (Helpers)
    private lateinit var scannerHelper: BarcodeScannerHelper
    private lateinit var permissionManager: PermissionManager
    private lateinit var listHandler: ProductListHandler
    private lateinit var databaseHelper: DatabaseHelper

    // البيانات
    private val productList = mutableListOf<Product>()

    // مشغل استقبال النتيجة من صفحة إضافة المنتج
    private val addProductLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // تحديث القائمة فور العودة من صفحة الإضافة بنجاح
            loadProductsFromDatabase()
            Toast.makeText(this, "تم حفظ المنتج بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. تهيئة قاعدة البيانات
        databaseHelper = DatabaseHelper(this)

        // 2. إعداد القائمة (عبر الملف المساعد الجديد)
        listHandler = ProductListHandler(findViewById(R.id.recyclerView)) { product ->
            Toast.makeText(this, "منتج: ${product.name}", Toast.LENGTH_SHORT).show()
        }

        // 3. إعداد إدارة الباركود
        scannerHelper = BarcodeScannerHelper(
            activity = this,
            onScanResult = { barcode -> handleBarcodeResult(barcode) },
            onScanCancelled = { Toast.makeText(this, "تم إلغاء المسح", Toast.LENGTH_SHORT).show() }
        )

        // 4. إعداد مدير التصاريح
        permissionManager = PermissionManager(
            activity = this,
            onPermissionGranted = { scannerHelper.startScanner() },
            onPermissionDenied = { Toast.makeText(this, "عذراً، يجب الموافقة على تصريح الكاميرا", Toast.LENGTH_SHORT).show() }
        )

        // 5. إعداد واجهة المستخدم (Toolbar & FAB)
        setupToolbar()
        
        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            permissionManager.checkAndRequestCameraPermission()
        }

        // تحميل البيانات لأول مرة
        loadProductsFromDatabase()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.toolbar_menu)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.btnSearch -> { /* منطق البحث */ true }
                R.id.btnSettings -> { /* منطق الإعدادات */ true }
                else -> false
            }
        }
    }

    private fun handleBarcodeResult(barcode: String) {
        val existingProduct = databaseHelper.getProductByBarcode(barcode)

        if (existingProduct != null) {
            Toast.makeText(this, "⚠️ المنتج موجود مسبقاً: ${existingProduct.name}", Toast.LENGTH_SHORT).show()
        } else {
            // فتح صفحة الإضافة الجديدة وتمرير الباركود لها
            val intent = Intent(this, AddProductActivity::class.java)
            intent.putExtra("BARCODE_EXTRA", barcode)
            addProductLauncher.launch(intent)
        }
    }

    private fun loadProductsFromDatabase() {
        productList.clear()
        productList.addAll(databaseHelper.getAllProducts())
        listHandler.setup(productList) // تحديث الواجهة عبر المساعد
    }
}
