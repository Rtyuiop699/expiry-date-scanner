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
import android.widget.Spinner
import android.widget.Button
import android.widget.ArrayAdapter
import android.widget.EditText
import android.app.AlertDialog
import android.view.View
import android.widget.AutoCompleteTextView

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private lateinit var databaseHelper: DatabaseHelper

    private var currentImagePath: String? = null
private val REQUEST_PRODUCT_CAMERA = 1001
private val REQUEST_DATE_SCAN = 1002
    // القائمة الأساسية للتصنيفات
    private val categories = mutableListOf(
        "عصائر",
        "مشروبات غازية",
        "خضار معلبة ومخللات",
        "أسماك معلبة",
        "كيك وبسكويت",
        "آيسكريم ومثلجات"
    )

  // 1. بداية الكلاس
//class YourActivity : AppCompatActivity() {

    // 2. دالة onCreate هي المكان الذي نضع فيه الـ Listeners (أزرار الضغط)
    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityAddProductBinding.inflate(layoutInflater)
    setContentView(binding.root)

    databaseHelper = DatabaseHelper(this)

        // زر الكاميرا
        binding.btnCaptureImage.setOnClickListener {
            val intent = Intent(this, ProductCameraActivity::class.java)
            startActivityForResult(intent, REQUEST_PRODUCT_CAMERA)
        }

        // زر الماسح للتاريخ
        binding.btnOpenCalendar.setOnClickListener {
            val intent = Intent(this, DateScannerActivity::class.java)
            startActivityForResult(intent, REQUEST_DATE_SCAN)
        }

        // زر يوجد باكت
        binding.btnHasPack.setOnClickListener {
            Toast.makeText(this, "تم تحديد أن المنتج يحتوي على باكت", Toast.LENGTH_SHORT).show()
        }

        // زر احسب
        binding.btnCalculate.setOnClickListener {
            calculateQuantity()
        }

        setupToolbar()
        
    } // <-- هذا القوس يغلق دالة onCreate فقط

    // 3. هذه الدالة يجب أن تكون خارج onCreate ولكن داخل الكلاس
    private fun processProductImage(imagePathValue: String?) {
        if (!imagePathValue.isNullOrEmpty()) {
            currentImagePath = imagePathValue
            if (imagePathValue.startsWith("http")) {
                Glide.with(this)
                    .load(imagePathValue)
                    .placeholder(android.R.drawable.progress_horizontal)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(binding.imageViewProduct)
            } else {
                val file = File(imagePathValue)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    binding.imageViewProduct.setImageBitmap(bitmap)
                }
            }
        }
    } // <-- هذا القوس يغلق دالة processProductImage

} 

