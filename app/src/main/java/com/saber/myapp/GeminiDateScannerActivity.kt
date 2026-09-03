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

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var geminiDateService: GeminiDateService

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
            takePhoto()
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

            val cameraProvider =
                cameraProviderFuture.get()

            val preview =
                Preview.Builder().build()

            preview.setSurfaceProvider(
                previewView.surfaceProvider
            )

            imageCapture =
                ImageCapture.Builder()
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
                    Toast.LENGTH_LONG
                ).show()

                tvResult.text =
                    "❌ خطأ في تشغيل الكاميرا:\n${e.message}"

                finish()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // =====================================================
    // التقاط الصورة
    // =====================================================

    private fun takePhoto() {

        val imageCapture =
            imageCapture ?: return

        val photoFile =
            createImageFile()

        val outputOptions =
            ImageCapture.OutputFileOptions
                .Builder(photoFile)
                .build()

        btnCapture.isEnabled = false

        btnCapture.text =
            "⏳ جاري إرسال الصورة..."

        tvResult.text =
            "📤 جاري إرسال الصورة إلى Gemini..."

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),

            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    output: ImageCapture.OutputFileResults
                ) {

                    val bitmap =
                        BitmapFactory.decodeFile(
                            photoFile.absolutePath
                        )

                    if (bitmap == null) {

                        btnCapture.isEnabled = true

                        btnCapture.text =
                            "📸 التقاط الصورة"

                        tvResult.text =
                            "❌ فشل قراءة الصورة"

                        return
                    }

                    sendImageToGemini(bitmap)
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {

                    btnCapture.isEnabled = true

                    btnCapture.text =
                        "📸 التقاط الصورة"

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
    // إرسال الصورة إلى Gemini
    // =====================================================

    private fun sendImageToGemini(
        bitmap: android.graphics.Bitmap
    ) {

        tvResult.text =
            "🤖 Gemini يقوم بتحليل الصورة..."

        lifecycleScope.launch {

            val result =
                geminiDateService.fetchExpiryDate(
                    bitmap
                )

            // إعادة تفعيل الزر
            btnCapture.isEnabled = true

            btnCapture.text =
                "📸 التقاط الصورة"

            // =================================================
            // نجاح
            // =================================================

            result.onSuccess { expiryDate ->

                tvResult.text =
                    "✅ تاريخ الانتهاء:\n$expiryDate"
            }

            // =================================================
            // خطأ
            // =================================================

            result.onFailure { error ->

                val errorMessage =
                    error.message
                        ?: "خطأ غير معروف"

                tvResult.text =
                    "❌ حدث خطأ:\n\n$errorMessage"

                Toast.makeText(
                    this@GeminiDateScannerActivity,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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
    // إغلاق الكاميرا
    // =====================================================

    override fun onDestroy() {

        super.onDestroy()

        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}
