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

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private lateinit var databaseHelper: DatabaseHelper

    private var currentImagePath: String? = null

    private val REQUEST_PRODUCT_CAMERA = 1001
    private val REQUEST_DATE_SCAN = 1002

    // التصنيفات
    private val categories = mutableListOf(
        "عصائر",
        "مشروبات غازية",
        "خضار معلبة ومخللات",
        "أسماك معلبة",
        "كيك وبسكويت",
        "آيسكريم ومثلجات"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)

        setupToolbar()

        // زر الكاميرا
        binding.btnCaptureImage.setOnClickListener {
            val intent = Intent(this, ProductCameraActivity::class.java)
            startActivityForResult(intent, REQUEST_PRODUCT_CAMERA)
        }

        // زر الماسح
        binding.btnOpenCalendar.setOnClickListener {
            val intent = Intent(this, DateScannerActivity::class.java)
            startActivityForResult(intent, REQUEST_DATE_SCAN)
        }

        // زر يوجد باكت
        binding.btnHasPack.setOnClickListener {
            Toast.makeText(
                this,
                "تم تحديد أن المنتج يحتوي على باكت",
                Toast.LENGTH_SHORT
            ).show()
        }

        // زر الحساب
        binding.btnCalculate.setOnClickListener {
            calculateQuantity()
        }

        // زر الحفظ
        binding.btnSave.setOnClickListener {
            saveProduct()
        }
    }

    private fun setupToolbar() {

    }

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
    }
