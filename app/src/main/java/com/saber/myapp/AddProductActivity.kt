package com.saber.myapp

import android.view.View
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.saber.myapp.databinding.ActivityAddProductBinding
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import java.time.DateTimeException


class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private var hasPack = true
    private lateinit var databaseHelper: DatabaseHelper
    private var currentImagePath: String? = null
    private val REQUEST_PRODUCT_CAMERA = 1001
    private val REQUEST_DATE_SCAN = 1002
    private lateinit var categoriesAdapter: ArrayAdapter<String>

    // القائمة الأساسية للتصنيفات
    private val categories = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityAddProductBinding.inflate(layoutInflater)
    setContentView(binding.root)

    databaseHelper = DatabaseHelper(this)

    // 1. --- إعداد Adapter التصنيفات أولاً ---
    categoriesAdapter = ArrayAdapter(
        this,
        android.R.layout.simple_dropdown_item_1line,
        categories
    )

    binding.autoCompleteCategories.setAdapter(categoriesAdapter)

    // تحميل التصنيفات من قاعدة البيانات بعد تهيئة الـ Adapter
    loadCategories()

    // 2. --- إضافة تصنيف جديد ---
    binding.btnAddCategory.setOnClickListener {

        val editText = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("إضافة تصنيف جديد")
            .setView(editText)
            .setPositiveButton("إضافة") { _, _ ->

                val newCategory = editText.text.toString().trim()

                // التأكد من أن الاسم ليس فارغاً
                if (newCategory.isEmpty()) {

                    Toast.makeText(
                        this,
                        "يرجى كتابة اسم التصنيف",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                // إضافة التصنيف إلى قاعدة البيانات
                val added = databaseHelper.addCategory(newCategory)

                if (added) {

                    // إعادة تحميل التصنيفات
                    loadCategories()

                    // اختيار التصنيف الجديد مباشرة
                    binding.autoCompleteCategories.setText(
                        newCategory,
                        false
                    )

                    Toast.makeText(
                        this,
                        "تمت إضافة التصنيف: $newCategory",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    // التصنيف موجود مسبقاً
                    loadCategories()

                    binding.autoCompleteCategories.setText(
                        newCategory,
                        false
                    )

                    Toast.makeText(
                        this,
                        "التصنيف موجود مسبقاً",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    // 3. --- التقاط صورة المنتج ---
    binding.btnCaptureImage.setOnClickListener {

        val intent = Intent(
            this,
            ProductCameraActivity::class.java
        )

        startActivityForResult(
            intent,
            REQUEST_PRODUCT_CAMERA
        )
    }

    // 4. --- اختيار صورة من المعرض ---
    binding.btnChooseImage.setOnClickListener {

        Toast.makeText(
            this,
            "ميزة اختيار من المعرض قريباً",
            Toast.LENGTH_SHORT
        ).show()
    }

   // 5. --- مسح تاريخ الانتهاء بواسطة OCR ---
binding.btnScanDate.setOnClickListener {

    val intent = Intent(
        this,
        DateScannerActivity::class.java
    )

    startActivityForResult(
        intent,
        REQUEST_DATE_SCAN
    )
}

    // 6. --- زر الباكت ---
    binding.btnHasPack.setOnClickListener {

    hasPack = !hasPack

    if (hasPack) {

        // يوجد باكت
        binding.btnHasPack.text = "يوجد باكت ✓"

        binding.layoutPackQuantity.visibility = View.VISIBLE

        binding.layoutPackPurchasePrice.visibility = View.VISIBLE
        binding.layoutPackSalePrice.visibility = View.VISIBLE
        binding.layoutPackProfit.visibility = View.VISIBLE

    } else {

        // لا يوجد باكت
        binding.btnHasPack.text = "لا يوجد باكت ✕"

        binding.layoutPackQuantity.visibility = View.GONE

        binding.layoutPackPurchasePrice.visibility = View.GONE
        binding.layoutPackSalePrice.visibility = View.GONE
        binding.layoutPackProfit.visibility = View.GONE

        // تنظيف قيمة الباكت حتى لا تدخل في الحساب
        binding.editPack.setText("")

        binding.tvPackPurchasePrice.setText("")
        binding.tvPackSalePrice.setText("")
        binding.tvPackProfit.setText("")
    }

    // إعادة الحساب مباشرة
    calculateQuantity()
    }

    // 7. --- حساب الكمية ---
    binding.btnCalculate.setOnClickListener {
        calculateQuantity()
    }

    // 8. --- استقبال البيانات القادمة من Intent ---
    loadIntentData()

    // 9. --- إعداد شريط الأدوات ---
    setupToolbar()
    }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
) {
    super.onActivityResult(
        requestCode,
        resultCode,
        data
    )

    // =====================================================
    // نتيجة كاميرا صورة المنتج
    // =====================================================

    if (
        requestCode == REQUEST_PRODUCT_CAMERA &&
        resultCode == RESULT_OK
    ) {

        val imagePath =
    data?.getStringExtra(
        ProductCameraActivity.EXTRA_IMAGE_PATH
    )

        if (!imagePath.isNullOrBlank()) {

            currentImagePath = imagePath

            processProductImage(
                imagePath
            )
        }

        return
    }

    // =====================================================
    // نتيجة OCR المحلي
    // =====================================================
    // وتشمل أيضًا:
    // DateScannerActivity
    //       ↓
    // Gemini S.OCR
    //       ↓
    // DateScannerActivity
    //       ↓
    // هنا
    // =====================================================

    if (
        requestCode == REQUEST_DATE_SCAN &&
        resultCode == RESULT_OK
    ) {

        val recognizedDate =
            data?.getStringExtra(
                DateScannerActivity.EXTRA_DATE
            )

        if (!recognizedDate.isNullOrBlank()) {

            binding.editTextDate.setText(
                recognizedDate
            )

            binding.editTextDate.setSelection(
                binding.editTextDate.text?.length ?: 0
            )
        }
    }
  }
                
    private fun loadIntentData() { 
        val barcodeValue = intent.getStringExtra("BARCODE_EXTRA") ?: "" 
        val nameValue = intent.getStringExtra("NAME_EXTRA") ?: "" 
        val expiryValue = intent.getStringExtra("EXPIRY_EXTRA") ?: "" 
        val imagePathValue = intent.getStringExtra("IMAGE_PATH_EXTRA") 

        binding.editTextBarcode.setText(barcodeValue) 
        binding.editTextProductName.setText(nameValue) 
        binding.editTextDate.setText(expiryValue) 
        processProductImage(imagePathValue) 

        if (barcodeValue.isNotEmpty() && nameValue.isBlank()) { 
            fetchProductFromApi(barcodeValue) 
        }
    }
private fun loadCategories() {

    categories.clear()
    categories.addAll(databaseHelper.getAllCategories())

    categoriesAdapter.notifyDataSetChanged()
}
    private fun processProductImage(imagePathValue: String?) {

    if (imagePathValue.isNullOrBlank()) {
        return
    }

    currentImagePath = imagePathValue

    // صورة من الإنترنت
    if (
        imagePathValue.startsWith("http://") ||
        imagePathValue.startsWith("https://")
    ) {

        Glide.with(this)
            .load(imagePathValue)
            .placeholder(android.R.color.darker_gray)
            .error(android.R.drawable.ic_menu_report_image)
            .into(binding.imageViewProduct)

        return
    }

    // صورة محلية
    val file = File(imagePathValue)

    if (file.exists()) {

        Glide.with(this)
            .load(file)
            .into(binding.imageViewProduct)

    } else {

        binding.imageViewProduct.setImageResource(
            android.R.drawable.ic_menu_report_image
        )
    }
    }
    private fun saveProduct() {

    // =========================
    // البيانات الأساسية
    // =========================

    val name = binding.editTextProductName.text.toString().trim()

    val rawDate = binding.editTextDate.text.toString().trim()
    val normalizedDate = normalizeDate(rawDate)

    val barcode = binding.editTextBarcode.text.toString().trim()

    val category =
        binding.autoCompleteCategories.text.toString().trim()

   // =========================
// التحقق من البيانات الأساسية
// =========================

if (
    name.isBlank() ||
    rawDate.isBlank() ||
    normalizedDate == null ||
    currentImagePath == null ||
    category.isBlank()
) {
    Toast.makeText(
        this,
        "يرجى ملء جميع الحقول وإضافة صورة",
        Toast.LENGTH_SHORT
    ).show()

    return
}


    // =========================
    // الكميات
    // =========================

    // عدد الكراتين
    val cartons =
        binding.editCarton.text.toString().toIntOrNull() ?: 0

    // عدد الباكت داخل الكرتون
    val packsPerCarton =
        binding.editPack.text.toString().toIntOrNull() ?: 0

    // عدد الحبات داخل الباكت
    val piecesPerPack =
        binding.editPiece.text.toString().toIntOrNull() ?: 0

    // =========================
    // الأسعار
    // =========================

    // سعر شراء الكرتون
    val cartonPurchasePrice =
        binding.editCartonPurchasePrice.text
            .toString()
            .toDoubleOrNull() ?: 0.0

    // سعر بيع الحبة
    val pieceSalePrice =
        binding.editPieceSalePrice.text
            .toString()
            .toDoubleOrNull() ?: 0.0

    // =========================
    // إنشاء المنتج
    // =========================

    val product = Product(
        id = 0,
        barcode = barcode,
        name = name,
        expiryDate = normalizedDate,

        cartons = cartons,
        packsPerCarton = packsPerCarton,
        piecesPerPack = piecesPerPack,

        cartonPurchasePrice = cartonPurchasePrice,
        pieceSalePrice = pieceSalePrice,

        imagePath = currentImagePath!!,
        category = category
    )

    // =========================
    // حفظ المنتج في قاعدة البيانات
    // =========================

    databaseHelper.addProduct(product)

    Toast.makeText(
        this,
        "تم الحفظ: $name - $category",
        Toast.LENGTH_SHORT
    ).show()

    setResult(RESULT_OK)

    finish()
    }


    private fun setupToolbar() {

    binding.topAppBar.setNavigationOnClickListener {
        finish()
    }

    binding.topAppBar.setOnMenuItemClickListener { menuItem ->

        when (menuItem.itemId) {

            R.id.btnSaveAction -> {
                saveProduct()
                true
            }

            R.id.btnPrint -> {

                Toast.makeText(
                    this,
                    "جاري الطباعة...",
                    Toast.LENGTH_SHORT
                ).show()

                true
            }

            R.id.btnPdf -> {

                // =========================
                // البيانات الأساسية
                // =========================

                val name =
                    binding.editTextProductName.text
                        .toString()
                        .trim()

                val rawDate =
                    binding.editTextDate.text
                        .toString()
                        .trim()

                val normalizedDate =
                    normalizeDate(rawDate)

                val category =
                    binding.autoCompleteCategories.text
                        .toString()
                        .trim()

                // =========================
                // التحقق من اسم المنتج
                // =========================

                if (name.isBlank()) {

                    Toast.makeText(
                        this,
                        "يرجى إدخال اسم المنتج أولاً",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnMenuItemClickListener true
                }

                // =========================
                // التحقق من التاريخ
                // =========================

                if (
                    rawDate.isBlank() ||
                    normalizedDate == null
                ) {

                    Toast.makeText(
                        this,
                        "يرجى إدخال تاريخ انتهاء صحيح أولاً",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnMenuItemClickListener true
                }

                // =========================
                // التحقق من التصنيف
                // =========================

                if (category.isBlank()) {

                    Toast.makeText(
                        this,
                        "يرجى اختيار التصنيف أولاً",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnMenuItemClickListener true
                }

                // =========================
                // التحقق من صورة المنتج
                // =========================

                if (currentImagePath.isNullOrBlank()) {

                    Toast.makeText(
                        this,
                        "يرجى إضافة صورة المنتج أولاً",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnMenuItemClickListener true
                }

                // =========================
                // الباركود
                // =========================

                val barcode =
                    binding.editTextBarcode.text
                        .toString()
                        .trim()

                // =========================
                // إنشاء المنتج مؤقتاً للـ PDF
                // =========================

                val product = Product(
                    id = 0,
                    barcode = barcode,
                    name = name,
                    expiryDate = normalizedDate,

                    cartons =
                        binding.editCarton.text
                            .toString()
                            .toIntOrNull() ?: 0,

                    packsPerCarton =
                        binding.editPack.text
                            .toString()
                            .toIntOrNull() ?: 0,

                    piecesPerPack =
                        binding.editPiece.text
                            .toString()
                            .toIntOrNull() ?: 0,

                    cartonPurchasePrice =
                        binding.editCartonPurchasePrice.text
                            .toString()
                            .toDoubleOrNull() ?: 0.0,

                    pieceSalePrice =
                        binding.editPieceSalePrice.text
                            .toString()
                            .toDoubleOrNull() ?: 0.0,

                    imagePath = currentImagePath!!,

                    category = category
                )

                // =========================
                // إنشاء PDF
                // =========================

                try {

                    val pdfFile =
                        ProductPdfGenerator(this)
                            .createPdf(product)

                    Toast.makeText(
                        this,
                        "تم إنشاء ملف PDF بنجاح",
                        Toast.LENGTH_LONG
                    ).show()

                } catch (e: Exception) {

                    Toast.makeText(
                        this,
                        "فشل إنشاء PDF: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    e.printStackTrace()
                }

                true
            }

            R.id.btnDelete -> {

                Toast.makeText(
                    this,
                    "تم حذف المنتج",
                    Toast.LENGTH_SHORT
                ).show()

                true
            }

            else -> false
        }
    }
    }

                

   private fun calculateQuantity() {

    // =========================
    // الكميات
    // =========================

    // عدد الكراتين
    val cartons =
        binding.editCarton.text.toString().toIntOrNull() ?: 0

    // عدد الباكت داخل الكرتون
    val packsPerCarton =
        binding.editPack.text.toString().toIntOrNull() ?: 0

    // الرقم الموجود في editPiece
    // إذا يوجد باكت = الحبات داخل الباكت
    // إذا لا يوجد باكت = الحبات داخل الكرتون
    val pieces =
        binding.editPiece.text.toString().toIntOrNull() ?: 0


    // =========================
    // الأسعار التي يدخلها المستخدم
    // =========================

    // سعر شراء الكرتون
    val cartonPurchasePrice =
        binding.editCartonPurchasePrice.text
            .toString()
            .toDoubleOrNull() ?: 0.0

    // سعر بيع الحبة
    val pieceSalePrice =
        binding.editPieceSalePrice.text
            .toString()
            .toDoubleOrNull() ?: 0.0


    // =====================================================
    // الحالة الأولى: المنتج يحتوي على باكت
    // =====================================================

    if (hasPack) {

        // =========================
        // أسعار الشراء
        // =========================

        val packPurchasePrice =
            if (packsPerCarton > 0)
                cartonPurchasePrice / packsPerCarton
            else
                0.0

        val piecePurchasePrice =
            if (pieces > 0)
                packPurchasePrice / pieces
            else
                0.0

        val totalPurchasePrice =
            cartonPurchasePrice * cartons


        // =========================
        // أسعار البيع
        // =========================

        val packSalePrice =
            pieceSalePrice * pieces

        val cartonSalePrice =
            packSalePrice * packsPerCarton

        val totalSalePrice =
            cartonSalePrice * cartons


        // =========================
        // الأرباح
        // =========================

        val pieceProfit =
            pieceSalePrice - piecePurchasePrice

        val packProfit =
            packSalePrice - packPurchasePrice

        val cartonProfit =
            cartonSalePrice - cartonPurchasePrice

        val totalProfit =
            totalSalePrice - totalPurchasePrice


        // =========================
        // عرض أسعار الشراء
        // =========================

        binding.tvPackPurchasePrice.setText(
            formatPrice(packPurchasePrice)
        )

        binding.tvPiecePurchasePrice.setText(
            formatPrice(piecePurchasePrice)
        )

        binding.tvTotalPurchasePrice.setText(
            formatPrice(totalPurchasePrice)
        )


        // =========================
        // عرض أسعار البيع
        // =========================

        binding.tvPackSalePrice.setText(
            formatPrice(packSalePrice)
        )

        binding.tvCartonSalePrice.setText(
            formatPrice(cartonSalePrice)
        )

        binding.tvTotalSalePrice.setText(
            formatPrice(totalSalePrice)
        )


        // =========================
        // عرض الأرباح
        // =========================

        binding.tvPieceProfit.setText(
            formatPrice(pieceProfit)
        )

        binding.tvPackProfit.setText(
            formatPrice(packProfit)
        )

        binding.tvCartonProfit.setText(
            formatPrice(cartonProfit)
        )

        binding.tvTotalProfit.setText(
            formatPrice(totalProfit)
        )

    } else {

        // =====================================================
        // الحالة الثانية: المنتج لا يحتوي على باكت
        // =====================================================

        // هنا pieces تعني:
        // عدد الحبات داخل الكرتون مباشرة

        val piecesPerCarton = pieces


        // =========================
        // أسعار الشراء
        // =========================

        val piecePurchasePrice =
            if (piecesPerCarton > 0)
                cartonPurchasePrice / piecesPerCarton
            else
                0.0

        val totalPurchasePrice =
            cartonPurchasePrice * cartons


        // =========================
        // أسعار البيع
        // =========================

        val cartonSalePrice =
            pieceSalePrice * piecesPerCarton

        val totalSalePrice =
            cartonSalePrice * cartons


        // =========================
        // الأرباح
        // =========================

        val pieceProfit =
            pieceSalePrice - piecePurchasePrice

        val cartonProfit =
            cartonSalePrice - cartonPurchasePrice

        val totalProfit =
            totalSalePrice - totalPurchasePrice


        // =========================
        // أسعار الشراء
        // =========================

        // لا يوجد باكت
        binding.tvPackPurchasePrice.setText("")

        binding.tvPiecePurchasePrice.setText(
            formatPrice(piecePurchasePrice)
        )

        binding.tvTotalPurchasePrice.setText(
            formatPrice(totalPurchasePrice)
        )


        // =========================
        // أسعار البيع
        // =========================

        // لا يوجد باكت
        binding.tvPackSalePrice.setText("")

        binding.tvCartonSalePrice.setText(
            formatPrice(cartonSalePrice)
        )

        binding.tvTotalSalePrice.setText(
            formatPrice(totalSalePrice)
        )


        // =========================
        // الأرباح
        // =========================

        binding.tvPieceProfit.setText(
            formatPrice(pieceProfit)
        )

        // لا يوجد باكت
        binding.tvPackProfit.setText("")

        binding.tvCartonProfit.setText(
            formatPrice(cartonProfit)
        )

        binding.tvTotalProfit.setText(
            formatPrice(totalProfit)
        )
    }
   }

        

    private fun formatPrice(price: Double): String {
    return if (price % 1.0 == 0.0) {
        price.toInt().toString()
    } else {
        String.format(
            Locale.US,
            "%.2f",
            price
        )
    }
    }

    
   // =====================================================
// معالجة تاريخ OCR باستخدام java.time
// =====================================================

private val dateFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun normalizeDate(input: String): String? {

    val text = input
        .replace("\n", " ")
        .replace(",", " ")
        .trim()

    if (text.isBlank()) {
        return null
    }

    // ---------------------------------------------
    // DD/MM/YYYY أو DD-MM-YYYY
    // ---------------------------------------------

    Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{4})\b""")
        .find(text)
        ?.let {

            val day = it.groupValues[1].toIntOrNull()
            val month = it.groupValues[2].toIntOrNull()
            val year = it.groupValues[3].toIntOrNull()

            if (day != null && month != null && year != null) {

                return createDate(
                    year,
                    month,
                    day
                )
            }
        }

    // ---------------------------------------------
    // YYYY/MM/DD أو YYYY-MM-DD
    // ---------------------------------------------

    Regex("""\b(\d{4})[/-](\d{1,2})[/-](\d{1,2})\b""")
        .find(text)
        ?.let {

            val year = it.groupValues[1].toIntOrNull()
            val month = it.groupValues[2].toIntOrNull()
            val day = it.groupValues[3].toIntOrNull()

            if (day != null && month != null && year != null) {

                return createDate(
                    year,
                    month,
                    day
                )
            }
        }

    // ---------------------------------------------
    // DD/MM/YY
    // ---------------------------------------------

    Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{2})\b""")
        .find(text)
        ?.let {

            val day = it.groupValues[1].toIntOrNull()
            val month = it.groupValues[2].toIntOrNull()
            val year2 = it.groupValues[3].toIntOrNull()

            if (day != null && month != null && year2 != null) {

                return createDate(
                    2000 + year2,
                    month,
                    day
                )
            }
        }

    // ---------------------------------------------
    // DD MM YYYY
    // ---------------------------------------------

    Regex("""\b(\d{1,2})\s+(\d{1,2})\s+(\d{4})\b""")
        .find(text)
        ?.let {

            val day = it.groupValues[1].toIntOrNull()
            val month = it.groupValues[2].toIntOrNull()
            val year = it.groupValues[3].toIntOrNull()

            if (day != null && month != null && year != null) {

                return createDate(
                    year,
                    month,
                    day
                )
            }
        }

    // ---------------------------------------------
    // YYYYMMDD
    // ---------------------------------------------

    Regex("""\b(\d{8})\b""")
        .find(text)
        ?.let {

            val value = it.groupValues[1]

            val year = value.substring(0, 4).toIntOrNull()
            val month = value.substring(4, 6).toIntOrNull()
            val day = value.substring(6, 8).toIntOrNull()

            if (year != null && month != null && day != null) {

                return createDate(
                    year,
                    month,
                    day
                )
            }
        }

    // ---------------------------------------------
    // DDMMYY
    // ---------------------------------------------

    Regex("""\b(\d{6})\b""")
        .find(text)
        ?.let {

            val value = it.groupValues[1]

            val day = value.substring(0, 2).toIntOrNull()
            val month = value.substring(2, 4).toIntOrNull()
            val year2 = value.substring(4, 6).toIntOrNull()

            if (day != null && month != null && year2 != null) {

                return createDate(
                    2000 + year2,
                    month,
                    day
                )
            }
        }

    // ---------------------------------------------
    // MM/YYYY
    // اليوم غير موجود → اليوم الأول
    // ---------------------------------------------

    Regex("""\b(\d{1,2})[/-](\d{4})\b""")
        .find(text)
        ?.let {

            val month = it.groupValues[1].toIntOrNull()
            val year = it.groupValues[2].toIntOrNull()

            if (month != null && year != null) {

                return createDate(
                    year,
                    month,
                    1
                )
            }
        }

    return null
}


// =====================================================
// إنشاء التاريخ والتحقق الحقيقي منه بواسطة java.time
// =====================================================

private fun createDate(
    year: Int,
    month: Int,
    day: Int
): String? {

    return try {

        val date = LocalDate.of(
            year,
            month,
            day
        )

        date.format(dateFormatter)

    } catch (e: DateTimeParseException) {

        null

    } catch (e: DateTimeException) {

        null
    }
}

   
    // الدالة المضافة حديثاً
    private fun fetchProductFromApi(barcode: String) {

    Toast.makeText(
        this,
        "جاري البحث عن المنتج...",
        Toast.LENGTH_SHORT
    ).show()

    OpenFoodFactsApi.getProduct(barcode) { productResponse ->

        runOnUiThread {

            if (productResponse != null &&
                productResponse.status == 1) {

                val product = productResponse.product

                if (product != null) {

                    binding.editTextProductName.setText(
                        product.productName ?: ""
                    )

                    val imageUrl = product.imageUrl

                    if (!imageUrl.isNullOrBlank()) {
                        processProductImage(imageUrl)
                    }

                    Toast.makeText(
                        this,
                        "تم جلب بيانات: ${product.productName}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } else {

                Toast.makeText(
                    this,
                    "المنتج غير موجود في قاعدة البيانات",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
 } 
}    
