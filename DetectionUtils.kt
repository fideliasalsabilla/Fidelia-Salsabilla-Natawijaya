package org.tensorflow.lite.examples.objectdetection.utils

import android.graphics.RectF
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import android.graphics.Color

data class DetectionResult(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF,
    var color: Int = Color.GRAY
)

object DetectionUtils {

    /**
     * Applies Non-Maximum Suppression (NMS) to remove overlapping bounding boxes
     */
    fun applyNMS(
        detections: List<DetectionResult>,
        iouThreshold: Float = 0.5f,
        scoreThreshold: Float = 0.3f
    ): List<DetectionResult> {

        // Filter detections by confidence score
        val filteredDetections = detections.filter { it.confidence >= scoreThreshold }

        if (filteredDetections.isEmpty()) return emptyList()

        // Sort by confidence in descending order
        val sortedDetections = filteredDetections.sortedByDescending { it.confidence }

        val finalDetections = mutableListOf<DetectionResult>()
        val suppressed = BooleanArray(sortedDetections.size)

        for (i in sortedDetections.indices) {
            if (suppressed[i]) continue

            finalDetections.add(sortedDetections[i])

            // Suppress overlapping detections
            for (j in i + 1 until sortedDetections.size) {
                if (suppressed[j]) continue

                val iou = calculateIoU(sortedDetections[i].boundingBox, sortedDetections[j].boundingBox)
                if (iou > iouThreshold) {
                    suppressed[j] = true
                }
            }
        }

        return finalDetections
    }

    /**
     * Calculates Intersection over Union (IoU) between two bounding boxes
     */
    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectionLeft = max(box1.left, box2.left)
        val intersectionTop = max(box1.top, box2.top)
        val intersectionRight = min(box1.right, box2.right)
        val intersectionBottom = min(box1.bottom, box2.bottom)

        val intersectionWidth = max(0f, intersectionRight - intersectionLeft)
        val intersectionHeight = max(0f, intersectionBottom - intersectionTop)
        val intersectionArea = intersectionWidth * intersectionHeight

        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)
        val unionArea = box1Area + box2Area - intersectionArea

        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }

    /**
     * Converts YOLO output format to DetectionResult objects
     */
    fun parseYoloOutput(
        outputArray: Array<Array<FloatArray>>,
        labels: List<String>,
        confidenceThreshold: Float = 0.5f,
        inputWidth: Int = 640,
        inputHeight: Int = 640
    ): List<DetectionResult> {
        val detections = mutableListOf<DetectionResult>()

        try {
            val predictions = outputArray[0] // Shape: [1, 25200, 85] for YOLO

            for (i in predictions.indices) {
                val prediction = predictions[i]

                // YOLO format: [x, y, w, h, objectness, class1, class2, ...]
                val x = prediction[0]
                val y = prediction[1]
                val width = prediction[2]
                val height = prediction[3]
                val objectness = prediction[4]

                // Skip if objectness is too low
                if (objectness < confidenceThreshold) continue

                // Get class scores
                val classScores = prediction.sliceArray(5 until prediction.size)

                // Find the class with highest score
                var maxScore = 0f
                var maxIndex = 0

                for (j in classScores.indices) {
                    val score = classScores[j] * objectness // Multiply by objectness
                    if (score > maxScore) {
                        maxScore = score
                        maxIndex = j
                    }
                }

                // Skip if confidence is too low
                if (maxScore < confidenceThreshold) continue

                // Convert center coordinates to corner coordinates
                val left = x - width / 2f
                val top = y - height / 2f
                val right = x + width / 2f
                val bottom = y + height / 2f

                // Create bounding box
                val boundingBox = RectF(left, top, right, bottom)
                val label = if (maxIndex < labels.size) labels[maxIndex] else "Unknown"

                val color = when (label) {
                    "Sehat" -> Color.GREEN
                    "Kurang_Air" -> Color.BLUE
                    "Kalium" -> Color.rgb(255, 165, 0)
                    "Kalsium" -> Color.MAGENTA
                    "Nitrogen" -> Color.CYAN
                    else -> Color.GRAY
                }

                detections.add(
                    DetectionResult(
                        label = label,
                        confidence = maxScore,
                        boundingBox = boundingBox
                    )
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return detections
    }

    /**
     * Scales bounding boxes from model input size to actual image size
     */
    fun scaleBoundingBoxes(
        detections: List<DetectionResult>,
        modelInputWidth: Int,
        modelInputHeight: Int,
        actualWidth: Int,
        actualHeight: Int
    ): List<DetectionResult> {
        val scaleX = actualWidth.toFloat() / modelInputWidth
        val scaleY = actualHeight.toFloat() / modelInputHeight

        return detections.map { detection ->
            val scaledBox = RectF(
                detection.boundingBox.left * scaleX,
                detection.boundingBox.top * scaleY,
                detection.boundingBox.right * scaleX,
                detection.boundingBox.bottom * scaleY
            )

            detection.copy(boundingBox = scaledBox)
        }
    }

    /**
     * Applies sigmoid activation function
     */
    fun sigmoid(x: Float): Float {
        return 1f / (1f + exp(-x))
    }

    /**
     * Applies softmax activation function
     */
    fun softmax(values: FloatArray): FloatArray {
        val maxValue = values.maxOrNull() ?: 0f
        val expValues = values.map { exp(it - maxValue) }
        val sumExp = expValues.sum()
        return expValues.map { (it / sumExp).toFloat() }.toFloatArray()
    }

    /**
     * Filters detections by confidence threshold
     */
    fun filterByConfidence(
        detections: List<DetectionResult>,
        threshold: Float
    ): List<DetectionResult> {
        return detections.filter { it.confidence >= threshold }
    }

    /**
     * Groups detections by label
     */
    fun groupByLabel(detections: List<DetectionResult>): Map<String, List<DetectionResult>> {
        return detections.groupBy { it.label }
    }

    /**
     * Gets the best detection (highest confidence) for each label
     */
    fun getBestDetectionPerLabel(detections: List<DetectionResult>): List<DetectionResult> {
        return detections.groupBy { it.label }
            .values
            .mapNotNull { group -> group.maxByOrNull { it.confidence } }
    }
}