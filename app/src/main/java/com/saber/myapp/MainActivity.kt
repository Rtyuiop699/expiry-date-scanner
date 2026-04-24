package com.saber.myapp

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageView
import android.widget.Toast
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
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

    private lateinit var searchLayout: TextInputLayout
    private lateinit var searchField: EditText
    private lateinit var btnSearch: ImageView

    private var currentDialog: AddProductDialog? = null

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "تم إلغاء المسح", Toast.LENGTH_SHORT).show()
        } else {
            val barcodeValue = result.contents
            val existingProduct = databaseHelper.getProductByBarcode(barcodeValue)

            if (existingProduct != null) {
                Toast.makeText(this, "⚠️ المنتج موجود مسبقاً: ${existingProduct.name}", Toast.LENGTH_SHORT).show()
            } else {
                searchProductOnWeb(barcodeValue)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        databaseHelper = DatabaseHelper(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ProductAdapter(mutableListOf()) { product ->
            openManualAddDialog(product.barcode, product.name, product.imagePath)
        }
        recyclerView.adapter = adapter

        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            checkCameraPermissionAndOpenScanner()
        }

        // ✅ ربط عناصر البحث
        searchLayout = findViewById(R.id.searchLayout)
        searchField = findViewById(R.id.searchField)
        btnSearch = findViewById(R.id.btnSearch)

        btnSearch.setOnClickListener {
            if (searchLayout.visibility == android.view.View.GONE) {
                searchLayout.visibility = android.view.View.VISIBLE
                searchField.requestFocus()

                // إظهار لوحة المفاتيح
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(searchField, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            } else {
                searchLayout.visibility = android.view.View.GONE
            }
        }

        // ✅ فلترة المنتجات عند الكتابة
        searchField.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

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
        currentDialog = AddProductDialog(
            this,
            barcode,
            name,
            "",
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
        currentDialog?.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        currentDialog?.handleActivityResult(requestCode, resultCode, data)
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
        val products = databaseHelper.getAllProducts()
        adapter.setProducts(products)   // ✅ التعديل الجديد
    }
}
