package com.saber.myapp

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
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

            val page = PDPage(PDRectangle.A4)
            document.addPage(page)

            PDPageContentStream(
                document,
                page
            ).use { content ->

                val pageWidth = page.mediaBox.width
                val pageHeight = page.mediaBox.height

                // =========================
                // العنوان
                // =========================

                content.beginText()

                content.setFont(
                    PDType1Font.HELVETICA_BOLD,
                    20f
                )

                content.setNonStrokingColor(
                    Color.BLACK
                )

                content.newLineAtOffset(
                    pageWidth / 2 - 60f,
                    pageHeight - 50f
                )

                content.showText(
                    "Inventory"
                )

                content.endText()

                // =========================
                // الخط الفاصل
                // =========================

                content.setStrokingColor(
                    Color.DKGRAY
                )

                content.setLineWidth(1f)

                content.moveTo(
                    40f,
                    pageHeight - 70f
                )

                content.lineTo(
                    pageWidth - 40f,
                    pageHeight - 70f
                )

                content.stroke()

                // =========================
                // صورة المنتج
                // =========================

                val imagePath = product.imagePath

                if (!imagePath.isNullOrBlank()) {

                    val imageFile = File(imagePath)

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
                                pageHeight - 270f,
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

                drawText(
                    content,
                    "Product Name: ${product.name}",
                    250f,
                    pageHeight - 120f,
                    14f
                )

                drawText(
                    content,
                    "Category: ${product.category}",
                    250f,
                    pageHeight - 155f,
                    14f
                )

                drawText(
                    content,
                    "EXP: ${product.expiryDate}",
                    250f,
                    pageHeight - 190f,
                    14f
                )

                drawText(
                    content,
                    "Barcode: ${product.barcode}",
                    250f,
                    pageHeight - 225f,
                    14f
                )
            }

            document.save(pdfFile)
        }

        return pdfFile
    }

    private fun drawText(
        content: PDPageContentStream,
        text: String,
        x: Float,
        y: Float,
        size: Float
    ) {
        content.beginText()

        content.setFont(
            PDType1Font.HELVETICA,
            size
        )

        content.setNonStrokingColor(
            Color.BLACK
        )

        content.newLineAtOffset(
            x,
            y
        )

        content.showText(
            text
        )

        content.endText()
    }
}
