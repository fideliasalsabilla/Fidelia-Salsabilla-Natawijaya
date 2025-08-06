package org.tensorflow.lite.examples.objectdetection

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import org.tensorflow.lite.examples.objectdetection.databinding.ActivityMainBinding
import androidx.navigation.ui.AppBarConfiguration


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate started")

        try {
            // Initialize view binding
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Log.d(TAG, "View binding initialized successfully")

            // Setup toolbar
            setupToolbar()

            // Setup navigation
            setupNavigation()

            // Setup bottom navigation
            setupBottomNavigation()

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            Log.e(TAG, "Stack trace: ", e)
        }
    }

    private fun setupToolbar() {
        try {
            setSupportActionBar(binding.toolbar)
            Log.d(TAG, "Toolbar setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up toolbar: ${e.message}")
        }
    }

    private fun setupNavigation() {
        try {
            // Get NavHostFragment
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

            navController = navHostFragment.navController

            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.cameraFragment,
                    R.id.historyFragment,
                    R.id.statisticsFragment,
                    R.id.aboutFragment
                )
            )

            // Hubungkan Toolbar dengan NavController dan konfigurasi
            setupActionBarWithNavController(navController, appBarConfiguration)

            Log.d(TAG, "Navigation setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up navigation: ${e.message}")
        }
    }

    private fun setupBottomNavigation() {
        try {
            // Connect Bottom Navigation with NavController
            binding.bottomNavigation.setupWithNavController(navController)
            Log.d(TAG, "Bottom navigation setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom navigation: ${e.message}")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}