package com.saber.myapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.saber.myapp.databinding.ActivityAddProductBinding
import java.io.File
import java.util.Calendar
import java.util.Locale
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import android.view.View

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
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
        loadCategories()
        // 1. --- إعداد الـ AutoCompleteTextView للتصنيفات ---
        categoriesAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.autoCompleteCategories.setAdapter(categoriesAdapter)

        // 2. --- Listeners ---
        binding.btnAddCategory.setOnClickListener {

    val editText = EditText(this)

    AlertDialog.Builder(this)
        .setTitle("إضافة تصنيف جديد")
        .setView(editText)
        .setPositiveButton("إضافة") { _, _ ->

            val newCategory = editText.text.toString().trim()

            if (newCategory.isEmpty()) {
                Toast.makeText(
                    this,
                    "يرجى كتابة اسم التصنيف",
                    Toast.LENGTH_SHORT
                ).show()
                return@setPositiveButton
            }

            val added = databaseHelper.addCategory(newCategory)

            if (added) {

                loadCategories()

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

        binding.btnCaptureImage.setOnClickListener {
            val intent = Intent(this, ProductCameraActivity::class.java)
            startActivityForResult(intent, REQUEST_PRODUCT_CAMERA)
        }

        binding.btnChooseImage.setOnClickListener {
            Toast.makeText(this, "ميزة اختيار من المعرض قريباً", Toast.LENGTH_SHORT).show()
        }

        binding.btnOpenCalendar.setOnClickListener {
            val intent = Intent(this, DateScannerActivity::class.java)
            startActivityForResult(intent, REQUEST_DATE_SCAN)
        }

        binding.btnHasPack.setOnClickListener {
            Toast.makeText(this, "تم تحديد أن المنتج يحتوي على باكت", Toast.LENGTH_SHORT).show()
        }

        binding.btnCalculate.setOnClickListener { calculateQuantity() }

        // 3. --- استقبال البيانات من Intent ---
        loadIntentData()
        setupToolbar()
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
        if (!imagePathValue.isNullOrEmpty()) { 
            currentImagePath = imagePathValue 
            if (imagePathValue.startsWith("http")) { 
                Glide.with(this).load(imagePathValue).into(binding.imageViewProduct) 
            } else { 
                val file = File(imagePathValue) 
                if (file.exists()) { 
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath) 
                    binding.imageViewProduct.setImageBitmap(bitmap) 
                } 
            } 
        } 
    }

    private fun saveProduct() {
        val name = binding.editTextProductName.text.toString().trim()
        val rawDate = binding.editTextDate.text.toString().trim()
        val normalizedDate = normalizeDate(rawDate)
        val barcode = binding.editTextBarcode.text.toString().trim()
        val category = binding.autoCompleteCategories.text.toString()

        if (name.isBlank() || rawDate.isBlank() || normalizedDate == null || currentImagePath == null) {
            Toast.makeText(this, "يرجى ملء جميع الحقول وإضافة صورة", Toast.LENGTH_SHORT).show()
            return
        }

        val carton = binding.editCarton.text.toString().toIntOrNull() ?: 0
        val pack = binding.editPack.text.toString().toIntOrNull() ?: 0
        val piece = binding.editPiece.text.toString().toIntOrNull() ?: 0
        val quantity = if (carton > 0 && pack > 0 && piece > 0) carton * pack * piece else 1

        val product = Product(
    0,
    barcode,
    name,
    normalizedDate,
    quantity,
    currentImagePath!!,
    category
)
        databaseHelper.addProduct(product)
        Toast.makeText(this, "تم الحفظ: $name - $category", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.btnSaveAction -> { saveProduct(); true }
                R.id.btnPrint -> { Toast.makeText(this, "جاري الطباعة...", Toast.LENGTH_SHORT).show(); true }
                R.id.btnPdf -> { Toast.makeText(this, "جاري إنشاء ملف PDF...", Toast.LENGTH_SHORT).show(); true }
                R.id.btnDelete -> { Toast.makeText(this, "تم حذف المنتج", Toast.LENGTH_SHORT).show(); true }
                else -> false
            }
        }
    }

    private fun calculateQuantity() {
        val carton = binding.editCarton.text.toString().toIntOrNull() ?: 0
        val pack = binding.editPack.text.toString().toIntOrNull() ?: 0
        val piece = binding.editPiece.text.toString().toIntOrNull() ?: 0
        binding.editResult.setText((carton * pack * piece).toString())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PRODUCT_CAMERA) {
                val imagePath = data?.getStringExtra(ProductCameraActivity.EXTRA_IMAGE_PATH)
                processProductImage(imagePath)
                Toast.makeText(this, "تم تحديث الصورة", Toast.LENGTH_SHORT).show()
            }
            if (requestCode == REQUEST_DATE_SCAN) {
                val date = data?.getStringExtra(DateScannerActivity.EXTRA_DATE)
                if (!date.isNullOrEmpty()) {
                    binding.editTextDate.setText(date)
                    Toast.makeText(this, "تم تحديث التاريخ", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // === دوال التاريخ OCR ===
    private fun normalizeDate(input: String): String? { return extractDateFromText(input) }
    
    private fun extractDateFromText(text: String): String? {
        val cleanedText = fixCommonOCRMistakes(text.replace("\n", " ").replace(",", " ").trim())
        val patterns = listOf(
            Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{4})\b"""),
            Regex("""\b(\d{4})[/-](\d{1,2})[/-](\d{1,2})\b"""),
            Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{2})\b"""),
            Regex("""\b(\d{1,2})[/-](\d{4})\b"""),
            Regex("""\b(\d{2})\s+(\d{2})\s+(\d{4})\b"""),
            Regex("""\b(\d{8})\b"""), Regex("""\b(\d{6})\b"""),
            Regex("""(?:DATE:\s*)?([A-Za-z]+)\s+(\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""(?:EXP|BEST BEFORE|صلاحية|ينتهي)[\s:]*(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})""", RegexOption.IGNORE_CASE),
            Regex("""\b(\d{4})\b""")
        )
        val foundDates = mutableListOf<Pair<String, String>>()
        for (pattern in patterns) {
            val matches = pattern.findAll(cleanedText)
            for (match in matches) {
                val g = match.groupValues
                val result = when {
                    g.size == 4 && g[3].length == 4 -> "${g[3]}-${g[2].padStart(2, '0')}-${g[1].padStart(2, '0')}"
                    g.size == 4 && g[1].length == 4 -> "${g[1]}-${g[2].padStart(2, '0')}-${g[3].padStart(2, '0')}"
                    g.size == 4 && g[3].length == 2 -> "20${g[3]}-${g[2].padStart(2, '0')}-${g[1].padStart(2, '0')}"
                    g.size == 3 && g[2].length == 4 -> "${g[2]}-${g[1].padStart(2, '0')}-01"
                    g.size == 2 && g[1].length == 6 -> { val n = g[1]; val day = n.substring(0, 2); val month = n.substring(2, 4); val year = "20" + n.substring(4, 6); if (month.toInt() in 1..12 && day.toInt() in 1..31) "$year-$month-$day" else null }
                    g.size == 2 && g[1].length == 8 -> { val n = g[1]; "${n.substring(0, 4)}-${n.substring(4, 6)}-${n.substring(6, 8)}" }
                    g.size == 3 && g[1].matches(Regex("[A-Za-z]+")) -> { val month = monthNameToNumber(g[1]); val year = g[2]; if (month != null) "$year-$month-01" else null }
                    g.size == 2 && g[1].length == 4 -> "${g[1]}-01-01"
                    else -> null
                }
                if (result != null && isValidDateFromString(result)) { foundDates.add(Pair(result, cleanedText)) }
            }
        }
        return chooseBestDate(foundDates)
    }

    private fun chooseBestDate(dates: List<Pair<String, String>>): String? {
        if (dates.isEmpty()) return null
        val today = Calendar.getInstance()
        val parsed = dates.mapNotNull { try { val parts = it.first.split("-"); val cal = Calendar.getInstance().apply { set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt()) }; Pair(cal, it.second) } catch (e: Exception) { null } }
        val future = parsed.filter { it.first.after(today) }
        return if (future.isNotEmpty()) { formatCalendar(future.minByOrNull { it.first.timeInMillis }!!.first) } else { formatCalendar(parsed.maxByOrNull { it.first.timeInMillis }!!.first) }
    }

    private fun formatCalendar(cal: Calendar): String { return String.format(Locale.ENGLISH, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)) }
    private fun fixCommonOCRMistakes(text: String): String { return text.replace("O", "0").replace("I", "1").replace("S", "5") }
    private fun isValidDateFromString(date: String): Boolean { val parts = date.split("-"); if (parts.size != 3) return false; val year = parts[0].toIntOrNull() ?: return false; val month = parts[1].toIntOrNull() ?: return false; val day = parts[2].toIntOrNull() ?: return false; return year in 2000..2100 && month in 1..12 && day in 1..31 }

    private fun monthNameToNumber(month: String): String? { 
        return when (month.uppercase(Locale.ENGLISH)) { 
            "JAN", "JANUARY" -> "01" 
            "FEB", "FEBRUARY" -> "02" 
            "MAR", "MARCH" -> "03" 
            "APR", "APRIL" -> "04" 
            "MAY" -> "05" 
            "JUN", "JUNE" -> "06" 
            "JUL", "JULY" -> "07" 
            "AUG", "AUGUST" -> "08" 
            "SEP", "SEPT", "SEPTEMBER" -> "09" 
            "OCT", "OCTOBER" -> "10" 
            "NOV", "NOVEMBER" -> "11" 
            "DEC", "DECEMBER" -> "12" 
            else -> null 
        } 
    }

    // الدالة المضافة حديثاً
    private fun fetchProductFromApi(barcode: String) {
    Toast.makeText(this, "جاري البحث عن المنتج...", Toast.LENGTH_SHORT).show()
    
    OpenFoodFactsApi.getProduct(barcode) { productResponse ->
        runOnUiThread {
            if (productResponse != null && productResponse.status == 1) {
                val product = productResponse.product
                if (product != null) {
                    // استخدم productName و imageUrl الجداد
                    binding.editTextProductName.setText(product.productName ?: "")
                    product.imageUrl?.let { 
                        processProductImage(it) 
                    }
                    Toast.makeText(this, "تم جلب بيانات: ${product.productName}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "المنتج غير موجود في قاعدة البيانات", Toast.LENGTH_SHORT).show()
             }
         }
      }
   } 
}
