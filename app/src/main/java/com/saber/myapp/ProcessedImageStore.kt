package com.saber.myapp

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ProcessedImageStore {

    private const val DIRECTORY_NAME = "processed_images"

    fun save(
        context: Context,
        bitmap: Bitmap,
        type: String
    ): File {

        val directory = File(
            context.filesDir,
            DIRECTORY_NAME
        )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val timestamp =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss_SSS",
                Locale.US
            ).format(Date())

        val file = File(
            directory,
            "${type}_$timestamp.png"
        )

        file.outputStream().use { output ->
            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                output
            )
        }

        return file
    }

    fun getAll(context: Context): List<File> {

        val directory = File(
            context.filesDir,
            DIRECTORY_NAME
        )

        if (!directory.exists()) {
            return emptyList()
        }

        return directory
            .listFiles()
            ?.filter {
                it.isFile &&
                it.extension.equals("png", ignoreCase = true)
            }
            ?.sortedByDescending {
                it.lastModified()
            }
            ?: emptyList()
    }
}
