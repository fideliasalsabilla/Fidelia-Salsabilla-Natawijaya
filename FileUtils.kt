package org.tensorflow.lite.examples.objectdetection.utils

import android.content.Context
import com.google.gson.Gson
import org.tensorflow.lite.examples.objectdetection.model.Detection
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {

    fun exportDetectionsToJson(context: Context, detections: List<Detection>): File? {
        return try {
            val fileName = "detections_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
            val file = File(context.getExternalFilesDir(null), fileName)

            val gson = Gson()
            val json = gson.toJson(detections)

            FileWriter(file).use { writer ->
                writer.write(json)
            }

            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun exportDetectionsToCsv(context: Context, detections: List<Detection>): File? {
        return try {
            val fileName = "detections_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)

            FileWriter(file).use { writer ->
                // Header
                writer.write("RoomID,ID,Label,Info,Confidence,ImageURL,Suggestion,Timestamp,DateString,ImagePath\n")

                // Data
                for (detection in detections) {
                    writer.write("${detection.roomId},")
                    writer.write("${detection.id},")
                    writer.write("${detection.label},")
                    writer.write("${detection.info},")
                    writer.write("${detection.confidence},")
                    writer.write("${detection.imageUrl},")
                    writer.write("${detection.suggestion},")
                    writer.write("${detection.timestamp},")
                    writer.write("${detection.dateString},")
                    writer.write("\"${detection.imagePath ?: ""}\"\n")
                }
            }

            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
