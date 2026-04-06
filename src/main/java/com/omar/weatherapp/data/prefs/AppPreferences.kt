package com.omar.weatherapp.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.omar.weatherapp.data.models.SavedLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_prefs")

class AppPreferences(private val context: Context) {

    private val gson = Gson()

    companion object {
        val KEY_API_PROVIDER   = stringPreferencesKey("api_provider")
        val KEY_OWM_API_KEY    = stringPreferencesKey("owm_api_key")
        val KEY_WAPI_API_KEY   = stringPreferencesKey("wapi_api_key")
        val KEY_VC_API_KEY     = stringPreferencesKey("vc_api_key")
        val KEY_TEMP_UNIT      = stringPreferencesKey("temp_unit")      // "F" or "C"
        val KEY_WIND_UNIT      = stringPreferencesKey("wind_unit")      // "mph" / "kmh" / "ms"
        val KEY_SAVED_LOCS     = stringPreferencesKey("saved_locations")
        val KEY_SELECTED_LAT   = floatPreferencesKey("selected_lat")
        val KEY_SELECTED_LON   = floatPreferencesKey("selected_lon")
        val KEY_LOCATION_NAME  = stringPreferencesKey("location_name")
        val KEY_USE_CURR_LOC   = booleanPreferencesKey("use_current_location")
        val KEY_CACHED_WEATHER   = stringPreferencesKey("cached_weather_json")
        val KEY_LAST_UPDATED_MS  = longPreferencesKey("last_updated_epoch_ms")
    }

    val apiProvider: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_API_PROVIDER] ?: "average_consensus" }

    val owmApiKey: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_OWM_API_KEY] ?: "4c910b43b72d4c037504c07234fd2278" }

    val wapiApiKey: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_WAPI_API_KEY] ?: "d997e000a2bf4a7f997173913262903" }

    val vcApiKey: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_VC_API_KEY] ?: "Y8DDHK3Z62N3PN7E65YJRH5FT" }

    val tempUnit: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_TEMP_UNIT] ?: "F" }

    val windUnit: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_WIND_UNIT] ?: "mph" }

    val savedLocations: Flow<List<SavedLocation>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val json = prefs[KEY_SAVED_LOCS] ?: return@map emptyList()
            val type = object : TypeToken<List<SavedLocation>>() {}.type
            gson.fromJson<List<SavedLocation>>(json, type) ?: emptyList()
        }

    val selectedLatitude: Flow<Float?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_SELECTED_LAT] }

    val selectedLongitude: Flow<Float?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_SELECTED_LON] }

    val locationName: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_LOCATION_NAME] ?: "Your Location" }

    val useCurrentLocation: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_USE_CURR_LOC] ?: true }

    val cachedWeatherJson: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_CACHED_WEATHER] }

    val lastUpdatedMs: Flow<Long> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_LAST_UPDATED_MS] ?: 0L }

    suspend fun saveCachedWeather(json: String) {
        context.dataStore.edit {
            it[KEY_CACHED_WEATHER]  = json
            it[KEY_LAST_UPDATED_MS] = System.currentTimeMillis()
        }
    }

    suspend fun setApiProvider(provider: String) {
        context.dataStore.edit { it[KEY_API_PROVIDER] = provider }
    }

    suspend fun setOwmApiKey(key: String) {
        context.dataStore.edit { it[KEY_OWM_API_KEY] = key }
    }

    suspend fun setWapiApiKey(key: String) {
        context.dataStore.edit { it[KEY_WAPI_API_KEY] = key }
    }

    suspend fun setVcApiKey(key: String) {
        context.dataStore.edit { it[KEY_VC_API_KEY] = key }
    }

    suspend fun setTempUnit(unit: String) {
        context.dataStore.edit { it[KEY_TEMP_UNIT] = unit }
    }

    suspend fun setWindUnit(unit: String) {
        context.dataStore.edit { it[KEY_WIND_UNIT] = unit }
    }

    suspend fun setSelectedLocation(lat: Double, lon: Double, name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_LAT] = lat.toFloat()
            prefs[KEY_SELECTED_LON] = lon.toFloat()
            prefs[KEY_LOCATION_NAME] = name
            prefs[KEY_USE_CURR_LOC] = false
        }
    }

    suspend fun setUseCurrentLocation(use: Boolean) {
        context.dataStore.edit { it[KEY_USE_CURR_LOC] = use }
    }

    suspend fun saveLocation(location: SavedLocation) {
        context.dataStore.edit { prefs ->
            val json = prefs[KEY_SAVED_LOCS] ?: "[]"
            val type = object : TypeToken<MutableList<SavedLocation>>() {}.type
            val list: MutableList<SavedLocation> = gson.fromJson(json, type) ?: mutableListOf()
            if (list.none { it.latitude == location.latitude && it.longitude == location.longitude }) {
                list.add(location)
            }
            prefs[KEY_SAVED_LOCS] = gson.toJson(list)
        }
    }

    suspend fun removeLocation(location: SavedLocation) {
        context.dataStore.edit { prefs ->
            val json = prefs[KEY_SAVED_LOCS] ?: "[]"
            val type = object : TypeToken<MutableList<SavedLocation>>() {}.type
            val list: MutableList<SavedLocation> = gson.fromJson(json, type) ?: mutableListOf()
            list.removeAll { it.latitude == location.latitude && it.longitude == location.longitude }
            prefs[KEY_SAVED_LOCS] = gson.toJson(list)
        }
    }
}
