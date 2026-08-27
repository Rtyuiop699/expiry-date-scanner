package com.saber.myapp
import android.view.View
import android.widget.PopupMenu
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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

        // تفعيل التولبار
        setupToolbar()

        // إعداد الـ Chips
        setupChips()

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            permissionManager.checkAndRequestCameraPermission()
        }

        // تحميل المنتجات من القاعدة
        loadProductsFromDatabase()
    }

    override fun onResume() {
        super.onResume()
        setupChips()
        loadProductsFromDatabase()
    }

    private fun setupToolbar() {

    // بحث
    findViewById<ImageView>(R.id.btnSearch).setOnClickListener {
        Toast.makeText(this, "البحث", Toast.LENGTH_SHORT).show()
    }

    // مساعدة
    findViewById<ImageView>(R.id.btnHelp).setOnClickListener {
        Toast.makeText(this, "مساعدة", Toast.LENGTH_SHORT).show()
    }

    // إعدادات
findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
    showSettingsMenu(it)
}

    // PDF
    findViewById<ImageView>(R.id.btnPdf).setOnClickListener {
        Toast.makeText(this, "PDF", Toast.LENGTH_SHORT).show()
    }
    }

   private fun showSettingsMenu(anchor: View) {

    val popup = PopupMenu(this, anchor)

    popup.menu.add("اللغة")
    popup.menu.add("المظهر")
    popup.menu.add("تصدير المنتجات إلى قاعدة البيانات العالمية")
    popup.menu.add("تصدير صور OCR التي يلتقطها المستخدم لتحسين منتجاتنا")

    popup.setOnMenuItemClickListener { item ->

        when (item.title.toString()) {

            "اللغة" -> {
                Toast.makeText(
                    this,
                    "إعداد اللغة",
                    Toast.LENGTH_SHORT
                ).show()
            }

            "المظهر" -> {
                Toast.makeText(
                    this,
                    "إعداد المظهر",
                    Toast.LENGTH_SHORT
                ).show()
            }

            "تصدير المنتجات إلى قاعدة البيانات العالمية" -> {
                Toast.makeText(
                    this,
                    "تصدير المنتجات",
                    Toast.LENGTH_SHORT
                ).show()
            }

            "تصدير صور OCR التي يلتقطها المستخدم لتحسين منتجاتنا" -> {
                Toast.makeText(
                    this,
                    "تصدير صور OCR",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        true
    }

    popup.show()
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

    private fun setupChips() {
    val chipGroup = findViewById<ChipGroup>(R.id.chipGroupCategories) ?: return

    chipGroup.removeAllViews()

    // إنشاء قائمة التصنيفات
    val categories = mutableListOf<String>()

    // إضافة "الكل" أولاً
    categories.add("الكل")

    // إضافة التصنيفات من قاعدة البيانات
    categories.addAll(databaseHelper.getAllCategories())

    categories.forEach { category ->

        val chip = Chip(this)

        chip.text = category
        chip.isCheckable = true
        chip.textSize = 14f

        // الشكل الافتراضي
        chip.setTextColor(Color.BLACK)

        chip.chipBackgroundColor =
            ColorStateList.valueOf(Color.TRANSPARENT)

        chip.chipStrokeWidth = 1.5f

        chip.chipStrokeColor =
            ColorStateList.valueOf(
                Color.parseColor("#CCCCCC")
            )

        chip.setChipCornerRadius(50f)

        chip.setPadding(16, 8, 16, 8)

        chip.setOnCheckedChangeListener { _, isChecked ->

            if (isChecked) {

                // اللون عند الاختيار
                chip.chipBackgroundColor =
                    ColorStateList.valueOf(
                        Color.parseColor("#025144")
                    )

                chip.chipStrokeColor =
                    ColorStateList.valueOf(
                        Color.parseColor("#025144")
                    )

                chip.setTextColor(Color.WHITE)

                // عرض المنتجات حسب التصنيف
                filterProducts(category)

            } else {

                // اللون عند عدم الاختيار
                chip.chipBackgroundColor =
                    ColorStateList.valueOf(Color.TRANSPARENT)

                chip.chipStrokeColor =
                    ColorStateList.valueOf(
                        Color.parseColor("#CCCCCC")
                    )

                chip.setTextColor(Color.BLACK)
            }
        }

        chipGroup.addView(chip)
    }

    // تحديد "الكل" افتراضياً
    if (chipGroup.childCount > 0) {
        val allChip = chipGroup.getChildAt(0) as Chip
        allChip.isChecked = true
    }
    }
    

    private fun filterProducts(category: String) {
        val allProducts = databaseHelper.getAllProducts()

        val filteredList = if (category == "الكل") {
            allProducts
        } else {
            allProducts.filter { it.category == category }
        }

        listHandler.setup(filteredList.toMutableList())
    }
}