private fun saveProduct() {
    val name = binding.editTextProductName.text.toString().trim()
    val rawDate = binding.editTextDate.text.toString().trim()
    val normalizedDate = normalizeDate(rawDate)
    val barcode = binding.editTextBarcode.text.toString().trim()

    if (name.isBlank()) {
        Toast.makeText(this, "يرجى إدخال اسم المنتج", Toast.LENGTH_SHORT).show()
        return
    }
    if (rawDate.isBlank()) {
        Toast.makeText(this, "يرجى إدخال تاريخ الصلاحية", Toast.LENGTH_SHORT).show()
        return
    }
    if (normalizedDate == null) {
        Toast.makeText(this, "❌ صيغة التاريخ غير مفهومة", Toast.LENGTH_SHORT).show()
        return
    }
    if (currentImagePath == null) {
        Toast.makeText(this, "يرجى إضافة صورة", Toast.LENGTH_SHORT).show()
        return
    }

    // حساب الكمية من الحقول الجديدة
    val carton = binding.editCarton.text.toString().toIntOrNull() ?: 0
    val pack = binding.editPack.text.toString().toIntOrNull() ?: 0
    val piece = binding.editPiece.text.toString().toIntOrNull() ?: 0
    val quantity = if (carton > 0 && pack > 0 && piece > 0) carton * pack * piece else 1

    Toast.makeText(this, "✅ تم تحويل التاريخ إلى: $normalizedDate", Toast.LENGTH_LONG).show()

    // إنشاء المنتج وتمرير كل الباراميترات المطلوبة
    val product = Product(
        id = 0, // افتراضي
        barcode = barcode,
        name = name,
        expiryDate = normalizedDate,
        quantity = quantity,
        imagePath = currentImagePath!!
    )

    databaseHelper.addProduct(product)

    setResult(RESULT_OK)
    finish()
}

    private fun calculateQuantity() {
        val carton = binding.editCarton.text.toString().toIntOrNull() ?: 0
        val pack = binding.editPack.text.toString().toIntOrNull() ?: 0
        val piece = binding.editPiece.text.toString().toIntOrNull() ?: 0

        val result = carton * pack * piece
        binding.editResult.setText(result.toString())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PRODUCT_CAMERA) {
                val imagePath = data?.getStringExtra(ProductCameraActivity.EXTRA_IMAGE_PATH)
                if (!imagePath.isNullOrEmpty()) {
                    currentImagePath = imagePath
                    val bitmap = BitmapFactory.decodeFile(imagePath)
                    binding.imageViewProduct.setImageBitmap(bitmap)
                    Toast.makeText(this, "تم تحديث الصورة", Toast.LENGTH_SHORT).show()
                }
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

    // 🔁 دوال التاريخ (من الديالوج)
   // private fun normalizeDate(input: String): String? {
    //    return extractDateFromText(input)
  //  }

  //  private fun extractDateFromText(text: String): String? {
     //   val cleanedText = fixCommonOCRMistakes(
     //       text.replace("\n", " ").replace(",", " ").trim()
    //    )
        // انسخ الـ Regex والدوال المساعدة من AddProductDialog هنا
      //  return null
  //  }

 //   private fun fixCommonOCRMistakes(text: String): String {
   //     return text.replace("O", "0").replace("I", "1").replace("S", "5")
  //  }
    // 🔁 دوال التاريخ (تم دمجها وتجهيزها بالكامل)

    private fun normalizeDate(input: String): String? {
        return extractDateFromText(input)
    }

    private fun extractDateFromText(text: String): String? {
        val cleanedText = fixCommonOCRMistakes(
            text.replace("\n", " ").replace(",", " ").trim()
        )

        val patterns = listOf(
            Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{4})\b"""),
            Regex("""\b(\d{4})[/-](\d{1,2})[/-](\d{1,2})\b"""),
            Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{2})\b"""),
            Regex("""\b(\d{1,2})[/-](\d{4})\b"""),
            Regex("""\b(\d{2})\s+(\d{2})\s+(\d{4})\b"""),
            Regex("""\b(\d{8})\b"""),
            Regex("""\b(\d{6})\b"""),
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
                    g.size == 2 && g[1].length == 6 -> {
                        val n = g[1]
                        val day = n.substring(0, 2); val month = n.substring(2, 4); val year = "20" + n.substring(4, 6)
                        if (month.toInt() in 1..12 && day.toInt() in 1..31) "$year-$month-$day" else null
                    }
                    g.size == 2 && g[1].length == 8 -> {
                        val n = g[1]
                        "${n.substring(0, 4)}-${n.substring(4, 6)}-${n.substring(6, 8)}"
                    }
                    g.size == 3 && g[1].matches(Regex("[A-Za-z]+")) -> {
                        val month = monthNameToNumber(g[1])
                        val year = g[2]
                        if (month != null) "$year-$month-01" else null
                    }
                    g.size == 2 && g[1].length == 4 -> "${g[1]}-01-01"
                    else -> null
                }
                if (result != null && isValidDateFromString(result)) {
                    foundDates.add(Pair(result, cleanedText))
                }
            }
        }
        return chooseBestDate(foundDates)
    }

    private fun chooseBestDate(dates: List<Pair<String, String>>): String? {
        if (dates.isEmpty()) return null
        val today = Calendar.getInstance()
        val parsed = dates.mapNotNull {
            try {
                val parts = it.first.split("-")
                val cal = Calendar.getInstance().apply {
                    set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                }
                Pair(cal, it.second)
            } catch (e: Exception) { null }
        }
        val future = parsed.filter { it.first.after(today) }
        return if (future.isNotEmpty()) {
            formatCalendar(future.minByOrNull { it.first.timeInMillis }!!.first)
        } else {
            formatCalendar(parsed.maxByOrNull { it.first.timeInMillis }!!.first)
        }
    }

    private fun formatCalendar(cal: Calendar): String {
        return String.format(Locale.ENGLISH, "%04d-%02d-%02d", 
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    private fun fixCommonOCRMistakes(text: String): String {
        return text.replace("O", "0").replace("I", "1").replace("S", "5")
    }

    private fun isValidDateFromString(date: String): Boolean {
        val parts = date.split("-")
        if (parts.size != 3) return false
        val year = parts[0].toIntOrNull() ?: return false
        val month = parts[1].toIntOrNull() ?: return false
        val day = parts[2].toIntOrNull() ?: return false
        return year in 2000..2100 && month in 1..12 && day in 1..31
    }

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
    
  
   // باقي الدوال: chooseBestDate, formatCalendar, isValidDateFromString, monthNameToNumber



