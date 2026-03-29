package com.omar.weatherapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omar.weatherapp.data.models.DailyWeather
import com.omar.weatherapp.data.models.weatherCodeToEmoji
import com.omar.weatherapp.ui.theme.*

@Composable
fun DailyForecastList(
    days: List<DailyWeather>,
    tempUnit: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionLabel("7-DAY FORECAST")
        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(WeatherCardBg)
        ) {
            days.forEachIndexed { index, day ->
                DayRow(day = day, tempUnit = tempUnit)
                if (index < days.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = WeatherDivider,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun DayRow(day: DailyWeather, tempUnit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day name
        Text(
            text = day.dayName,
            fontSize = 15.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
            color = WeatherText,
            modifier = Modifier.width(90.dp)
        )

        Spacer(Modifier.weight(1f))

        // Precipitation if any
        if (day.precipProb > 0) {
            Text(
                text = "💧${day.precipProb}%",
                fontSize = 11.sp,
                color = WeatherSubText,
                modifier = Modifier.width(52.dp)
            )
        } else {
            Spacer(Modifier.width(52.dp))
        }

        // Weather emoji
        Text(
            text = weatherCodeToEmoji(day.conditionCode),
            fontSize = 24.sp,
            modifier = Modifier.width(36.dp)
        )

        Spacer(Modifier.width(16.dp))

        // Low/High temps
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${day.minTemp}°",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = WeatherSubText,
                modifier = Modifier.width(36.dp)
            )
            Text(
                text = "${day.maxTemp}°",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = WeatherText,
                modifier = Modifier.width(36.dp)
            )
        }
    }
}
