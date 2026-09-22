package com.saber.myapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProcessedImagesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: ProcessedImagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_processed_images)

        recyclerView =
            findViewById(R.id.recyclerProcessedImages)

        emptyText =
            findViewById(R.id.tvNoProcessedImages)

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        val images =
            ProcessedImageStore.getAll(this)

        adapter =
            ProcessedImagesAdapter(images) { file ->

                val intent =
                    Intent(
                        this,
                        ProcessedImageViewerActivity::class.java
                    )

                intent.putExtra(
                    ProcessedImageViewerActivity.EXTRA_IMAGE_PATH,
                    file.absolutePath
                )

                startActivity(intent)
            }

        recyclerView.adapter = adapter

        if (images.isEmpty()) {
            recyclerView.visibility =
                RecyclerView.GONE

            emptyText.visibility =
                TextView.VISIBLE

        } else {
            recyclerView.visibility =
                RecyclerView.VISIBLE

            emptyText.visibility =
                TextView.GONE
        }

        val toolbar =
            findViewById<com.google.android.material.appbar.MaterialToolbar>(
                R.id.toolbarProcessedImages
            )

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
}
