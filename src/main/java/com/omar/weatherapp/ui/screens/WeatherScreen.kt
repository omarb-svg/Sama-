package com.omar.weatherapp.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.omar.weatherapp.WeatherViewModel
import com.omar.weatherapp.data.models.WeatherUiState
import com.omar.weatherapp.ui.components.*
import com.omar.weatherapp.ui.theme.*

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Haptic feedback when weather data loads
    LaunchedEffect(state.currentTemp, state.conditionCode) {
        if (!state.isLoading && state.error == null && state.currentTemp != 0) {
            triggerHaptic(context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WeatherBackground)
    ) {
        if (state.isLoading) {
            LoadingView()
        } else if (state.error != null) {
            ErrorView(
                message = state.error!!,
                onRetry = { viewModel.refresh() },
                onSettings = onNavigateToSettings
            )
        } else {
            WeatherContent(
                state = state,
                onNavigateToSettings = onNavigateToSettings,
                onRefresh = { viewModel.refresh() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeatherContent(
    state: WeatherUiState,
    onNavigateToSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Fade hero out as user scrolls
    val heroAlpha by remember(scrollState.value) {
        derivedStateOf {
            val maxScroll = 600f
            (1f - scrollState.value / maxScroll).coerceIn(0f, 1f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .statusBarsPadding()
    ) {
        // ── Top bar ──────────────────────────────────────────────────────
        TopBar(
            locationName = state.locationName,
            isCurrentLocation = state.isCurrentLocation,
            onSettings = onNavigateToSettings,
            onRefresh = onRefresh,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        // ── Hero: Cloud artwork + temperature ────────────────────────────
        HeroSection(
            state = state,
            modifier = Modifier.alpha(heroAlpha)
        )

        Spacer(Modifier.height(24.dp))

        // ── Scrollable content below the hero ────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            WeatherDetails(state = state)

            if (state.hourlyForecast.isNotEmpty()) {
                HourlyForecastRow(
                    hours = state.hourlyForecast,
                    tempUnit = state.tempUnit
                )
            }

            if (state.dailyForecast.isNotEmpty()) {
                DailyForecastList(
                    days = state.dailyForecast,
                    tempUnit = state.tempUnit
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TopBar(
    locationName: String,
    isCurrentLocation: Boolean,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Location name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = locationName.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = WeatherSubText,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.width(6.dp))
            if (isCurrentLocation) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(WeatherAccentBlue)
                )
            }
        }

        // Refresh button
        IconButton(onClick = onRefresh, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "Refresh",
                tint = WeatherSubText,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(4.dp))

        // Settings button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(WeatherSurface)
                .clickable { onSettings() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = WeatherSubText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroSection(state: WeatherUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cloud artwork (animated inside CloudArtwork)
        CloudArtwork(
            conditionCode = state.conditionCode,
            isDay = state.isDay,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Giant 3D-style temperature number
        AnimatedTemperature(temp = state.currentTemp, unit = state.tempUnit)

        Spacer(Modifier.height(4.dp))

        // Condition label — bold, spaced, no extra bar
        Text(
            text = state.condition.uppercase(),
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = WeatherText
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AnimatedTemperature(temp: Int, unit: String) {
    val animatedTemp by animateIntAsState(
        targetValue = temp,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "temp_anim"
    )

    Box(contentAlignment = Alignment.Center) {
        // Shadow/3D layer (offset behind)
        Text(
            text = "$animatedTemp°",
            style = TextStyle(
                fontSize = 140.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF2A2A2A).copy(alpha = 0.35f),
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.4f),
                    offset = Offset(6f, 10f),
                    blurRadius = 0f
                )
            ),
            modifier = Modifier.offset(x = 5.dp, y = 8.dp)
        )
        // Main text
        Text(
            text = "$animatedTemp°",
            style = TextStyle(
                fontSize = 140.sp,
                fontWeight = FontWeight.Black,
                color = WeatherText,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.15f),
                    offset = Offset(3f, 6f),
                    blurRadius = 8f
                )
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = WeatherText,
                strokeWidth = 2.dp,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "FETCHING WEATHER",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                color = WeatherSubText
            )
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚠️", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 15.sp,
                color = WeatherSubText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onSettings) { Text("Settings") }
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = WeatherText)) {
                    Text("Retry", color = WeatherBackground)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
private fun triggerHaptic(context: android.content.Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 30, 60, 20),
                    intArrayOf(0, 180, 0, 100),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (_: Exception) {}
}
