package com.saber.myapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var tvResult: TextView

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var geminiDateService: GeminiDateService

    private var isProcessing = false

    companion object {
        private const val REQUEST_CAMERA = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_gemini_date_scanner
        )

        previewView =
            findViewById(R.id.previewViewGemini)

        btnCapture =
            findViewById(R.id.btnCaptureGemini)

        tvResult =
            findViewById(R.id.tvGeminiResult)

        geminiDateService =
            GeminiDateService()

        cameraExecutor =
            Executors.newSingleThreadExecutor()

        btnCapture.setOnClickListener {

            if (!isProcessing) {
                takePhoto()
            }
        }

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
    // تشغيل الكاميرا
    // =====================================================

    private fun startCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            try {

                val provider =
                    cameraProviderFuture.get()

                cameraProvider = provider

                preview =
                    Preview.Builder().build()

                preview?.setSurfaceProvider(
                    previewView.surfaceProvider
                )

                imageCapture =
                    ImageCapture.Builder()
                        .setCaptureMode(
                            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                        )
                        .build()

                provider.unbindAll()

                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )

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

        isProcessing = true

        btnCapture.isEnabled = false
        btnCapture.text = "⏳ جاري التحليل..."

        tvResult.text =
            "📸 تم التقاط الصورة\n\n🤖 جاري تحليل التاريخ..."

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),

            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    output: ImageCapture.OutputFileResults
                ) {

                    // =================================================
                    // إيقاف الكاميرا فور نجاح الالتقاط
                    // =================================================

                    stopCameraPreview()

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
    // إيقاف الكاميرا أثناء تحليل Gemini
    // =====================================================

    private fun stopCameraPreview() {

        try {

            cameraProvider?.unbindAll()

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    // =====================================================
    // إرسال الصورة إلى Gemini
    // =====================================================

    private fun sendImageToGemini(
        bitmap: android.graphics.Bitmap
    ) {

        lifecycleScope.launch {

            tvResult.text =
                "🤖 Gemini يقوم بتحليل الصورة...\n\nيرجى الانتظار"

            val startTime =
                System.currentTimeMillis()

            val result =
                geminiDateService.fetchExpiryDate(
                    bitmap
                )

            val elapsed =
                System.currentTimeMillis() - startTime

            // =================================================
            // النتيجة
            // =================================================

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
    // إنهاء حالة المعالجة
    // =====================================================

    private fun finishProcessing() {

        isProcessing = false

        btnCapture.isEnabled = true

        btnCapture.text =
            "📸 التقاط الصورة"
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
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onDestroy()

        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}
