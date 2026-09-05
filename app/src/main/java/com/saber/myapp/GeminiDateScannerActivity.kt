package com.saber.myapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GeminiDateScannerActivity : AppCompatActivity() {

// =====================================================
// عناصر الواجهة
// =====================================================

private lateinit var previewView: PreviewView
private lateinit var capturedImageView: ImageView

private lateinit var btnCapture: ImageButton
private lateinit var btnHelp: ImageButton
private lateinit var btnFlash: ImageButton

private lateinit var tvResult: TextView
private lateinit var geminiProgressOverlay: LinearLayout

// =====================================================
// CameraX
// =====================================================

private var imageCapture: ImageCapture? = null
private var cameraProvider: ProcessCameraProvider? = null
private var preview: Preview? = null
private var camera: Camera? = null

// =====================================================
// Gemini
// =====================================================

private lateinit var geminiDateService: GeminiDateService

// =====================================================
// Executor
// =====================================================

private lateinit var cameraExecutor: ExecutorService

// =====================================================
// حالة المعالجة
// =====================================================

private var isProcessing = false

// =====================================================
// الفلاش
// =====================================================

private var isFlashOn = false

companion object {
    private const val REQUEST_CAMERA = 200
}

// =====================================================
// onCreate
// =====================================================

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(
        R.layout.activity_gemini_date_scanner
    )

    // =================================================
    // ربط عناصر الواجهة
    // =================================================

    previewView =
        findViewById(R.id.previewViewGemini)

    capturedImageView =
        findViewById(R.id.capturedImageView)

    btnCapture =
        findViewById(R.id.btnCaptureGemini)

    btnHelp =
        findViewById(R.id.btnHelp)

    btnFlash =
        findViewById(R.id.btnFlash)

    tvResult =
        findViewById(R.id.tvGeminiResult)

    geminiProgressOverlay =
        findViewById(R.id.geminiProgressOverlay)

    // =================================================
    // Gemini
    // =================================================

    geminiDateService =
        GeminiDateService()

    // =================================================
    // Executor
    // =================================================

    cameraExecutor =
        Executors.newSingleThreadExecutor()

    // =================================================
    // زر التصوير
    // =================================================

    btnCapture.setOnClickListener {

        if (!isProcessing) {
            takePhoto()
        }
    }

    // =================================================
    // زر الفلاش
    // =================================================

    btnFlash.setOnClickListener {

        toggleFlash()
    }

    // =================================================
    // زر المساعدة
    // =================================================

    btnHelp.setOnClickListener {

        Toast.makeText(
            this,
            "التقط صورة واضحة للمنتج ليقوم Gemini بقراءة تاريخ الانتهاء",
            Toast.LENGTH_LONG
        ).show()
    }

    // =================================================
    // فحص صلاحية الكاميرا
    // =================================================

    checkCameraPermission()
}

// =====================================================
// صلاحية الكاميرا
// =====================================================

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

// =====================================================
// نتيجة طلب صلاحية الكاميرا
// =====================================================

