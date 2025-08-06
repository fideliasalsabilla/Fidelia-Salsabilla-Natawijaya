package org.tensorflow.lite.examples.objectdetection.tflite

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.examples.objectdetection.utils.DetectionUtils
import org.tensorflow.lite.examples.objectdetection.utils.DetectionResult
import org.tensorflow.lite.examples.objectdetection.utils.ImageUtils
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TensorFlowLiteHelper(
    private val context: Context,
    private val modelPath: String,
    private val labels: List<String>,
    private val inputWidth: Int = 640,
    private val inputHeight: Int = 640,
    private val useGPU: Boolean = false,
    private val numThreads: Int = 4
) {

    private var interpreter: Interpreter? = null
    private var imageProcessor: ImageProcessor? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "TensorFlowLiteHelper"
    }

    /**
     * Initialize TensorFlow Lite interpreter
     */
    fun initialize(): Boolean {
        return try {
            // Load model
            val modelBuffer = loadModelFile()

            // Create interpreter options
            val options = Interpreter.Options().apply {
                setNumThreads(numThreads)
            }

            // Create interpreter
            interpreter = Interpreter(modelBuffer, options)

            // Setup image processor
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputHeight, inputWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))
                .build()

            isInitialized = true
            Log.d(TAG, "TensorFlow Lite initialized successfully")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TensorFlow Lite", e)
            false
        }
    }

    /**
     * Load model file from assets
     */
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Run inference on input bitmap
     */
    fun runInference(
        bitmap: Bitmap,
        confidenceThreshold: Float = 0.5f,
        iouThreshold: Float = 0.5f
    ): List<DetectionResult>? {

        if (!isInitialized) {
            Log.e(TAG, "TensorFlow Lite not initialized")
            return null
        }

        return try {
            // Preprocess image
            val resizedBitmap = ImageUtils.resizeBitmap(bitmap, inputWidth, inputHeight)
            val tensorImage = TensorImage.fromBitmap(resizedBitmap)
            val processedImage = imageProcessor?.process(tensorImage) ?: return null

            // Prepare input buffer
            val inputBuffer = processedImage.buffer

            // Get model input/output details
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            val outputShape = interpreter?.getOutputTensor(0)?.shape()

            Log.d(TAG, "Input shape: ${inputShape?.contentToString()}")
            Log.d(TAG, "Output shape: ${outputShape?.contentToString()}")

            // Prepare output buffer based on model output shape
            val outputBuffer = when {
                outputShape != null && outputShape.size >= 3 -> {
                    Array(outputShape[0]) {
                        Array(outputShape[1]) {
                            FloatArray(outputShape[2])
                        }
                    }
                }
                else -> {
                    // Default YOLO output format
                    Array(1) { Array(25200) { FloatArray(labels.size + 5) } }
                }
            }

            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)

            // Post-process results
            val rawDetections = DetectionUtils.parseYoloOutput(
                outputBuffer,
                labels,
                confidenceThreshold = confidenceThreshold,
                inputWidth = inputWidth,
                inputHeight = inputHeight
            )

            // Apply NMS to remove overlapping detections
            val filteredDetections = DetectionUtils.applyNMS(
                rawDetections,
                iouThreshold = iouThreshold,
                scoreThreshold = confidenceThreshold
            )

            // Scale bounding boxes to original image size
            val scaledDetections = DetectionUtils.scaleBoundingBoxes(
                filteredDetections,
                inputWidth,
                inputHeight,
                bitmap.width,
                bitmap.height
            )

            Log.d(TAG, "Detected ${scaledDetections.size} objects")
            scaledDetections

        } catch (e: Exception) {
            Log.e(TAG, "Error running inference", e)
            null
        }
    }

    /**
     * Get model input size
     */
    fun getInputSize(): Pair<Int, Int> {
        return Pair(inputWidth, inputHeight)
    }

    /**
     * Get model information
     */
    fun getModelInfo(): String {
        return if (isInitialized) {
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            val outputShape = interpreter?.getOutputTensor(0)?.shape()
            """
            Model: $modelPath
            Input shape: ${inputShape?.contentToString()}
            Output shape: ${outputShape?.contentToString()}
            Labels: ${labels.size}
            GPU: ${if (useGPU) "Enabled" else "Disabled"}
            Threads: $numThreads
            """.trimIndent()
        } else {
            "Model not initialized"
        }
    }

    /**
     * Check if TensorFlow Lite is initialized
     */
    fun isInitialized(): Boolean {
        return isInitialized
    }

    /**
     * Clean up resources
     */
    fun close() {
        try {
            interpreter?.close()
            isInitialized = false
            Log.d(TAG, "TensorFlow Lite resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up TensorFlow Lite resources", e)
        }
    }
}