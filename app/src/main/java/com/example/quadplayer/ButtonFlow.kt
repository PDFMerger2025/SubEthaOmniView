package com.example.quadplayer

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * Lays out equal-size square buttons in right-aligned rows, wrapping to as many rows as the
 * available width needs. Used for each tile's control cluster so it reflows in small tiles.
 */
class ButtonFlow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    private val d = resources.displayMetrics.density
    private val size = (36 * d).toInt()
    private val gap = (4 * d).toInt()

    private fun visibleKids(): List<View> =
        (0 until childCount).map { getChildAt(it) }.filter { it.visibility != GONE }

    private fun perRow(avail: Int) = ((avail + gap) / (size + gap)).coerceAtLeast(1)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val kids = visibleKids()
        val pr = perRow(MeasureSpec.getSize(widthMeasureSpec))
        val cols = minOf(kids.size, pr)
        val rows = if (kids.isEmpty()) 0 else (kids.size + pr - 1) / pr
        val spec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        kids.forEach { it.measure(spec, spec) }
        setMeasuredDimension(
            if (cols == 0) 0 else cols * size + (cols - 1) * gap,
            if (rows == 0) 0 else rows * size + (rows - 1) * gap
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val kids = visibleKids()
        val width = r - l
        val pr = perRow(width)
        kids.forEachIndexed { i, kid ->
            val row = i / pr
            val col = i % pr
            val inRow = minOf(pr, kids.size - row * pr)
            val startX = width - (inRow * size + (inRow - 1) * gap)
            val left = startX + col * (size + gap)
            val top = row * (size + gap)
            kid.layout(left, top, left + size, top + size)
        }
    }
}
