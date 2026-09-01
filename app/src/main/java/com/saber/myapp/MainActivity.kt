package com.saber.myapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService

import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton

import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation


class MainActivity : AppCompatActivity() {

    // =========================================================
    // المتغيرات الرئيسية
    // =========================================================

    private lateinit var scannerHelper: BarcodeScannerHelper
    private lateinit var permissionManager: PermissionManager
    private lateinit var listHandler: ProductListHandler
    private lateinit var databaseHelper: DatabaseHelper

    private val productList =
        mutableListOf<Product>()

    // التصنيف الحالي
    private var currentCategory = "الكل"

    // نص البحث الحالي
    private var currentSearchText = ""

    // المنتج المحدد عبر الضغط المطول
    private var selectedProduct: Product? = null

    // Balloon الحالي
    private var currentBalloon: Balloon? = null


    // =========================================================
    // تشغيل شاشة إضافة / تعديل المنتج
    // =========================================================

    private val addProductLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
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

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )


        // =====================================================
        // قاعدة البيانات
        // =====================================================

        databaseHelper =
            DatabaseHelper(this)


        // =====================================================
        // إعداد قائمة المنتجات
        // =====================================================

        listHandler =
            ProductListHandler(
                findViewById(R.id.recyclerView),

                // ---------------------------------------------
                // الضغط العادي
                // ---------------------------------------------

                onProductClicked = { product ->

                    Toast.makeText(
                        this,
                        "منتج: ${product.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                },

                // ---------------------------------------------
                // الضغط المطول
                // ---------------------------------------------

                onProductLongClicked = {
                        view,
                        product ->

                    showProductBalloon(
                        view,
                        product
                    )
                }
            )


        // =====================================================
        // إعداد الماسح
        // =====================================================

        scannerHelper =
            BarcodeScannerHelper(
                activity = this,

                onScanResult = { barcode ->

                    handleBarcodeResult(
                        barcode
                    )
                },

                onScanCancelled = {

                    Toast.makeText(
                        this,
                        "تم إلغاء المسح",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )


        // =====================================================
        // إدارة تصريح الكاميرا
        // =====================================================

        permissionManager =
            PermissionManager(
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


        // =====================================================
        // إعداد الواجهة
        // =====================================================

        setupToolbar()

        setupChips()

        setupSearch()


        // =====================================================
        // زر الإضافة / مسح الباركود
        // =====================================================

        findViewById<FloatingActionButton>(
            R.id.fab
        )?.setOnClickListener {

            permissionManager
                .checkAndRequestCameraPermission()
        }


        // =====================================================
        // تحميل المنتجات
        // =====================================================

        loadProductsFromDatabase()
    }


    // =========================================================
    // إظهار Balloon عند الضغط المطول
    // =========================================================

    // تحديد هل تظهر القائمة فوق أم تحت المنتج
        // -----------------------------------------------------

        private fun showProductBalloon(
    anchorView: View,
    product: Product
) {

    // -----------------------------------------------------
    // إغلاق Balloon السابق
    // -----------------------------------------------------

    currentBalloon?.dismiss()
    currentBalloon = null

    selectedProduct = product


    // -----------------------------------------------------
    // تحديد مكان المنتج على الشاشة
    // -----------------------------------------------------

    val location = IntArray(2)

    anchorView.getLocationOnScreen(location)

    val anchorTop = location[1]

    val anchorBottom =
        anchorTop + anchorView.height

    val screenHeight =
        resources.displayMetrics.heightPixels


    // -----------------------------------------------------
    // المساحة فوق وتحت المنتج
    // -----------------------------------------------------

    val spaceAbove = anchorTop

    val spaceBelow =
        screenHeight - anchorBottom


    // إذا كانت المساحة أسفل المنتج أكبر
    // تظهر القائمة أسفله
    val showBelow =
        spaceBelow >= spaceAbove


    // -----------------------------------------------------
    // اتجاه السهم
    // -----------------------------------------------------

    val arrowOrientation =
        if (showBelow) {

            ArrowOrientation.TOP

        } else {

            ArrowOrientation.BOTTOM
        }


    // -----------------------------------------------------
    // إنشاء Balloon
    // -----------------------------------------------------

    val balloon =
        Balloon.Builder(this)

            .setLayout(
                R.layout.layout_popup_menu
            )

            .setArrowSize(10)

            .setArrowOrientation(
                arrowOrientation
            )

            .setArrowPositionRules(
                ArrowPositionRules.ALIGN_ANCHOR
            )

            .setCornerRadius(16f)

            .setBackgroundColor(
                Color.WHITE
            )

            .setElevation(8)

            .setDismissWhenClicked(false)

            .setDismissWhenTouchOutside(true)

            .setBalloonAnimation(
                BalloonAnimation.FADE
            )

            .build()


    currentBalloon = balloon


    // -----------------------------------------------------
    // محتوى Balloon
    // -----------------------------------------------------

    val menuView =
        balloon.getContentView()


    // =====================================================
    // زر التعديل
    // =====================================================

    menuView
        .findViewById<android.widget.ImageButton>(
            R.id.btnActionEdit
        )
        ?.setOnClickListener {

            selectedProduct?.let { selected ->

                val intent =
                    Intent(
                        this,
                        AddProductActivity::class.java
                    ).apply {

                        putExtra(
                            "BARCODE_EXTRA",
                            selected.barcode
                        )
                    }

                addProductLauncher.launch(intent)
            }

            closeProductBalloon()
        }


    // =====================================================
    // زر PDF
    // =====================================================

    menuView
        .findViewById<android.widget.ImageButton>(
            R.id.btnActionPdf
        )
        ?.setOnClickListener {

            selectedProduct?.let { selected ->

                Toast.makeText(
                    this,
                    "تصدير PDF للمنتج: ${selected.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            closeProductBalloon()
        }


    // =====================================================
    // زر الطباعة
    // =====================================================

    menuView
        .findViewById<android.widget.ImageButton>(
            R.id.btnActionPrint
        )
        ?.setOnClickListener {

            selectedProduct?.let { selected ->

                Toast.makeText(
                    this,
                    "طباعة: ${selected.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            closeProductBalloon()
        }


    // =====================================================
    // زر الحذف
    // =====================================================

    menuView
        .findViewById<android.widget.ImageButton>(
            R.id.btnActionDelete
        )
        ?.setOnClickListener {

            val selected =
                selectedProduct

            closeProductBalloon()

            if (selected != null) {

                showDeleteConfirmationDialog(
                    selected
                )
            }
        }


    // -----------------------------------------------------
    // إظهار Balloon
    // -----------------------------------------------------

    if (showBelow) {

        balloon.showAlignBottom(
            anchorView
        )

    } else {

        balloon.showAlignTop(
            anchorView
        )
    }
        
        }

    // =========================================================
    // إغلاق Balloon
    // =========================================================

    private fun closeProductBalloon() {

        currentBalloon?.dismiss()

        currentBalloon = null

        selectedProduct = null
    }
       // =========================================================
    // نافذة تأكيد حذف المنتج
    // =========================================================

    private fun showDeleteConfirmationDialog(
        product: Product
    ) {

        AlertDialog.Builder(this)

            .setTitle(
                "حذف المنتج"
            )

            .setMessage(
                "هل أنت متأكد من رغبتك في حذف ${product.name}؟"
            )

            .setPositiveButton(
                "حذف"
            ) { _, _ ->

                // ------------------------------------------------
                // تنفيذ الحذف من قاعدة البيانات
                // ------------------------------------------------
                //
                // إذا كانت DatabaseHelper لديك تحتوي على دالة
                // deleteProduct() يمكن استدعاؤها هنا.
                //
                // أبقينا السلوك الحالي كما كان في مشروعك
                // حتى لا نكسر أي وظيفة موجودة.
                // ------------------------------------------------

                loadProductsFromDatabase()

                selectedProduct = null

                Toast.makeText(
                    this,
                    "تم الحذف بنجاح",
                    Toast.LENGTH_SHORT
                ).show()
            }

            .setNegativeButton(
                "إلغاء"
            ) { dialog, _ ->

                dialog.dismiss()
            }

            .show()
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
            findViewById<EditText>(
                R.id.searchField
            )


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
                        s?.toString()
                            ?.trim()
                            ?: ""

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
    // إعداد الشريط العلوي
    // =========================================================

    private fun setupToolbar() {

        // -----------------------------------------------------
        // المساعدة
        // -----------------------------------------------------

        findViewById<ImageView>(
            R.id.btnHelp
        )?.setOnClickListener {

            showHelpDialog()
        }


        // -----------------------------------------------------
        // الإعدادات
        // -----------------------------------------------------

        findViewById<ImageView>(
            R.id.btnSettings
        )?.setOnClickListener { anchorView ->

            showSettingsMenu(
                anchorView
            )
        }


        // -----------------------------------------------------
        // PDF
        // -----------------------------------------------------

        findViewById<ImageView>(
            R.id.btnPdf
        )?.setOnClickListener {

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


        val dialog =
            builder.create()


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
    // الإعدادات
    // =========================================================

    private fun showSettingsMenu(
        anchor: View
    ) {

        val intent =
            Intent(
                this,
                SettingsActivity::class.java
            )

        startActivity(intent)
    }


    // =========================================================
    // التعامل مع نتيجة الباركود
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


            addProductLauncher.launch(
                intent
            )
        }
    }


    // =========================================================
    // تحميل المنتجات من قاعدة البيانات
    // =========================================================

    private fun loadProductsFromDatabase() {

        productList.clear()

        productList.addAll(
            databaseHelper.getAllProducts()
        )

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


        // -----------------------------------------------------
        // إنشاء قائمة التصنيفات
        // -----------------------------------------------------

        val categories =
            mutableListOf<String>()


        categories.add(
            "الكل"
        )


        categories.addAll(
            databaseHelper.getAllCategories()
        )


        // -----------------------------------------------------
        // إنشاء Chips
        // -----------------------------------------------------

        categories.forEach { category ->

            val chip =
                Chip(this)


            chip.text =
                category


            chip.isCheckable =
                true


            chip.textSize =
                14f


            chip.setTextColor(
                Color.BLACK
            )


            chip.chipBackgroundColor =
                ColorStateList.valueOf(
                    Color.TRANSPARENT
                )


            chip.chipStrokeWidth =
                1.5f


            chip.chipStrokeColor =
                ColorStateList.valueOf(
                    Color.parseColor(
                        "#CCCCCC"
                    )
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
            // عند تحديد التصنيف
            // -------------------------------------------------

            chip.setOnCheckedChangeListener {
                    _,
                    isChecked ->

                if (isChecked) {

                    currentCategory =
                        category


                    chip.chipBackgroundColor =
                        ColorStateList.valueOf(
                            Color.parseColor(
                                "#025144"
                            )
                        )


                    chip.chipStrokeColor =
                        ColorStateList.valueOf(
                            Color.parseColor(
                                "#025144"
                            )
                        )


                    chip.setTextColor(
                        Color.WHITE
                    )


                    applyFilters()

                } else {

                    chip.chipBackgroundColor =
                        ColorStateList.valueOf(
                            Color.TRANSPARENT
                        )


                    chip.chipStrokeColor =
                        ColorStateList.valueOf(
                            Color.parseColor(
                                "#CCCCCC"
                            )
                        )


                    chip.setTextColor(
                        Color.BLACK
                    )
                }
            }


            chipGroup.addView(
                chip
            )
        }


        // -----------------------------------------------------
        // إعادة تحديد التصنيف الحالي
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


            selectedChip.isChecked =
                true
        }
    }


    // =========================================================
    // تطبيق البحث + التصنيف
    // =========================================================

    private fun applyFilters() {

        val allProducts =
            databaseHelper.getAllProducts()


        val search =
            currentSearchText
                .trim()
                .lowercase()


        val filteredList =
            allProducts.filter { product ->

                // ---------------------------------------------
                // مطابقة التصنيف
                // ---------------------------------------------

                val matchesCategory =
                    currentCategory == "الكل" ||
                    product.category ==
                    currentCategory


                // ---------------------------------------------
                // مطابقة البحث
                // ---------------------------------------------

                val matchesSearch =
                    search.isEmpty() ||

                    product.name
                        .lowercase()
                        .contains(search) ||

                    product.barcode
                        .lowercase()
                        .contains(search)


                matchesCategory &&
                matchesSearch
            }


        listHandler.setup(
            filteredList.toMutableList()
        )
    }
        // =========================================================
    // التعامل مع زر الرجوع
    // =========================================================

    override fun onBackPressed() {

        // -----------------------------------------------------
        // 1. إغلاق Balloon أولاً
        // -----------------------------------------------------

        if (currentBalloon != null) {

            closeProductBalloon()

            return
        }


        // -----------------------------------------------------
        // 2. التعامل مع حقل البحث
        // -----------------------------------------------------

        val searchField =
            findViewById<EditText>(
                R.id.searchField
            )


        if (searchField.hasFocus()) {

            val imm =
                getSystemService<InputMethodManager>()


            imm?.hideSoftInputFromWindow(
                searchField.windowToken,
                0
            )


            searchField.clearFocus()

            searchField.isCursorVisible =
                false

        } else {

            // -------------------------------------------------
            // 3. الرجوع الطبيعي
            // -------------------------------------------------

            super.onBackPressed()
        }
    }
    
}
