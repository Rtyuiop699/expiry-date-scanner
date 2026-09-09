package com.saber.myapp

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class BarcodeScannerHelper(
    private val activity: ComponentActivity,
    private val onScanResult: (String) -> Unit,
    private val onScanCancelled: () -> Unit
) {

    private val barcodeLauncher: ActivityResultLauncher<ScanOptions> =
        activity.registerForActivityResult(ScanContract()) { result ->

            if (result.contents == null) {
                onScanCancelled()
            } else {
                onScanResult(result.contents)
            }
        }

    fun startScanner(
        prompt: String = "وجّه الكاميرا نحو الباركود"
    ) {

        val options = ScanOptions().apply {

            // =========================================================
            // أنواع الباركود التجارية الخطية
            // =========================================================
            //
            // EAN-13 : الأكثر استخدامًا في المنتجات الغذائية والمعلبات
            // EAN-8  : المنتجات الصغيرة
            // UPC-A  : شائع في المنتجات التجارية
            // UPC-E  : النسخة المختصرة من UPC
            // CODE-128: باركود خطي عام
            // ITF     : يستخدم في بعض العبوات والمنتجات
            //
            // تم الاستغناء عن ALL_CODE_TYPES لتقليل أنواع الباركود
            // التي يبحث عنها الماسح.
            //
            setDesiredBarcodeFormats(
                ScanOptions.EAN_13,
                ScanOptions.EAN_8,
                ScanOptions.UPC_A,
                ScanOptions.UPC_E,
                ScanOptions.CODE_128,
                ScanOptions.ITF
            )

            // =========================================================
            // إعدادات الكاميرا
            // =========================================================

            // الكاميرا الخلفية
            setCameraId(0)

            // صوت عند نجاح القراءة
            setBeepEnabled(true)

            // لا نحتاج إلى حفظ صورة الباركود
            setBarcodeImageEnabled(false)

            // تثبيت الاتجاه العمودي
            setOrientationLocked(true)

            // شاشة المسح المخصصة للتطبيق
            setCaptureActivity(PortraitScanActivity::class.java)

            // النص الظاهر للمستخدم
            setPrompt(prompt)
        }

        barcodeLauncher.launch(options)
    }
}
