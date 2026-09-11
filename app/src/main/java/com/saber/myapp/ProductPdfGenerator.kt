package com.saber.myapp

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.icu.text.ArabicShaping
import android.icu.text.Bidi
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

        val documentsDir = File(
            context.getExternalFilesDir(null),
            "Documents/إدارة المخزون"
        )

        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }

        val safeName = product.name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .ifBlank { "product" }

        val pdfFile = File(
            documentsDir,
            "$safeName.pdf"
        )

        PDDocument().use { document ->

            // =========================
            // تحميل الخط العربي
            // =========================

            val font = context.assets.open(
                "fonts/NotoNaskhArabic-Regular.ttf"
            ).use { inputStream ->
                PDType0Font.load(
                    document,
                    inputStream,
                    true
                )
            }

            // =========================
            // إنشاء الصفحة
            // =========================

            val page = PDPage(
                PDRectangle.A4
            )

            document.addPage(page)

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

                drawArabicText(
                    content = content,
                    font = font,
                    text = "إدارة المخزون",
                    x = pageWidth - 40f,
                    y = pageHeight - 55f,
                    size = 20f,
                    alignRight = true
                )

                // =========================
                // الخط الفاصل
                // =========================

                content.setStrokingColor(
                    Color.DKGRAY
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

                    val imageFile =
                        File(imagePath)

                    if (imageFile.exists()) {

                        val bitmap =
                            BitmapFactory.decodeFile(
                                imageFile.absolutePath
                            )

                        if (bitmap != null) {

                            val pdfImage =
                                LosslessFactory.createFromImage(
                                    document,
                                    bitmap
                                )

                            content.drawImage(
                                pdfImage,
                                40f,
                                pageHeight - 280f,
                                180f,
                                180f
                            )

                            bitmap.recycle()
                        }
                    }
                }

                // =========================
                // بيانات المنتج
                // =========================

                drawArabicText(
                    content,
                    font,
                    "اسم المنتج: ${product.name}",
                    pageWidth - 40f,
                    pageHeight - 125f,
                    15f,
                    true
                )

                drawArabicText(
                    content,
                    font,
                    "التصنيف: ${product.category}",
                    pageWidth - 40f,
                    pageHeight - 165f,
                    15f,
                    true
                )

                drawArabicText(
                    content,
                    font,
                    "تاريخ الانتهاء: ${product.expiryDate}",
                    pageWidth - 40f,
                    pageHeight - 205f,
                    15f,
                    true
                )

                drawArabicText(
                    content,
                    font,
                    "الباركود: ${product.barcode}",
                    pageWidth - 40f,
                    pageHeight - 245f,
                    15f,
                    true
                )
            }

            document.save(pdfFile)
        }

        return pdfFile
    }

    // =========================
    // معالجة النص العربي
    // =========================

    private fun shapeArabicText(
        text: String
    ): String {

        val shaper = ArabicShaping(
            ArabicShaping.LETTERS_SHAPE
        )

        val shapedText =
            shaper.shape(text)

        val bidi = Bidi(
            shapedText,
            Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT
        )

        return bidi.writeReordered(
            Bidi.DO_MIRRORING
        )
    }

    // =========================
    // كتابة النص العربي في PDF
    // =========================

    private fun drawArabicText(
        content: PDPageContentStream,
        font: PDType0Font,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        alignRight: Boolean
    ) {

        val processedText =
            shapeArabicText(text)

        content.beginText()

        content.setFont(
            font,
            size
        )

        content.setNonStrokingColor(
            Color.BLACK
        )

        val textWidth =
            font.getStringWidth(
                processedText
            ) / 1000f * size

        val finalX =
            if (alignRight) {
                x - textWidth
            } else {
                x
            }

        content.newLineAtOffset(
            finalX,
            y
        )

        content.showText(
            processedText
        )

        content.endText()
    }
}
