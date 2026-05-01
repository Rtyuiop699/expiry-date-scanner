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

    fun startScanner(prompt: String = "قم بتوجيه الكاميرا نحو الباركود") {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            setPrompt(prompt)
            setCameraId(0) // الكاميرا الخلفية
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(true)
            setCaptureActivity(PortraitScanActivity::class.java)
        }
        
        barcodeLauncher.launch(options)
    }
}
