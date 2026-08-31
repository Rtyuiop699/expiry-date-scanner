package com.saber.myapp.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint

class ImageProcessor {

    // =====================================================
    // قص المنطقة المركزية من الصورة
    // =====================================================

    fun cropCenter(bitmap: Bitmap): Bitmap {

        val width = bitmap.width
        val height = bitmap.height

        val cropWidth =
            (width * 0.7).toInt()

        val cropHeight =
            (height * 0.3).toInt()

        val left =
            (width - cropWidth) / 2

        val top =
            (height - cropHeight) / 2

        return Bitmap.createBitmap(
            bitmap,
            left,
            top,
            cropWidth,
            cropHeight
        )
    }

    // =====================================================
    // معالجة الصورة قبل OCR
    // =====================================================

    fun preprocessImage(bitmap: Bitmap): Bitmap {

        val matrix = Matrix()

        matrix.postScale(
            2f,
            2f
        )

        val scaled =
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )

        val grayBitmap =
            Bitmap.createBitmap(
                scaled.width,
                scaled.height,
                Bitmap.Config.ARGB_8888
            )

        val canvas = Canvas(grayBitmap)
        val paint = Paint()

        val colorMatrix =
            ColorMatrix().apply {
                setSaturation(0f)
            }

        paint.colorFilter =
            ColorMatrixColorFilter(colorMatrix)

        canvas.drawBitmap(
            scaled,
            0f,
            0f,
            paint
        )

        return toBlackWhite(grayBitmap)
    }

    // =====================================================
    // تحويل الصورة إلى أبيض وأسود
    // =====================================================

    private fun toBlackWhite(bitmap: Bitmap): Bitmap {

        val width = bitmap.width
        val height = bitmap.height

        val result =
            Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

        for (x in 0 until width) {

            for (y in 0 until height) {

                val pixel =
                    bitmap.getPixel(x, y)

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val gray =
                    (r + g + b) / 3

                val newColor =
                    if (gray > 140)
                        Color.WHITE
                    else
                        Color.BLACK

                result.setPixel(
                    x,
                    y,
                    newColor
                )
            }
        }

        return result
    }
}
