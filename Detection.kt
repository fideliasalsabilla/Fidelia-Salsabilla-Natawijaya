package org.tensorflow.lite.examples.objectdetection.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detections")
data class Detection(
    @PrimaryKey(autoGenerate = true)
    var roomId: Int = 0,
    var id: String = "",
    val label: String = "",
    val info: String = "",
    val confidence: Float = 0f,
    val imageUrl: String = "",
    val suggestion: String = "",
    val timestamp: Long = 0L,
    val dateString: String = "",
    val imagePath: String? = null
)
