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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var scannerHelper: BarcodeScannerHelper
    private lateinit var permissionManager: PermissionManager
    private lateinit var listHandler: ProductListHandler
    private lateinit var databaseHelper: DatabaseHelper

    private val productList = mutableListOf<Product>()

    // التصنيف الحالي
    private var currentCategory = "الكل"

    // نص البحث الحالي
    private var currentSearchText = ""

    // المنتج المحدد حالياً عبر الضغط المطول
    private var selectedProduct: Product? = null

    // قائمة الأزرار العائمة
    

    // فتح شاشة إضافة المنتج
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

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // =====================================================
        // قاعدة البيانات
        // =====================================================

        databaseHelper = DatabaseHelper(this)

        // =====================================================
        // قائمة الأزرار العائمة
        // =====================================================



        // =====================================================
        // إعداد قائمة المنتجات
        // =====================================================

        listHandler = ProductListHandler(
            recyclerView = findViewById(R.id.recyclerView),

            // الضغط العادي
            onProductClicked = { product ->

                if (
                    floatingActionsMenu.visibility ==
                    View.VISIBLE
                ) {

                    hideFloatingMenu()

                } else {

                    Toast.makeText(
                        this,
                        "منتج: ${product.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },

            // الضغط المطول
        

        // =====================================================
        // إعداد الأزرار العائمة
        // =====================================================

    

        // =====================================================
        // إعداد الماسح
        // =====================================================

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
    

        // =====================================================
        // إدارة التصاريح
        // =====================================================

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

        // =====================================================
        // إعداد الواجهة
        // =====================================================

        setupToolbar()

        setupChips()

        setupSearch()

        // =====================================================
        // زر الإضافة / المسح
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
    // إعداد وظائف الأزرار العائمة
    // =========================================================


        // -----------------------------------------------------
        // زر التعديل
        // -----------------------------------------------------

        
        // -----------------------------------------------------
        // زر PDF
        // -----------------------------------------------------

        

        // -----------------------------------------------------
        // زر الحذف
        // -----------------------------------------------------

        
        // إعادة الموقع إلى الوضع الطبيعي
        
    // =========================================================
    // إعداد الشريط العلوي
    // =========================================================

    private fun setupToolbar() {

        findViewById<ImageView>(
            R.id.btnHelp
        )?.setOnClickListener {

            showHelpDialog()
        }

        findViewById<ImageView>(
            R.id.btnSettings
        )?.setOnClickListener { anchorView ->

            showSettingsMenu(anchorView)
        }

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

        dialogView.findViewById<android.widget.Button>(
            R.id.btnCloseHelp
        )?.setOnClickListener {

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
    // نتيجة مسح الباركود
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
            ) ?: return

        chipGroup.removeAllViews()

        val categories =
            mutableListOf<String>()

        categories.add("الكل")

        categories.addAll(
            databaseHelper.getAllCategories()
        )

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
    // تطبيق البحث والتصنيف
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

                val matchesCategory =
                    currentCategory == "الكل" ||
                            product.category ==
                            currentCategory

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
        // 1. إذا كانت الأزرار العائمة ظاهرة
        //    نخفيها أولاً
        // -----------------------------------------------------

        if (
            floatingActionsMenu.visibility ==
            View.VISIBLE
        ) {

            hideFloatingMenu()

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
                getSystemService(
                    INPUT_METHOD_SERVICE
                ) as android.view.inputmethod.InputMethodManager

            imm.hideSoftInputFromWindow(
                searchField.windowToken,
                0
            )

            searchField.clearFocus()

            searchField.isCursorVisible =
                false

        } else {

            super.onBackPressed()
        }
    }
}
