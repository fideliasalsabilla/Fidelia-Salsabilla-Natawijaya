package org.tensorflow.lite.examples.objectdetection

// Data class untuk hasil deteksi yang kompatibel dengan overlay
data class YoloResult(
    val classIndex: Int,
    val score: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val rect: android.graphics.RectF
        get() = android.graphics.RectF(left, top, right, bottom)

    // Constructor tambahan untuk kompatibilitas
    constructor(
        classIndex: Int,
        score: Float,
        boundingBox: android.graphics.RectF
    ) : this(
        classIndex = classIndex,
        score = score,
        left = boundingBox.left,
        top = boundingBox.top,
        right = boundingBox.right,
        bottom = boundingBox.bottom
    )
}