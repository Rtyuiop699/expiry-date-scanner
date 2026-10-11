package com.saber.myapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class CurvedBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(67, 157, 255)
        style = Paint.Style.FILL
    }

    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val center = w / 2f
        val notchHalfWidth = 82f * resources.displayMetrics.density
        val notchDepth = 36f * resources.displayMetrics.density

        path.reset()
        path.moveTo(0f, 0f)
        path.lineTo(center - notchHalfWidth, 0f)

        path.cubicTo(
            center - notchHalfWidth * 0.35f, 0f,
            center - notchHalfWidth * 0.48f, notchDepth,
            center, notchDepth
        )

        path.cubicTo(
            center + notchHalfWidth * 0.48f, notchDepth,
            center + notchHalfWidth * 0.35f, 0f,
            center + notchHalfWidth, 0f
        )

        path.lineTo(w, 0f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()

        canvas.drawPath(path, paint)
    }
}
