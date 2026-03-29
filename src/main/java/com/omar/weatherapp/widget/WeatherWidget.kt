package com.omar.weatherapp.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.omar.weatherapp.data.WeatherRepository
import com.omar.weatherapp.data.prefs.AppPreferences
import kotlinx.coroutines.flow.first

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = AppPreferences(context)
        val repository = WeatherRepository()

        val lat = prefs.selectedLatitude.first()?.toDouble()
        val lon = prefs.selectedLongitude.first()?.toDouble()
        val name = prefs.locationName.first()
        val tUnit = prefs.tempUnit.first()

        val weatherResult = if (lat != null && lon != null) {
            repository.fetchWeather(lat, lon, tUnit)
        } else {
            null
        }

        provideContent {
            val state = weatherResult?.getOrNull()
            
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF121212)))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state != null) {
                    Text(
                        text = name,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    
                    Spacer(GlanceModifier.height(8.dp))
                    
                    Text(
                        text = "${state.currentTemp}°",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Text(
                        text = state.condition,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFE8372A)),
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Text(
                        text = "No Weather Data",
                        style = TextStyle(color = ColorProvider(Color.Gray))
                    )
                }
            }
        }
    }
}
