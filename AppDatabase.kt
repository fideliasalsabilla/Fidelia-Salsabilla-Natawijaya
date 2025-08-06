package org.tensorflow.lite.examples.objectdetection.database


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.tensorflow.lite.examples.objectdetection.model.Detection
import org.tensorflow.lite.examples.objectdetection.database.AppDatabase

@Database(entities = [Detection::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun detectionDao(): DetectionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "detection_db"
                ).build().also { instance = it }
            }
        }
    }
}
