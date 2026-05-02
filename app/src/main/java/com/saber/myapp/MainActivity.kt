package com.saber.myapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var scannerHelper: BarcodeScannerHelper
    private lateinit var permissionManager: PermissionManager
    private lateinit var listHandler: ProductListHandler
    private lateinit var databaseHelper: DatabaseHelper

    private val productList = mutableListOf<Product>()

    private val addProductLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadProductsFromDatabase()
            Toast.makeText(this, "تم حفظ المنتج بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        databaseHelper = DatabaseHelper(this)

        // إعداد القائمة
        listHandler = ProductListHandler(findViewById(R.id.recyclerView)) { product ->
            Toast.makeText(this, "منتج: ${product.name}", Toast.LENGTH_SHORT).show()
        }

        // إعداد الماسح والتصاريح
        scannerHelper = BarcodeScannerHelper(
            activity = this,
            onScanResult = { barcode -> handleBarcodeResult(barcode) },
            onScanCancelled = { Toast.makeText(this, "تم إلغاء المسح", Toast.LENGTH_SHORT).show() }
        )

        permissionManager = PermissionManager(
            activity = this,
            onPermissionGranted = { scannerHelper.startScanner() },
            onPermissionDenied = { Toast.makeText(this, "عذراً، يجب الموافقة على تصريح الكاميرا", Toast.LENGTH_SHORT).show() }
        )

        // تفعيل التولبار مع جميع أزرارك الأصلية
        setupToolbar()

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            permissionManager.checkAndRequestCameraPermission()
        }

        loadProductsFromDatabase()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        
        // إذا لم يكن المنيو منفوخاً في XML، نقوم بنفخه هنا
        if (toolbar.menu.size() == 0) {
            toolbar.inflateMenu(R.menu.toolbar_menu)
        }

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
    }

    private fun handleBarcodeResult(barcode: String) {
        val existingProduct = databaseHelper.getProductByBarcode(barcode)
        if (existingProduct != null) {
            Toast.makeText(this, "⚠️ المنتج موجود مسبقاً: ${existingProduct.name}", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(this, AddProductActivity::class.java)
            intent.putExtra("BARCODE_EXTRA", barcode)
            addProductLauncher.launch(intent)
        }
    }

    private fun loadProductsFromDatabase() {
        productList.clear()
        productList.addAll(databaseHelper.getAllProducts())
        listHandler.setup(productList)
    }
}
