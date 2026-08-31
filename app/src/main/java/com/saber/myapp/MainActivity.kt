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

    private var currentCategory = "الكل"
    private var currentSearchText = ""

    // المنتج المحدد حالياً عبر الضغط المطول
    private var selectedProduct: Product? = null
    private lateinit var floatingActionsMenu: View

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

        // الشريط العائم
        floatingActionsMenu = findViewById(R.id.floatingActionsMenu)

        // -----------------------------------------------------
        // إعداد قائمة المنتجات (مع دعم الضغط العادي والمطول)
        // -----------------------------------------------------
        listHandler = ProductListHandler(
            recyclerView = findViewById(R.id.recyclerView),
            onItemClick = { product ->
                if (floatingActionsMenu.visibility == View.VISIBLE) {
                    hideFloatingMenu()
                } else {
                    Toast.makeText(this, "منتج: ${product.name}", Toast.LENGTH_SHORT).show()
                }
            },
            onItemLongClick = { product ->
                selectedProduct = product
                showFloatingMenu()
            }
        )

        // إعداد أزرار القائمة العائمة
        setupFloatingMenuListeners()

        // -----------------------------------------------------
        // إعداد الماسح والتصاريح والتولبار والبحث
        // -----------------------------------------------------
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

        setupToolbar()
        setupChips()
        setupSearch()

        findViewById<FloatingActionButton>(R.id.fab)?.setOnClickListener {
            permissionManager.checkAndRequestCameraPermission()
        }

        loadProductsFromDatabase()
    }

    // =========================================================
    // إعداد أزرار الشريط العائم
    // =========================================================
    private fun setupFloatingMenuListeners() {
        // 1. زر التعديل
        findViewById<ImageView>(R.id.btnActionEdit)?.setOnClickListener {
            selectedProduct?.let { product ->
                val intent = Intent(this, AddProductActivity::class.java).apply {
                    putExtra("PRODUCT_ID", product.id) // أو مرر بيانات المنتج المُراد تعديله
                }
                addProductLauncher.launch(intent)
            }
            hideFloatingMenu()
        }

        // 2. زر PDF
        findViewById<ImageView>(R.id.btnActionPdf)?.setOnClickListener {
            selectedProduct?.let { product ->
                Toast.makeText(this, "تصدير PDF للمنتج: ${product.name}", Toast.LENGTH_SHORT).show()
            }
            hideFloatingMenu()
        }

        // 3. زر الطباعة
        findViewById<ImageView>(R.id.btnActionPrint)?.setOnClickListener {
            selectedProduct?.let { product ->
                Toast.makeText(this, "جاري طباعة: ${product.name}", Toast.LENGTH_SHORT).show()
            }
            hideFloatingMenu()
        }

        // 4. زر المسح (الحذف)
        findViewById<ImageView>(R.id.btnActionDelete)?.setOnClickListener {
            selectedProduct?.let { product ->
                showDeleteDialog(product)
            }
        }
    }

    private fun showFloatingMenu() {
        floatingActionsMenu.visibility = View.VISIBLE
    }

    private fun hideFloatingMenu() {
        floatingActionsMenu.visibility = View.GONE
        selectedProduct = null
    }

    private fun showDeleteDialog(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("تأكيد الحذف")
            .setMessage("هل أنت تأكد من حذف المنتج: ${product.name}؟")
            .setPositiveButton("حذف") { _, _ ->
                databaseHelper.deleteProduct(product.id) // افترض وجود هذه الدالة في DatabaseHelper
                loadProductsFromDatabase()
                hideFloatingMenu()
                Toast.makeText(this, "تم الحذف بنجاح", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("إلغاء") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        setupChips()
        loadProductsFromDatabase()
    }

    private fun setupSearch() {
        val searchField = findViewById<EditText>(R.id.searchField)
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchText = s?.toString()?.trim() ?: ""
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupToolbar() {
        findViewById<ImageView>(R.id.btnHelp)?.setOnClickListener { showHelpDialog() }
        findViewById<ImageView>(R.id.btnSettings)?.setOnClickListener { anchorView -> showSettingsMenu(anchorView) }
        findViewById<ImageView>(R.id.btnPdf)?.setOnClickListener {
            Toast.makeText(this, "PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showHelpDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialoghelp, null)
        val builder = AlertDialog.Builder(this).setView(dialogView).setCancelable(true)
        val dialog = builder.create()
        dialogView.findViewById<android.widget.Button>(R.id.btnCloseHelp)?.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showSettingsMenu(anchor: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
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
        applyFilters()
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
                    currentCategory = category
                    chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#025144"))
                    chip.chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#025144"))
                    chip.setTextColor(Color.WHITE)
                    applyFilters()
                } else {
                    chip.chipBackgroundColor = ColorStateList.valueOf(Color.TRANSPARENT)
                    chip.chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#CCCCCC"))
                    chip.setTextColor(Color.BLACK)
                }
            }
            chipGroup.addView(chip)
        }

        if (chipGroup.childCount > 0) {
            val selectedIndex = categories.indexOf(currentCategory)
            val index = if (selectedIndex >= 0) selectedIndex else 0
            val selectedChip = chipGroup.getChildAt(index) as Chip
            selectedChip.isChecked = true
        }
    }

    private fun applyFilters() {
        val allProducts = databaseHelper.getAllProducts()
        val search = currentSearchText.trim().lowercase()

        val filteredList = allProducts.filter { product ->
            val matchesCategory = currentCategory == "الكل" || product.category == currentCategory
            val matchesSearch = search.isEmpty() ||
                    product.name.lowercase().contains(search) ||
                    product.barcode.lowercase().contains(search)

            matchesCategory && matchesSearch
        }

        listHandler.setup(filteredList.toMutableList())
    }

    // =========================================================
    // إدارة زر الرجوع (إلغاء الأزرار العائمة أولاً)
    // =========================================================
    override fun onBackPressed() {
        // 1. إذا كانت الأزرار العائمة ظاهرة، يتم إخفاؤها وإلغاء التحديد
        if (floatingActionsMenu.visibility == View.VISIBLE) {
            hideFloatingMenu()
            return
        }

        val searchField = findViewById<EditText>(R.id.searchField)

        // 2. إذا كان حقل البحث يمتلك التركيز
        if (searchField.hasFocus()) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(searchField.windowToken, 0)
            searchField.clearFocus()
            searchField.isCursorVisible = false
        } else {
            super.onBackPressed()
        }
    }
}
