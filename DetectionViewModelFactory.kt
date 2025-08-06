package org.tensorflow.lite.examples.objectdetection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.tensorflow.lite.examples.objectdetection.database.DetectionDao

class DetectionViewModelFactory(private val dao: DetectionDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetectionViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}