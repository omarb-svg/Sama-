package com.omar.weatherapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omar.weatherapp.data.models.HourlyWeather
import com.omar.weatherapp.data.models.weatherCodeToEmoji
import com.omar.weatherapp.ui.theme.*

@Composable
fun HourlyForecastRow(
    hours: List<HourlyWeather>,
    tempUnit: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionLabel("HOURLY FORECAST")
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            hours.forEach { hour ->
                HourlyItem(hour = hour, tempUnit = tempUnit)
            }
        }
    }
}

@Composable
private fun HourlyItem(hour: HourlyWeather, tempUnit: String) {
    val isSelected = hour.isCurrent
    val bgColor    = if (isSelected) WeatherAccentBlue else WeatherHourBg
    val textColor  = if (isSelected) Color.White else WeatherText
    val subColor   = if (isSelected) Color.White.copy(alpha = 0.75f) else WeatherSubText

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .width(72.dp)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Hour label
        Text(
            text = hour.hour,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = subColor
        )

        // Weather emoji
        Text(
            text = weatherCodeToEmoji(hour.conditionCode),
            fontSize = 22.sp
        )

        // Temperature
        Text(
            text = "${hour.temp}°",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        // Feels like
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Feels",
                fontSize = 9.sp,
                color = subColor
            )
            Text(
                text = "${hour.feelsLike}°",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = subColor
            )
        }

        // Precipitation probability
        if (hour.precipProb > 0) {
            Text(
                text = "💧 ${hour.precipProb}%",
                fontSize = 10.sp,
                color = subColor
            )
        }
    }
}
