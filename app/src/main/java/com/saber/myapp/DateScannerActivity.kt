package com.saber.myapp
import com.saber.myapp.image.ImageProcessor
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
    private lateinit var imageProcessor: ImageProcessor
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

        btnConfirm.isEnabled = false

        btnCapture.setOnClickListener {
            takePhoto()
        }

        btnConfirm.setOnClickListener {
            if (recognizedDate != null) {
                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_DATE, recognizedDate)
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(
                    this,
                    "لم يتم التعرف على تاريخ بعد",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA
            )
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(
                    ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                )
                .build()

            try {

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "فشل تشغيل الكاميرا: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun isInternetAvailable(): Boolean {

        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE)
                    as android.net.ConnectivityManager

        val network =
            connectivityManager.activeNetwork ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
    }

       // =====================================================
// التقاط الصورة
// =====================================================

private fun takePhoto() {

    val imageCapture = imageCapture ?: return

    val photoFile = createImageFile()

    val outputOptions =
        ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .build()

    btnCapture.isEnabled = false
    btnCapture.text = "⏳ جاري..."

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(this),

        object : ImageCapture.OnImageSavedCallback {

            override fun onImageSaved(
                output: ImageCapture.OutputFileResults
            ) {

                btnCapture.isEnabled = true
                btnCapture.text = "📸 تصوير"

                val bitmap =
                    BitmapFactory.decodeFile(
                        photoFile.absolutePath
                    )

                if (bitmap != null) {
                    recognizeDate(bitmap)
                }
            }

            override fun onError(
                exception: ImageCaptureException
            ) {

                btnCapture.isEnabled = true
                btnCapture.text = "📸 تصوير"

                Toast.makeText(
                    this@DateScannerActivity,
                    "فشل التقاط الصورة",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )
}

private fun createImageFile(): File {

    val timeStamp =
        SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

    return File.createTempFile(
        "DATE_$timeStamp",
        ".jpg",
        getExternalFilesDir(null)
    )
} 

    // =====================================================
    // التعرف على التاريخ بواسطة OCR
    // =====================================================
        private fun recognizeDate(bitmap: Bitmap) {

        val cropped =
            cropCenter(bitmap)

        val processedBitmap =
            preprocessImage(cropped)

        val image =
            InputImage.fromBitmap(
                processedBitmap,
                0
            )

        val recognizer =
            TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
            )

        tvResult.text =
            "جاري التعرف..."

        recognizer.process(image)
            .addOnSuccessListener { result ->

                val text = result.text

                val extractedDate =
                    extractDateFromText(text)

                if (extractedDate != null) {

                    recognizedDate =
                        extractedDate

                    tvResult.text =
                        "✅ $extractedDate\n$text"

                    btnConfirm.isEnabled = true

                } else {

                    recognizedDate = null

                    tvResult.text =
                        "❌ لم يتم التعرف\n$text"

                    btnConfirm.isEnabled = false
                }
            }
            .addOnFailureListener {

                recognizedDate = null

                tvResult.text =
                    "❌ حدث خطأ أثناء التعرف"

                btnConfirm.isEnabled = false
            }
    }

    // =====================================================
    // استخراج التاريخ
    // =====================================================

    private fun extractDateFromText(text: String): String? {

        val cleanedText =
            fixCommonOCRMistakes(
                text
                    .replace("\n", " ")
                    .replace(",", " ")
                    .trim()
            )

        val patterns = listOf(

            // 12/09/2026
            Regex(
                """\b(\d{1,2})[/-](\d{1,2})[/-](\d{4})\b"""
            ),

            // 2026/09/12
            Regex(
                """\b(\d{4})[/-](\d{1,2})[/-](\d{1,2})\b"""
            ),

            // 12/09/26
            Regex(
                """\b(\d{1,2})[/-](\d{1,2})[/-](\d{2})\b"""
            ),

            // حرف + 12/09/26
            Regex(
                """[A-Z](\d{2})/(\d{2})/(\d{2})"""
            ),

            // 12 09 2026
            Regex(
                """\b(\d{2})\s+(\d{2})\s+(\d{4})\b"""
            ),

            // 12 09 26
            Regex(
                """\b(\d{2})\s+(\d{2})\s+(\d{2})\b"""
            ),

            // 12 9 2026
            Regex(
                """\b(\d{1,2})\s+(\d{1,2})\s+(\d{2,4})\b"""
            ),

            // A12 9 2026
            Regex(
                """[A-Z]\d{1,2}\s+(\d{1,2})\s+(\d{2,4})"""
            ),

            // 12092026
            Regex(
                """\b(\d{8})\b"""
            ),

            // 120926
            Regex(
                """\b(\d{6})\b"""
            ),

            // 09/2026
            Regex(
                """\b(\d{1,2})[/-](\d{4})\b"""
            ),

            // February 2026
            Regex(
                """(?:DATE:\s*)?([A-Za-z]+)\s+(\d{4})""",
                RegexOption.IGNORE_CASE
            ),

            // EXP 12/09/2026
            Regex(
                """(?:EXP|BEST BEFORE|صلاحية|ينتهي|valid|expiry)[\s:]*(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})""",
                RegexOption.IGNORE_CASE
            ),

            // سنة فقط
            Regex(
                """\b(\d{4})\b"""
            )
        )

        val foundDates =
            mutableListOf<DetectedDate>()

        for (pattern in patterns) {

            val matches =
                pattern.findAll(cleanedText)

            for (match in matches) {

                val groups =
                    match.groupValues

                val result: String?
                val hasDay: Boolean

                when {

                    groups.size == 4 &&
                            groups[3].length == 4 -> {

                        val day =
                            groups[1].padStart(2, '0')

                        val month =
                            groups[2].padStart(2, '0')

                        val year =
                            groups[3]

                        result =
                            "$year-$month-$day"

                        hasDay = true
                    }

                    groups.size == 4 &&
                            groups[1].length == 4 -> {

                        val year =
                            groups[1]

                        val month =
                            groups[2].padStart(2, '0')

                        val day =
                            groups[3].padStart(2, '0')

                        result =
                            "$year-$month-$day"

                        hasDay = true
                    }

                    groups.size == 4 &&
                            groups[3].length == 2 -> {

                        val day =
                            groups[1].padStart(2, '0')

                        val month =
                            groups[2].padStart(2, '0')

                        val year =
                            "20${groups[3]}"

                        result =
                            "$year-$month-$day"

                        hasDay = true
                    }

                    groups.size == 4 &&
                            groups[3].length == 4 &&
                            groups[1].length <= 2 &&
                            groups[2].length <= 2 -> {

                        val day =
                            groups[1].padStart(2, '0')

                        val month =
                            groups[2].padStart(2, '0')

                        val year =
                            groups[3]

                        result =
                            "$year-$month-$day"

                        hasDay = true
                    }

                    groups.size == 4 &&
                            groups[3].length == 2 &&
                            groups[1].length <= 2 &&
                            groups[2].length <= 2 -> {

                        val day =
                            groups[1].padStart(2, '0')

                        val month =
                            groups[2].padStart(2, '0')

                        val year =
                            "20${groups[3]}"

                        result =
                            "$year-$month-$day"

                        hasDay = true
                    }

                    groups.size == 2 &&
                            groups[1].length == 6 -> {

                        val numbers =
                            groups[1]

                        val day =
                            numbers.substring(0, 2)

                        val month =
                            numbers.substring(2, 4)

                        val year =
                            "20" + numbers.substring(4, 6)

                        result =
                            if (
                                month.toIntOrNull() in 1..12 &&
                                day.toIntOrNull() in 1..31
                            ) {
                                "$year-$month-$day"
                            } else {
                                null
                            }

                        hasDay = true
                    }

                    groups.size == 2 &&
                            groups[1].length == 8 -> {

                        val numbers =
                            groups[1]

                        val year =
                            numbers.substring(0, 4)

                        val month =
                            numbers.substring(4, 6)

                        val day =
                            numbers.substring(6, 8)

                        result =
                            "$year-$month-$day"

                        hasDay = true
                    }

                    groups.size == 3 &&
                            groups[2].length == 4 -> {

                        val month =
                            groups[1].padStart(2, '0')

                        val year =
                            groups[2]

                        result =
                            "$year-$month-01"

                        hasDay = false
                    }

                    groups.size == 3 &&
                            groups[1].matches(
                                Regex("[A-Za-z]+")
                            ) -> {

                        val month =
                            monthNameToNumber(
                                groups[1]
                            )

                        val year =
                            groups[2]

                        result =
                            if (month != null) {
                                "$year-$month-01"
                            } else {
                                null
                            }

                        hasDay = false
                    }

                    groups.size == 2 &&
                            groups[1].length == 4 -> {

                        result =
                            "${groups[1]}-01-01"

                        hasDay = false
                    }

                    else -> {

                        result = null
                        hasDay = false
                    }
                }

                if (
                    result != null &&
                    isValidDateFromString(result)
                ) {

                    val matchText =
                        match.value

                    val beforeStart =
                        maxOf(0, match.range.first - 15)

                    val afterEnd =
                        minOf(
                            cleanedText.length,
                            match.range.last + 16
                        )

                    val surroundingText =
                        cleanedText.substring(
                            beforeStart,
                            afterEnd
                        )

                    val isExpiry =
                        surroundingText.contains(
                            "EXP",
                            ignoreCase = true
                        ) ||
                        surroundingText.contains(
                            "BEST BEFORE",
                            ignoreCase = true
                        ) ||
                        surroundingText.contains(
                            "EXPIRY",
                            ignoreCase = true
                        ) ||
                        surroundingText.contains(
                            "ينتهي"
                        ) ||
                        surroundingText.contains(
                            "صلاحية"
                        )

                    foundDates.add(
                        DetectedDate(
                            date = result,
                            hasRealDay = hasDay,
                            isExpiry = isExpiry,
                            position = match.range.first
                        )
                    )
                }
            }
        }

        return chooseBestDate(foundDates)
    }

    // =====================================================
    // نموذج نتيجة OCR
    // =====================================================
        private data class DetectedDate(
        val date: String,
        val hasRealDay: Boolean,
        val isExpiry: Boolean,
        val position: Int
    )

    // =====================================================
    // اختيار أفضل تاريخ
    // =====================================================

    private fun chooseBestDate(
        dates: List<DetectedDate>
    ): String? {

        if (dates.isEmpty()) return null

        val today =
            Calendar.getInstance()

        val expiryFullDates =
            dates.filter {
                it.isExpiry &&
                        it.hasRealDay
            }

        if (expiryFullDates.isNotEmpty()) {

            return chooseClosestFutureOrLatest(
                expiryFullDates,
                today
            )
        }

        val fullDates =
            dates.filter {
                it.hasRealDay
            }

        if (fullDates.isNotEmpty()) {

            return chooseClosestFutureOrLatest(
                fullDates,
                today
            )
        }

        val expiryPartialDates =
            dates.filter {
                it.isExpiry &&
                        !it.hasRealDay
            }

        if (expiryPartialDates.isNotEmpty()) {

            return chooseClosestFutureOrLatest(
                expiryPartialDates,
                today
            )
        }

        return chooseClosestFutureOrLatest(
            dates,
            today
        )
    }

    // =====================================================
    // اختيار التاريخ الأقرب للمستقبل
    // =====================================================

    private fun chooseClosestFutureOrLatest(
        dates: List<DetectedDate>,
        today: Calendar
    ): String? {

        val parsedDates =
            dates.mapNotNull { detected ->

                try {

                    val parts =
                        detected.date.split("-")

                    if (parts.size != 3) {
                        return@mapNotNull null
                    }

                    val cal =
                        Calendar.getInstance().apply {

                            clear()

                            set(
                                Calendar.YEAR,
                                parts[0].toInt()
                            )

                            set(
                                Calendar.MONTH,
                                parts[1].toInt() - 1
                            )

                            set(
                                Calendar.DAY_OF_MONTH,
                                parts[2].toInt()
                            )

                            set(
                                Calendar.HOUR_OF_DAY,
                                23
                            )

                            set(
                                Calendar.MINUTE,
                                59
                            )

                            set(
                                Calendar.SECOND,
                                59
                            )
                        }

                    Pair(
                        cal,
                        detected
                    )

                } catch (e: Exception) {
                    null
                }
            }

        if (parsedDates.isEmpty()) {
            return null
        }

        val futureDates =
            parsedDates.filter {
                !it.first.before(today)
            }

        if (futureDates.isNotEmpty()) {

            return futureDates
                .minByOrNull {
                    it.first.timeInMillis
                }
                ?.second
                ?.date
        }

        return parsedDates
            .maxByOrNull {
                it.first.timeInMillis
            }
            ?.second
            ?.date
    }

    // =====================================================
    // تصحيح أخطاء OCR
    // =====================================================

    private fun fixCommonOCRMistakes(
        text: String
    ): String {

        return text
            .replace("O", "0")
            .replace("I", "1")
            .replace("S", "5")
    }

    // =====================================================
    // التحقق من التاريخ
    // =====================================================
        private fun isValidDateFromString(
        date: String
    ): Boolean {

        val parts =
            date.split("-")

        if (parts.size != 3) {
            return false
        }

        val year =
            parts[0].toIntOrNull()
                ?: return false

        val month =
            parts[1].toIntOrNull()
                ?: return false

        val day =
            parts[2].toIntOrNull()
                ?: return false

        if (
            year !in 2000..2100 ||
            month !in 1..12 ||
            day !in 1..31
        ) {
            return false
        }

        return try {

            val calendar =
                Calendar.getInstance().apply {

                    isLenient = false

                    set(
                        Calendar.YEAR,
                        year
                    )

                    set(
                        Calendar.MONTH,
                        month - 1
                    )

                    set(
                        Calendar.DAY_OF_MONTH,
                        day
                    )

                    set(
                        Calendar.HOUR_OF_DAY,
                        0
                    )

                    set(
                        Calendar.MINUTE,
                        0
                    )

                    set(
                        Calendar.SECOND,
                        0
                    )

                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }

            calendar.time

            true

        } catch (e: Exception) {
            false
        }
    }

    // =====================================================
    // أسماء الأشهر
    // =====================================================

    private fun monthNameToNumber(
        month: String
    ): String? {

        return when (
            month.uppercase(Locale.ROOT)
        ) {

            "JAN",
            "JANUARY" -> "01"

            "FEB",
            "FEBRUARY" -> "02"

            "MAR",
            "MARCH" -> "03"

            "APR",
            "APRIL" -> "04"

            "MAY" -> "05"

            "JUN",
            "JUNE" -> "06"

            "JUL",
            "JULY" -> "07"

            "AUG",
            "AUGUST" -> "08"

            "SEP",
            "SEPT",
            "SEPTEMBER" -> "09"

            "OCT",
            "OCTOBER" -> "10"

            "NOV",
            "NOVEMBER" -> "11"

            "DEC",
            "DECEMBER" -> "12"

            else -> null
        }
    }

    override fun onDestroy() {

        super.onDestroy()

        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}
