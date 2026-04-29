package com.saber.myapp

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Window
import android.widget.*
import androidx.activity.ComponentActivity
import com.bumptech.glide.Glide
import java.io.File
import java.util.Calendar
import java.util.Locale

class AddProductDialog(
    private val context: Context,
    private val barcodeValue: String,
    private val nameValue: String,
    private val expiryValue: String,
    private val imagePathValue: String?,
    private val callback: (name: String, expiryDate: String, imagePath: String) -> Unit
) : Dialog(context) {

    private lateinit var editName: EditText
    private lateinit var editDate: EditText
    private lateinit var editBarcode: EditText
    private lateinit var imageView: ImageView
    private lateinit var btnCapture: Button
    private lateinit var btnSave: Button

    private var currentImagePath: String? = null

    companion object {
        private const val REQUEST_PRODUCT_CAMERA = 202
        private const val REQUEST_DATE_SCAN = 201
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_add_product)

        editName = findViewById(R.id.editTextProductName)
        editDate = findViewById(R.id.editTextProductDate)
        editBarcode = findViewById(R.id.editTextBarcode)
        imageView = findViewById(R.id.imageViewProduct)
        btnCapture = findViewById(R.id.btnCaptureImage)
        btnSave = findViewById(R.id.btnSaveProduct)
        val btnScanDate = findViewById<ImageButton>(R.id.btnScanDate)

        editBarcode.setText(barcodeValue)
        editName.setText(nameValue)
        editDate.setText(expiryValue)

        if (!imagePathValue.isNullOrEmpty()) {

            if (imagePathValue.startsWith("http")) {
                Glide.with(context)
                    .load(imagePathValue)
                    .placeholder(android.R.drawable.progress_horizontal)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imageView)

                currentImagePath = imagePathValue

            } else {
                val file = File(imagePathValue)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    imageView.setImageBitmap(bitmap)
                    currentImagePath = imagePathValue
                }
            }
        }

        btnCapture.setOnClickListener {
            val intent = Intent(context, ProductCameraActivity::class.java)
            (context as ComponentActivity)
                .startActivityForResult(intent, REQUEST_PRODUCT_CAMERA)
        }

        btnScanDate.setOnClickListener {
            val activity = context as? ComponentActivity
            if (activity != null) {
                val intent = Intent(context, DateScannerActivity::class.java)
                activity.startActivityForResult(intent, REQUEST_DATE_SCAN)
            } else {
                Toast.makeText(context, "خطأ في فتح الماسح", Toast.LENGTH_SHORT).show()
            }
        }

        btnSave.setOnClickListener {

            val name = editName.text.toString().trim()
            val rawDate = editDate.text.toString().trim()

            val normalizedDate = normalizeDate(rawDate)

            if (name.isBlank()) {
                Toast.makeText(context, "يرجى إدخال اسم المنتج", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (rawDate.isBlank()) {
                Toast.makeText(context, "يرجى إدخال تاريخ الصلاحية", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (normalizedDate == null) {
                Toast.makeText(context, "❌ صيغة التاريخ غير مفهومة", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(context, "✅ تم تحويل التاريخ إلى: $normalizedDate", Toast.LENGTH_LONG).show()

            if (currentImagePath == null) {
                Toast.makeText(context, "يرجى إضافة صورة", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            callback(name, normalizedDate, currentImagePath!!)
            dismiss()
        }
    }

    // 🔁 استقبال النتائج
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == android.app.Activity.RESULT_OK) {

            if (requestCode == REQUEST_PRODUCT_CAMERA) {

                val imagePath =
                    data?.getStringExtra(ProductCameraActivity.EXTRA_IMAGE_PATH)

                if (!imagePath.isNullOrEmpty()) {
                    currentImagePath = imagePath
                    val bitmap = BitmapFactory.decodeFile(imagePath)
                    imageView.setImageBitmap(bitmap)

                    Toast.makeText(context, "تم تحديث الصورة", Toast.LENGTH_SHORT).show()
                }
            }

            if (requestCode == REQUEST_DATE_SCAN) {

                val date =
                    data?.getStringExtra(DateScannerActivity.EXTRA_DATE)

                if (!date.isNullOrEmpty()) {
                    editDate.setText(date)
                    Toast.makeText(context, "تم تحديث التاريخ", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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

                    g.size == 4 && g[3].length == 4 -> {
                        "${g[3]}-${g[2].padStart(2, '0')}-${g[1].padStart(2, '0')}"
                    }

                    g.size == 4 && g[1].length == 4 -> {
                        "${g[1]}-${g[2].padStart(2, '0')}-${g[3].padStart(2, '0')}"
                    }

                    g.size == 4 && g[3].length == 2 -> {
                        "20${g[3]}-${g[2].padStart(2, '0')}-${g[1].padStart(2, '0')}"
                    }

                    g.size == 3 && g[2].length == 4 -> {
                        "${g[2]}-${g[1].padStart(2, '0')}-01"
                    }

                    g.size == 2 && g[1].length == 6 -> {
                        val n = g[1]
                        val day = n.substring(0, 2)
                        val month = n.substring(2, 4)
                        val year = "20" + n.substring(4, 6)

                        if (month.toInt() in 1..12 && day.toInt() in 1..31)
                            "$year-$month-$day"
                        else null
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

                    g.size == 2 && g[1].length == 4 -> {
                        "${g[1]}-01-01"
                    }

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
            } catch (e: Exception) {
                null
            }
        }

        val future = parsed.filter { it.first.after(today) }

        return if (future.isNotEmpty()) {
            formatCalendar(future.minByOrNull { it.first.timeInMillis }!!.first)
        } else {
            formatCalendar(parsed.maxByOrNull { it.first.timeInMillis }!!.first)
        }
    }

    private fun formatCalendar(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$y-$m-$d"
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

        val year = parts[0].toIntOrNull() ?: return false
        val month = parts[1].toIntOrNull() ?: return false
        val day = parts[2].toIntOrNull() ?: return false

        return year in 2000..2100 && month in 1..12 && day in 1..31
    }

    private fun monthNameToNumber(month: String): String? {
        return when (month.uppercase(Locale.ROOT)) {
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
