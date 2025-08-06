package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import org.tensorflow.lite.examples.objectdetection.utils.DetectionResult

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var results: List<DetectionResult> = listOf()
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val backgroundPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        alpha = 160
    }
    private val bounds = Rect()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (result in results) {
            val boxPaint = Paint().apply {
                color = result.color
                strokeWidth = 6f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }

            val rect = result.boundingBox
            canvas.drawRect(rect, boxPaint)

            // Teks label + confidence
            val label = "${result.label} ${(result.confidence * 100).toInt()}%"
            textPaint.getTextBounds(label, 0, label.length, bounds)

            val textX = rect.left
            val textY = rect.top - 10

            canvas.drawRect(
                textX,
                textY - bounds.height(),
                textX + bounds.width() + 20,
                textY + 10,
                backgroundPaint
            )

            canvas.drawText(label, textX + 10, textY, textPaint)
        }
    }

    fun setResults(
        detectionResults: List<DetectionResult>,
        imageWidth: Int,
        imageHeight: Int
    ) {
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        this.results = detectionResults.map {
            val scaledBox = RectF(
                it.boundingBox.left * scaleX,
                it.boundingBox.top * scaleY,
                it.boundingBox.right * scaleX,
                it.boundingBox.bottom * scaleY
            )
            DetectionResult(it.label, it.confidence, scaledBox, getColorFromLabel(it.label))
        }

        invalidate()
    }

    private fun getColorFromLabel(label: String): Int {
        return when (label.lowercase()) {
            "sehat" -> Color.GREEN
            "kurang_air" -> Color.BLUE
            "kalium" -> Color.YELLOW
            "kalsium" -> Color.CYAN
            "nitrogen" -> Color.MAGENTA
            "busuk" -> Color.RED
            "segar" -> Color.rgb(144, 238, 144) // light green
            else -> Color.WHITE
        }
    }
}