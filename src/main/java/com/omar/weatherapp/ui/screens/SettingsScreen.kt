package com.omar.weatherapp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omar.weatherapp.WeatherViewModel
import com.omar.weatherapp.data.models.GeoLocation
import com.omar.weatherapp.data.models.SavedLocation

// ── Kinetic-aesthetic colors (mirrored from WeatherScreen) ────────────────────
private val KBg      = Color(0xFFF0EDE8)
private val KSurface = Color(0xFFE8E5E0)
private val KText    = Color(0xFF111111)
private val KSub     = Color(0xFF666666)
private val KDim     = Color(0xFF999999)
private val KDiv     = Color(0xFFD0CCC8)
private val KAccent  = Color(0xFF111111)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WeatherViewModel,
    onNavigateBack: () -> Unit
) {
    val savedLocations by viewModel.savedLocations.collectAsState()
    val searchResults  by viewModel.searchResults.collectAsState()
    val isSearching    by viewModel.isSearching.collectAsState()
    val tempUnit       by viewModel.tempUnit.collectAsState()
    val windUnit       by viewModel.windUnit.collectAsState()
    val apiProvider    by viewModel.apiProvider.collectAsState()

    var locationQuery by remember { mutableStateOf("") }
    var owmKeyInput   by remember { mutableStateOf("") }
    var wapiKeyInput  by remember { mutableStateOf("") }
    var vcKeyInput    by remember { mutableStateOf("") }
    var showOwmKey    by remember { mutableStateOf(false) }
    var showWapiKey   by remember { mutableStateOf(false) }
    var showVcKey     by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("SETTINGS", style = TextStyle(
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.sp, color = KSub
                    ))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back", tint = KText)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = KBg)
            )
        },
        containerColor = KBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {

            // ── Location Search ──────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                KSection("LOCATION SEARCH")
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = locationQuery,
                    onValueChange = { locationQuery = it; viewModel.searchLocations(it) },
                    placeholder = { Text("City or region…", color = KDim, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = KSub, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (locationQuery.isNotEmpty()) {
                            IconButton(onClick = { locationQuery = ""; viewModel.clearSearch() }) {
                                Icon(Icons.Outlined.Clear, null, tint = KSub, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.searchLocations(locationQuery) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KText, unfocusedBorderColor = KDiv,
                        focusedContainerColor = KSurface, unfocusedContainerColor = KSurface,
                        focusedTextColor = KText, unfocusedTextColor = KText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isSearching) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        color = KText, trackColor = KDiv,
                        modifier = Modifier.fillMaxWidth().height(1.dp)
                    )
                }
            }

            // ── Search Results ───────────────────────────────────────────
            if (searchResults.isNotEmpty()) {
                item {
                    KCard {
                        searchResults.forEach { loc ->
                            SearchRow(loc,
                                onSelect = {
                                    viewModel.selectLocation(loc.latitude, loc.longitude, loc.displayName)
                                    locationQuery = ""; viewModel.clearSearch(); onNavigateBack()
                                },
                                onSave = { viewModel.saveLocation(loc) }
                            )
                            if (loc != searchResults.last()) KDivider()
                        }
                    }
                }
            }

            // ── Saved Locations ──────────────────────────────────────────
            if (savedLocations.isNotEmpty()) {
                item {
                    KSection("SAVED LOCATIONS")
                    Spacer(Modifier.height(10.dp))
                    KCard {
                        savedLocations.forEach { loc ->
                            SavedRow(loc,
                                onSelect = { viewModel.selectLocation(loc.latitude, loc.longitude, loc.displayName); onNavigateBack() },
                                onDelete = { viewModel.removeLocation(loc) }
                            )
                            if (loc != savedLocations.last()) KDivider()
                        }
                    }
                }
            }

            // ── Temperature Unit ─────────────────────────────────────────
            item {
                KSection("TEMPERATURE")
                Spacer(Modifier.height(10.dp))
                KSegmented(
                    options = listOf("F" to "°F — Fahrenheit", "C" to "°C — Celsius"),
                    selected = tempUnit,
                    onSelect = { viewModel.setTempUnit(it) }
                )
            }

            // ── Wind Unit ────────────────────────────────────────────────
            item {
                KSection("WIND SPEED")
                Spacer(Modifier.height(10.dp))
                KSegmented(
                    options = listOf("mph" to "mph", "kmh" to "km/h", "ms" to "m/s"),
                    selected = windUnit,
                    onSelect = { viewModel.setWindUnit(it) }
                )
            }

            // ── API Provider ─────────────────────────────────────────────
            item {
                KSection("WEATHER SOURCE")
                Spacer(Modifier.height(10.dp))
                KCard {
                    listOf(
                        Triple("open_meteo",        "Open-Meteo",        "Free · No key required"),
                        Triple("openweathermap",     "OpenWeatherMap",    "Free tier · API key required"),
                        Triple("weatherapi",         "WeatherAPI.com",    "Free tier · API key required"),
                        Triple("visualcrossing",     "Visual Crossing",   "Free tier · API key required"),
                        Triple("average_consensus",  "Average Consensus", "Aggregates all providers")
                    ).forEachIndexed { idx, (key, name, desc) ->
                        ProviderRow(name, desc, key, apiProvider) { viewModel.setApiProvider(it) }
                        if (idx < 4) KDivider()
                    }
                }
            }

            // ── API Keys ─────────────────────────────────────────────────
            if (apiProvider in listOf("openweathermap", "weatherapi", "visualcrossing")) {
                item {
                    KSection("API KEYS")
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (apiProvider == "openweathermap") {
                            ApiKeyField("OpenWeatherMap Key", owmKeyInput, { owmKeyInput = it },
                                showOwmKey, { showOwmKey = !showOwmKey }, { viewModel.setOwmApiKey(owmKeyInput) })
                        }
                        if (apiProvider == "weatherapi") {
                            ApiKeyField("WeatherAPI.com Key", wapiKeyInput, { wapiKeyInput = it },
                                showWapiKey, { showWapiKey = !showWapiKey }, { viewModel.setWapiApiKey(wapiKeyInput) })
                        }
                        if (apiProvider == "visualcrossing") {
                            ApiKeyField("Visual Crossing Key", vcKeyInput, { vcKeyInput = it },
                                showVcKey, { showVcKey = !showVcKey }, { viewModel.setVcApiKey(vcKeyInput) })
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}

// ── Reusable kinetic-style UI atoms ───────────────────────────────────────────

@Composable
private fun KSection(text: String) {
    Text(text, style = TextStyle(
        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.5.sp, color = KDim
    ))
}

@Composable
private fun KCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KSurface),
        content = content
    )
}

@Composable
private fun KDivider() {
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(KDiv))
}

