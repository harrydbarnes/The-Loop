package com.example.theloop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.theloop.models.CalendarEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CalendarCard(
    events: List<CalendarEvent>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(text = "Agenda", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (events.isEmpty()) {
                Text("No upcoming events")
            } else {
                events.forEach { event ->
                    EventItem(event)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun EventItem(event: CalendarEvent) {
    Row(Modifier.padding(vertical = 8.dp)) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        Text(
            text = timeFormat.format(Date(event.startTime)),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp)
        )
        Column {
            Text(text = event.title, style = MaterialTheme.typography.bodyLarge)
            if (!event.location.isNullOrEmpty()) {
                Text(text = event.location, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
