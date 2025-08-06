package org.tensorflow.lite.examples.objectdetection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.objectdetection.fragments.AboutFragment

class AboutContainerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_container)

        // Menampilkan fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.about_fragment_container, AboutFragment())
            .commit()
    }
}
