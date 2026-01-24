package com.example.theloop.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import com.example.theloop.utils.AppConstants

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    val totalSteps = 5

    // Permissions state
    var locationGranted by remember { mutableStateOf(false) }
    var calendarGranted by remember { mutableStateOf(false) }
    var healthGranted by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationGranted = isGranted
        if (isGranted) Toast.makeText(context, "Location permission granted", Toast.LENGTH_SHORT).show()
        else Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        calendarGranted = isGranted
        if (isGranted) Toast.makeText(context, "Calendar permission granted", Toast.LENGTH_SHORT).show()
        else Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    val healthLauncher = rememberLauncherForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.contains(HealthPermission.getReadPermission(StepsRecord::class))) {
            healthGranted = true
            Toast.makeText(context, "Health permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dots
                Row {
                    repeat(totalSteps) { index ->
                        val color = if (index == currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        // Simple dot
                        Surface(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(8.dp),
                            shape = MaterialTheme.shapes.small,
                            color = color
                        ) {}
                    }
                }

                Row {
                    if (currentStep < totalSteps - 1) {
                        TextButton(onClick = {
                            if (currentStep < totalSteps - 1) currentStep++ else onFinish()
                        }) {
                            Text("Skip")
                        }
                    }

                    Button(onClick = {
                        if (currentStep == 0) {
                            // Save Name
                            if (name.isNotEmpty()) {
                                context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                                    .edit()
                                    .putString(AppConstants.KEY_USER_NAME, name)
                                    .apply()
                            }
                        }

                        if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            // Finish
                            context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean(AppConstants.KEY_ONBOARDING_COMPLETED, true)
                                .apply()
                            onFinish()
                        }
                    }) {
                        Text(if (currentStep == totalSteps - 1) "Go to Dashboard" else "Next")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (currentStep) {
                0 -> WelcomeStep(name) { name = it }
                1 -> PermissionStep(
                    title = "Enable Location",
                    description = "We need your location to show local weather.",
                    buttonText = if (locationGranted) "Permission Granted" else "Grant Location",
                    isGranted = locationGranted,
                    onGrant = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                )
                2 -> PermissionStep(
                    title = "Connect Calendar",
                    description = "See your upcoming events on the dashboard.",
                    buttonText = if (calendarGranted) "Permission Granted" else "Grant Calendar",
                    isGranted = calendarGranted,
                    onGrant = { calendarLauncher.launch(Manifest.permission.READ_CALENDAR) }
                )
                3 -> PermissionStep(
                    title = "Connect Health",
                    description = "Track your steps and activity.",
                    buttonText = if (healthGranted) "Permission Granted" else "Grant Health Access",
                    isGranted = healthGranted,
                    onGrant = {
                         if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                            healthLauncher.launch(setOf(HealthPermission.getReadPermission(StepsRecord::class)))
                        } else {
                            Toast.makeText(context, "Health Connect not available", Toast.LENGTH_SHORT).show()
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata")))
                            } catch (e: Exception) {
                                // Log error
                            }
                        }
                    }
                )
                4 -> FinishStep()
            }
        }
    }
}

@Composable
fun WelcomeStep(name: String, onNameChange: (String) -> Unit) {
    Text("Welcome to The Loop", style = MaterialTheme.typography.displaySmall)
    Spacer(Modifier.height(16.dp))
    Text("Your daily dashboard for life.", style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(32.dp))
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("What's your name?") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PermissionStep(title: String, description: String, buttonText: String, isGranted: Boolean, onGrant: () -> Unit) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))
    Text(description, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(32.dp))
    Button(
        onClick = onGrant,
        enabled = !isGranted
    ) {
        Text(buttonText)
    }
}

@Composable
fun FinishStep() {
    Text("All Set!", style = MaterialTheme.typography.displayMedium)
    Spacer(Modifier.height(16.dp))
    Text("You're ready to start using The Loop.", style = MaterialTheme.typography.bodyLarge)
}
