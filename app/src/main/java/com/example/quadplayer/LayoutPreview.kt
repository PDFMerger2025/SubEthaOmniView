package com.example.quadplayer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** Small schematic of the chosen layout, drawn in cyan on dark. */
class LayoutPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val d = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x2200E5FF }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00E5FF.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f * d
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00E5FF.toInt(); textAlign = Paint.Align.CENTER; textSize = 20f * d
        isFakeBoldText = true
    }

    private var mode = LayoutMode.SIDE_BY_SIDE

    fun set(mode: LayoutMode) {
        this.mode = mode
        invalidate()
    }

    private fun columns(b: RectF, n: Int) = List(n) { i ->
        RectF(b.left + b.width() * i / n, b.top, b.left + b.width() * (i + 1) / n, b.bottom)
    }

    private fun rows(b: RectF, n: Int) = List(n) { i ->
        RectF(b.left, b.top + b.height() * i / n, b.right, b.top + b.height() * (i + 1) / n)
    }

    private fun rects(b: RectF): List<RectF> {
        val cx = b.centerX()
        val cy = b.centerY()
        return when (mode) {
            LayoutMode.SINGLE -> listOf(b)
            LayoutMode.SIDE_BY_SIDE -> columns(b, 2)
            LayoutMode.STACKED -> rows(b, 2)
            LayoutMode.BIG_LEFT -> listOf(
                RectF(b.left, b.top, cx, b.bottom),
                RectF(cx, b.top, b.right, cy),
                RectF(cx, cy, b.right, b.bottom)
            )
            LayoutMode.BIG_TOP -> listOf(
                RectF(b.left, b.top, b.right, cy),
                RectF(b.left, cy, cx, b.bottom),
                RectF(cx, cy, b.right, b.bottom)
            )
            LayoutMode.BIG_RIGHT -> listOf(
                RectF(cx, b.top, b.right, b.bottom),
                RectF(b.left, b.top, cx, cy),
                RectF(b.left, cy, cx, b.bottom)
            )
            LayoutMode.COLUMNS_3 -> columns(b, 3)
            LayoutMode.GRID_2X2 -> listOf(
                RectF(b.left, b.top, cx, cy), RectF(cx, b.top, b.right, cy),
                RectF(b.left, cy, cx, b.bottom), RectF(cx, cy, b.right, b.bottom)
            )
            LayoutMode.COLUMNS_4 -> columns(b, 4)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val pad = 3f * d
        val bounds = RectF(pad, pad, width - pad, height - pad)
        rects(bounds).forEachIndexed { i, r ->
            r.inset(3f * d, 3f * d)
            canvas.drawRoundRect(r, 8f * d, 8f * d, fill)
            canvas.drawRoundRect(r, 8f * d, 8f * d, stroke)
            canvas.drawText("${i + 1}", r.centerX(), r.centerY() - (text.ascent() + text.descent()) / 2f, text)
        }
    }
}
