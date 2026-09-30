package com.saber.myapp

import android.content.Context
import android.provider.MediaStore
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.bumptech.glide.Glide
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.io.File

class ProductPdfGenerator(
    private val context: Context
) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun createPdf(product: Product): File {

        // =========================
        // اسم الملف
        // =========================

        val safeName = product.name
            .replace(
                Regex("[\\\\/:*?\"<>|]"),
                "_"
            )
            .ifBlank {
                "product"
            }

        val fileName = "$safeName.pdf"

        // ملف مؤقت يتم إنشاء PDF بداخله أولاً.
        val pdfFile =
            File(
                context.cacheDir,
                fileName
            )

        // =========================
        // إنشاء ملف PDF
        // =========================

        PDDocument().use { document ->

            // =========================
            // تحميل الخط العربي
            // =========================

            val font =
                context.assets
                    .open(
                        "fonts/NotoNaskhArabic-Regular.ttf"
                    )
                    .use { inputStream ->

                        PDType0Font.load(
                            document,
                            inputStream,
                            true
                        )
                    }

            // =========================
            // إنشاء صفحة A4
            // =========================

            val page =
                PDPage(
                    PDRectangle.A4
                )

            document.addPage(page)

            // =========================
            // الكتابة داخل الصفحة
            // =========================

            PDPageContentStream(
                document,
                page
            ).use { content ->

                val pageWidth =
                    page.mediaBox.width

                val pageHeight =
                    page.mediaBox.height

                // =========================
                // عنوان التقرير
                // =========================

                drawText(
                    document,
                    content,
                    font,
                    context.getString(R.string.pdf_title),
                    220f,
                    pageHeight - 55f,
                    20f
                )

                // =========================
                // الخط الفاصل
                // =========================

                content.setStrokingColor(
                    64,
                    64,
                    64
                )

                content.setLineWidth(1f)

                content.moveTo(
                    40f,
                    pageHeight - 75f
                )

                content.lineTo(
                    pageWidth - 40f,
                    pageHeight - 75f
                )

                content.stroke()

                // =========================
                // صورة المنتج
                // =========================

                val imagePath =
                    product.imagePath

                if (!imagePath.isNullOrBlank()) {

                    val bitmap =
                        loadProductBitmap(imagePath)

                    if (bitmap != null) {

                        try {

                            val pdfImage =
                                LosslessFactory.createFromImage(
                                    document,
                                    bitmap
                                )

                            val maxImageWidth = 180f
                            val maxImageHeight = 180f

                            val scale =
                                minOf(
                                    maxImageWidth / bitmap.width.toFloat(),
                                    maxImageHeight / bitmap.height.toFloat()
                                )

                            val imageWidth =
                                bitmap.width * scale

                            val imageHeight =
                                bitmap.height * scale

                            val imageX =
                                40f +
                                    (maxImageWidth - imageWidth) / 2f

                            val imageY =
                                pageHeight - 100f - imageHeight

                            content.drawImage(
                                pdfImage,
                                imageX,
                                imageY,
                                imageWidth,
                                imageHeight
                            )

                        } finally {

                            bitmap.recycle()
                        }
                    }
                }

                // =========================
                // بيانات المنتج
                // =========================

                drawText(
                    document,
                    content,
                    font,
                    context.getString(R.string.product_name) + ": ${product.name}",
                    250f,
                    pageHeight - 125f,
                    15f
                )

                drawText(
                    document,
                    content,
                    font,
                    context.getString(R.string.category) + ": ${getLocalizedCategoryName(product.category)}",
                    250f,
                    pageHeight - 165f,
                    15f
                )

                drawText(
                    document,
                    content,
                    font,
                    context.getString(R.string.expiry_date) + ": ${product.expiryDate}",
                    250f,
                    pageHeight - 205f,
                    15f
                )

                drawText(
                    document,
                    content,
                    font,
                    context.getString(R.string.barcode) + ": ${product.barcode}",
                    250f,
                    pageHeight - 245f,
                    15f
                )
            }

            // =========================
            // حفظ الملف
            // =========================

            document.save(
                pdfFile
            )
        }

        copyToDownloads(
            pdfFile,
            pdfFile.name
        )

        return pdfFile
    }

    // =====================================================
    // كتابة النص
    // =====================================================

    private fun copyToDownloads(
        sourceFile: File,
        fileName: String
    ) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {

            val values =
                ContentValues().apply {

                    put(
                        MediaStore.Downloads.DISPLAY_NAME,
                        fileName
                    )

                    put(
                        MediaStore.Downloads.MIME_TYPE,
                        "application/pdf"
                    )

                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        "Download/إدارة المخزون"
                    )

                    put(
                        MediaStore.Downloads.IS_PENDING,
                        1
                    )
                }

            val resolver =
                context.contentResolver

            val uri =
                resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                ) ?: throw IllegalStateException(
                    "تعذر إنشاء ملف PDF في Download"
                )

            try {

                resolver.openOutputStream(uri)?.use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException(
                    "تعذر كتابة ملف PDF"
                )

                val completedValues =
                    ContentValues().apply {
                        put(
                            MediaStore.Downloads.IS_PENDING,
                            0
                        )
                    }

                resolver.update(
                    uri,
                    completedValues,
                    null,
                    null
                )

            } catch (e: Exception) {

                resolver.delete(
                    uri,
                    null,
                    null
                )

                throw e
            }

        } else {

            val downloadsDir =
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )

            val appDownloadsDir =
                File(
                    downloadsDir,
                    "إدارة المخزون"
                )

            if (!appDownloadsDir.exists()) {
                appDownloadsDir.mkdirs()
            }

            val destination =
                File(
                    appDownloadsDir,
                    fileName
                )

            sourceFile.inputStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun loadProductBitmap(imagePath: String): Bitmap? {
        return try {

            if (
                imagePath.startsWith("http://") ||
                imagePath.startsWith("https://")
            ) {

                Glide.with(context.applicationContext)
                    .asBitmap()
                    .load(imagePath)
                    .submit()
                    .get()

            } else {

                BitmapFactory.decodeFile(imagePath)
            }

        } catch (e: Exception) {

            e.printStackTrace()
            null
        }
    }

    private fun getLocalizedCategoryName(category: String): String {
        return when (category) {
            "عصائر" ->
                context.getString(R.string.category_juices)

            "مشروبات غازية" ->
                context.getString(R.string.category_soft_drinks)

            "خضار معلبة ومخللات" ->
                context.getString(R.string.category_canned_vegetables_pickles)

            "أسماك معلبة" ->
                context.getString(R.string.category_canned_fish)

            "كيك وبسكويت" ->
                context.getString(R.string.category_cakes_biscuits)

            "آيسكريم ومثلجات" ->
                context.getString(R.string.category_ice_cream_frozen)

            else -> category
        }
    }

    private fun drawText(
        document: PDDocument,
        content: PDPageContentStream,
        font: PDType0Font,
        text: String,
        x: Float,
        y: Float,
        size: Float
    ) {

        // Android Canvas يتولى تشكيل الحروف العربية واتجاه RTL.

        val bitmapWidth = 600
        val bitmapHeight =
            (size * 3.0f)
                .toInt()
                .coerceAtLeast(60)

        val bitmap =
            Bitmap.createBitmap(
                bitmapWidth,
                bitmapHeight,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(bitmap)

        canvas.drawColor(
            Color.TRANSPARENT
        )

        val typeface =
            Typeface.createFromAsset(
                context.assets,
                "fonts/NotoNaskhArabic-Regular.ttf"
            )

        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {

                this.typeface = typeface
                textSize = size * 2.0f
                color = Color.BLACK
                textAlign = Paint.Align.RIGHT
                isSubpixelText = true
            }

        val baseline =
            bitmapHeight - size * 0.55f

        canvas.drawText(
            text,
            bitmapWidth - 10f,
            baseline,
            paint
        )

        val pdfImage =
            LosslessFactory.createFromImage(
                document,
                bitmap
            )

        content.drawImage(
            pdfImage,
            x,
            y,
            bitmapWidth * 0.5f,
            bitmapHeight * 0.5f
        )

        bitmap.recycle()
    }

}
