package com.saber.myapp.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint

import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

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

    // =====================================================
    // معالجة الخطوط النقطية والرفيعة باستخدام OpenCV
    // =====================================================
fun processDotMatrix(bitmap: Bitmap): Bitmap {

    val mat = Mat()
    Utils.bitmapToMat(bitmap, mat)

    // 1. تحويل إلى Grayscale
    val grayMat = Mat()

    Imgproc.cvtColor(
        mat,
        grayMat,
        Imgproc.COLOR_RGBA2GRAY
    )

    // 2. تحسين التباين محليًا
    val clahe =
        Imgproc.createCLAHE(
            2.5,
            Size(8.0, 8.0)
        )

    val contrastMat = Mat()

    clahe.apply(
        grayMat,
        contrastMat
    )

    // 3. تكبير الصورة
    // يساعد ML Kit على قراءة النقاط الصغيرة
    val resizedMat = Mat()

    Imgproc.resize(
        contrastMat,
        resizedMat,
        Size(
            contrastMat.cols() * 2.0,
            contrastMat.rows() * 2.0
        ),
        0.0,
        0.0,
        Imgproc.INTER_CUBIC
    )

    // 4. Adaptive Threshold
    val threshMat = Mat()

    Imgproc.adaptiveThreshold(
        resizedMat,
        threshMat,
        255.0,
        Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
        Imgproc.THRESH_BINARY,
        21,
        7.0
    )

    // 5. وصل النقاط المتقاربة
    // نستخدم نواة صغيرة حتى لا تندمج الأرقام
    val kernel =
        Imgproc.getStructuringElement(
            Imgproc.MORPH_RECT,
            Size(2.0, 2.0)
        )

    val morphedMat = Mat()

    Imgproc.morphologyEx(
        threshMat,
        morphedMat,
        Imgproc.MORPH_CLOSE,
        kernel
    )

    // 6. تحويل إلى RGBA
    val rgbaMat = Mat()

    Imgproc.cvtColor(
        morphedMat,
        rgbaMat,
        Imgproc.COLOR_GRAY2RGBA
    )

    // 7. إنشاء Bitmap بالحجم الجديد
    val resultBitmap =
        Bitmap.createBitmap(
            rgbaMat.cols(),
            rgbaMat.rows(),
            Bitmap.Config.ARGB_8888
        )

    Utils.matToBitmap(
        rgbaMat,
        resultBitmap
    )

    // 8. تحرير موارد OpenCV
    mat.release()
    grayMat.release()
    contrastMat.release()
    resizedMat.release()
    threshMat.release()
    morphedMat.release()
    rgbaMat.release()
    kernel.release()
    clahe.collectGarbage()

    return resultBitmap
}

}
