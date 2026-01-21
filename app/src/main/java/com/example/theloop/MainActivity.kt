package com.example.theloop

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.theloop.ui.HomeScreen
import com.example.theloop.ui.theme.TheLoopTheme
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions granted/denied. ViewModel might reload if needed.
        val anyGranted = permissions.any { it.value }
        val anyDenied = permissions.any { !it.value }

        if (anyGranted) {
            fetchLocation()
        }

        if (anyDenied) {
            Toast.makeText(
                this,
                "Permissions denied. Some features (Weather, Calendar) may be unavailable.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.refreshAll()
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                viewModel.updateLocation(location.latitude, location.longitude)
            } else {
                viewModel.refreshAll()
            }
        }.addOnFailureListener {
            viewModel.refreshAll()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_CALENDAR
            )
        )

        setContent {
            TheLoopTheme {
                HomeScreen()
            }
        }
    }
}
