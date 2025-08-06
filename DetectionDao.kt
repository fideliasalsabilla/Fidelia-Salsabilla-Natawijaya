package org.tensorflow.lite.examples.objectdetection.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import org.tensorflow.lite.examples.objectdetection.model.Detection
import androidx.lifecycle.LiveData

@Dao
interface DetectionDao {

    @Insert
    suspend fun insert(detection: Detection)

    @Query("SELECT * FROM detections ORDER BY timestamp DESC")
    fun getAllDetections(): LiveData<List<Detection>>

    @Query("SELECT * FROM detections WHERE label = :label ORDER BY timestamp DESC")
    fun getDetectionsByLabel(label: String): LiveData<List<Detection>>

    @Query("SELECT COUNT(*) FROM detections")
    fun getDetectionCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM detections")
    fun count(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM detections WHERE label = :label")
    fun countByLabel(label: String): LiveData<Int>

    @Query("SELECT * FROM detections WHERE roomId = :id")
    suspend fun getDetectionById(id: Int): Detection?

    @Query("SELECT * FROM detections ORDER BY timestamp DESC")
    fun getAllDetectionsLive(): LiveData<List<Detection>>

    @Query("DELETE FROM detections WHERE roomId = :id")
    suspend fun deleteDetectionById(id: Int)

    @Delete
    suspend fun deleteDetection(detection: Detection)

    @Update
    suspend fun updateDetection(detection: Detection)

    @Query("DELETE FROM detections")
    suspend fun deleteAllDetections(): Int

    @Query("DELETE FROM detections")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(detection: Detection)
}