package com.saber.myapp

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * مولّد PDF للمنتجات (منتج واحد أو قائمة).
 *
 * - يستخدم android.graphics.pdf.PdfDocument (لا يحتاج PDFBox).
 * - Canvas يتولى تشكيل الحروف العربية واتجاه RTL، والنص يبقى نصاً حقيقياً (قابل للتحديد والبحث).
 * - الدوال متزامنة (blocking) ويجب استدعاؤها من خيط خلفي (Dispatchers.IO)،
 *   لأن تحميل الصور بـ Glide يستخدم get().
 */
class ProductPdfGenerator(
    private val context: Context
) {

    private companion object {
        const val PAGE_WIDTH = 595            // A4 بالنقاط
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40f
        const val HEADER_HEIGHT = 70f
        const val FOOTER_HEIGHT = 30f
        const val CARD_HEIGHT = 150f
        const val CARD_GAP = 10f
        const val CARD_PADDING = 12f
        const val IMAGE_SIZE = 120f
        const val FONT_PATH = "fonts/NotoNaskhArabic-Regular.ttf"
        const val DOWNLOAD_FOLDER = "إدارة المخزون"
    }

    private val isRtl: Boolean =
        context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

    // الخط يُحمَّل مرة واحدة فقط
    private val baseTypeface: Typeface by lazy {
        try {
            Typeface.createFromAsset(context.assets, FONT_PATH)
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }

    private val boldTypeface: Typeface by lazy {
        Typeface.create(baseTypeface, Typeface.BOLD)
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.rgb(160, 160, 160)
    }

    private val placeholderPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.rgb(240, 240, 240)
    }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val separatorPaint = Paint().apply {
        color = Color.rgb(64, 64, 64)
        strokeWidth = 1f
    }

    // =====================================================
    // الواجهة العامة (نفس اسم الدالة القديمة، فلا يتغير الكود الذي يعمل)
    // =====================================================

    fun createPdf(product: Product): File = createPdf(listOf(product))

    fun createPdf(products: List<Product>): File {
        require(products.isNotEmpty()) { "No products to export" }

        val pdfFile = File(context.cacheDir, buildFileName(products))
        val document = PdfDocument()

        try {
            if (products.size == 1) {
                // منتج واحد (الضغط المطول): صفحة كاملة بالتصميم القديم
                writeSingleProductPage(document, products[0])
            } else {
                // عدة منتجات (التحديد المتعدد): بطاقات متعددة في الصفحة
                writePages(document, products)
            }
            FileOutputStream(pdfFile).use { output ->
                document.writeTo(output)
            }
        } finally {
            document.close()
        }

        copyToDownloads(pdfFile, pdfFile.name)
        return pdfFile
    }

    // =====================================================
    // بناء الصفحات (ترقيم تلقائي عند امتلاء الصفحة)
    // =====================================================

    private fun writePages(document: PdfDocument, products: List<Product>) {
        var pageNumber = 0
        var currentPage: PdfDocument.Page? = null
        var y = 0f
        val maxY = PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT

        fun closePage() {
            currentPage?.let { page ->
                drawFooter(page.canvas, pageNumber)
                document.finishPage(page)
            }
            currentPage = null
        }

        fun openPage() {
            pageNumber++
            val info = PdfDocument.PageInfo
                .Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber)
                .create()
            val page = document.startPage(info)
            currentPage = page
            y = if (pageNumber == 1) drawHeader(page.canvas) else MARGIN
        }

        try {
            products.forEach { product ->
                if (currentPage == null || y + CARD_HEIGHT > maxY) {
                    closePage()
                    openPage()
                }
                drawProductCard(currentPage!!.canvas, product, y)
                y += CARD_HEIGHT + CARD_GAP
            }
        } finally {
            closePage()
        }
    }

    // =====================================================
    // صفحة منتج واحد (نفس التصميم القديم):
    // عنوان في الأعلى، خط فاصل، صورة كبيرة، وأربعة أسطر من البيانات
    // =====================================================

    private fun writeSingleProductPage(document: PdfDocument, product: Product) {
        val info = PdfDocument.PageInfo
            .Builder(PAGE_WIDTH, PAGE_HEIGHT, 1)
            .create()
        val page = document.startPage(info)

        try {
            val canvas = page.canvas
            val contentWidth = PAGE_WIDTH - 2 * MARGIN

            // العنوان
            drawText(
                canvas,
                context.getString(R.string.pdf_title),
                textPaint(20f, bold = true),
                MARGIN, 28f, contentWidth,
                Layout.Alignment.ALIGN_CENTER
            )

            // الخط الفاصل
            canvas.drawLine(MARGIN, 75f, PAGE_WIDTH - MARGIN, 75f, separatorPaint)

            // الصورة (يسار)
            val imageRect = RectF(MARGIN, 100f, MARGIN + 180f, 280f)
            drawProductImage(canvas, product.imagePath, imageRect, drawPlaceholder = false)

            // البيانات (يمين الصورة)
            val textLeft = 250f
            val textWidth = PAGE_WIDTH - MARGIN - textLeft
            val paint = textPaint(15f)

            val lines = listOf(
                context.getString(R.string.product_name) + ": ${product.name}",
                context.getString(R.string.category) + ": " + getLocalizedCategoryName(product.category),
                context.getString(R.string.expiry_date) + ": ${product.expiryDate}",
                context.getString(R.string.barcode) + ": ${product.barcode}"
            )

            var ty = 100f
            lines.forEachIndexed { index, line ->
                // اسم المنتج قد يطول فنسمح له بسطرين
                val maxLines = if (index == 0) 2 else 1
                ty += drawText(canvas, line, paint, textLeft, ty, textWidth, maxLines = maxLines) + 14f
            }

        } finally {
            document.finishPage(page)
        }
    }

    // =====================================================
    // الرأس والتذييل
    // =====================================================

    private fun drawHeader(canvas: Canvas): Float {
        val contentWidth = PAGE_WIDTH - 2 * MARGIN

        drawText(
            canvas,
            context.getString(R.string.pdf_title),
            textPaint(20f, bold = true),
            MARGIN, MARGIN, contentWidth,
            Layout.Alignment.ALIGN_CENTER
        )

        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        drawText(
            canvas, date,
            textPaint(10f, color = Color.DKGRAY),
            MARGIN, MARGIN + 34f, contentWidth,
            Layout.Alignment.ALIGN_CENTER
        )

        val lineY = MARGIN + HEADER_HEIGHT - 12f
        canvas.drawLine(MARGIN, lineY, PAGE_WIDTH - MARGIN, lineY, separatorPaint)

        return MARGIN + HEADER_HEIGHT
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        drawText(
            canvas,
            pageNumber.toString(),
            textPaint(10f, color = Color.GRAY),
            MARGIN, PAGE_HEIGHT - MARGIN, PAGE_WIDTH - 2 * MARGIN,
            Layout.Alignment.ALIGN_CENTER
        )
    }

    // =====================================================
    // بطاقة المنتج
    // =====================================================

    private fun drawProductCard(canvas: Canvas, product: Product, top: Float) {
        val left = MARGIN
        val right = PAGE_WIDTH - MARGIN

        canvas.drawRoundRect(
            RectF(left, top, right, top + CARD_HEIGHT),
            8f, 8f, borderPaint
        )

        // العربية: الصورة يميناً (مثل القائمة في التطبيق)، الإنجليزية: يساراً
        val imageLeft = if (isRtl) right - CARD_PADDING - IMAGE_SIZE else left + CARD_PADDING
        val imageRect = RectF(
            imageLeft,
            top + CARD_PADDING,
            imageLeft + IMAGE_SIZE,
            top + CARD_PADDING + IMAGE_SIZE
        )
        drawProductImage(canvas, product.imagePath, imageRect)

        val textLeft: Float
        val textRight: Float
        if (isRtl) {
            textLeft = left + CARD_PADDING
            textRight = imageRect.left - CARD_PADDING
        } else {
            textLeft = imageRect.right + CARD_PADDING
            textRight = right - CARD_PADDING
        }
        val textWidth = textRight - textLeft

        var ty = top + CARD_PADDING

        ty += drawText(
            canvas, product.name,
            textPaint(15f, bold = true),
            textLeft, ty, textWidth, maxLines = 2
        ) + 8f

        val lines = listOf(
            context.getString(R.string.category) + ": " + getLocalizedCategoryName(product.category),
            context.getString(R.string.expiry_date) + ": ${product.expiryDate}",
            context.getString(R.string.barcode) + ": ${product.barcode}"
        )

        val bodyPaint = textPaint(12f)
        lines.forEach { line ->
            ty += drawText(canvas, line, bodyPaint, textLeft, ty, textWidth) + 5f
        }
    }

    // =====================================================
    // صورة المنتج (Glide يصغّرها قبل الرسم لتوفير الذاكرة)
    // =====================================================

    private fun drawProductImage(
        canvas: Canvas,
        imagePath: String?,
        rect: RectF,
        drawPlaceholder: Boolean = true
    ) {
        if (drawPlaceholder) canvas.drawRect(rect, placeholderPaint)

        if (imagePath.isNullOrBlank()) return

        val glide = Glide.with(context.applicationContext)
        val request = glide.asBitmap()
        val builder =
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                request.load(imagePath)
            } else {
                request.load(File(imagePath))
            }

        val target = builder
            .disallowHardwareConfig()   // الرسم على PDF يحتاج Bitmap برمجياً
            .override(400, 400)
            .submit()

        try {
            val bitmap = target.get(15, TimeUnit.SECONDS)

            val scale = min(
                rect.width() / bitmap.width,
                rect.height() / bitmap.height
            )
            val w = bitmap.width * scale
            val h = bitmap.height * scale

            val dst = RectF(
                rect.centerX() - w / 2f,
                rect.centerY() - h / 2f,
                rect.centerX() + w / 2f,
                rect.centerY() + h / 2f
            )
            canvas.drawBitmap(bitmap, null, dst, bitmapPaint)

        } catch (e: Exception) {
            // فشل تحميل الصورة (لا إنترنت مثلاً): نترك المربع الرمادي ونكمل
            e.printStackTrace()
        } finally {
            glide.clear(target)
        }
    }

    // =====================================================
    // رسم نص بتشكيل عربي صحيح وقص تلقائي (...) عند الطول الزائد
    // يعيد ارتفاع النص المرسوم
    // =====================================================

    private fun drawText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        y: Float,
        width: Float,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        maxLines: Int = 1
    ): Float {
        val layout = buildLayout(text, paint, width.toInt(), alignment, maxLines)

        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()

        return layout.height.toFloat()
    }

    private fun buildLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment,
        maxLines: Int
    ): StaticLayout {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder
                .obtain(text, 0, text.length, paint, width)
                .setAlignment(alignment)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setTextDirection(
                    if (isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR
                )
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width, alignment, 1f, 0f, false)
        }
    }

    private fun textPaint(
        size: Float,
        bold: Boolean = false,
        color: Int = Color.BLACK
    ): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = if (bold) boldTypeface else baseTypeface
        textSize = size
        this.color = color
    }

    // =====================================================
    // اسم الملف
    // =====================================================

    private fun buildFileName(products: List<Product>): String {
        if (products.size == 1) {
            val safeName = products[0].name
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .trim()
                .ifBlank { "product" }
            return "$safeName.pdf"
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "inventory_$stamp.pdf"
    }

    // =====================================================
    // الحفظ في Downloads
    // ملاحظة: على Android 9 وما دون يلزم تصريح WRITE_EXTERNAL_STORAGE
    // =====================================================

    private fun copyToDownloads(sourceFile: File, fileName: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/$DOWNLOAD_FOLDER")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = context.contentResolver

            val uri = resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw IllegalStateException("Could not create PDF in Downloads")

            try {
                resolver.openOutputStream(uri)?.use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("Could not write PDF")

                val done = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                resolver.update(uri, done, null, null)

            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }

        } else {

            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val appDir = File(downloadsDir, DOWNLOAD_FOLDER)
            if (!appDir.exists()) appDir.mkdirs()

            sourceFile.inputStream().use { input ->
                File(appDir, fileName).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    // =====================================================
    // أسماء الفئات المترجمة
    // =====================================================

    private fun getLocalizedCategoryName(category: String): String {
        return when (category) {
            "عصائر" -> context.getString(R.string.category_juices)
            "مشروبات غازية" -> context.getString(R.string.category_soft_drinks)
            "خضار معلبة ومخللات" -> context.getString(R.string.category_canned_vegetables_pickles)
            "أسماك معلبة" -> context.getString(R.string.category_canned_fish)
            "كيك وبسكويت" -> context.getString(R.string.category_cakes_biscuits)
            "آيسكريم ومثلجات" -> context.getString(R.string.category_ice_cream_frozen)
            else -> category
        }
    }
}
