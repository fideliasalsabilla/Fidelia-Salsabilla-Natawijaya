package org.tensorflow.lite.examples.objectdetection

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.objectdetection.databinding.ActivityBerandaBinding
import android.view.View
import org.tensorflow.lite.examples.objectdetection.fragments.AboutFragment

class BerandaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBerandaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBerandaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tombol Mulai Deteksi Kamera
        binding.btnDeteksi.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Tombol Riwayat Deteksi
        binding.btnRiwayat.setOnClickListener {
            val intent = Intent(this, SavedDetectionsContainerActivity::class.java)
            startActivity(intent)
        }

        binding.btnTentang.setOnClickListener {
            val intent = Intent(this, AboutContainerActivity::class.java)
            startActivity(intent)
        }
    }
}