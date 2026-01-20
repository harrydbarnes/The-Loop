package com.example.theloop.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.theloop.MainViewModel
import com.example.theloop.ui.components.CalendarCard
import com.example.theloop.ui.components.NewsCard
import com.example.theloop.ui.components.WeatherCard

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding)
        ) {
            item {
                Text(
                    text = state.userGreeting,
                    style = MaterialTheme.typography.displayLarge
                )
            }
            if (state.summary != null) {
                item {
                    Text(
                        text = state.summary ?: "",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            item {
                WeatherCard(
                    weather = state.weather,
                    locationName = state.locationName,
                    tempUnit = state.tempUnit,
                    onClick = { /* TODO: Implement navigation to settings */ }
                )
            }

            if (state.calendarEvents.isNotEmpty()) {
                item {
                    CalendarCard(events = state.calendarEvents)
                }
            }

            if (state.funFact != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                         Column(Modifier.padding(16.dp)) {
                             Text("Did you know?", style = MaterialTheme.typography.titleSmall)
                             Spacer(Modifier.height(4.dp))
                             Text(state.funFact ?: "")
                         }
                    }
                }
            }

            item {
                Text("Headlines", style = MaterialTheme.typography.headlineSmall)
            }

            items(state.newsArticles) { article ->
                NewsCard(
                    article = article,
                    onClick = {
                        article.url?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Handle exception
                            }
                        }
                    }
                )
            }
        }
    }
}
