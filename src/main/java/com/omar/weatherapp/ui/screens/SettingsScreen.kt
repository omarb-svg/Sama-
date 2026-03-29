package com.omar.weatherapp.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omar.weatherapp.WeatherViewModel
import com.omar.weatherapp.data.models.GeoLocation
import com.omar.weatherapp.data.models.SavedLocation
import com.omar.weatherapp.ui.components.SectionLabel
import com.omar.weatherapp.ui.theme.*

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
    var showApiKeyFields by remember { mutableStateOf(false) }
    var owmKeyInput  by remember { mutableStateOf("") }
    var wapiKeyInput by remember { mutableStateOf("") }
    var vcKeyInput   by remember { mutableStateOf("") }
    var showOwmKey   by remember { mutableStateOf(false) }
    var showWapiKey  by remember { mutableStateOf(false) }
    var showVcKey    by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SETTINGS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                        color = WeatherSubText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, "Back", tint = WeatherText)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = WeatherBackground
                )
            )
        },
        containerColor = WeatherBackground
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Location Search ──────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                SectionLabel("LOCATION SEARCH")
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = locationQuery,
                    onValueChange = {
                        locationQuery = it
                        viewModel.searchLocations(it)
                    },
                    placeholder = { Text("Search city, zip, or region…", color = WeatherSubText) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = WeatherSubText) },
                    trailingIcon = {
                        if (locationQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                locationQuery = ""
                                viewModel.clearSearch()
                            }) {
                                Icon(Icons.Outlined.Clear, null, tint = WeatherSubText)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.searchLocations(locationQuery) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WeatherAccentBlue,
                        unfocusedBorderColor = WeatherDivider,
                        focusedContainerColor = WeatherCardBg,
                        unfocusedContainerColor = WeatherCardBg,
                        focusedTextColor = WeatherText,
                        unfocusedTextColor = WeatherText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Loading indicator
                if (isSearching) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        color = WeatherAccentBlue,
                        trackColor = WeatherDivider,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            // ── Search Results ───────────────────────────────────────────
            if (searchResults.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(WeatherCardBg)
                    ) {
                        searchResults.forEach { location ->
                            SearchResultRow(
                                location = location,
                                onSelect = {
                                    viewModel.selectLocation(
                                        location.latitude,
                                        location.longitude,
                                        location.displayName
                                    )
                                    locationQuery = ""
                                    viewModel.clearSearch()
                                    onNavigateBack()
                                },
                                onSave = {
                                    viewModel.saveLocation(location)
                                }
                            )
                            if (location != searchResults.last()) {
                                HorizontalDivider(color = WeatherDivider, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // ── Saved Locations ──────────────────────────────────────────
            if (savedLocations.isNotEmpty()) {
                item {
                    SectionLabel("SAVED LOCATIONS")
                    Spacer(Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(WeatherCardBg)
                    ) {
                        savedLocations.forEach { loc ->
                            SavedLocationRow(
                                location = loc,
                                onSelect = {
                                    viewModel.selectLocation(loc.latitude, loc.longitude, loc.displayName)
                                    onNavigateBack()
                                },
                                onDelete = { viewModel.removeLocation(loc) }
                            )
                            if (loc != savedLocations.last()) {
                                HorizontalDivider(color = WeatherDivider, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // ── Temperature Unit ─────────────────────────────────────────
            item {
                SectionLabel("TEMPERATURE UNIT")
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(WeatherCardBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("F" to "Fahrenheit (°F)", "C" to "Celsius (°C)").forEach { (key, label) ->
                        val selected = tempUnit == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) WeatherText else Color.Transparent)
                                .clickable { viewModel.setTempUnit(key) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) WeatherBackground else WeatherSubText
                            )
                        }
                    }
                }
            }

            // ── Wind Unit ────────────────────────────────────────────────
            item {
                SectionLabel("WIND UNIT")
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(WeatherCardBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("mph" to "mph", "kmh" to "km/h", "ms" to "m/s").forEach { (key, label) ->
                        val selected = windUnit == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) WeatherText else Color.Transparent)
                                .clickable { viewModel.setWindUnit(key) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) WeatherBackground else WeatherSubText
                            )
                        }
                    }
                }
            }

            // ── Weather API Provider ─────────────────────────────────────
            item {
                SectionLabel("WEATHER API SOURCE")
                Spacer(Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(WeatherCardBg)
                ) {
                    ApiProviderRow(
                        name = "Open-Meteo",
                        description = "Free · No key required · High accuracy",
                        key = "open_meteo",
                        selectedKey = apiProvider,
                        onSelect = { viewModel.setApiProvider(it) }
                    )
                    HorizontalDivider(color = WeatherDivider, thickness = 0.5.dp)
                    ApiProviderRow(
                        name = "OpenWeatherMap",
                        description = "Free tier · API key required",
                        key = "owm",
                        selectedKey = apiProvider,
                        onSelect = {
                            viewModel.setApiProvider(it)
                            showApiKeyFields = true
                        }
                    )
                    HorizontalDivider(color = WeatherDivider, thickness = 0.5.dp)
                    ApiProviderRow(
                        name = "WeatherAPI.com",
                        description = "Free tier · API key required",
                        key = "wapi",
                        selectedKey = apiProvider,
                        onSelect = {
                            viewModel.setApiProvider(it)
                            showApiKeyFields = true
                        }
                    )
                    HorizontalDivider(color = WeatherDivider, thickness = 0.5.dp)
                    ApiProviderRow(
                        name = "Visual Crossing",
                        description = "Free tier · API key required",
                        key = "visualcrossing",
                        selectedKey = apiProvider,
                        onSelect = {
                            viewModel.setApiProvider(it)
                            showApiKeyFields = true
                        }
                    )
                    HorizontalDivider(color = WeatherDivider, thickness = 0.5.dp)
                    ApiProviderRow(
                        name = "Average Consensus",
                        description = "Aggregated average of all backends",
                        key = "average_consensus",
                        selectedKey = apiProvider,
                        onSelect = { viewModel.setApiProvider(it) }
                    )
                }
            }

            // ── API Key inputs ───────────────────────────────────────────
            if (apiProvider == "owm" || apiProvider == "wapi") {
                item {
                    SectionLabel("API KEYS")
                    Spacer(Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (apiProvider == "owm") {
                            ApiKeyField(
                                label = "OpenWeatherMap API Key",
                                value = owmKeyInput,
                                onValueChange = { owmKeyInput = it },
                                showKey = showOwmKey,
                                onToggleVisibility = { showOwmKey = !showOwmKey },
                                onSave = { viewModel.setOwmApiKey(owmKeyInput) }
                            )
                        }
                        if (apiProvider == "wapi") {
                            ApiKeyField(
                                label = "WeatherAPI.com API Key",
                                value = wapiKeyInput,
                                onValueChange = { wapiKeyInput = it },
                                showKey = showWapiKey,
                                onToggleVisibility = { showWapiKey = !showWapiKey },
                                onSave = { viewModel.setWapiApiKey(wapiKeyInput) }
                            )
                        }
                        if (apiProvider == "visualcrossing") {
                            ApiKeyField(
                                label = "Visual Crossing API Key",
                                value = vcKeyInput,
                                onValueChange = { vcKeyInput = it },
                                showKey = showVcKey,
                                onToggleVisibility = { showVcKey = !showVcKey },
                                onSave = { viewModel.setVcApiKey(vcKeyInput) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Note: Consensus mode averages all four providers to provide the most reliable data. Ensure all API keys are valid for full accuracy.",
                        fontSize = 11.sp,
                        color = WeatherSubText,
                        lineHeight = 16.sp
                    )
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SearchResultRow(
    location: GeoLocation,
    onSelect: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.LocationOn, null, tint = WeatherSubText, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(location.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = WeatherText)
            Text(
                buildString {
                    if (!location.admin1.isNullOrBlank()) append(location.admin1)
                    if (!location.country.isNullOrBlank()) append(", ${location.country}")
                },
                fontSize = 12.sp, color = WeatherSubText
            )
        }
        IconButton(onClick = onSave, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Bookmarks, "Save", tint = WeatherAccentBlue, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SavedLocationRow(
    location: SavedLocation,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.BookmarkAdded, null, tint = WeatherAccentBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            location.displayName,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = WeatherText,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Delete, "Remove", tint = WeatherSubText, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ApiProviderRow(
    name: String,
    description: String,
    key: String,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    val selected = key == selectedKey
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(key) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = WeatherText)
            Text(description, fontSize = 12.sp, color = WeatherSubText)
        }
        RadioButton(
            selected = selected,
            onClick = { onSelect(key) },
            colors = RadioButtonDefaults.colors(selectedColor = WeatherText, unselectedColor = WeatherDivider)
        )
    }
}

@Composable
private fun ApiKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    showKey: Boolean,
    onToggleVisibility: () -> Unit,
    onSave: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 12.sp) },
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        if (showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        null,
                        tint = WeatherSubText
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WeatherAccentBlue,
                unfocusedBorderColor = WeatherDivider,
                focusedContainerColor = WeatherCardBg,
                unfocusedContainerColor = WeatherCardBg,
                focusedTextColor = WeatherText,
                unfocusedTextColor = WeatherText,
                focusedLabelColor = WeatherAccentBlue,
                unfocusedLabelColor = WeatherSubText
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = onSave,
            enabled = value.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = WeatherText),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save API Key", color = WeatherBackground, fontWeight = FontWeight.SemiBold)
        }
    }
}
