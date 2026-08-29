package com.saber.myapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MainActivity : AppCompatActivity() {

    private lateinit var scannerHelper: BarcodeScannerHelper
    private lateinit var permissionManager: PermissionManager
    private lateinit var listHandler: ProductListHandler
    private lateinit var databaseHelper: DatabaseHelper

    private val productList = mutableListOf<Product>()

    // =========================================================
    // التصنيف الحالي
    // =========================================================

    private var currentCategory = "الكل"

    // =========================================================
    // نص البحث الحالي
    // =========================================================

    private var currentSearchText = ""

    // =========================================================
    // فتح شاشة إضافة المنتج
    // =========================================================

    private val addProductLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == RESULT_OK) {

            loadProductsFromDatabase()

            Toast.makeText(
                this,
                "تم حفظ المنتج بنجاح",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // onCreate
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // -----------------------------------------------------
        // قاعدة البيانات
        // -----------------------------------------------------

        databaseHelper = DatabaseHelper(this)


        // -----------------------------------------------------
        // إعداد قائمة المنتجات
        // -----------------------------------------------------

        listHandler = ProductListHandler(
            findViewById(R.id.recyclerView)
        ) { product ->

            Toast.makeText(
                this,
                "منتج: ${product.name}",
                Toast.LENGTH_SHORT
            ).show()
        }


        // -----------------------------------------------------
        // إعداد الماسح
        // -----------------------------------------------------

        scannerHelper = BarcodeScannerHelper(
            activity = this,

            onScanResult = { barcode ->
                handleBarcodeResult(barcode)
            },

            onScanCancelled = {
                Toast.makeText(
                    this,
                    "تم إلغاء المسح",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )


        // -----------------------------------------------------
        // إدارة التصاريح
        // -----------------------------------------------------

        permissionManager = PermissionManager(
            activity = this,

            onPermissionGranted = {
                scannerHelper.startScanner()
            },

            onPermissionDenied = {
                Toast.makeText(
                    this,
                    "عذراً، يجب الموافقة على تصريح الكاميرا",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )


        // -----------------------------------------------------
        // إعداد التولبار
        // -----------------------------------------------------

        setupToolbar()


        // -----------------------------------------------------
        // إعداد التصنيفات
        // -----------------------------------------------------

        setupChips()


        // -----------------------------------------------------
        // إعداد البحث
        // -----------------------------------------------------

        setupSearch()


        // -----------------------------------------------------
        // زر الإضافة / المسح
        // -----------------------------------------------------

        findViewById<FloatingActionButton>(R.id.fab)?.setOnClickListener {

            permissionManager.checkAndRequestCameraPermission()
        }


        // -----------------------------------------------------
        // تحميل المنتجات
        // -----------------------------------------------------

        loadProductsFromDatabase()
    }


    // =========================================================
    // onResume
    // =========================================================

    override fun onResume() {

        super.onResume()

        setupChips()

        loadProductsFromDatabase()
    }


    // =========================================================
    // إعداد البحث
    // =========================================================

    private fun setupSearch() {

        val searchField =
            findViewById<EditText>(R.id.searchField)

        searchField.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }


                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    currentSearchText =
                        s?.toString()?.trim() ?: ""

                    applyFilters()
                }


                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }


    // =========================================================
    // إعداد التولبار
    // =========================================================

    private fun setupToolbar() {

        // -----------------------------------------------------
        // المساعدة
        // -----------------------------------------------------

        findViewById<ImageView>(R.id.btnHelp)?.setOnClickListener {

            showHelpDialog()
        }


        // -----------------------------------------------------
        // الإعدادات
        // -----------------------------------------------------

        findViewById<ImageView>(R.id.btnSettings)?.setOnClickListener {

            anchorView ->

            showSettingsMenu(anchorView)
        }


        // -----------------------------------------------------
        // PDF
        // -----------------------------------------------------

        findViewById<ImageView>(R.id.btnPdf)?.setOnClickListener {

            Toast.makeText(
                this,
                "PDF",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // نافذة المساعدة
    // =========================================================

    private fun showHelpDialog() {

        val dialogView =
            layoutInflater.inflate(
                R.layout.dialoghelp,
                null
            )

        val builder =
            AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)

        val dialog = builder.create()


        dialogView
            .findViewById<android.widget.Button>(
                R.id.btnCloseHelp
            )
            ?.setOnClickListener {

                dialog.dismiss()
            }


        dialog.show()
    }


    // =========================================================
    // قائمة الإعدادات
    // =========================================================

    private fun showSettingsMenu(anchor: View) {

        val popup =
            PopupMenu(this, anchor)


        popup.menu.add("اللغة")

        popup.menu.add("المظهر")

        popup.menu.add(
            "تصدير المنتجات إلى قاعدة البيانات العالمية"
        )

        popup.menu.add(
            "تصدير صور OCR التي يلتقطها المستخدم لتحسين منتجاتنا"
        )


        popup.setOnMenuItemClickListener { item ->

            when (item.title.toString()) {

                "اللغة" -> {

                    showLanguageDialog()
                }


                "المظهر" -> {

                    showThemeDialog()
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


    // =========================================================
    // نافذة المظهر
    // =========================================================

    private fun showThemeDialog() {

        val themes =
            arrayOf(
                "فاتح",
                "داكن"
            )


        AlertDialog.Builder(this)
            .setTitle("اختر المظهر")
            .setItems(themes) { _, which ->

                when (which) {

                    0 -> {

                        AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_NO
                        )
                    }


                    1 -> {

                        AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_YES
                        )
                    }
                }
            }
            .show()
    }


    // =========================================================
    // نافذة اللغة
    // =========================================================

    private fun showLanguageDialog() {

        val languages =
            arrayOf(
                "عربي",
                "English"
            )


        AlertDialog.Builder(this)
            .setTitle(
                "اختر اللغة / Select Language"
            )
            .setItems(languages) { _, which ->

                when (which) {

                    0 -> {

                        setAppLocale("ar")
                    }


                    1 -> {

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


    // =========================================================
    // تغيير لغة التطبيق
    // =========================================================

    private fun setAppLocale(
        languageCode: String
    ) {

        val appLocale =
            LocaleListCompat.forLanguageTags(
                languageCode
            )

        AppCompatDelegate.setApplicationLocales(
            appLocale
        )
    }


    // =========================================================
    // نتيجة الباركود
    // =========================================================

    private fun handleBarcodeResult(
        barcode: String
    ) {

        val existingProduct =
            databaseHelper.getProductByBarcode(
                barcode
            )


        if (existingProduct != null) {

            Toast.makeText(
                this,
                "⚠️ المنتج موجود مسبقاً: ${existingProduct.name}",
                Toast.LENGTH_SHORT
            ).show()

        } else {

            val intent =
                Intent(
                    this,
                    AddProductActivity::class.java
                )

            intent.putExtra(
                "BARCODE_EXTRA",
                barcode
            )

            addProductLauncher.launch(intent)
        }
    }


    // =========================================================
    // تحميل المنتجات
    // =========================================================

    private fun loadProductsFromDatabase() {

        productList.clear()

        productList.addAll(
            databaseHelper.getAllProducts()
        )

        // تطبيق التصنيف + البحث
        applyFilters()
    }


    // =========================================================
    // إعداد التصنيفات
    // =========================================================

    private fun setupChips() {

        val chipGroup =
            findViewById<ChipGroup>(
                R.id.chipGroupCategories
            )
                ?: return


        chipGroup.removeAllViews()


        val categories =
            mutableListOf<String>()


        // التصنيف الأساسي
        categories.add("الكل")


        // التصنيفات من قاعدة البيانات
        categories.addAll(
            databaseHelper.getAllCategories()
        )


        categories.forEach { category ->

            val chip =
                Chip(this)


            chip.text = category

            chip.isCheckable = true

            chip.textSize = 14f


            // الشكل الافتراضي
            chip.setTextColor(
                Color.BLACK
            )


            chip.chipBackgroundColor =
                ColorStateList.valueOf(
                    Color.TRANSPARENT
                )


            chip.chipStrokeWidth = 1.5f


            chip.chipStrokeColor =
                ColorStateList.valueOf(
                    Color.parseColor("#CCCCCC")
                )


            chip.setChipCornerRadius(
                50f
            )


            chip.setPadding(
                16,
                8,
                16,
                8
            )


            // -------------------------------------------------
            // عند اختيار التصنيف
            // -------------------------------------------------

            chip.setOnCheckedChangeListener { _, isChecked ->

                if (isChecked) {

                    // حفظ التصنيف الحالي
                    currentCategory = category


                    // شكل التصنيف المحدد
                    chip.chipBackgroundColor =
                        ColorStateList.valueOf(
                            Color.parseColor("#025144")
                        )


                    chip.chipStrokeColor =
                        ColorStateList.valueOf(
                            Color.parseColor("#025144")
                        )


                    chip.setTextColor(
                        Color.WHITE
                    )


                    // تطبيق التصنيف + البحث
                    applyFilters()

                } else {

                    // الشكل الطبيعي
                    chip.chipBackgroundColor =
                        ColorStateList.valueOf(
                            Color.TRANSPARENT
                        )


                    chip.chipStrokeColor =
                        ColorStateList.valueOf(
                            Color.parseColor("#CCCCCC")
                        )


                    chip.setTextColor(
                        Color.BLACK
                    )
                }
            }


            chipGroup.addView(chip)
        }


        // -----------------------------------------------------
        // استعادة التصنيف الحالي
        // -----------------------------------------------------

        if (chipGroup.childCount > 0) {

            val selectedIndex =
                categories.indexOf(
                    currentCategory
                )


            val index =
                if (selectedIndex >= 0) {
                    selectedIndex
                } else {
                    0
                }


            val selectedChip =
                chipGroup.getChildAt(
                    index
                ) as Chip


            selectedChip.isChecked = true
        }
    }


    // =========================================================
    // تطبيق الفلاتر
    //
    // التصنيف + البحث
    // =========================================================

    private fun applyFilters() {

        val allProducts =
            databaseHelper.getAllProducts()


        // النص الذي أدخله المستخدم
        val search =
            currentSearchText
                .trim()
                .lowercase()


        val filteredList =
            allProducts.filter { product ->


                // -------------------------------------------------
                // فلترة التصنيف
                // -------------------------------------------------

                val matchesCategory =
                    currentCategory == "الكل" ||
                    product.category == currentCategory


                // -------------------------------------------------
                // فلترة البحث
                // -------------------------------------------------

                val matchesSearch =
                    search.isEmpty() ||

                    product.name
                        .lowercase()
                        .contains(search) ||

                    product.barcode
                        .lowercase()
                        .contains(search)


                // -------------------------------------------------
                // يجب تحقق الشرطين
                // -------------------------------------------------

                matchesCategory &&
                        matchesSearch
            }


        // عرض النتائج
        listHandler.setup(
            filteredList.toMutableList()
        )
    }
}
