package com.saber.myapp

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class GeminiDateService {

    companion object {

        private const val SUPABASE_URL =
    "https://gmelnyxzsbwbsmhgrhhu.supabase.co/functions/v1/extract-date"

        private const val SUPABASE_ANON_KEY =
            "sb_publishable_BW2yXX7rchLZncfc3Qbog_3rDz3GE"
    }

    // =====================================================
    // تحويل الصورة إلى Base64
    // =====================================================

    private fun bitmapToBase64(bitmap: Bitmap): String {

        val outputStream =
            ByteArrayOutputStream()

        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            80,
            outputStream
        )

        val byteArray =
            outputStream.toByteArray()

        return Base64.encodeToString(
            byteArray,
            Base64.NO_WRAP
        )
    }

    // =====================================================
    // إرسال الصورة إلى Supabase
    // =====================================================

    suspend fun fetchExpiryDate(
        bitmap: Bitmap
    ): String? = withContext(Dispatchers.IO) {

        var connection: HttpURLConnection? = null

        try {

            val base64Image =
                bitmapToBase64(bitmap)

            val url =
                URL(SUPABASE_URL)

            connection =
                url.openConnection()
                        as HttpURLConnection

            connection.requestMethod = "POST"

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            connection.setRequestProperty(
                "apikey",
                SUPABASE_ANON_KEY
            )
            connection.setRequestProperty(
    "Authorization",
    "Bearer $SUPABASE_ANON_KEY"
)
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            connection.doOutput = true

            val jsonInput =
                JSONObject().apply {

                    put(
                        "imageBase64",
                        base64Image
                    )
                }

            connection.outputStream.use { outputStream ->

                val input =
                    jsonInput
                        .toString()
                        .toByteArray(Charsets.UTF_8)

                outputStream.write(input)
            }

            val responseCode =
                connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {

                val responseText =
                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                val jsonResponse =
                    JSONObject(responseText)

                val expiryDate =
                    jsonResponse.optString(
                        "expiryDate"
                    )

                if (
                    expiryDate.isNotBlank() &&
                    expiryDate != "NOT_FOUND"
                ) {
                    expiryDate
                } else {
                    null
                }

            } else {

                null
            }

        } catch (e: Exception) {

            e.printStackTrace()

            null

        } finally {

            connection?.disconnect()
        }
    }
}
