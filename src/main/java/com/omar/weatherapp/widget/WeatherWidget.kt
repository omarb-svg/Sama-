package com.omar.weatherapp.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.omar.weatherapp.MainActivity
import com.omar.weatherapp.data.WeatherRepository
import com.omar.weatherapp.data.models.calculateMoonPhaseAbbr
import com.omar.weatherapp.data.models.windDirectionToText
import com.omar.weatherapp.data.prefs.AppPreferences
import kotlinx.coroutines.flow.first

class WeatherWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs      = AppPreferences(context)
        val repository = WeatherRepository()

        val lat   = prefs.selectedLatitude.first()?.toDouble()
        val lon   = prefs.selectedLongitude.first()?.toDouble()
        val name  = prefs.locationName.first()
        val tUnit = prefs.tempUnit.first()
        val wUnit = prefs.windUnit.first()

        val state = if (lat != null && lon != null) {
            repository.fetchWeather(lat, lon, tUnit, wUnit).getOrNull()
        } else null

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFFF0EDE8)))
                    .cornerRadius(20.dp)
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state != null) {
                    Column(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Location — small caps
                        Text(
                            text = name.uppercase(),
                            style = TextStyle(
                                color     = ColorProvider(Color(0xFF888888)),
                                fontSize  = 9.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(GlanceModifier.height(4.dp))

                        // Giant temperature
                        Text(
                            text = "${state.currentTemp}",
                            style = TextStyle(
                                color     = ColorProvider(Color(0xFF444444)),
                                fontSize  = 52.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )

                        // Condition label
                        Text(
                            text = state.condition.uppercase(),
                            style = TextStyle(
                                color     = ColorProvider(Color(0xFF333333)),
                                fontSize  = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(GlanceModifier.height(6.dp))

                        // Mini stats row: wind + moon
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${state.windSpeed.toInt()}${state.windUnit} ${windDirectionToText(state.windDirection)}",
                                style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 9.sp)
                            )
                            Spacer(GlanceModifier.width(10.dp))
                            Text(
                                text = state.moonPhase.ifBlank { calculateMoonPhaseAbbr() },
                                style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 9.sp)
                            )
                        }
                    }
                } else {
                    // No data state
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SAMĀ'",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFAAAAAA)),
                                fontSize = 11.sp, fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = "Tap to load",
                            style = TextStyle(color = ColorProvider(Color(0xFFBBBBBB)), fontSize = 10.sp)
                        )
                    }
                }
            }
        }
    }
}
