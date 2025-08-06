package org.tensorflow.lite.examples.objectdetection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.objectdetection.databinding.ActivitySavedDetectionsContainerBinding
import org.tensorflow.lite.examples.objectdetection.fragments.SavedDetectionsFragment

class SavedDetectionsContainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedDetectionsContainerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedDetectionsContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, SavedDetectionsFragment())
            .commit()
    }
}
