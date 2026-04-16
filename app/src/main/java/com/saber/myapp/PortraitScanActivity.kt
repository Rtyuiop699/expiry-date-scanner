package com.saber.myapp

import android.content.pm.ActivityInfo
import com.journeyapps.barcodescanner.CaptureActivity

class PortraitScanActivity : CaptureActivity() {
    override fun onResume() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onResume()
    }
}
