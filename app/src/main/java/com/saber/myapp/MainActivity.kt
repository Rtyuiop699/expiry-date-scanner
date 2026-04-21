package com.saber.myapp

import android.Manifest
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val REQUEST_CAMERA_PERMISSION = 100
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var fab: FloatingActionButton

    private val productList = mutableListOf<Product>()

    // ✅ نظام ماسح الباركود المطور
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "تم إلغاء المسح", Toast.LENGTH_SHORT).show()
        } else {
            val barcodeValue = result.contents
            val existingProduct = databaseHelper.getProductByBarcode(barcodeValue)

            if (existingProduct != null) {
                Toast.makeText(this, "⚠️ المنتج موجود مسبقاً: ${existingProduct.name}", Toast.LENGTH_SHORT).show()
            } else {
                // البحث في الإنترنت بدلاً من الفتح المباشر
                searchProductOnWeb(barcodeValue)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        databaseHelper = DatabaseHelper(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.toolbar_menu)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ProductAdapter(productList) { product ->
            // إجراء عند الضغط على المنتج (مثلاً التعديل)
            openManualAddDialog(product.barcode, product.name, product.imagePath)
        }

        recyclerView.adapter = adapter

        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            checkCameraPermissionAndOpenScanner()
        }

        loadProductsFromDatabase()
    }

    private fun searchProductOnWeb(barcode: String) {
        Toast.makeText(this, "جاري البحث عن المنتج عالمياً...", Toast.LENGTH_SHORT).show()

        ApiClient.instance.getProduct(barcode).enqueue(object : Callback<ProductApiResponse> {
            override fun onResponse(call: Call<ProductApiResponse>, response: Response<ProductApiResponse>) {
                if (response.isSuccessful && response.body()?.status == 1) {
                    val product = response.body()?.product
                    openManualAddDialog(barcode, product?.productName ?: "", product?.imageUrl)
                } else {
                    openManualAddDialog(barcode, "", null)
                }
            }

            override fun onFailure(call: Call<ProductApiResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "فشل الاتصال بالإنترنت", Toast.LENGTH_SHORT).show()
                openManualAddDialog(barcode, "", null)
            }
        })
    }

    private fun openManualAddDialog(barcode: String, name: String, imagePath: String?) {
        val dialog = AddProductDialog(
            this,
            barcode,
            name,
            "", // تاريخ انتهاء فارغ ليقوم المستخدم بمسحه أو إدخاله
            imagePath
        ) { finalName, finalExpiry, finalImagePath ->
            
            val newProduct = Product(
                barcode = barcode,
                name = finalName,
                expiryDate = finalExpiry,
                imagePath = finalImagePath
            )
            databaseHelper.addProduct(newProduct)
            loadProductsFromDatabase()
            Toast.makeText(this, "تم حفظ المنتج بنجاح", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }

    private fun checkCameraPermissionAndOpenScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            openScanner()
        }
    }

    private fun openScanner() {
        val options = ScanOptions()
        options.setPrompt("وجه الكاميرا نحو الباركود")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        options.setCaptureActivity(PortraitScanActivity::class.java)
        barcodeLauncher.launch(options)
    }

    private fun loadProductsFromDatabase() {
        productList.clear()
        productList.addAll(databaseHelper.getAllProducts())
        adapter.notifyDataSetChanged()
    }
}
