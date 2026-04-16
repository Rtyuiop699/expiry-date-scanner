package com.saber.myapp

import android.Manifest
import com.journeyapps.barcodescanner.CaptureActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

// Retrofit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val REQUEST_CAMERA_PERMISSION = 100

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var fab: FloatingActionButton

    // أزرار الشريط العلوي
    private lateinit var btnSearch: ImageView
    private lateinit var btnHelp: ImageView
    private lateinit var btnSettings: ImageView

    private val productList = mutableListOf<Product>()
    private var currentDialog: AddProductDialog? = null

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
                searchProductInGlobalDatabase(barcodeValue)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        databaseHelper = DatabaseHelper(this)

        // ربط RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ProductAdapter(productList) { product ->

            val dialog = AddProductDialog(
                this,
                product.barcode,
                product.name,
                product.expiryDate,
                product.imagePath
            ) { name, expiryDate, imagePath ->

                val updatedProduct = product.copy(
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

        // FAB
        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            checkCameraPermissionAndOpenScanner()
        }

        // ===== ربط أزرار الشريط العلوي =====
        btnSearch = findViewById(R.id.btnSearch)
        btnHelp = findViewById(R.id.btnHelp)
        btnSettings = findViewById(R.id.btnSettings)

        btnSearch.setOnClickListener {
            Toast.makeText(this, "بحث", Toast.LENGTH_SHORT).show()
        }

        btnHelp.setOnClickListener {
            Toast.makeText(this, "مساعدة", Toast.LENGTH_SHORT).show()
        }

        btnSettings.setOnClickListener {
            Toast.makeText(this, "إعدادات", Toast.LENGTH_SHORT).show()
        }

        loadProductsFromDatabase()
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
        options.setTorchEnabled(true)
        barcodeLauncher.launch(options)
    }

    private fun searchProductInGlobalDatabase(barcode: String) {

        ApiClient.instance.getProduct(barcode).enqueue(object : Callback<ProductApiResponse> {

            override fun onResponse(
                call: Call<ProductApiResponse>,
                response: Response<ProductApiResponse>
            ) {

                if (response.isSuccessful && response.body()?.status == 1) {

                    val apiProduct = response.body()?.product

                    if (apiProduct != null) {
                        showApiProductDialog(barcode, apiProduct)
                    } else {
                        openManualAddDialog(barcode)
                    }

                } else {
                    openManualAddDialog(barcode)
                }
            }

            override fun onFailure(call: Call<ProductApiResponse>, t: Throwable) {
                Toast.makeText(
                    this@MainActivity,
                    "فشل الاتصال: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()

                openManualAddDialog(barcode)
            }
        })
    }

    private fun showApiProductDialog(barcode: String, apiProduct: ProductDetails) {

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("منتج موجود في الإنترنت")

        val message = """
            الاسم: ${apiProduct.productName ?: "غير معروف"}
            العلامة: ${apiProduct.brands ?: "غير معروف"}
            البلد: ${apiProduct.countries ?: "غير معروف"}
        """.trimIndent()

        builder.setMessage(message)

        builder.setPositiveButton("استخدام البيانات") { _, _ ->
            openManualAddDialog(
                barcode,
                apiProduct.productName ?: "",
                apiProduct.brands ?: ""
            )
        }

        builder.setNegativeButton("إدخال يدوي") { _, _ ->
            openManualAddDialog(barcode)
        }

        builder.show()
    }

    private fun openManualAddDialog(
        barcodeValue: String,
        name: String = "",
        extra: String = ""
    ) {

        val dialog = AddProductDialog(
            this,
            barcodeValue,
            name,
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openScanner()
        } else {
            Toast.makeText(this, "إذن الكاميرا مطلوب", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        currentDialog?.handleActivityResult(requestCode, resultCode, data)
    }
}
