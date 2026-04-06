package com.omar.weatherapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omar.weatherapp.WeatherViewModel
import com.omar.weatherapp.data.models.DailyWeather
import com.omar.weatherapp.data.models.GeoLocation
import com.omar.weatherapp.data.models.WeatherUiState
import com.omar.weatherapp.data.models.windDirectionToText
import com.omar.weatherapp.ui.components.CloudArtwork
import kotlin.math.roundToInt
import kotlin.math.sin

// ── Kinetic Display Mode ──────────────────────────────────────────────────────
private enum class PoppedStat { NONE, HUMIDITY, DEW, FEELS, PRESSURE, UV, VISIBILITY }

// ── Entry Point ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    onNavigateToSettings: () -> Unit
) {
    val state            by viewModel.uiState.collectAsState()
    val selectedHourIndex by viewModel.selectedHourIndex.collectAsState()
    val searchResults    by viewModel.searchResults.collectAsState()
    val isSearching      by viewModel.isSearching.collectAsState()

    var showLocationSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KineticBg)
    ) {
        when {
            state.isLoading -> KineticLoadingView()
            state.error != null -> KineticErrorView(
                message = state.error!!,
                onRetry = { viewModel.refresh() },
                onSettings = onNavigateToSettings
            )
            else -> KineticMainLayout(
                state = state,
                selectedHourIndex = selectedHourIndex,
                onScrub = { viewModel.scrubBy(it) },
                onResetTimeline = { viewModel.resetTimeline() },
                onNavigateToSettings = onNavigateToSettings,
                onRefresh = { viewModel.refresh() },
                onLocationTap = { showLocationSheet = true }
            )
        }
    }

    // ── Location Search Bottom Sheet ──────────────────────────────────────
    if (showLocationSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showLocationSheet = false
                viewModel.clearSearch()
            },
            containerColor = KineticBg,
            tonalElevation = 0.dp
        ) {
            KineticLocationSheet(
                searchResults = searchResults,
                isSearching = isSearching,
                onQuery = { viewModel.searchLocations(it) },
                onSelect = { loc ->
                    viewModel.selectLocation(loc.latitude, loc.longitude, loc.displayName)
                    showLocationSheet = false
                    viewModel.clearSearch()
                },
                onDismiss = {
                    showLocationSheet = false
                    viewModel.clearSearch()
                }
            )
        }
    }
}

// ── Color Palette ─────────────────────────────────────────────────────────────
private val KineticBg      = Color(0xFFF0EDE8)
private val KineticText    = Color(0xFF111111)
private val KineticSub     = Color(0xFF666666)
private val KineticSubDim  = Color(0xFF999999)
private val KineticRed     = Color(0xFFD42B1E)
private val KineticLine    = Color(0xFF888888)
private val KineticTempGray = Color(0xFF7A7A7A)

