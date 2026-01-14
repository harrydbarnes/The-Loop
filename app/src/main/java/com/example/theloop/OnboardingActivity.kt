package com.example.theloop

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import com.example.theloop.utils.AppConstants
import com.google.android.material.textfield.TextInputEditText

class OnboardingActivity : AppCompatActivity() {

    companion object {
        private val TAG = OnboardingActivity::class.java.simpleName
    }

    private lateinit var stepViews: Array<View>
    private lateinit var dots: Array<ImageView>
    private lateinit var btnNext: Button
    private lateinit var btnSkip: Button
    private lateinit var nameInput: TextInputEditText
    private var currentStep = 0

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
            // Could auto-advance, but let user click Next
            findViewById<Button>(R.id.btn_grant_location).text = "Permission Granted"
            findViewById<Button>(R.id.btn_grant_location).isEnabled = false
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCalendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Calendar permission granted", Toast.LENGTH_SHORT).show()
            findViewById<Button>(R.id.btn_grant_calendar).text = "Permission Granted"
            findViewById<Button>(R.id.btn_grant_calendar).isEnabled = false
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val healthPermissionLauncher = registerForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.contains(HealthPermission.getReadPermission(StepsRecord::class))) {
             Toast.makeText(this, "Health permission granted", Toast.LENGTH_SHORT).show()
             findViewById<Button>(R.id.btn_grant_health).text = "Permission Granted"
             findViewById<Button>(R.id.btn_grant_health).isEnabled = false
        } else {
             Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        stepViews = arrayOf(
            findViewById(R.id.step_welcome),
            findViewById(R.id.step_location),
            findViewById(R.id.step_calendar),
            findViewById(R.id.step_health),
            findViewById(R.id.step_finish)
        )

        dots = arrayOf(
            findViewById(R.id.dot_0),
            findViewById(R.id.dot_1),
            findViewById(R.id.dot_2),
            findViewById(R.id.dot_3),
            findViewById(R.id.dot_4)
        )

        btnNext = findViewById(R.id.btn_next)
        btnSkip = findViewById(R.id.btn_skip)
        nameInput = findViewById(R.id.name_input)

        btnNext.setOnClickListener {
            if (currentStep == 0) {
                saveName()
            }
            if (currentStep < stepViews.size - 1) {
                currentStep++
                updateUI()
            } else {
                finishOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            if (currentStep < stepViews.size - 1) {
                currentStep++
                updateUI()
            } else {
                finishOnboarding()
            }
        }

        setupPermissionButtons()
        updateUI()
    }

    private fun setupPermissionButtons() {
        findViewById<Button>(R.id.btn_grant_location).setOnClickListener {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        findViewById<Button>(R.id.btn_grant_calendar).setOnClickListener {
            requestCalendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }

        findViewById<Button>(R.id.btn_grant_health).setOnClickListener {
            if (HealthConnectClient.getSdkStatus(this) == HealthConnectClient.SDK_AVAILABLE) {
                val permissions = setOf(
                    HealthPermission.getReadPermission(StepsRecord::class)
                )
                healthPermissionLauncher.launch(permissions)
            } else {
                Toast.makeText(this, "Health Connect not available", Toast.LENGTH_SHORT).show()
                 try {
                     startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata")))
                 } catch (e: Exception) {
                     android.util.Log.e(TAG, "Failed to open Play Store for Health Connect", e)
                 }
            }
        }
    }

    private fun saveName() {
        val name = nameInput.text.toString()
        if (!TextUtils.isEmpty(name)) {
            getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(AppConstants.KEY_USER_NAME, name)
                .apply()
        }
    }

    private fun updateUI() {
        // Update Step Views
        for (i in stepViews.indices) {
            stepViews[i].visibility = if (i == currentStep) View.VISIBLE else View.GONE
        }

        // Update Dots
        for (i in dots.indices) {
            dots[i].setImageResource(if (i == currentStep) R.drawable.dot_active else R.drawable.dot_inactive)
        }

        // Update Buttons
        if (currentStep == stepViews.size - 1) {
            btnNext.text = "Go to Dashboard"
            btnSkip.visibility = View.GONE
        } else {
            btnNext.text = "Next"
            btnSkip.visibility = View.VISIBLE
        }

        if (currentStep == 0) {
            // On the first step, allow skipping name input by making the skip button visible.
            btnSkip.visibility = View.VISIBLE
        }
    }

    private fun finishOnboarding() {
        getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(AppConstants.KEY_ONBOARDING_COMPLETED, true)
            .apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
