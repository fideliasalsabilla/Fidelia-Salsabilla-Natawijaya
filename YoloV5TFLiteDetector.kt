package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class YoloV5TFLiteDetector(context: Context) {

    private val interpreter: Interpreter
    private val inputSize = 640
    private val labels: List<String>
    private val numClasses: Int

    init {
        // Load model dan labels
        val model = FileUtil.loadMappedFile(context, "DeteksiCabai.tflite")
        interpreter = Interpreter(model)
        labels = FileUtil.loadLabels(context, "label.txt")
        numClasses = labels.size
    }

    fun detect(inputBuffer: ByteBuffer): List<YoloResult> {
        // Persiapkan buffer output [1,25200,7] -> (x, y, w, h, confidence, class scores...)
        val outputShape = interpreter.getOutputTensor(0).shape() // [1, 25200, 7]
        val outputBuffer = Array(1) { Array(outputShape[1]) { FloatArray(outputShape[2]) } }

        // Jalankan inferensi
        interpreter.run(inputBuffer, outputBuffer)

        // Proses hasil
        val results = mutableListOf<YoloResult>()
        val threshold = 0.25f

        for (i in 0 until outputShape[1]) {
            val row = outputBuffer[0][i]
            val confidence = row[4]

            if (confidence >= threshold) {
                val classScores = row.slice(5 until row.size)
                val (classIndex, classScore) = classScores.withIndex().maxByOrNull { it.value } ?: continue

                if (classScore * confidence > threshold) {
                    val cx = row[0]
                    val cy = row[1]
                    val w = row[2]
                    val h = row[3]
                    val left = cx - w / 2
                    val top = cy - h / 2
                    val right = cx + w / 2
                    val bottom = cy + h / 2

                    results.add(
                        YoloResult(
                            classIndex = classIndex,
                            score = classScore * confidence,
                            boundingBox = RectF(left, top, right, bottom)
                        )
                    )
                }
            }
        }

        return results
    }

    fun getLabel(index: Int): String {
        return labels.getOrElse(index) { "Unknown" }
    }

    fun close() {
        interpreter.close()
    }
}