// ── Main Kinetic Layout ───────────────────────────────────────────────────────
@Composable
private fun KineticMainLayout(
    state: WeatherUiState,
    selectedHourIndex: Int,
    onScrub: (Int) -> Unit,
    onResetTimeline: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRefresh: () -> Unit,
    onLocationTap: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var poppedStat by remember { mutableStateOf(PoppedStat.NONE) }
    var dragAccum by remember { mutableFloatStateOf(0f) }

    // ── Derive display values from selected hour or current ───────────────
    val hour = state.hourlyForecast.getOrNull(selectedHourIndex)
    val displayTemp      = hour?.temp        ?: state.currentTemp
    val displayFeels     = hour?.feelsLike   ?: state.feelsLike
    val displayClouds    = hour?.cloudCover  ?: state.cloudCover
    val displayCondCode  = hour?.conditionCode ?: state.conditionCode
    val displayHumidity  = state.humidity
    val displayDew       = hour?.dewPoint    ?: state.dewPoint
    val displayPressure  = hour?.pressure?.div(33.8639)?.let { String.format("%.1f", it).toDouble() }
                           ?: state.pressureInHg
    val displayUv        = hour?.uvIndex     ?: state.uvIndex
    val displayVis       = hour?.visibility  ?: state.visibility
    val displayWind      = hour?.windSpeed   ?: state.windSpeed
    val displayWindDir   = hour?.windDirection ?: state.windDirection
    val isScrubbing      = selectedHourIndex >= 0 && state.hourlyForecast.isNotEmpty()
    val scrubLabel       = if (isScrubbing) hour?.hour ?: "" else ""

    // ── Sun progress (0=sunrise, 0.5=noon, 1=sunset, -1=night) ──────────
    val sunProgress = remember(state.sunrise, state.sunset) {
        calcSunProgress(state.sunrise, state.sunset)
    }

    // ── Haptic feedback per degree change ─────────────────────────────────
    var lastHapticTemp by remember { mutableIntStateOf(displayTemp) }
    // Item 8: bouncy spring with low-dampening overshoot on load
    val animTemp by animateIntAsState(
        targetValue = displayTemp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "kinetic_temp"
    )
    LaunchedEffect(displayTemp) {
        if (displayTemp != lastHapticTemp) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastHapticTemp = displayTemp
        }
    }

    // ── Positions for pointer lines ───────────────────────────────────────
    var cloudLayerPos  by remember { mutableStateOf(Offset.Zero) }
    var cloudLayerSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var astroBlockPos  by remember { mutableStateOf(Offset.Zero) }
    var tempDisplayPos by remember { mutableStateOf(Offset.Zero) }
    var tempDisplaySz  by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var statsBlockPos  by remember { mutableStateOf(Offset.Zero) }
    var statsBlockSz   by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val density = LocalDensity.current

    // ── Popped stat display value ─────────────────────────────────────────
    val (poppedValue, poppedLabel) = when (poppedStat) {
        PoppedStat.HUMIDITY   -> Pair("${displayHumidity}", "HUM")
        PoppedStat.DEW        -> Pair("$displayDew", "DEW")
        PoppedStat.FEELS      -> Pair("$displayFeels", "FEELS")
        PoppedStat.PRESSURE   -> Pair(String.format("%.1f", displayPressure), "INHG")
        PoppedStat.UV         -> Pair(displayUv.toInt().toString(), "UV")
        PoppedStat.VISIBILITY -> Pair(String.format("%.1f", displayVis), "VIS")
        PoppedStat.NONE       -> Pair("", "")
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            // Item 10: double-tap snaps back to current hour
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onResetTimeline() }
                )
            }
            .pointerInput(state.hourlyForecast) {
                detectHorizontalDragGestures(
                    onDragEnd   = { dragAccum = 0f },
                    onDragStart = { },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        dragAccum += delta
                        val steps = (dragAccum / 55f).toInt()
                        if (steps != 0) {
                            dragAccum -= steps * 55f
                            // positive delta = drag right = earlier hours (subtract)
                            onScrub(-steps)
                        }
                    }
                )
            }
    ) {
        val screenH = maxHeight
        val screenW = maxWidth

        // ══════════════════════════════════════════════════════════════════
        // LAYER 1: Cloud Artwork (upper ~45% of screen after header)
        // ══════════════════════════════════════════════════════════════════
        val headerH = 52.dp
        val cloudH  = screenH * 0.40f
        val cloudOffsetY = headerH

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cloudH)
                .offset(y = cloudOffsetY)
                .onGloballyPositioned { coords ->
                    cloudLayerPos  = coords.positionInParent()
                    cloudLayerSize = androidx.compose.ui.geometry.Size(
                        coords.size.width.toFloat(),
                        coords.size.height.toFloat()
                    )
                }
        ) {
            CloudArtwork(
                conditionCode = displayCondCode,
                isDay = state.isDay,
                sunProgress = sunProgress,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ══════════════════════════════════════════════════════════════════
        // LAYER 2: Pointer Lines (Canvas overlay)
        // ══════════════════════════════════════════════════════════════════
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawKineticPointerLines(
                cloudPos  = cloudLayerPos,
                cloudSz   = cloudLayerSize,
                astroPos  = astroBlockPos,
                tempPos   = tempDisplayPos,
                tempSz    = tempDisplaySz,
                statsPos  = statsBlockPos,
                statsSz   = statsBlockSz
            )
        }

        // ══════════════════════════════════════════════════════════════════
        // LAYER 3: Location Header
        // ══════════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                text = state.locationName.uppercase(),
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = KineticSub
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    onClick = onLocationTap
                )
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE4E0DB))
                    .clickable { onNavigateToSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = KineticSub,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // LAYER 4: Wind info (centered, just below header)
        // ══════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = headerH + cloudH * 0.04f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WindArrow(direction = displayWindDir, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${displayWind.roundToInt()}MPH ${windDirectionToText(displayWindDir)}",
                style = kineticLabelStyle()
            )
        }

        // ══════════════════════════════════════════════════════════════════
        // LAYER 5: Clouds / Precip block (left of cloud)
        // ══════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = headerH + cloudH * 0.35f),
        ) {
            Text("CLOUDS:${displayClouds}%", style = kineticLabelStyle())
            Text("PREC:${state.precipStatus}", style = kineticLabelStyle())
        }

        // ══════════════════════════════════════════════════════════════════
        // LAYER 6: Astro block (right of cloud, with callout line)
        // ══════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 18.dp, top = headerH + cloudH * 0.22f)
                .onGloballyPositioned { coords ->
                    astroBlockPos = coords.positionInParent()
                },
            horizontalAlignment = Alignment.End
        ) {
            if (state.sunrise.isNotBlank()) {
                Text("RISE:${formatAstroTime(state.sunrise)}", style = kineticLabelStyle())
                Text("SET:${formatAstroTime(state.sunset)}", style = kineticLabelStyle())
            }
            Text("MOON:${state.moonPhase}", style = kineticLabelStyle())
        }

        // ══════════════════════════════════════════════════════════════════
        // LAYER 7: MASSIVE Central Temperature (or popped stat)
        // ══════════════════════════════════════════════════════════════════
        val tempScale by animateFloatAsState(
            targetValue = if (poppedStat == PoppedStat.NONE) 1f else 0.35f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            label = "temp_scale"
        )
        val poppedScale by animateFloatAsState(
            targetValue = if (poppedStat != PoppedStat.NONE) 1f else 0.3f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            label = "popped_scale"
        )

        // Container for the big display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = headerH + cloudH * 0.62f)
                .onGloballyPositioned { coords ->
                    tempDisplayPos = coords.positionInParent()
                    tempDisplaySz  = androidx.compose.ui.geometry.Size(
                        coords.size.width.toFloat(), coords.size.height.toFloat()
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Temperature (shrinks when something is popped)
            if (poppedStat == PoppedStat.NONE || tempScale > 0.4f) {
                KineticTempText(
                    value = animTemp.toString(),
                    scale = tempScale,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                            poppedStat = PoppedStat.NONE
                        }
                )
            }
            // Popped stat (grows when selected)
            if (poppedStat != PoppedStat.NONE) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                            poppedStat = PoppedStat.NONE
                        }
                ) {
                    KineticTempText(value = poppedValue, scale = poppedScale)
                    Text(
                        text = poppedLabel,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 3.sp,
                            color = KineticSub
                        )
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // LAYER 8: Pointer Stats Block (lower-left)
        // ══════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 130.dp)
                .onGloballyPositioned { coords ->
                    statsBlockPos = coords.positionInParent()
                    statsBlockSz  = androidx.compose.ui.geometry.Size(
                        coords.size.width.toFloat(), coords.size.height.toFloat()
                    )
                }
        ) {
            PointerStatRow("TEMP", "${state.currentTemp}", state.tempUnit, onTap = null)
            PointerStatRow("FEELS", "$displayFeels", state.tempUnit) {
                poppedStat = if (poppedStat == PoppedStat.FEELS) PoppedStat.NONE else PoppedStat.FEELS
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            PointerStatRow("HUM", "${displayHumidity}%", "") {
                poppedStat = if (poppedStat == PoppedStat.HUMIDITY) PoppedStat.NONE else PoppedStat.HUMIDITY
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            PointerStatRow("DEW", "$displayDew", "") {
                poppedStat = if (poppedStat == PoppedStat.DEW) PoppedStat.NONE else PoppedStat.DEW
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            PointerStatRow("INHG", String.format("%.1f", displayPressure), "") {
                poppedStat = if (poppedStat == PoppedStat.PRESSURE) PoppedStat.NONE else PoppedStat.PRESSURE
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // LAYER 9: Side Stats (lower-right: UV + VIS)
        // ══════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 148.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                "MAX UV:${displayUv.toInt()}",
                style = kineticLabelStyle(),
                modifier = Modifier.clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                    poppedStat = if (poppedStat == PoppedStat.UV) PoppedStat.NONE else PoppedStat.UV
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "VIS:${String.format("%.1f", displayVis)}${if (state.windUnit == "mph") "MI" else "KM"}",
                style = kineticLabelStyle(),
                modifier = Modifier.clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                    poppedStat = if (poppedStat == PoppedStat.VISIBILITY) PoppedStat.NONE else PoppedStat.VISIBILITY
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
        }

        // ══════════════════════════════════════════════════════════════════
        // LAYER 10: Condition label + scrub time indicator
        // ══════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 108.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isScrubbing && scrubLabel.isNotBlank()) {
                Text(
                    text = "◀  $scrubLabel  ▶",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp,
                        color = KineticSubDim
                    )
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = state.condition.uppercase(),
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = KineticText
                )
            )
        }

        // ══════════════════════════════════════════════════════════════════
        // LAYER 11: 7-Day Forecast Strip (pinned to bottom)
        // ══════════════════════════════════════════════════════════════════
        KineticDailyStrip(
            days = state.dailyForecast,
            tempUnit = state.tempUnit,
            onSettingsClick = onNavigateToSettings,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ── Kinetic Temp Text: multi-layer 3D extrusion ───────────────────────────────
@Composable
private fun KineticTempText(
    value: String,
    scale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val fontSize = 148.sp
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Shadow layer 3 (deepest)
        Text(
            text = value,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = Color(0xFF333333).copy(alpha = 0.18f * scale)
            ),
            modifier = Modifier.offset(x = (8 * scale).dp, y = (14 * scale).dp)
        )
        // Shadow layer 2
        Text(
            text = value,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = Color(0xFF444444).copy(alpha = 0.22f * scale)
            ),
            modifier = Modifier.offset(x = (4 * scale).dp, y = (7 * scale).dp)
        )
        // Main visible layer
        Text(
            text = value,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = KineticTempGray,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.12f),
                    offset = Offset(2f, 3f),
                    blurRadius = 4f
                )
            )
        )
    }
}

