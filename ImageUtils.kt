@file:OptIn(androidx.camera.core.ExperimentalGetImage::class)
package org.tensorflow.lite.examples.objectdetection.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class ImageUtils {
    companion object {
        private var yuvToRgbConverter: YuvToRgbConverter? = null

        fun imageProxyToBitmap(imageProxy: ImageProxy, context: Context): Bitmap {
            val image = imageProxy.image ?: throw IllegalArgumentException("Image is null")

            val converter = yuvToRgbConverter ?: YuvToRgbConverter(context).also {
                yuvToRgbConverter = it
            }

            val bitmap = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Config.ARGB_8888
            )

            converter.yuvToRgb(image, bitmap)
            return bitmap
        }

        fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
            val matrix = Matrix().apply { postRotate(degrees) }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
            return Bitmap.createScaledBitmap(bitmap, width, height, true)
        }

        fun bitmapToByteBuffer(bitmap: Bitmap, inputSize: Int): ByteBuffer {
            val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
            byteBuffer.order(java.nio.ByteOrder.nativeOrder())

            val intValues = IntArray(inputSize * inputSize)
            bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            var pixel = 0
            for (i in 0 until inputSize) {
                for (j in 0 until inputSize) {
                    val pixelValue = intValues[pixel++]
                    byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f) // R
                    byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)  // G
                    byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)         // B
                }
            }

            return byteBuffer
        }

        fun preprocessBitmapForYolo(bitmap: Bitmap, inputSize: Int): Bitmap {
            return resizeBitmap(bitmap, inputSize, inputSize)
        }

        fun release() {
            yuvToRgbConverter?.destroy()
            yuvToRgbConverter = null
        }
    }
}