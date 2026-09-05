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

        // =====================================================
        // Supabase Edge Function
        // =====================================================

        private const val SUPABASE_URL =
            "https://gmelnyxzsbwbsmhgrhhu.supabase.co/functions/v1/extract-date"

        // =====================================================
        // ضع هنا مفتاح Supabase الموجود لديك
        // =====================================================

        private const val SUPABASE_ANON_KEY =
            "ضع_مفتاح_Supabase_الحالي_هنا"
    }

    // =====================================================
    // تصغير وضغط الصورة
    // =====================================================

    private fun bitmapToBase64(
        originalBitmap: Bitmap
    ): String {

        val maxSize = 1024

        val width =
            originalBitmap.width

        val height =
            originalBitmap.height

        val scale =
            if (width >= height) {

                maxSize.toFloat() / width

            } else {

                maxSize.toFloat() / height
            }

        val resizedBitmap: Bitmap

        if (scale < 1f) {

            val newWidth =
                (width * scale)
                    .toInt()

            val newHeight =
                (height * scale)
                    .toInt()

            resizedBitmap =
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    newWidth,
                    newHeight,
                    true
                )

        } else {

            resizedBitmap =
                originalBitmap
        }

        val outputStream =
            ByteArrayOutputStream()

        resizedBitmap.compress(
            Bitmap.CompressFormat.JPEG,
            75,
            outputStream
        )

        if (resizedBitmap !== originalBitmap) {
            resizedBitmap.recycle()
        }

        val byteArray =
            outputStream.toByteArray()

        return Base64.encodeToString(
            byteArray,
            Base64.NO_WRAP
        )
    }

    // =====================================================
    // إرسال الصورة
    // =====================================================

    suspend fun fetchExpiryDate(
        bitmap: Bitmap
    ): Result<String> = withContext(Dispatchers.IO) {

        var connection: HttpURLConnection? = null

        try {

            val startTime =
                System.currentTimeMillis()

            // =================================================
            // ضغط الصورة
            // =================================================

            val base64Image =
                bitmapToBase64(bitmap)

            val compressionTime =
                System.currentTimeMillis() - startTime

            android.util.Log.d(
                "GeminiDateService",
                "Image preparation time: ${compressionTime}ms"
            )

            android.util.Log.d(
                "GeminiDateService",
                "Base64 size: ${base64Image.length}"
            )

            // =================================================
            // إنشاء الاتصال
            // =================================================

            val url =
                URL(SUPABASE_URL)

            connection =
                url.openConnection()
                    as HttpURLConnection

            connection.requestMethod =
                "POST"

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            connection.setRequestProperty(
                "apikey",
                SUPABASE_ANON_KEY
            )

            connection.connectTimeout =
                15000

            connection.readTimeout =
                30000

            connection.doOutput =
                true

            // =================================================
            // JSON
            // =================================================

            val jsonInput =
                JSONObject().apply {

                    put(
                        "imageBase64",
                        base64Image
                    )
                }

            val requestBytes =
                jsonInput
                    .toString()
                    .toByteArray(
                        Charsets.UTF_8
                    )

            android.util.Log.d(
                "GeminiDateService",
                "Request size: ${requestBytes.size} bytes"
            )

            // =================================================
            // إرسال
            // =================================================

            val networkStart =
                System.currentTimeMillis()

            connection.outputStream.use { outputStream ->

                outputStream.write(
                    requestBytes
                )

                outputStream.flush()
            }

            val responseCode =
                connection.responseCode

            val networkTime =
                System.currentTimeMillis() -
                        networkStart

            android.util.Log.d(
                "GeminiDateService",
                "Network/Gemini time: ${networkTime}ms"
            )

            // =================================================
            // نجاح
            // =================================================

            if (
                responseCode ==
                HttpURLConnection.HTTP_OK
            ) {

                val responseText =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                android.util.Log.d(
                    "GeminiDateService",
                    "Response received"
                )

                val jsonResponse =
                    JSONObject(responseText)

                val expiryDate =
                    jsonResponse.optString(
                        "expiryDate"
                    ).trim()

                if (
                    expiryDate.isNotBlank() &&
                    expiryDate != "NOT_FOUND"
                ) {

                    Result.success(
                        expiryDate
                    )

                } else {

                    Result.failure(
                        Exception(
                            "Gemini لم يعثر على تاريخ انتهاء واضح."
                        )
                    )
                }

            } else {

                val errorText =
                    try {

                        connection.errorStream
                            ?.bufferedReader()
                            ?.use {
                                it.readText()
                            }
                            ?: "لا توجد تفاصيل إضافية"

                    } catch (e: Exception) {

                        "تعذر قراءة تفاصيل الخطأ"
                    }

                Result.failure(
                    Exception(
                        "HTTP $responseCode\n$errorText"
                    )
                )
            }

        } catch (e: Exception) {

            e.printStackTrace()

            Result.failure(e)

        } finally {

            connection?.disconnect()
        }
    }
}
