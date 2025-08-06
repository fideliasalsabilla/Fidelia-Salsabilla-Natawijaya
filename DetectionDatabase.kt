package org.tensorflow.lite.examples.objectdetection.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.tensorflow.lite.examples.objectdetection.model.Detection

@Database(
    entities = [Detection::class],
    version =2,
    exportSchema = false
)
abstract class DetectionDatabase : RoomDatabase() {

    abstract fun detectionDao(): DetectionDao

    companion object {
        @Volatile
        private var INSTANCE: DetectionDatabase? = null

        fun getDatabase(context: Context): DetectionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DetectionDatabase::class.java,
                    "detection_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}