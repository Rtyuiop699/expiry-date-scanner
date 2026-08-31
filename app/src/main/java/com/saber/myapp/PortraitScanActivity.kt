package com.saber.myapp

import android.content.pm.ActivityInfo
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView


class PortraitScanActivity : CaptureActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var flashButton: FloatingActionButton

    private var isFlashOn = false


    override fun onCreate(savedInstanceState: Bundle?) {

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        super.onCreate(savedInstanceState)
    }


    override fun initializeContent(): DecoratedBarcodeView {

        // تحميل واجهة الماسح الخاصة بنا
        setContentView(R.layout.activity_portrait_scan)

        // الحصول على شاشة الماسح
        barcodeView =
            findViewById(R.id.barcodeScannerView)

        // الحصول على زر الفلاش
        flashButton =
            findViewById(R.id.btnFlash)

        // الحالة الابتدائية
        flashButton.setImageResource(
            R.drawable.ic_flash_off
        )

        flashButton.contentDescription =
            "تشغيل الفلاش"

        // عند الضغط على زر الفلاش
        flashButton.setOnClickListener {

            toggleFlash()
        }

        return barcodeView
    }


    private fun toggleFlash() {

        if (isFlashOn) {

            // إطفاء الفلاش
            barcodeView.setTorchOff()

            isFlashOn = false

            flashButton.setImageResource(
                R.drawable.ic_flash_off
            )

            flashButton.contentDescription =
                "تشغيل الفلاش"

        } else {

            // تشغيل الفلاش
            barcodeView.setTorchOn()

            isFlashOn = true

            flashButton.setImageResource(
                R.drawable.ic_flash_on
            )

            flashButton.contentDescription =
                "إيقاف الفلاش"
        }
    }


    override fun onPause() {

        // إطفاء الفلاش عند مغادرة الشاشة
        if (::barcodeView.isInitialized) {
            barcodeView.setTorchOff()
        }

        isFlashOn = false

        super.onPause()
    }


    override fun onDestroy() {

        // التأكد من إطفاء الفلاش
        if (::barcodeView.isInitialized) {
            barcodeView.setTorchOff()
        }

        super.onDestroy()
    }


    override fun onResume() {

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        super.onResume()
    }
}
