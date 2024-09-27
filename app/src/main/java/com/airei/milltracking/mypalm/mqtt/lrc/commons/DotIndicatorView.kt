package com.airei.milltracking.mypalm.mqtt.lrc.commons

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class DotIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dotCount = 3             // Number of dots
    private var selectedPosition = 0      // The selected (current) dot
    private var dotRadius = 8f            // Radius of unselected dots
    private var selectedDotRadius = 12f   // Radius of selected dot
    var dotSpacing = 20f          // Spacing between dots

    private val selectedPaint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.holo_green_dark)
        isAntiAlias = true
    }

    private val unselectedPaint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculate total width of dots
        val totalWidth = (dotCount - 1) * dotSpacing + dotCount * (dotRadius * 2)
        val startX = (width - totalWidth) / 2f
        val centerY = height / 2f

        for (i in 0 until dotCount) {
            val x = startX + i * (dotSpacing + dotRadius * 2)
            if (i == selectedPosition) {
                canvas.drawCircle(x, centerY, selectedDotRadius, selectedPaint)
            } else {
                canvas.drawCircle(x, centerY, dotRadius, unselectedPaint)
            }
        }
    }

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        invalidate() // Redraw view
    }

    fun setDotCount(count: Int) {
        dotCount = count
        invalidate() // Redraw view
    }
}