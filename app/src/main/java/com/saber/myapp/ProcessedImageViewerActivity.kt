package com.saber.myapp

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class ProcessedImageViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_processed_image_viewer)

        val imageView =
            findViewById<ImageView>(R.id.imgFullProcessed)

        val toolbar =
            findViewById<MaterialToolbar>(R.id.toolbarImageViewer)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        val imagePath =
            intent.getStringExtra(EXTRA_IMAGE_PATH)

        if (imagePath != null) {

            val bitmap =
                BitmapFactory.decodeFile(imagePath)

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            }
        }
    }
}
