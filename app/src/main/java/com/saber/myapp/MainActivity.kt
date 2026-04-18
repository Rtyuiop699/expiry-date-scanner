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
    private var currentDialog: AddProductDialog? = null

    // ✅ الباركود
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "تم إلغاء المسح", Toast.LENGTH_SHORT).show()
        } else {
            val barcodeValue = result.contents

            val existingProduct = databaseHelper.getProductByBarcode(barcodeValue)

            if (existingProduct != null) {
                // ✅ المنتج موجود محليًا → افتح نافذة الإدخال اليدوي مع البيانات المحفوظة
                openManualAddDialog(
                    barcodeValue,
                    existingProduct.name,
                    existingProduct.expiryDate,
                    existingProduct.imagePath
                )
            } else {
                // ✅ المنتج غير موجود محليًا → جرب البحث في قاعدة البيانات العالمية
                val apiService = ApiClient.getClient().create(OpenFoodFactsApi::class.java)
                val call = apiService.getProduct(barcodeValue)

                call.enqueue(object : Callback<ProductApiResponse> {
                    override fun onResponse(
                        call: Call<ProductApiResponse>,
                        response: Response<ProductApiResponse>
                    ) {
                        if (response.isSuccessful && response.body()?.product != null) {
                            val productApi = response.body()!!.product

                            // افتح نافذة الإدخال اليدوي مع البيانات القادمة من الـ API
                            openManualAddDialog(
                                barcodeValue,
                                productApi.productName ?: "",
                                "", // تاريخ الصلاحية غير موجود في API
                                productApi.imageUrl
                            )
                        } else {
                            // المنتج غير موجود في قاعدة البيانات العالمية → إدخال يدوي فارغ
                            openManualAddDialog(barcodeValue, "", "", null)
                        }
                    }

                    override fun onFailure(call: Call<ProductApiResponse>, t: Throwable) {
                        // فشل الاتصال → إدخال يدوي فارغ + رسالة خطأ
                        Toast.makeText(this@MainActivity, "فشل الاتصال بالشبكة", Toast.LENGTH_SHORT).show()
                        openManualAddDialog(barcodeValue, "", "", null)
                    }
                })
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

        adapter = ProductAdapter(productList) { product ->
            Toast.makeText(this, product.name, Toast.LENGTH_SHORT).show()
        }

        recyclerView.adapter = adapter

        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            checkCameraPermissionAndOpenScanner()
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

        barcodeLauncher.launch(options)
    }

    private fun openManualAddDialog(barcodeValue: String, name: String, expiryDate: String, imagePath: String?) {
        val dialog = AddProductDialog(
            this,
            barcodeValue,
            name,
            expiryDate,
            imagePath
        ) { newName, newExpiryDate, newImagePath ->

            val newProduct = Product(
                barcode = barcodeValue,
                name = newName,
                expiryDate = newExpiryDate,
                imagePath = newImagePath
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
}
