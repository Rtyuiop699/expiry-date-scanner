package com.saber.myapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DateScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var btnConfirm: Button
    private lateinit var tvResult: TextView
    private var recognizedDate: String? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private const val REQUEST_CAMERA = 100
        const val EXTRA_DATE = "recognized_date"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_date_scanner)

        previewView = findViewById(R.id.previewView)
        btnCapture = findViewById(R.id.btnTakePicture)
        btnConfirm = findViewById(R.id.btnUseDate)
        tvResult = findViewById(R.id.tvRecognizedText)

        btnCapture.setOnClickListener { takePhoto() }

        btnConfirm.setOnClickListener {
            if (recognizedDate != null) {
                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_DATE, recognizedDate)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "لم يتم التعرف على تاريخ بعد", Toast.LENGTH_SHORT).show()
            }
        }

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(this, "فشل تشغيل الكاميرا: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = createImageFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        btnCapture.isEnabled = false
        btnCapture.text = "⏳ جاري..."

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    btnCapture.isEnabled = true
                    btnCapture.text = "📸 تصوير"

                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    if (bitmap != null) {
                        recognizeDate(bitmap)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    btnCapture.isEnabled = true
                    btnCapture.text = "📸 تصوير"
                }
            })
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File.createTempFile("DATE_$timeStamp", ".jpg", getExternalFilesDir(null))
    }

    private fun cropCenter(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val cropWidth = (width * 0.7).toInt()
        val cropHeight = (height * 0.3).toInt()

        val left = (width - cropWidth) / 2
        val top = (height - cropHeight) / 2

        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }

    private fun preprocessImage(bitmap: Bitmap): Bitmap {

    // تكبير الصورة
    val matrix = Matrix()
    matrix.postScale(2f, 2f)
    val scaled = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    // تحويل رمادي
    val grayBitmap = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(grayBitmap)
    val paint = Paint()

    val colorMatrix = ColorMatrix().apply {
        setSaturation(0f)
    }

    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(scaled, 0f, 0f, paint)

    // 🔥 الجديد: تحويل أبيض وأسود
    return toBlackWhite(grayBitmap)
}
   private fun toBlackWhite(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = bitmap.getPixel(x, y)

            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            val gray = (r + g + b) / 3

            val newColor = if (gray > 140) Color.WHITE else Color.BLACK

            result.setPixel(x, y, newColor)
        }
    }

    return result
}
    private fun recognizeDate(bitmap: Bitmap) {

        val cropped = cropCenter(bitmap)
        val processedBitmap = preprocessImage(cropped)

        val image = InputImage.fromBitmap(processedBitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        tvResult.text = "جاري التعرف..."

        recognizer.process(image)
            .addOnSuccessListener { result ->

                val text = result.text
                val extractedDate = extractDateFromText(text)

                if (extractedDate != null) {
                    recognizedDate = extractedDate
                    tvResult.text = "✅ $extractedDate\n$text"
                    btnConfirm.isEnabled = true
                } else {
                    tvResult.text = "❌ لم يتم التعرف\n$text"
                    btnConfirm.isEnabled = false
                }
            }
    }

    // =========================
    // 🔥 استخراج التواريخ (محسن)
    // =========================
    private fun extractDateFromText(text: String): String? {

        val cleanedText = fixCommonOCRMistakes(
            text.replace("\n", " ").replace(",", " ").trim()
        )

        val patterns = listOf(
    Regex("""[A-Z](\d{2})/(\d{2})/(\d{2})"""),
    Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{4})\b"""),
    Regex("""\b(\d{4})[/-](\d{1,2})[/-](\d{1,2})\b"""),
    Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{2})\b"""),
    Regex("""\b(\d{1,2})[/-](\d{4})\b"""),
    Regex("""[A-Z]{2}\s+(\d{2})\s+(\d{2})\s+(\d{2})"""),
    Regex("""\b(\d{2})\s+(\d{2})\s+(\d{4})\b"""),
    Regex("""\b(\d{2})\s+(\d{2})\s+(\d{2})\b"""),
    Regex("""\b(\d{1,2})\s+(\d{1,2})\s+(\d{2,4})\b"""),
    Regex("""[A-Z]\d{1,2}\s+(\d{1,2})\s+(\d{2,4})"""),
    Regex("""\b(\d{8})\b"""),
    Regex("""(?:DATE:\s*)?([A-Za-z]+)\s+(\d{4})""", RegexOption.IGNORE_CASE),
    Regex("""(?:EXP|BEST BEFORE|صلاحية|ينتهي|valid|expiry)[\s:]*(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})""", RegexOption.IGNORE_CASE),
    Regex("""\b(\d{4})\b"""),
    Regex("""\b(\d{6})\b""") // ✅ هذا مهم
)

        val foundDates = mutableListOf<Pair<String, String>>()

        for (pattern in patterns) {
            val matches = pattern.findAll(cleanedText)

            for (match in matches) {
                val groups = match.groupValues

                val result = when {
                    groups.size == 4 && groups[3].length == 4 && groups[1].length <= 2 -> {
                        "${groups[3]}-${groups[2].padStart(2, '0')}-${groups[1].padStart(2, '0')}"
                    }
                    groups.size == 4 && groups[1].length == 4 -> {
                        "${groups[1]}-${groups[2].padStart(2, '0')}-${groups[3].padStart(2, '0')}"
                    }
                    groups.size == 4 && groups[3].length == 2 -> {
                        "20${groups[3]}-${groups[2].padStart(2, '0')}-${groups[1].padStart(2, '0')}"
                    }
                    groups.size == 3 && groups[2].length == 4 && groups[1].length <= 2 -> {
                        "${groups[2]}-${groups[1].padStart(2, '0')}-01"
                    }
                    groups.size == 4 && groups[1].length <= 2 && groups[2].length <= 2 && groups[3].length in 2..4 -> {
                        val year = if (groups[3].length == 2) "20${groups[3]}" else groups[3]
                        val month = groups[2].padStart(2, '0')
                        val day = groups[1].padStart(2, '0')
                        if (month.toInt() in 1..12 && day.toInt() in 1..31) "$year-$month-$day" else null
                    }
                    groups.size == 2 && groups[1].length == 6 -> {
    val numbers = groups[1]

    val day = numbers.substring(0, 2)
    val month = numbers.substring(2, 4)
    val year = "20" + numbers.substring(4, 6)

    if (month.toInt() in 1..12 && day.toInt() in 1..31) {
        "$year-$month-$day"
    } else null
}
                    groups.size == 2 && groups[1].length == 8 -> {
                        val numbers = groups[1]
                        "${numbers.substring(0, 4)}-${numbers.substring(4, 6)}-${numbers.substring(6, 8)}"
                    }
                    groups.size == 3 && groups[1].matches(Regex("[A-Za-z]+")) -> {
                        val month = monthNameToNumber(groups[1])
                        val year = groups[2]
                        if (month != null) "$year-$month-01" else null
                    }
                    groups.size == 2 && groups[1].length == 4 -> {
                        "${groups[1]}-01-01"
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

    // =========================
    // 🧠 اختيار أفضل تاريخ (مع EXP / MFG)
    // =========================
    private fun chooseBestDate(dates: List<Pair<String, String>>): String? {
        if (dates.isEmpty()) return null

        val today = Calendar.getInstance()

        val parsedDates = dates.mapNotNull {
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

        val expDates = parsedDates.filter { it.second.contains("EXP", true) }
        val targetList = if (expDates.isNotEmpty()) expDates else parsedDates

        val futureDates = targetList.filter { it.first.after(today) }

        return if (futureDates.isNotEmpty()) {
            futureDates.minByOrNull { it.first.timeInMillis }?.let {
                formatCalendar(it.first)
            }
        } else {
            targetList.maxByOrNull { it.first.timeInMillis }?.let {
                formatCalendar(it.first)
            }
        }
    }

    private fun formatCalendar(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    // =========================
    // 🔧 تصحيح أخطاء OCR
    // =========================
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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
