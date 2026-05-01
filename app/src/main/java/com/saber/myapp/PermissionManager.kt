package com.saber.myapp

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionManager(
    private val activity: ComponentActivity,
    private val onPermissionGranted: () -> Unit,
    private val onPermissionDenied: () -> Unit
) {

    private val requestPermissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }

    fun checkAndRequestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                onPermissionGranted()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}