// ── Wind direction arrow (Canvas) ─────────────────────────────────────────────
@Composable
private fun WindArrow(direction: Int, modifier: Modifier = Modifier) {
    val angle = direction.toFloat()
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val len = size.minDimension * 0.42f
        val rad = Math.toRadians(angle.toDouble())
        val dx = (kotlin.math.sin(rad) * len).toFloat()
        val dy = (-kotlin.math.cos(rad) * len).toFloat()

        drawLine(
            color = KineticSub,
            start = Offset(cx - dx * 0.4f, cy - dy * 0.4f),
            end   = Offset(cx + dx, cy + dy),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        // Arrow head
        val headLen = len * 0.35f
        val headAngle = 0.5f
        val hx1 = cx + dx - (kotlin.math.sin(rad + headAngle) * headLen).toFloat()
        val hy1 = cy + dy + (kotlin.math.cos(rad + headAngle) * headLen).toFloat()
        val hx2 = cx + dx - (kotlin.math.sin(rad - headAngle) * headLen).toFloat()
        val hy2 = cy + dy + (kotlin.math.cos(rad - headAngle) * headLen).toFloat()
        drawLine(KineticSub, Offset(cx + dx, cy + dy), Offset(hx1, hy1), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawLine(KineticSub, Offset(cx + dx, cy + dy), Offset(hx2, hy2), strokeWidth = 2.5f, cap = StrokeCap.Round)
    }
}

// ── Pointer stat row: "LABEL:VALUE" with tap-to-pop ──────────────────────────
@Composable
private fun PointerStatRow(
    label: String,
    value: String,
    unit: String,
    onTap: (() -> Unit)?
) {
    val displayStr = if (unit.isNotBlank()) "$label:$value" else "$label:$value"
    val baseStyle = kineticLabelStyle()
    val tappable = onTap != null
    Text(
        text = displayStr,
        style = if (tappable) baseStyle.copy(color = KineticText) else baseStyle,
        modifier = if (tappable) Modifier.clickable(
            indication = null,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            onClick = onTap!!
        ) else Modifier
    )
}

// ── 7-Day Kinetic Strip ───────────────────────────────────────────────────────
@Composable
private fun KineticDailyStrip(
    days: List<DailyWeather>,
    tempUnit: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KineticBg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Thin separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color(0xFFCCCAC6))
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            days.take(7).forEach { day ->
                DayColumn(
                    day = day,
                    tempUnit = tempUnit,
                    modifier = Modifier.weight(1f)
                )
            }
            // Three-dot menu
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(KineticSubDim)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun DayColumn(
    day: DailyWeather,
    tempUnit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Day name
        Text(
            text = day.dayName.take(3).uppercase(),
            style = TextStyle(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                color = KineticSubDim
            )
        )

        // Weather icon area
        if (day.isToday) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(KineticRed),
                contentAlignment = Alignment.Center
            ) { /* red dot for today */ }
        } else {
            StripWeatherIcon(
                conditionCode = day.conditionCode,
                modifier = Modifier.size(22.dp)
            )
        }

        // Temperature
        Text(
            text = "${day.maxTemp}",
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = KineticText
            )
        )
    }
}