override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {

    super.onRequestPermissionsResult(
        requestCode,
        permissions,
        grantResults
    )

    if (requestCode == REQUEST_CAMERA) {

        if (
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {

            startCamera()

        } else {

            Toast.makeText(
                this,
                "يجب السماح باستخدام الكاميرا",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

// =====================================================
// تشغيل الكاميرا
// =====================================================

private fun startCamera() {

    val cameraProviderFuture =
        ProcessCameraProvider.getInstance(this)

    cameraProviderFuture.addListener({

        try {

            val provider =
                cameraProviderFuture.get()

            cameraProvider =
                provider

            // =========================================
            // Preview
            // =========================================

            preview =
                Preview.Builder().build()

            preview?.setSurfaceProvider(
                previewView.surfaceProvider
            )

            // =========================================
            // ImageCapture
            // =========================================

            imageCapture =
                ImageCapture.Builder()
                    .setCaptureMode(
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                    )
                    .build()

            // =========================================
            // ربط الكاميرا
            // =========================================

            provider.unbindAll()

            camera =
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )

            // =========================================
            // تحديث حالة الفلاش
            // =========================================

            isFlashOn = false

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "فشل تشغيل الكاميرا: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

            tvResult.text =
                "❌ خطأ في تشغيل الكاميرا:\n${e.message}"
        }

    }, ContextCompat.getMainExecutor(this))
}

// =====================================================
// تشغيل / إيقاف الفلاش
// =====================================================

private fun toggleFlash() {

    val currentCamera =
        camera ?: run {

            Toast.makeText(
                this,
                "الكاميرا غير جاهزة",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

    if (!currentCamera.cameraInfo.hasFlashUnit()) {

        Toast.makeText(
            this,
            "الفلاش غير متوفر",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    isFlashOn =
        !isFlashOn

    currentCamera.cameraControl.enableTorch(
        isFlashOn
    )
}

// =====================================================
// التقاط الصورة
// =====================================================

private fun takePhoto() {

    if (isProcessing) {
        return
    }

    val capture =
        imageCapture ?: run {

            Toast.makeText(
                this,
                "الكاميرا غير جاهزة",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

    val photoFile =
        createImageFile()

    val outputOptions =
        ImageCapture.OutputFileOptions
            .Builder(photoFile)
            .build()

    // =================================================
    // بدء المعالجة
    // =================================================

    isProcessing = true

    btnCapture.isEnabled = false
    btnFlash.isEnabled = false

    // =================================================
    // التقاط الصورة
    // =================================================

    capture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(this),

        object : ImageCapture.OnImageSavedCallback {

            override fun onImageSaved(
                output: ImageCapture.OutputFileResults
            ) {

                // =====================================
                // قراءة الصورة
                // =====================================

                val bitmap =
                    BitmapFactory.decodeFile(
                        photoFile.absolutePath
                    )

                if (bitmap == null) {

                    finishProcessing()

                    tvResult.text =
                        "❌ تعذر قراءة الصورة"

                    return
                }

                // =====================================
                // عرض الصورة الثابتة
                // =====================================

                showCapturedImage(bitmap)

                // =====================================
                // إيقاف الكاميرا
                // =====================================

                stopCamera()

                // =====================================
                // إظهار رسالة Gemini فوق الصورة
                // =====================================

                showGeminiProgress()

                // =====================================
                // إرسال الصورة للتحليل
                // =====================================

                sendImageToGemini(bitmap)
            }

            override fun onError(
                exception: ImageCaptureException
            ) {

                finishProcessing()

                tvResult.text =
                    "❌ فشل التقاط الصورة:\n${exception.message}"

                Toast.makeText(
                    this@GeminiDateScannerActivity,
                    "حدث خطأ أثناء التقاط الصورة",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )
}

// =====================================================
// عرض الصورة الملتقطة
// =====================================================

private fun showCapturedImage(
    bitmap: Bitmap
) {

    capturedImageView.setImageBitmap(
        bitmap
    )

    capturedImageView.visibility =
        View.VISIBLE
}

// =====================================================
// إيقاف الكاميرا
// =====================================================

private fun stopCamera() {

    try {

        camera?.cameraControl?.enableTorch(false)

        cameraProvider?.unbindAll()

        camera = null
        imageCapture = null

    } catch (e: Exception) {

        e.printStackTrace()
    }
}

// =====================================================
// إظهار رسالة Gemini
// =====================================================

private fun showGeminiProgress() {

    geminiProgressOverlay.visibility =
        View.VISIBLE
}

// =====================================================
// إخفاء رسالة Gemini
// =====================================================

private fun hideGeminiProgress() {

    geminiProgressOverlay.visibility =
        View.GONE
}

// =====================================================
// إرسال الصورة إلى Gemini
// =====================================================

private fun sendImageToGemini(
    bitmap: Bitmap
) {

    lifecycleScope.launch {

        val startTime =
            System.currentTimeMillis()

        val result =
            geminiDateService.fetchExpiryDate(
                bitmap
            )

        val elapsed =
            System.currentTimeMillis() -
                    startTime

        // =============================================
        // إخفاء رسالة الانتظار
        // =============================================

        hideGeminiProgress()

        // =============================================
        // النتيجة
        // =============================================

        result.onSuccess { expiryDate ->

            tvResult.text =
                "✅ تاريخ الانتهاء:\n\n$expiryDate\n\n⚡ ${elapsed}ms"

        }.onFailure { error ->

            val errorMessage =
                error.message
                    ?: "خطأ غير معروف"

            tvResult.text =
                "❌ لم يتم العثور على تاريخ\n\n$errorMessage"

            Toast.makeText(
                this@GeminiDateScannerActivity,
                errorMessage,
                Toast.LENGTH_LONG
            ).show()
        }

        finishProcessing()
    }
}

// =====================================================
// إنهاء المعالجة
// =====================================================

private fun finishProcessing() {

    isProcessing = false

    btnCapture.isEnabled = true
    btnFlash.isEnabled = true

    btnCapture.alpha = 1.0f

    // لا نعيد تشغيل الكاميرا تلقائيًا.
    // الصورة تبقى ثابتة حتى يقرر المستخدم التقاط صورة جديدة.
}

// =====================================================
// إنشاء ملف الصورة
// =====================================================

private fun createImageFile(): File {

    val timeStamp =
        SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

    return File.createTempFile(
        "GEMINI_DATE_$timeStamp",
        ".jpg",
        getExternalFilesDir(null)
    )
}

// =====================================================
// تنظيف الموارد
// =====================================================

override fun onDestroy() {

    try {

        camera?.cameraControl?.enableTorch(false)

        cameraProvider?.unbindAll()

    } catch (e: Exception) {

        e.printStackTrace()
    }

    if (::cameraExecutor.isInitialized) {

        cameraExecutor.shutdown()
    }

    super.onDestroy()
}

}
