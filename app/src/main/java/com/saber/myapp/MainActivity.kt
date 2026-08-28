package com.saber.myapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
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

        findViewById<FloatingActionButton>(R.id.fab)?.setOnClickListener {
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
        // مساعدة (عرض نافذة المساعدة بدلاً من Toast)
        findViewById<ImageView>(R.id.btnHelp)?.setOnClickListener {
            showHelpDialog()
        }

        // إعدادات (تظهر القائمة عند الضغط عليها)
        findViewById<ImageView>(R.id.btnSettings)?.setOnClickListener { anchorView ->
            showSettingsMenu(anchorView)
        }

        // PDF
        findViewById<ImageView>(R.id.btnPdf)?.setOnClickListener {
            Toast.makeText(this, "PDF", Toast.LENGTH_SHORT).show()
        }
    }

    // دالة عرض نافذة المساعدة
    private fun showHelpDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialoghelp, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val dialog = builder.create()

        // زر الإغلاق داخل نافذة المساعدة
        dialogView.findViewById<android.widget.Button>(R.id.btnCloseHelp)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSettingsMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)

        // إضافة عناصر القائمة المطلوب ظهورها
        popup.menu.add("اللغة")
        popup.menu.add("المظهر")
        popup.menu.add("تصدير المنتجات إلى قاعدة البيانات العالمية")
        popup.menu.add("تصدير صور OCR التي يلتقطها المستخدم لتحسين منتجاتنا")

        popup.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "اللغة" -> {
                    showLanguageDialog()
                }
                "المظهر" -> {
                    showThemeDialog() // فتح نافذة تغيير المظهر
                }
                "تصدير المنتجات إلى قاعدة البيانات العالمية" -> {
                    Toast.makeText(this, "تصدير المنتجات", Toast.LENGTH_SHORT).show()
                }
                "تصدير صور OCR التي يلتقطها المستخدم لتحسين منتجاتنا" -> {
                    Toast.makeText(this, "تصدير صور OCR", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }

        popup.show()
    }

    // نافذة اختيار المظهر (فاتح / داكن)
    private fun showThemeDialog() {
        val themes = arrayOf("فاتح", "داكن")

        AlertDialog.Builder(this)
            .setTitle("اختر المظهر")
            .setItems(themes) { _, which ->
                when (which) {
                    0 -> {
                        // الوضع الفاتح (Light Mode)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                    1 -> {
                        // الوضع الداكن (Dark Mode)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                }
            }
            .show()
    }

    // نافذة اختيار اللغة
    private fun showLanguageDialog() {
        val languages = arrayOf("عربي", "English")

        AlertDialog.Builder(this)
            .setTitle("اختر اللغة / Select Language")
            .setItems(languages) { _, which ->
                when (which) {
                    0 -> {
                        // إعادة التطبيق إلى اللغة العربية
                        setAppLocale("ar")
                    }
                    1 -> {
                        // إظهار تنبيه بإضافة اللغة الإنجليزية قريباً
                        Toast.makeText(
                            this,
                            "English language support will be added soon.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    // دالة ضبط لغة التطبيق
    private fun setAppLocale(languageCode: String) {
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
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

        val categories = mutableListOf<String>()
        categories.add("الكل")
        categories.addAll(databaseHelper.getAllCategories())

        categories.forEach { category ->
            val chip = Chip(this)

            chip.text = category
            chip.isCheckable = true
            chip.textSize = 14f

            chip.setTextColor(Color.BLACK)
            chip.chipBackgroundColor = ColorStateList.valueOf(Color.TRANSPARENT)
            chip.chipStrokeWidth = 1.5f
            chip.chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#CCCCCC"))
            chip.setChipCornerRadius(50f)
            chip.setPadding(16, 8, 16, 8)

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#025144"))
                    chip.chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#025144"))
                    chip.setTextColor(Color.WHITE)
                    filterProducts(category)
                } else {
                    chip.chipBackgroundColor = ColorStateList.valueOf(Color.TRANSPARENT)
                    chip.chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#CCCCCC"))
                    chip.setTextColor(Color.BLACK)
                }
            }

            chipGroup.addView(chip)
        }

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
