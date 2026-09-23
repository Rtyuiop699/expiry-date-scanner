package com.saber.myapp.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint

import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

// =====================================================
// Data class لمراحل المعالجة (للـ Debug)
// =====================================================

data class DotMatrixStages(
    val original: Bitmap,
    val gray: Bitmap,
    val contrast: Bitmap,
    val resized: Bitmap,
    val threshold: Bitmap,
    val morphology: Bitmap
)

// =====================================================
// كلاس معالجة الصور
// =====================================================

class ImageProcessor {

    // =====================================================
    // قص المنطقة المركزية من الصورة
    // =====================================================

    fun cropCenter(bitmap: Bitmap): Bitmap {

        val width = bitmap.width
        val height = bitmap.height

        val cropWidth = (width * 0.85).toInt()
        val cropHeight = (height * 0.20).toInt()

        val left = (width - cropWidth) / 2
        val top = (height - cropHeight) / 2

        return Bitmap.createBitmap(
            bitmap,
            left,
            top,
            cropWidth,
            cropHeight
        )
    }

    // =====================================================
    // معالجة الصورة قبل OCR (محسّنة)
    // =====================================================

    fun preprocessImage(bitmap: Bitmap): Bitmap {

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // 1. تحويل إلى تدرج رمادي
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)

        // 2. قياس جودة الصورة (تباين)
        val mean = MatOfDouble()
        val stdDev = MatOfDouble()
        Core.meanStdDev(grayMat, mean, stdDev)
        val contrast = stdDev.toArray()[0]

        // 3. معالجة تكيفية حسب جودة الصورة
        val resultMat = when {
            contrast > 50.0 -> {
                grayMat.clone()
            }
            contrast in 30.0..50.0 -> {
                val clahe = Imgproc.createCLAHE(1.5, Size(8.0, 8.0))
                val out = Mat()
                clahe.apply(grayMat, out)
                clahe.collectGarbage()
                out
            }
            else -> {
                val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
                val enhanced = Mat()
                clahe.apply(grayMat, enhanced)
                clahe.collectGarbage()

                val blurred = Mat()
                Imgproc.GaussianBlur(enhanced, blurred, Size(3.0, 3.0), 0.0)
                blurred
            }
        }

        // 4. تحويل إلى RGBA للإرجاع
        val rgbaMat = Mat()
        Imgproc.cvtColor(resultMat, rgbaMat, Imgproc.COLOR_GRAY2RGBA)

        val resultBitmap = Bitmap.createBitmap(
            rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(rgbaMat, resultBitmap)

        // تحرير الموارد
        mat.release()
        grayMat.release()
        resultMat.release()
        rgbaMat.release()

        return resultBitmap
    }

    // =====================================================
    // تحويل ثنائي — للـ Debug فقط
    // =====================================================

    fun toBinary(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)

        val threshMat = Mat()
        Imgproc.threshold(
            grayMat, threshMat, 0.0, 255.0,
            Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU
        )

        val result = matToBitmap(threshMat)

        mat.release()
        grayMat.release()
        threshMat.release()

        return result
    }

    // =====================================================
    // معالجة الخطوط النقطية (محسّنة)
    // =====================================================

    fun processDotMatrix(bitmap: Bitmap): Bitmap {

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // 1. تدرج رمادي
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)

        // 2. تقليل الضوضاء أولاً
        val blurredMat = Mat()
        Imgproc.GaussianBlur(grayMat, blurredMat, Size(3.0, 3.0), 0.0)

        // 3. تحسين التباين بـ CLAHE
        val clahe = Imgproc.createCLAHE(2.5, Size(8.0, 8.0))
        val contrastMat = Mat()
        clahe.apply(blurredMat, contrastMat)
        clahe.collectGarbage()

        // 4. تكبير 2× (يساعد ML Kit)
        val resizedMat = Mat()
        Imgproc.resize(
            contrastMat, resizedMat,
            Size(contrastMat.cols() * 2.0, contrastMat.rows() * 2.0),
            0.0, 0.0, Imgproc.INTER_CUBIC
        )

        // 5. عتبة تكيفية
        val threshMat = Mat()
        Imgproc.adaptiveThreshold(
            resizedMat, threshMat, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            21, 7.0
        )

        // 6. لحام النقاط
        val kernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_RECT, Size(2.0, 2.0)
        )
        val morphedMat = Mat()
        Imgproc.morphologyEx(
            threshMat, morphedMat,
            Imgproc.MORPH_CLOSE, kernel
        )

        // 7. تحويل إلى Bitmap
        val resultBitmap = matToBitmap(morphedMat)

        // تحرير الموارد
        mat.release()
        grayMat.release()
        blurredMat.release()
        contrastMat.release()
        resizedMat.release()
        threshMat.release()
        morphedMat.release()
        kernel.release()

        return resultBitmap
    }

    // =====================================================
    // مراحل المعالجة (للـ Debug)
    // =====================================================

    fun processDotMatrixStages(bitmap: Bitmap): DotMatrixStages {

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGBA2GRAY)

        val clahe = Imgproc.createCLAHE(2.5, Size(8.0, 8.0))
        val contrastMat = Mat()
        clahe.apply(grayMat, contrastMat)
        clahe.collectGarbage()

        val resizedMat = Mat()
        Imgproc.resize(
            contrastMat, resizedMat,
            Size(contrastMat.cols() * 2.0, contrastMat.rows() * 2.0),
            0.0, 0.0, Imgproc.INTER_CUBIC
        )

        val threshMat = Mat()
        Imgproc.adaptiveThreshold(
            resizedMat, threshMat, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            21, 7.0
        )

        val kernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_RECT, Size(2.0, 2.0)
        )
        val morphedMat = Mat()
        Imgproc.morphologyEx(
            threshMat, morphedMat,
            Imgproc.MORPH_CLOSE, kernel
        )

        val stages = DotMatrixStages(
            original = bitmap,
            gray = matToBitmap(grayMat),
            contrast = matToBitmap(contrastMat),
            resized = matToBitmap(resizedMat),
            threshold = matToBitmap(threshMat),
            morphology = matToBitmap(morphedMat)
        )

        mat.release()
        grayMat.release()
        contrastMat.release()
        resizedMat.release()
        threshMat.release()
        morphedMat.release()
        kernel.release()

        return stages
    }

    // =====================================================
    // دالة مساعدة: Mat → Bitmap
    // =====================================================

    private fun matToBitmap(mat: Mat): Bitmap {

        val rgbaMat = Mat()

        if (mat.channels() == 1) {
            Imgproc.cvtColor(mat, rgbaMat, Imgproc.COLOR_GRAY2RGBA)
        } else {
            Imgproc.cvtColor(mat, rgbaMat, Imgproc.COLOR_RGB2RGBA)
        }

        val bitmap = Bitmap.createBitmap(
            rgbaMat.cols(), rgbaMat.rows(),
            Bitmap.Config.ARGB_8888
        )

        Utils.matToBitmap(rgbaMat, bitmap)
        rgbaMat.release()

        return bitmap
    }
}
