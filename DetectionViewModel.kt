package org.tensorflow.lite.examples.objectdetection.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.objectdetection.model.Detection
import org.tensorflow.lite.examples.objectdetection.database.DetectionDao
import org.tensorflow.lite.examples.objectdetection.database.DetectionDatabase

class DetectionViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: DetectionDao

    init {
        val database = DetectionDatabase.getDatabase(application)
        dao = database.detectionDao()
    }

    fun insert(detectionResult: Detection) {
        viewModelScope.launch {
            dao.insert(detectionResult)
        }
    }

    fun getAllResults(): LiveData<List<Detection>> {
        return dao.getAllDetections()
    }

    fun countByLabel(label: String): LiveData<Int> {
        return dao.countByLabel(label)
    }

    fun count(): LiveData<Int> {
        return dao.count()
    }

    fun deleteAll() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }

    fun deleteResult(detectionResult: Detection) {
        viewModelScope.launch {
            dao.delete(detectionResult)
        }
    }
}