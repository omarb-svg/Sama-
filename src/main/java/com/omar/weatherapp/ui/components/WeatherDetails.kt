package com.omar.weatherapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omar.weatherapp.data.models.WeatherUiState
import com.omar.weatherapp.data.models.windDirectionToText
import com.omar.weatherapp.ui.theme.*

@Composable
fun WeatherDetails(state: WeatherUiState, modifier: Modifier = Modifier) {
    val unit  = "°${state.tempUnit}"
    val wUnit = state.windUnit

    Column(modifier = modifier) {
        SectionLabel("CONDITIONS")
        Spacer(Modifier.height(10.dp))

        // 2-column grid
        val items = buildList {
            add("Feels Like"  to "${state.feelsLike}$unit")
            add("Humidity"    to "${state.humidity}%")
            add("Wind"        to "${state.windSpeed.toInt()} $wUnit ${windDirectionToText(state.windDirection)}")
            add("UV Index"    to uvLabel(state.uvIndex))
            if (state.sunrise.isNotBlank()) add("Sunrise" to state.sunrise)
            if (state.sunset.isNotBlank())  add("Sunset"  to state.sunset)
        }

        val rows = (items.size + 1) / 2
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val left  = items.getOrNull(row * 2)
                val right = items.getOrNull(row * 2 + 1)
                if (left  != null) DetailCard(left.first,  left.second,  Modifier.weight(1f))
                if (right != null) DetailCard(right.first, right.second, Modifier.weight(1f))
                    else Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DetailCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(WeatherCardBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = WeatherSubText,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = WeatherText
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = WeatherSubText,
        letterSpacing = 2.sp
    )
}

private fun uvLabel(uv: Double): String {
    val level = when {
        uv < 3  -> "Low"
        uv < 6  -> "Moderate"
        uv < 8  -> "High"
        uv < 11 -> "Very High"
        else    -> "Extreme"
    }
    return "${String.format("%.1f", uv)} • $level"
}
