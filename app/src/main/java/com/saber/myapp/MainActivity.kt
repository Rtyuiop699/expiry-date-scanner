package com.saber.myapp

import android.animation.ValueAnimator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager

import android.content.Context
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
import android.widget.LinearLayout
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.transition.TransitionManager

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

    private var currentCategory = "الكل"

    private var currentSearchText = ""

    // =========================================================
    // وضع التحديد المتعدد
    // =========================================================

    private var isSelectionMode = false

    // =========================================================
    // المنتج المحدد عبر الضغط المطول
    // =========================================================

    private var selectedProduct: Product? = null

    // =========================================================
    // Balloon الحالي
    // =========================================================

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

                onProductClicked = { product ->

                    if (!isSelectionMode) {

                        Toast.makeText(
                            this,
                            "اضغط ضغط مطول لمزيد من الخيارات: ${product.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },

                onProductLongClicked = { view, product ->

                    if (!isSelectionMode) {

                        showProductBalloon(
                            view,
                            product
                        )
                    }
                }
            )


        // =====================================================
        // عناصر البحث والأزرار
        // =====================================================

        val searchField =
            findViewById<EditText>(
                R.id.searchField
            )

        val actionsContainer =
            findViewById<LinearLayout>(
                R.id.actionsContainer
            )

        val searchAndActionsBar =
            findViewById<LinearLayout>(
                R.id.searchAndActionsBar
            )

        val searchContainer =
            findViewById<View>(
                R.id.searchContainer
            )

        val btnMultiSelect =
            findViewById<ImageView>(
                R.id.btnMultiSelect
            )


        // =====================================================
        // زر التحديد المتعدد
        // =====================================================

        btnMultiSelect?.setOnClickListener {

            enterSelectionMode(
                searchField,
                actionsContainer,
                searchAndActionsBar,
                searchContainer
            )
        }


        // =====================================================
        // الضغط على البحث
        // =====================================================

        searchField.setOnFocusChangeListener { _, hasFocus ->

            if (hasFocus && !isSelectionMode) {

                TransitionManager.beginDelayedTransition(
                    searchAndActionsBar
                )

                actionsContainer.visibility =
                    View.GONE

                searchField.visibility =
                    View.VISIBLE

                searchField.isCursorVisible =
                    true

                searchField.requestFocus()

                val imm =
                    getSystemService(
                        Context.INPUT_METHOD_SERVICE
                    ) as? InputMethodManager

                imm?.showSoftInput(
                    searchField,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }
        }


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

    private fun showProductBalloon(
        anchorView: View,
        product: Product
    ) {

        currentBalloon?.dismiss()
        currentBalloon = null

        selectedProduct = product

        val location =
            IntArray(2)

        anchorView.getLocationOnScreen(
            location
        )

        val anchorTop =
            location[1]

        val anchorBottom =
            anchorTop + anchorView.height

        val screenHeight =
            resources.displayMetrics.heightPixels

        val spaceAbove =
            anchorTop

        val spaceBelow =
            screenHeight - anchorBottom

        val showBelow =
            spaceBelow >= spaceAbove

        val arrowOrientation =
            if (showBelow) {

                ArrowOrientation.TOP

            } else {

                ArrowOrientation.BOTTOM
            }


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
                    Color.parseColor("#F1F3F4")
                )

                .setElevation(8)

                .setDismissWhenClicked(false)

                .setDismissWhenTouchOutside(true)

                .setBalloonAnimation(
                    BalloonAnimation.FADE
                )

                .build()


        currentBalloon =
            balloon

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


        // =====================================================
        // إظهار Balloon
        // =====================================================

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
    // الدخول إلى وضع التحديد المتعدد
    // =========================================================
  
    private fun enterSelectionMode(
    searchField: EditText,
    actionsContainer: LinearLayout,
    searchAndActionsBar: LinearLayout,
    searchContainer: View
) {
    if (isSelectionMode) return
    isSelectionMode = true

    // 1. إخفاء لوحة المفاتيح وفقدان التركيز
    searchField.clearFocus()
    searchField.isCursorVisible = false
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(searchField.windowToken, 0)

    closeProductBalloon()

    // 2. إخفاء النص أولاً لمنع تشوهه أثناء تصغير الحقل
    searchField.setText("")
    searchField.visibility = View.GONE

    // 3. تهيئة الانتقال السلس للأزرار والحاوية
    val transition = AutoTransition().apply {
        duration = 250
        interpolator = FastOutSlowInInterpolator()
    }
    TransitionManager.beginDelayedTransition(searchAndActionsBar, transition)

    // 4. تحريك عرض الحاوية بشكل تدريجي مخصص لضمان الانسيابية
    val initialWidth = searchContainer.width
    val targetWidth = dpToPx(52)

    val anim = ValueAnimator.ofInt(initialWidth, targetWidth).apply {
        duration = 250
        interpolator = FastOutSlowInInterpolator()
        addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Int
            val params = searchContainer.layoutParams
            params.width = animatedValue
            if (params is LinearLayout.LayoutParams) {
                params.weight = 0f
            }
            searchContainer.layoutParams = params
        }
    }
    anim.start()

    // 5. إظهار الإجراءات المحددة
    setupSelectionActions(actionsContainer)
    actionsContainer.visibility = View.VISIBLE

    listHandler.setSelectionMode(true)
    }
    

    // =========================================================
    // إعداد أزرار وضع التحديد
    // =========================================================

    private fun setupSelectionActions(
        actionsContainer: LinearLayout
    ) {
                // =====================================================
        // زر الحذف
        // =====================================================

        actionsContainer
            .findViewById<ImageView>(
                R.id.btnDeleteSelected
            )
            ?.setOnClickListener {

                deleteSelectedProducts()
            }


        // =====================================================
        // زر الطباعة
        // =====================================================

        actionsContainer
            .findViewById<ImageView>(
                R.id.btnPrintSelected
            )
            ?.setOnClickListener {

                printSelectedProducts()
            }


        // =====================================================
        // زر PDF
        // =====================================================

        actionsContainer
            .findViewById<ImageView>(
                R.id.btnPdfSelected
            )
            ?.setOnClickListener {

                exportSelectedProductsToPdf()
            }


        // =====================================================
        // إظهار الأزرار
        // =====================================================

        actionsContainer
            .findViewById<ImageView>(
                R.id.btnDeleteSelected
            )
            ?.visibility = View.VISIBLE

        actionsContainer
            .findViewById<ImageView>(
                R.id.btnPrintSelected
            )
            ?.visibility = View.VISIBLE

        actionsContainer
            .findViewById<ImageView>(
                R.id.btnPdfSelected
            )
            ?.visibility = View.VISIBLE
    }


    // =========================================================
    // الخروج من وضع التحديد المتعدد
    // =========================================================

    private fun exitSelectionMode() {

        if (!isSelectionMode) {
            return
        }

        isSelectionMode = false

        listHandler.clearSelection()

        listHandler.setSelectionMode(
            false
        )

        val searchField =
            findViewById<EditText>(
                R.id.searchField
            )

        val actionsContainer =
            findViewById<LinearLayout>(
                R.id.actionsContainer
            )

        val searchAndActionsBar =
            findViewById<LinearLayout>(
                R.id.searchAndActionsBar
            )

        val searchContainer =
            findViewById<View>(
                R.id.searchContainer
            )

        val btnMultiSelect =
            findViewById<ImageView>(
                R.id.btnMultiSelect
            )


        searchField.clearFocus()

        searchField.isCursorVisible =
            true


        val imm =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as? InputMethodManager

        imm?.hideSoftInputFromWindow(
            searchField.windowToken,
            0
        )


        TransitionManager.beginDelayedTransition(
            searchAndActionsBar
        )


        // =====================================================
        // إعادة البحث إلى الحجم الطبيعي
        // =====================================================

        searchContainer.layoutParams =
            searchContainer.layoutParams.apply {

                width = 0
                height = dpToPx(52)

                if (this is LinearLayout.LayoutParams) {
                    weight = 1f
                }
            }


        searchField.visibility =
            View.VISIBLE


        // =====================================================
        // إخفاء أزرار العمليات
        // =====================================================

        actionsContainer
            .findViewById<ImageView>(
                R.id.btnDeleteSelected
            )
            ?.visibility = View.GONE

        actionsContainer
            .findViewById<ImageView>(
                R.id.btnPrintSelected
            )
            ?.visibility = View.GONE

        actionsContainer
            .findViewById<ImageView>(
                R.id.btnPdfSelected
            )
            ?.visibility = View.GONE


        // =====================================================
        // إظهار زر التحديد
        // =====================================================

        btnMultiSelect.visibility =
            View.VISIBLE

        actionsContainer.visibility =
            View.VISIBLE
    }


    // =========================================================
    // حذف المنتجات المحددة
    // =========================================================

    private fun deleteSelectedProducts() {

        val selectedProducts =
            listHandler.getSelectedProducts()

        if (selectedProducts.isEmpty()) {

            Toast.makeText(
                this,
                "لم يتم تحديد أي منتج",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        AlertDialog.Builder(this)

            .setTitle("حذف المنتجات")

            .setMessage(
                "هل تريد حذف ${selectedProducts.size} منتج؟"
            )

            .setNegativeButton(
                "إلغاء",
                null
            )

            .setPositiveButton(
                "حذف"
            ) { _, _ ->

                for (product in selectedProducts) {

                    databaseHelper.deleteProduct(
                        product.barcode
                    )
                }

                exitSelectionMode()

                loadProductsFromDatabase()

                Toast.makeText(
                    this,
                    "تم حذف المنتجات المحددة",
                    Toast.LENGTH_SHORT
                ).show()
            }

            .show()
    }


    // =========================================================
    // طباعة المنتجات المحددة
    // =========================================================

    private fun printSelectedProducts() {

        val selectedProducts =
            listHandler.getSelectedProducts()

        if (selectedProducts.isEmpty()) {

            Toast.makeText(
                this,
                "لم يتم تحديد أي منتج",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        Toast.makeText(
            this,
            "طباعة ${selectedProducts.size} منتج",
            Toast.LENGTH_SHORT
        ).show()
    }


    // =========================================================
    // تصدير المنتجات المحددة إلى PDF
    // =========================================================

    private fun exportSelectedProductsToPdf() {

        val selectedProducts =
            listHandler.getSelectedProducts()

        if (selectedProducts.isEmpty()) {

            Toast.makeText(
                this,
                "لم يتم تحديد أي منتج",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        Toast.makeText(
            this,
            "تصدير ${selectedProducts.size} منتج إلى PDF",
            Toast.LENGTH_SHORT
        ).show()
    }
        // =========================================================
    // التعامل مع نتيجة الباركود
    // =========================================================

    private fun handleBarcodeResult(
        barcode: String
    ) {

        if (barcode.isBlank()) {
            return
        }

        val intent =
            Intent(
                this,
                AddProductActivity::class.java
            ).apply {

                putExtra(
                    "BARCODE_EXTRA",
                    barcode
                )
            }

        addProductLauncher.launch(
            intent
        )
    }


    // =========================================================
    // إعداد شريط الأدوات
    // =========================================================

    private fun setupToolbar() {

        findViewById<ImageView>(
            R.id.btnSettings
        )?.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SettingsActivity::class.java
                )
            )
        }
    }


    // =========================================================
    // إعداد التصنيفات
    // =========================================================

    private fun setupChips() {

        val chipGroup =
            findViewById<ChipGroup>(
                R.id.chipGroupCategories
            )

        chipGroup.removeAllViews()

        val categories =
            databaseHelper.getAllCategories()

        addCategoryChip(
            chipGroup,
            "الكل"
        )

        for (category in categories) {

            if (
                category.isNotBlank() &&
                category != "الكل"
            ) {

                addCategoryChip(
                    chipGroup,
                    category
                )
            }
        }
    }


    // =========================================================
    // إضافة Chip للتصنيف
    // =========================================================

    private fun addCategoryChip(
        chipGroup: ChipGroup,
        category: String
    ) {

        val chip =
            Chip(this).apply {

                text = category

                isCheckable = true

                isChecked =
                    category == currentCategory

                setTextColor(Color.BLACK)

chipStrokeWidth = 0f

chipStrokeColor =
    ColorStateList.valueOf(
        Color.TRANSPARENT
    )

chipBackgroundColor =
    ColorStateList.valueOf(
        Color.parseColor(
            if (isChecked) {
                "#E8F5E9"
            } else {
                "#F1F3F4"
            }
        )
    )

                setOnClickListener {

                    currentCategory =
                        category

                    updateCategoryChipColors(
                        chipGroup
                    )

                    applyFilters()
                }
            }

        chipGroup.addView(
            chip
        )
    }


    // =========================================================
    // تحديث ألوان التصنيفات
    // =========================================================

    private fun updateCategoryChipColors(
        chipGroup: ChipGroup
    ) {

        for (i in 0 until chipGroup.childCount) {

            val child =
                chipGroup.getChildAt(i)
            if (child is Chip) {

    child.chipBackgroundColor =
        ColorStateList.valueOf(
            Color.parseColor(
                if (child.isChecked) {
                    "#E8F5E9"
                } else {
                    "#F1F3F4"
                }
            )
        )
            }
            
        }
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
    // تطبيق البحث والتصنيف
    // =========================================================

    private fun applyFilters() {

        val searchText =
            currentSearchText
                .lowercase()
                .trim()

        val filtered =
            productList.filter { product ->

                val matchesCategory =
                    currentCategory == "الكل" ||
                    product.category == currentCategory

                val matchesSearch =
                    searchText.isEmpty() ||
                    product.name
                        .lowercase()
                        .contains(searchText) ||
                    product.barcode
                        .lowercase()
                        .contains(searchText)

                matchesCategory &&
                        matchesSearch
            }

        listHandler.setup(
            filtered.toMutableList()
        )

        listHandler.setSelectionMode(
            isSelectionMode
        )
    }


    // =========================================================
    // تحميل المنتجات من قاعدة البيانات
    // =========================================================

    private fun loadProductsFromDatabase() {

        productList.clear()

        productList.addAll(
            databaseHelper.getAllProducts()
        )

        setupChips()

        applyFilters()
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
    // تحويل dp إلى px
    // =========================================================

    private fun dpToPx(
        dp: Int
    ): Int {

        return (
            dp *
                resources.displayMetrics.density
            ).toInt()
    }
        // =========================================================
    // نافذة تأكيد حذف منتج واحد
    // =========================================================

    private fun showDeleteConfirmationDialog(
        product: Product
    ) {

        AlertDialog.Builder(this)

            .setTitle("حذف المنتج")

            .setMessage(
                "هل تريد حذف المنتج:\n\n${product.name}؟"
            )

            .setNegativeButton(
                "إلغاء",
                null
            )

            .setPositiveButton(
                "حذف"
            ) { _, _ ->

                databaseHelper.deleteProduct(
                    product.barcode
                )

                loadProductsFromDatabase()

                Toast.makeText(
                    this,
                    "تم حذف المنتج",
                    Toast.LENGTH_SHORT
                ).show()
            }

            .show()
    }


    // =========================================================
    // التعامل مع زر الرجوع
    // =========================================================

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        // =====================================================
        // إذا كان وضع التحديد المتعدد فعالاً
        // =====================================================

        if (isSelectionMode) {

            exitSelectionMode()

            return
        }


        // =====================================================
        // إذا كان البحث نشطاً
        // =====================================================

        val searchField =
            findViewById<EditText>(
                R.id.searchField
            )

        if (searchField.hasFocus()) {

            searchField.clearFocus()

            searchField.isCursorVisible =
                false

            val imm =
                getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as? InputMethodManager

            imm?.hideSoftInputFromWindow(
                searchField.windowToken,
                0
            )

            val actionsContainer =
                findViewById<LinearLayout>(
                    R.id.actionsContainer
                )

            actionsContainer.visibility =
                View.VISIBLE

            val btnMultiSelect =
                findViewById<ImageView>(
                    R.id.btnMultiSelect
                )

            btnMultiSelect.visibility =
                View.VISIBLE

            return
        }


        // =====================================================
        // السلوك الطبيعي لزر الرجوع
        // =====================================================

        super.onBackPressed()
    }


    // =========================================================
    // عند إيقاف النشاط
    // =========================================================

    override fun onDestroy() {

        closeProductBalloon()

        super.onDestroy()
    }
}
