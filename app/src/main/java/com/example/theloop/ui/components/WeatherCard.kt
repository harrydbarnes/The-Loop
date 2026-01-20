package com.example.theloop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.theloop.models.WeatherResponse
import com.example.theloop.utils.AppUtils
import com.example.theloop.R

@Composable
fun WeatherCard(
    weather: WeatherResponse?,
    locationName: String,
    tempUnit: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        if (weather == null) {
            Box(Modifier.padding(16.dp)) {
                Text("Loading Weather...")
            }
        } else {
            val current = weather.current
            val description = stringResource(AppUtils.getWeatherDescription(current.weatherCode))
            val iconRes = AppUtils.getWeatherIconResource(current.weatherCode)
            val tempSymbol = if (tempUnit == "celsius") "°C" else "°F"

            Column(Modifier.padding(16.dp)) {
                Text(text = locationName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = description,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "${Math.round(current.temperature)}$tempSymbol",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = description, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
