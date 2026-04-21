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

    // ✅ الباركود - تم التحديث ليدعم البحث العالمي
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
                // استدعاء دالة البحث العالمي بدلاً من فتح الديلوج فوراً
                searchProductOnWeb(barcodeValue)
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

    // ✅ دالة البحث في قاعدة البيانات العالمية (Open Food Facts)
    private fun searchProductOnWeb(barcode: String) {
        ApiClient.instance.getProduct(barcode).enqueue(object : Callback<ProductApiResponse> {
            override fun onResponse(call: Call<ProductApiResponse>, response: Response<ProductApiResponse>) {
                if (response.isSuccessful && response.body()?.status == 1) {
                    val productDetails = response.body()?.product
                    openManualAddDialog(barcode, productDetails?.productName ?: "", productDetails?.imageUrl)
                } else {
                    openManualAddDialog(barcode, "", null)
                }
            }
            override fun onFailure(call: Call<ProductApiResponse>, t: Throwable) {
                openManualAddDialog(barcode, "", null)
            }
        })
    }
                    // لم يتم العثور على المنتج، افتح الديلوج فارغاً
                    openManualAddDialog(barcode, "", null)
                }
            }

            override fun onFailure(call: Call<ProductApiResponse>, t: Throwable) {
                // فشل الاتصال، افتح الديلوج للإدخال اليدوي
                Toast.makeText(this@MainActivity, "فشل الاتصال: سيتم الإدخال يدوياً", Toast.LENGTH_SHORT).show()
                openManualAddDialog(barcode, "", null)
            }
        })
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

    // ✅ تم التعديل لاستقبال اسم المنتج ورابط الصورة
    private fun openManualAddDialog(barcodeValue: String, name: String = "", imageUrl: String? = null) {
        val dialog = AddProductDialog(
            this,
            barcodeValue,
            name,
            "",
            imageUrl // تمرير رابط الصورة لاستخدامه بـ Glide
        ) { productName, expiryDate, imagePath ->

            val newProduct = Product(
                barcode = barcodeValue,
                name = productName,
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
}
