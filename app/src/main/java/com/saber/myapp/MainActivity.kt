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
     
     private fun showDeleteDialog(product: Product, position: Int) {

    AlertDialog.Builder(this)
        .setTitle("حذف المنتج")
        .setMessage("هل تريد حذف هذا المنتج؟")
        .setPositiveButton("حذف") { _, _ ->

            databaseHelper.deleteProduct(product.id)
            adapter.removeAt(position)

            Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show()
        }
        .setNegativeButton("إلغاء") { dialog, _ ->
            dialog.dismiss()
            adapter.notifyItemChanged(position)
        }
        .show()
}   

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
                searchProductOnWeb(barcodeValue)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        databaseHelper = DatabaseHelper(this)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ProductAdapter(mutableListOf()) { product ->
    openManualAddDialog(
        product.barcode,
        product.name,
        product.expiryDate,
        product.imagePath
    )
}
        recyclerView.adapter = adapter
       val itemTouchHelper = ItemTouchHelper(object :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

        val position = viewHolder.adapterPosition
        val product = adapter.getProductAt(position)

        showDeleteDialog(product, position)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView

        val paint = Paint().apply {
            color = Color.RED
        }

        // خلفية حمراء
        c.drawRect(
            itemView.left.toFloat(),
            itemView.top.toFloat(),
            itemView.left + dX,
            itemView.bottom.toFloat(),
            paint
        )

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
})

itemTouchHelper.attachToRecyclerView(recyclerView)
        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            checkCameraPermissionAndOpenScanner()
        }

        searchLayout = findViewById(R.id.searchLayout)
        searchField = findViewById(R.id.searchField)
        btnSearch = findViewById(R.id.btnSearch)

        btnSearch.setOnClickListener {
            if (searchLayout.visibility == android.view.View.GONE) {
                searchLayout.visibility = android.view.View.VISIBLE
                searchField.requestFocus()

                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(searchField, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            } else {
                searchLayout.visibility = android.view.View.GONE
            }
        }

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

    override fun onResponse(
        call: Call<ProductApiResponse>,
        response: Response<ProductApiResponse>
    ) {
        if (response.isSuccessful && response.body()?.status == 1) {

            val product = response.body()?.product

            openManualAddDialog(
                barcode,
                product?.productName ?: "",
                "",   // 👈 لا يوجد تاريخ من الإنترنت
                product?.imageUrl
            )

        } else {

            openManualAddDialog(
                barcode,
                "",
                "",   // 👈 تاريخ فارغ
                null
            )
        }
    }

    override fun onFailure(call: Call<ProductApiResponse>, t: Throwable) {

        Toast.makeText(
            this@MainActivity,
            "فشل الاتصال بالإنترنت",
            Toast.LENGTH_SHORT
        ).show()

        openManualAddDialog(
            barcode,
            "",
            "",
            null
        )
    }
})
}
    private fun openManualAddDialog(
    barcode: String,
    name: String,
    expiryDate: String,
    imagePath: String?
) {

        currentDialog = AddProductDialog(
            this,
            barcode,
            name,
            expiryDate,
            imagePath
        ) { finalName, finalExpiry, finalImagePath ->

            val existing = databaseHelper.getProductByBarcode(barcode)

            if (existing == null) {
                // 🆕 إضافة منتج جديد
                val newProduct = Product(
                    barcode = barcode,
                    name = finalName,
                    expiryDate = finalExpiry,
                    imagePath = finalImagePath
                )

                databaseHelper.addProduct(newProduct)
                Toast.makeText(this, "تم إضافة المنتج", Toast.LENGTH_SHORT).show()

            } else {
                // ✏️ تعديل منتج موجود
                val isChanged =
                    existing.name != finalName ||
                    existing.expiryDate != finalExpiry ||
                    existing.imagePath != finalImagePath

                if (isChanged) {
                    val updatedProduct = Product(
                        barcode = barcode,
                        name = finalName,
                        expiryDate = finalExpiry,
                        imagePath = finalImagePath
                    )

                    databaseHelper.updateProduct(updatedProduct)
                    Toast.makeText(this, "تم تحديث المنتج", Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(this, "⚠️ لم يتم اكتشاف تعديلات", Toast.LENGTH_SHORT).show()
                }
            }

            loadProductsFromDatabase()
        }

        currentDialog?.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    currentDialog?.handleActivityResult(requestCode, resultCode, data)
}

    private fun checkCameraPermissionAndOpenScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {

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
        options.setPrompt("وجه الكاميرا نحو الباركود")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        options.setCaptureActivity(PortraitScanActivity::class.java)
        barcodeLauncher.launch(options)
    }

    private fun loadProductsFromDatabase() {
        val products = databaseHelper.getAllProducts()
        adapter.setProducts(products)
    }
}
