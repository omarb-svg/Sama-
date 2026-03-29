package com.omar.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.omar.weatherapp.data.WeatherRepository
import com.omar.weatherapp.data.models.GeoLocation
import com.omar.weatherapp.data.models.SavedLocation
import com.omar.weatherapp.data.models.WeatherUiState
import com.omar.weatherapp.data.prefs.AppPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class WeatherViewModel(private val context: Context) : ViewModel() {

    private val repository = WeatherRepository()
    val prefs = AppPreferences(context)

    // ── UI State ──────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(WeatherUiState(isLoading = true))
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    // ── Location search results ───────────────────────────────────────────
    private val _searchResults = MutableStateFlow<List<GeoLocation>>(emptyList())
    val searchResults: StateFlow<List<GeoLocation>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // ── Preferences as state ──────────────────────────────────────────────
    val savedLocations = prefs.savedLocations.stateIn(
        viewModelScope, SharingStarted.Lazily, emptyList()
    )
    val tempUnit = prefs.tempUnit.stateIn(
        viewModelScope, SharingStarted.Lazily, "F"
    )
    val windUnit = prefs.windUnit.stateIn(
        viewModelScope, SharingStarted.Lazily, "mph"
    )
    val apiProvider = prefs.apiProvider.stateIn(
        viewModelScope, SharingStarted.Lazily, "open_meteo"
    )

    // ── Load weather using saved/selected location ────────────────────────
    fun loadWeather() {
        viewModelScope.launch {
            val useCurrent = prefs.useCurrentLocation.first()
            if (useCurrent) return@launch // handled by fetchCurrentLocation

            val lat = prefs.selectedLatitude.first()?.toDouble()
            val lon = prefs.selectedLongitude.first()?.toDouble()
            val name = prefs.locationName.first()
            val tUnit = prefs.tempUnit.first()
            val wUnit = prefs.windUnit.first()

            if (lat == null || lon == null) {
                _uiState.update { it.copy(isLoading = false, error = "No location set. Open Settings to add one.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            fetchWeatherForCoords(lat, lon, name, tUnit, wUnit)
        }
    }

    // ── Fetch current device GPS location ────────────────────────────────
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                val location = fusedClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cts.token
                ).await()

                if (location != null) {
                    val name = resolveLocationName(context, location.latitude, location.longitude)
                    prefs.setUseCurrentLocation(true)

                    val tUnit = prefs.tempUnit.first()
                    val wUnit = prefs.windUnit.first()
                    fetchWeatherForCoords(location.latitude, location.longitude, name, tUnit, wUnit, isCurrentLoc = true)
                } else {
                    loadWeather()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Could not get location: ${e.message}") }
            }
        }
    }

    // ── Select a location from search/saved list ──────────────────────────
    fun selectLocation(lat: Double, lon: Double, name: String) {
        viewModelScope.launch {
            prefs.setSelectedLocation(lat, lon, name)
            val tUnit = prefs.tempUnit.first()
            val wUnit = prefs.windUnit.first()
            _uiState.update { it.copy(isLoading = true, error = null) }
            fetchWeatherForCoords(lat, lon, name, tUnit, wUnit)
        }
    }

    // ── Refresh with current settings ────────────────────────────────────
    fun refresh() {
        viewModelScope.launch {
            val useCurrent = prefs.useCurrentLocation.first()
            if (useCurrent) {
                fetchCurrentLocation(context)
            } else {
                loadWeather()
            }
        }
    }

    // ── Settings: save/remove location ───────────────────────────────────
    fun saveLocation(location: GeoLocation) {
        viewModelScope.launch {
            prefs.saveLocation(
                SavedLocation(
                    name = location.name,
                    displayName = location.displayName,
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )
        }
    }

    fun removeLocation(location: SavedLocation) {
        viewModelScope.launch { prefs.removeLocation(location) }
    }

    fun setTempUnit(unit: String) {
        viewModelScope.launch {
            prefs.setTempUnit(unit)
            refresh()
        }
    }

    fun setWindUnit(unit: String) {
        viewModelScope.launch {
            prefs.setWindUnit(unit)
            refresh()
        }
    }

    fun setApiProvider(provider: String) {
        viewModelScope.launch { prefs.setApiProvider(provider) }
    }

    fun setOwmApiKey(key: String) {
        viewModelScope.launch { prefs.setOwmApiKey(key) }
    }

    fun setWapiApiKey(key: String) {
        viewModelScope.launch { prefs.setWapiApiKey(key) }
    }

    fun setVcApiKey(key: String) {
        viewModelScope.launch { prefs.setVcApiKey(key) }
    }

    // ── Location search ───────────────────────────────────────────────────
    fun searchLocations(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            repository.searchLocations(query)
                .onSuccess { _searchResults.value = it }
                .onFailure { _searchResults.value = emptyList() }
            _isSearching.value = false
        }
    }

    fun clearSearch() { _searchResults.value = emptyList() }

    // ── Internal helpers ──────────────────────────────────────────────────
    private suspend fun fetchWeatherForCoords(
        lat: Double, lon: Double, name: String,
        tUnit: String, wUnit: String,
        isCurrentLoc: Boolean = false
    ) {
        val provider = prefs.apiProvider.first()
        val keys = mapOf(
            "owm" to prefs.owmApiKey.first(),
            "wapi" to prefs.wapiApiKey.first(),
            "vc" to prefs.vcApiKey.first()
        )

        repository.fetchWeather(lat, lon, tUnit, wUnit, provider, keys)
            .onSuccess { state ->
                _uiState.value = state.copy(
                    locationName = name,
                    isCurrentLocation = isCurrentLoc
                )
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to fetch weather: ${e.message}")
                }
            }
    }

    @Suppress("DEPRECATION")
    private fun resolveLocationName(context: Context, lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var result = "Your Location"
                geocoder.getFromLocation(lat, lon, 1) { addresses ->
                    result = addresses.firstOrNull()?.let {
                        it.locality ?: it.subAdminArea ?: it.adminArea
                    } ?: "Your Location"
                }
                result
            } else {
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                addresses?.firstOrNull()?.let {
                    it.locality ?: it.subAdminArea ?: it.adminArea
                } ?: "Your Location"
            }
        } catch (e: Exception) {
            "Your Location"
        }
    }
}

// ── ViewModel Factory ─────────────────────────────────────────────────────
class WeatherViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeatherViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