private fun setupToolbar() {
   binding.topAppBar.menu.clear() 
    // 1. شحن المنيو يدوياً باستخدام اسم الملف
    binding.topAppBar.inflateMenu(R.menu.addproductmenu) 

    // 2. تفعيل زر الرجوع في التولبار
    binding.topAppBar.setNavigationOnClickListener { finish() }

    // 3. ربط الأزرار بالمعرفات (IDs) الصحيحة الموجودة في ملف الـ XML
    binding.topAppBar.setOnMenuItemClickListener { menuItem ->
        when (menuItem.itemId) {
            R.id.btnSaveAction -> { 
                saveProduct()
                true 
            }
            R.id.btnPrint -> { 
                Toast.makeText(this, "جاري الطباعة...", Toast.LENGTH_SHORT).show()
                true 
            }
            R.id.btnPdf -> { 
                Toast.makeText(this, "جاري إنشاء ملف PDF...", Toast.LENGTH_SHORT).show()
                true 
            }
            R.id.btnDelete -> { 
                Toast.makeText(this, "تم حذف المنتج", Toast.LENGTH_SHORT).show()
                // هنا يمكنك إضافة كود الحذف الفعلي إذا أردت
                true 
            }
            else -> false
        }
    }
}
    private fun saveProduct() {

        val name = binding.editTextProductName.text.toString().trim()
        val rawDate = binding.editTextDate.text.toString().trim()
        val normalizedDate = normalizeDate(rawDate)
        val barcode = binding.editTextBarcode.text.toString().trim()

        if (name.isBlank()) {

            Toast.makeText(
                this,
                "يرجى إدخال اسم المنتج",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (rawDate.isBlank()) {

            Toast.makeText(
                this,
                "يرجى إدخال تاريخ الصلاحية",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (normalizedDate == null) {

            Toast.makeText(
                this,
                "صيغة التاريخ غير مفهومة",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (currentImagePath == null) {

            Toast.makeText(
                this,
                "يرجى إضافة صورة",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val carton =
            binding.editCarton.text.toString().toIntOrNull() ?: 0

        val pack =
            binding.editPack.text.toString().toIntOrNull() ?: 0

        val piece =
            binding.editPiece.text.toString().toIntOrNull() ?: 0

        val quantity =
            if (carton > 0 && pack > 0 && piece > 0) {
                carton * pack * piece
            } else {
                1
            }

        val product = Product(
            id = 0,
            barcode = barcode,
            name = name,
            expiryDate = normalizedDate,
            quantity = quantity,
            imagePath = currentImagePath!!
        )

        databaseHelper.addProduct(product)

        Toast.makeText(
            this,
            "تم حفظ المنتج",
            Toast.LENGTH_SHORT
        ).show()

        setResult(RESULT_OK)

        finish()
    }

    private fun calculateQuantity() {

        val carton =
            binding.editCarton.text.toString().toIntOrNull() ?: 0

        val pack =
            binding.editPack.text.toString().toIntOrNull() ?: 0

        val piece =
            binding.editPiece.text.toString().toIntOrNull() ?: 0

        val result = carton * pack * piece

        binding.editResult.setText(result.toString())
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_PRODUCT_CAMERA) {

                val imagePath =
                    data?.getStringExtra(
                        ProductCameraActivity.EXTRA_IMAGE_PATH
                    )

                if (!imagePath.isNullOrEmpty()) {

                    currentImagePath = imagePath

                    val bitmap =
                        BitmapFactory.decodeFile(imagePath)

                    binding.imageViewProduct.setImageBitmap(bitmap)

                    Toast.makeText(
                        this,
                        "تم تحديث الصورة",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            if (requestCode == REQUEST_DATE_SCAN) {

                val date =
                    data?.getStringExtra(
                        DateScannerActivity.EXTRA_DATE
                    )

                if (!date.isNullOrEmpty()) {

                    binding.editTextDate.setText(date)

                    Toast.makeText(
                        this,
                        "تم تحديث التاريخ",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // =========================
    // دوال التاريخ
    // =========================

    private fun normalizeDate(input: String): String? {
        return extractDateFromText(input)
    }

    private fun extractDateFromText(text: String): String? {

        val cleanedText = fixCommonOCRMistakes(
            text.replace("\n", " ")
                .replace(",", " ")
                .trim()
        )

        val patterns = listOf(

            Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{4})\b"""),

            Regex("""\b(\d{4})[/-](\d{1,2})[/-](\d{1,2})\b"""),

            Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{2})\b"""),

            Regex("""\b(\d{1,2})[/-](\d{4})\b"""),

            Regex("""\b(\d{2})\s+(\d{2})\s+(\d{4})\b"""),

            Regex("""\b(\d{8})\b"""),

            Regex("""\b(\d{6})\b"""),

            Regex(
                """(?:DATE:\s*)?([A-Za-z]+)\s+(\d{4})""",
                RegexOption.IGNORE_CASE
            )
        )

        val foundDates = mutableListOf<String>()

        for (pattern in patterns) {

            val matches = pattern.findAll(cleanedText)

            for (match in matches) {

                val g = match.groupValues

                val result = when {

                    g.size == 4 &&
                            g[3].length == 4 -> {

                        "${g[3]}-${
                            g[2].padStart(2, '0')
                        }-${
                            g[1].padStart(2, '0')
                        }"
                    }

                    g.size == 4 &&
                            g[1].length == 4 -> {

                        "${g[1]}-${
                            g[2].padStart(2, '0')
                        }-${
                            g[3].padStart(2, '0')
                        }"
                    }

                    else -> null
                }

                if (
                    result != null &&
                    isValidDateFromString(result)
                ) {
                    foundDates.add(result)
                }
            }
        }

        return foundDates.firstOrNull()
    }

    private fun fixCommonOCRMistakes(text: String): String {

        return text
            .replace("O", "0")
            .replace("I", "1")
            .replace("S", "5")
    }

    private fun isValidDateFromString(date: String): Boolean {

        val parts = date.split("-")

        if (parts.size != 3) return false

        val year =
            parts[0].toIntOrNull() ?: return false

        val month =
            parts[1].toIntOrNull() ?: return false

        val day =
            parts[2].toIntOrNull() ?: return false

        return year in 2000..2100 &&
                month in 1..12 &&
                day in 1..31
    }

    private fun monthNameToNumber(
        month: String
    ): String? {

        return when (
            month.uppercase(Locale.ENGLISH)
        ) {

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
}