// ── Minimal weather icon for the 7-day strip ─────────────────────────────────
@Composable
private fun StripWeatherIcon(conditionCode: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r  = size.minDimension * 0.38f
        val cloudColor = when {
            conditionCode in 61..82 -> Color(0xFF4488CC)
            conditionCode in 71..77 -> Color(0xFF88AACC)
            conditionCode == 0      -> Color(0xFFFFCC44)
            else                    -> Color(0xFF555555)
        }

        when {
            conditionCode == 0 -> {
                // Sun
                drawCircle(Color(0xFFFFCC44), radius = r * 0.9f, center = Offset(cx, cy))
            }
            conditionCode in 95..99 -> {
                // Storm cloud
                drawCircle(Color(0xFF222222), radius = r, center = Offset(cx, cy - r * 0.2f))
                drawLine(Color(0xFFFFEE44), Offset(cx, cy + r * 0.3f), Offset(cx - r * 0.3f, cy + r), strokeWidth = 2.5f, cap = StrokeCap.Round)
                drawLine(Color(0xFFFFEE44), Offset(cx - r * 0.3f, cy + r), Offset(cx + r * 0.15f, cy + r * 0.55f), strokeWidth = 2.5f, cap = StrokeCap.Round)
            }
            conditionCode in 61..82 -> {
                // Rain cloud
                drawCircle(Color(0xFF444444), radius = r, center = Offset(cx, cy - r * 0.15f))
                drawLine(Color(0xFF4488CC), Offset(cx - r * 0.3f, cy + r * 0.6f), Offset(cx - r * 0.55f, cy + r * 1.1f), strokeWidth = 2f, cap = StrokeCap.Round)
                drawLine(Color(0xFF4488CC), Offset(cx + r * 0.1f, cy + r * 0.7f), Offset(cx - r * 0.15f, cy + r * 1.2f), strokeWidth = 2f, cap = StrokeCap.Round)
            }
            conditionCode in 71..77 -> {
                // Snow cloud
                drawCircle(Color(0xFF555555), radius = r, center = Offset(cx, cy - r * 0.15f))
                drawCircle(Color(0xFFAAD8F0), radius = 2.5f, center = Offset(cx - r * 0.3f, cy + r * 0.85f))
                drawCircle(Color(0xFFAAD8F0), radius = 2.5f, center = Offset(cx + r * 0.2f, cy + r * 0.85f))
            }
            else -> {
                // Generic cloud
                drawCircle(cloudColor, radius = r, center = Offset(cx, cy - r * 0.1f))
                drawCircle(cloudColor, radius = r * 0.75f, center = Offset(cx - r * 0.6f, cy + r * 0.1f))
                drawCircle(cloudColor, radius = r * 0.65f, center = Offset(cx + r * 0.55f, cy + r * 0.15f))
            }
        }
    }
}