@Composable
private fun KSegmented(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KSurface)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { (key, label) ->
            val isSelected = key == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) KText else Color.Transparent)
                    .clickable { onSelect(key) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) KBg else KSub
                    )
                )
            }
        }
    }
}

@Composable
private fun SearchRow(loc: GeoLocation, onSelect: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.LocationOn, null, tint = KDim, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(loc.name, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KText))
            val sub = buildString {
                if (!loc.admin1.isNullOrBlank()) append(loc.admin1)
                if (!loc.country.isNullOrBlank()) append(if (isNotEmpty()) ", ${loc.country}" else loc.country)
            }
            if (sub.isNotBlank()) Text(sub, style = TextStyle(fontSize = 11.sp, color = KDim))
        }
        IconButton(onClick = onSave, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Bookmarks, "Save", tint = KSub, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SavedRow(loc: SavedLocation, onSelect: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(7.dp).clip(CircleShape).background(KText)
        )
        Spacer(Modifier.width(12.dp))
        Text(loc.displayName, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = KText),
            modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Delete, "Remove", tint = KDim, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun ProviderRow(name: String, description: String, key: String, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSelect(key) }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KText))
            Text(description, style = TextStyle(fontSize = 11.sp, color = KDim))
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (key == selected) KText else Color.Transparent)
                .border(1.5.dp, if (key == selected) KText else KDiv, CircleShape)
        ) {
            if (key == selected) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(KBg).align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun ApiKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    showKey: Boolean,
    onToggle: () -> Unit,
    onSave: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 11.sp, color = KDim) },
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggle) {
                    Icon(
                        if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        null, tint = KDim
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KText, unfocusedBorderColor = KDiv,
                focusedContainerColor = KSurface, unfocusedContainerColor = KSurface,
                focusedTextColor = KText, unfocusedTextColor = KText,
                focusedLabelColor = KText, unfocusedLabelColor = KDim
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = onSave,
            enabled = value.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = KText, disabledContainerColor = KDiv),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Key", color = KBg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}