// ── Canvas pointer lines ──────────────────────────────────────────────────────
private fun DrawScope.drawKineticPointerLines(
    cloudPos:  Offset,
    cloudSz:   androidx.compose.ui.geometry.Size,
    astroPos:  Offset,
    tempPos:   Offset,
    tempSz:    androidx.compose.ui.geometry.Size,
    statsPos:  Offset,
    statsSz:   androidx.compose.ui.geometry.Size
) {
    val lineColor = Color(0xFF888888)
    val sw = 1.2f

    // Line: cloud area (right edge, mid-height) → astro block (left edge, top)
    if (cloudSz != androidx.compose.ui.geometry.Size.Zero && astroPos != Offset.Zero) {
        val startX = cloudPos.x + cloudSz.width * 0.62f
        val startY = cloudPos.y + cloudSz.height * 0.38f
        val endX   = astroPos.x - 12f
        val endY   = astroPos.y + 16f
        drawLine(lineColor, Offset(startX, startY), Offset(endX, endY), strokeWidth = sw, cap = StrokeCap.Round)
        // Small dot at start
        drawCircle(lineColor, radius = 3f, center = Offset(startX, startY))
    }

    // Line: temperature (lower-left) → stats block (right edge, top)
    if (tempSz != androidx.compose.ui.geometry.Size.Zero && statsSz != androidx.compose.ui.geometry.Size.Zero) {
        val startX = tempPos.x + tempSz.width * 0.26f
        val startY = tempPos.y + tempSz.height * 0.72f
        val endX   = statsPos.x + statsSz.width + 8f
        val endY   = statsPos.y + 12f
        drawLine(lineColor, Offset(startX, startY), Offset(endX, endY), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun kineticLabelStyle() = TextStyle(
    fontSize = 11.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.3.sp,
    color = Color(0xFF444444)
)

private fun formatAstroTime(raw: String): String {
    // WeatherAPI returns "06:45 AM" or Open-Meteo returns "2024-01-01T06:45"
    return when {
        raw.contains("T") -> {
            runCatching {
                val ldt = java.time.LocalDateTime.parse(raw, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                val h = ldt.hour
                val m = ldt.minute
                val amPm = if (h < 12) "A" else "P"
                val hour = if (h == 0) 12 else if (h > 12) h - 12 else h
                "${hour}:${m.toString().padStart(2, '0')}$amPm"
            }.getOrDefault(raw)
        }
        raw.contains(":") -> {
            runCatching {
                val parts = raw.split(":")
                val h = parts[0].trim().toInt()
                val rest = parts[1].trim()
                val mStr = rest.take(2)
                val amPm = if (rest.contains("AM", true)) "A" else "P"
                val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
                "${h12}:${mStr}$amPm"
            }.getOrDefault(raw)
        }
        else -> raw
    }
}

// ── Location Search Bottom Sheet (Item 4) ────────────────────────────────────
@Composable
private fun KineticLocationSheet(
    searchResults: List<GeoLocation>,
    isSearching: Boolean,
    onQuery: (String) -> Unit,
    onSelect: (GeoLocation) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
    ) {
        Text(
            "SEARCH LOCATION",
            style = TextStyle(
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.5.sp, color = KineticSubDim
            )
        )
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it; onQuery(it) },
            placeholder = { Text("City, region…", color = KineticSubDim, fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.Search, null, tint = KineticSub, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; onQuery("") }) {
                        Icon(Icons.Outlined.Clear, null, tint = KineticSub, modifier = Modifier.size(18.dp))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onQuery(query) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = KineticText,
                unfocusedBorderColor = Color(0xFFCCCAC5),
                focusedContainerColor   = Color(0xFFE8E5E0),
                unfocusedContainerColor = Color(0xFFE8E5E0),
                focusedTextColor   = KineticText,
                unfocusedTextColor = KineticText
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (isSearching) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                color = KineticText,
                trackColor = Color(0xFFD0CCC8),
                modifier = Modifier.fillMaxWidth().height(1.dp)
            )
        }

        if (searchResults.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(searchResults) { loc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(loc) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn, null,
                            tint = KineticSub, modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                loc.name,
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KineticText)
                            )
                            val sub = buildString {
                                if (!loc.admin1.isNullOrBlank()) append(loc.admin1)
                                if (!loc.country.isNullOrBlank()) append(if (isNotEmpty()) ", ${loc.country}" else loc.country)
                            }
                            if (sub.isNotBlank()) {
                                Text(sub, style = TextStyle(fontSize = 11.sp, color = KineticSubDim))
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFD0CCC8)))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Sun progress (0=sunrise, 0.5=noon, 1.0=sunset, -1=night) ─────────────────
private fun calcSunProgress(sunriseRaw: String, sunsetRaw: String): Float {
    return try {
        val nowMinutes  = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
        val riseMinutes = parseAstroMinutes(sunriseRaw)
        val setMinutes  = parseAstroMinutes(sunsetRaw)
        if (riseMinutes <= 0 || setMinutes <= 0 || nowMinutes < riseMinutes || nowMinutes > setMinutes) {
            -1f // night
        } else {
            (nowMinutes - riseMinutes).toFloat() / (setMinutes - riseMinutes).toFloat()
        }
    } catch (e: Exception) { 0.5f }
}

private fun parseAstroMinutes(raw: String): Int {
    if (raw.isBlank()) return -1
    return try {
        when {
            raw.contains("T") -> {
                val ldt = java.time.LocalDateTime.parse(raw, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                ldt.hour * 60 + ldt.minute
            }
            raw.contains(":") -> {
                val parts = raw.trim().split("\\s+".toRegex())
                val timeParts = parts[0].split(":")
                var h = timeParts[0].toInt()
                val m = timeParts[1].toInt()
                if (parts.size > 1 && parts[1].equals("PM", true) && h < 12) h += 12
                if (parts.size > 1 && parts[1].equals("AM", true) && h == 12) h = 0
                h * 60 + m
            }
            else -> -1
        }
    } catch (e: Exception) { -1 }
}

// ── Loading & Error ───────────────────────────────────────────────────────────
@Composable
private fun KineticLoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = KineticText,
                strokeWidth = 1.5.dp,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "KINETIC WEATHER",
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.sp,
                    color = KineticSub
                )
            )
        }
    }
}

@Composable
private fun KineticErrorView(
    message: String,
    onRetry: () -> Unit,
    onSettings: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = TextStyle(
                    fontSize = 13.sp,
                    color = KineticSub,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onSettings) { Text("Settings") }
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = KineticText)
                ) {
                    Text("Retry", color = KineticBg)
                }
            }
        }
    }
}
