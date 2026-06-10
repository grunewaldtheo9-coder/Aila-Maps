package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.SavedPlace
import com.example.data.SavedPlacesRepository
import com.example.network.MapNetworkConfig
import com.example.network.RouteOption
import com.example.network.SearchResult
import com.example.network.NavigationStep
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val sender: String, // "USER" or "AI"
    val timestamp: Long = System.currentTimeMillis()
)

data class OfflineSectorByCoord(
    val id: String,
    val name: String,
    val centerLat: Double,
    val centerLon: Double,
    val radius: Double = 0.035,
    val sizeMb: Double = 3.2
)

enum class ActiveScreen {
    LOGIN, EXPLORE, FAVORITES, ROUTE_DETAILS, PROFILE
}

enum class TravelMode {
    DRIVING, BICYCLING, WALKING
}

data class AilaUiState(
    // Authentic state
    val isAuthenticated: Boolean = false,
    val userEmail: String? = null,
    val currentScreen: ActiveScreen = ActiveScreen.LOGIN,

    // Custom Profile customization state fields
    val profileName: String = "Guest",
    val profileBio: String = "Map explorer & Outdoor enthusiast",
    val profileAvatarUrl: String = "avatar_1", // avatar selection: avatar_1, avatar_2, avatar_3, avatar_4

    // Active travel mode choice
    val travelMode: TravelMode = TravelMode.DRIVING,

    // Map view bounds
    val mapCenterLat: Double = 37.7694,    // Start centered on Golden Gate Park design coordinates
    val mapCenterLon: Double = -122.4862,
    val mapZoom: Double = 14.5,

    // Active search items
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isLoadingSearch: Boolean = false,

    // Saved places from database
    val savedPlaces: List<SavedPlace> = emptyList(),

    // Navigation and route simulation state
    val userLocation: Pair<Double, Double> = Pair(37.7651, -122.4932), // User start slightly west inside GG Park
    val userBearing: Float? = null,
    val activeDestination: SavedPlace? = null,
    val computedRoutes: List<RouteOption> = emptyList(),
    val selectedRouteId: String? = null,
    val isRoutingLoading: Boolean = false,

    // Active navigation and turn-by-turn states
    val isNavigationActive: Boolean = false,
    val currentNavigationStepIndex: Int = 0,
    val navigationRemainingDistanceMeters: Double = 0.0,
    val navigationRemainingDurationSeconds: Double = 0.0,
    val navigationProgressFraction: Float = 0f,
    val navigationSteps: List<NavigationStep> = emptyList(),
    val isSimulationRunning: Boolean = false,
    val currentRoutePointIndex: Int = 0,
    val isCameraLockedToUser: Boolean = true,
    val isBirdsEyeView: Boolean = false,

    // Reverse geocode Cache (City & Country tracking)
    val userCity: String = "San Francisco",
    val userCountry: String = "United States",

    // Conversational Chat to maps & routes
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(text = "Hello! I am Aila, your smart neomorphic AI Map Companion. Ask me anything about routes, maps, and locations!", sender = "AI")
    ),
    val chatLoading: Boolean = false,
    val isChatOpen: Boolean = false,
    val isAiFeaturesEnabled: Boolean = true,
    val isTrafficOverlayEnabled: Boolean = false,
    val isVoiceGuidanceEnabled: Boolean = false,
    val isDynamicRouteComparisonEnabled: Boolean = true,
    val isSearchHistoryEnabled: Boolean = true,
    val searchHistory: List<String> = emptyList(),
    val isWeatherOverlayEnabled: Boolean = false,
    val weatherTemperature: Double? = null,
    val weatherDescription: String = "Loading...",
    val weatherIconEmoji: String = "🌤️",
    val isOfflineMode: Boolean = false,
    val downloadedMapSectors: Set<String> = emptySet(),
    val activeDownloads: Map<String, Float> = emptyMap(),
    val isDownloadingSector: String? = null
)

class AilaMapViewModel(
    private val repository: SavedPlacesRepository,
    private val sharedPreferences: android.content.SharedPreferences? = null
) : ViewModel() {

    private fun getDisplayNameFromEmail(email: String?): String {
        if (email.isNullOrBlank() || 
            email == "Guest Explorer" || 
            email.equals("guest", ignoreCase = true) || 
            email.equals("gest", ignoreCase = true) || 
            email.startsWith("guest", ignoreCase = true) || 
            email.startsWith("gest", ignoreCase = true)
        ) {
            return "Guest"
        }
        if (email.contains("grunewaldtheo", ignoreCase = true)) return "Theo Grunewald"
        val parts = email.split("@")
        if (parts.isNotEmpty()) {
            val rawName = parts[0].replace(".", " ").replace("_", " ")
            return rawName.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        }
        return "Explorer"
    }

    private val _uiState = MutableStateFlow(AilaUiState())
    val uiState: StateFlow<AilaUiState> = _uiState.asStateFlow()

    private var lastWeatherLat: Double? = null
    private var lastWeatherLon: Double? = null

    init {
        // Automatically fetch API credentials dynamically from private Supabase cloud storage
        viewModelScope.launch {
            com.example.network.SupabaseManager.fetchApiKeys()
        }

        // Automatically restore session if "Remember Me" is enabled
        sharedPreferences?.let { prefs ->
            val isRemembered = prefs.getBoolean("is_remembered", false)
            val storedHistory = prefs.getString("search_history", "") ?: ""
            val initialHistory = if (storedHistory.isNotEmpty()) {
                storedHistory.split("|||")
            } else {
                emptyList()
            }
            val isAiEnabled = prefs.getBoolean("is_ai_enabled", true)
            val isTrafficEnabled = prefs.getBoolean("is_traffic_enabled", false)
            val isVoiceEnabled = prefs.getBoolean("is_voice_enabled", false)
            val isComparisonEnabled = prefs.getBoolean("is_comparison_enabled", true)
            val isHistoryEnabled = prefs.getBoolean("is_history_enabled", true)
            val isWeatherEnabled = prefs.getBoolean("is_weather_enabled", false)
            val isOffline = prefs.getBoolean("is_offline_mode", false)
            val downloadedSectors = prefs.getStringSet("downloaded_map_sectors", emptySet()) ?: emptySet()

            val storedEmail = if (isRemembered) prefs.getString("remembered_email", "Guest Explorer") else null
            _uiState.update {
                it.copy(
                    isAuthenticated = isRemembered,
                    userEmail = storedEmail,
                    profileName = getDisplayNameFromEmail(storedEmail),
                    currentScreen = if (isRemembered) ActiveScreen.EXPLORE else ActiveScreen.LOGIN,
                    searchHistory = initialHistory,
                    isAiFeaturesEnabled = isAiEnabled,
                    isTrafficOverlayEnabled = isTrafficEnabled,
                    isVoiceGuidanceEnabled = isVoiceEnabled,
                    isDynamicRouteComparisonEnabled = isComparisonEnabled,
                    isSearchHistoryEnabled = isHistoryEnabled,
                    isWeatherOverlayEnabled = isWeatherEnabled,
                    isOfflineMode = isOffline,
                    downloadedMapSectors = downloadedSectors
                )
            }
        }

        // Collect saved places from DB to feed markers and pins instantly based on active user email
        var currentPlacesJob: kotlinx.coroutines.Job? = null
        viewModelScope.launch {
            _uiState.map { it.userEmail ?: "Guest Explorer" }
                .distinctUntilChanged()
                .collect { email ->
                    currentPlacesJob?.cancel()
                    currentPlacesJob = viewModelScope.launch {
                        repository.getSavedPlacesForUser(email).collect { places ->
                            _uiState.update { it.copy(savedPlaces = places) }
                        }
                    }
                }
        }
        refreshWeather()
    }

    // --- SERVICE FUNCTIONS ---

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.length >= 3) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingSearch = true, isSearching = true) }
                val currentState = _uiState.value
                val results = MapNetworkConfig.search(
                    query = query,
                    userLat = currentState.userLocation.first,
                    userLon = currentState.userLocation.second,
                    userCity = currentState.userCity
                )
                _uiState.update { it.copy(searchResults = results, isLoadingSearch = false) }
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    fun executePlacedQuery(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSearch = true, isSearching = true) }
            val currentState = _uiState.value
            val results = MapNetworkConfig.search(
                query = query,
                userLat = currentState.userLocation.first,
                userLon = currentState.userLocation.second,
                userCity = currentState.userCity
            )
            _uiState.update {
                it.copy(
                    searchResults = results,
                    isLoadingSearch = false,
                    isSearching = results.isNotEmpty()
                )
            }
        }
    }

    fun selectSearchResult(result: SearchResult) {
        val place = SavedPlace(
            name = result.name,
            address = result.address,
            latitude = result.latitude,
            longitude = result.longitude,
            category = result.type
        )
        triggerRouteCalculation(place)
    }

    fun triggerRouteCalculation(place: SavedPlace, preserveCamera: Boolean = false, updateScreen: Boolean = true) {
        val currentMode = _uiState.value.travelMode
        val optimalZoom = when (currentMode) {
            TravelMode.DRIVING -> 17.5
            TravelMode.BICYCLING -> 17.0
            TravelMode.WALKING -> 16.5
        }
        
        // Add destination name to search history
        addToSearchHistory(place.name)

        _uiState.update {
            it.copy(
                activeDestination = place,
                mapCenterLat = if (preserveCamera) it.mapCenterLat else place.latitude,
                mapCenterLon = if (preserveCamera) it.mapCenterLon else place.longitude,
                mapZoom = if (preserveCamera) it.mapZoom else optimalZoom,
                searchQuery = place.name,
                isSearching = false,
                isRoutingLoading = true
            )
        }

        viewModelScope.launch {
            val currentState = _uiState.value
            val userLoc = currentState.userLocation
            try {
                val routes = if (currentState.isOfflineMode) {
                    generateOfflineRoute(
                        startLat = userLoc.first, startLon = userLoc.second,
                        endLat = place.latitude, endLon = place.longitude,
                        travelMode = currentState.travelMode.name,
                        endName = place.name
                    )
                } else {
                    MapNetworkConfig.fetchRoutes(
                        startLat = userLoc.first, startLon = userLoc.second,
                        endLat = place.latitude, endLon = place.longitude,
                        travelMode = currentState.travelMode.name,
                        endName = place.name,
                        isAiEnabled = currentState.isAiFeaturesEnabled
                    )
                }
                _uiState.update {
                    it.copy(
                        computedRoutes = routes,
                        selectedRouteId = routes.firstOrNull()?.id,
                        isRoutingLoading = false,
                        currentScreen = if (updateScreen) ActiveScreen.ROUTE_DETAILS else it.currentScreen
                    )
                }

                if (_uiState.value.isNavigationActive) {
                    val activeRoute = routes.firstOrNull { it.id == _uiState.value.selectedRouteId } ?: routes.firstOrNull()
                    if (activeRoute != null) {
                        _uiState.update {
                            it.copy(
                                navigationSteps = activeRoute.steps,
                                currentNavigationStepIndex = 0,
                                currentRoutePointIndex = 0,
                                navigationRemainingDistanceMeters = activeRoute.distanceMiles * 1609.34,
                                navigationRemainingDurationSeconds = activeRoute.durationMin * 60.0
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRoutingLoading = false) }
            }
        }
    }

    fun setTravelMode(mode: TravelMode) {
        _uiState.update { 
            it.copy(
                travelMode = mode,
                mapZoom = when (mode) {
                    TravelMode.DRIVING -> 17.5
                    TravelMode.BICYCLING -> 17.0
                    TravelMode.WALKING -> 16.5
                },
                mapCenterLat = it.userLocation.first,
                mapCenterLon = it.userLocation.second
            ) 
        }
        _uiState.value.activeDestination?.let { dest ->
            val shouldUpdateScreen = _uiState.value.currentScreen == ActiveScreen.ROUTE_DETAILS
            triggerRouteCalculation(dest, preserveCamera = true, updateScreen = shouldUpdateScreen)
        }
    }

    fun updateProfile(name: String, bio: String, avatarUrl: String) {
        _uiState.update {
            it.copy(
                profileName = name,
                profileBio = bio,
                profileAvatarUrl = avatarUrl
            )
        }
    }

    fun toggleAiFeatures() {
        _uiState.update {
            val nextState = !it.isAiFeaturesEnabled
            sharedPreferences?.edit()?.putBoolean("is_ai_enabled", nextState)?.apply()
            it.copy(isAiFeaturesEnabled = nextState)
        }
    }

    fun toggleTrafficOverlay() {
        _uiState.update {
            val nextState = !it.isTrafficOverlayEnabled
            sharedPreferences?.edit()?.putBoolean("is_traffic_enabled", nextState)?.apply()
            it.copy(isTrafficOverlayEnabled = nextState)
        }
    }

    fun toggleVoiceGuidance() {
        _uiState.update {
            val nextState = !it.isVoiceGuidanceEnabled
            sharedPreferences?.edit()?.putBoolean("is_voice_enabled", nextState)?.apply()
            it.copy(isVoiceGuidanceEnabled = nextState)
        }
    }

    fun toggleDynamicRouteComparison() {
        _uiState.update {
            val nextState = !it.isDynamicRouteComparisonEnabled
            sharedPreferences?.edit()?.putBoolean("is_comparison_enabled", nextState)?.apply()
            it.copy(isDynamicRouteComparisonEnabled = nextState)
        }
    }

    fun toggleSearchHistory() {
        _uiState.update {
            val nextState = !it.isSearchHistoryEnabled
            sharedPreferences?.edit()?.putBoolean("is_history_enabled", nextState)?.apply()
            val finalHistory = if (nextState) it.searchHistory else emptyList()
            it.copy(isSearchHistoryEnabled = nextState, searchHistory = finalHistory)
        }
    }

    fun toggleWeatherOverlay() {
        _uiState.update {
            val nextState = !it.isWeatherOverlayEnabled
            sharedPreferences?.edit()?.putBoolean("is_weather_enabled", nextState)?.apply()
            it.copy(isWeatherOverlayEnabled = nextState)
        }
        refreshWeather()
    }

    fun refreshWeather() {
        val state = _uiState.value
        if (state.isWeatherOverlayEnabled) {
            val lat = state.mapCenterLat
            val lon = state.mapCenterLon

            val lastLat = lastWeatherLat
            val lastLon = lastWeatherLon
            if (lastLat != null && lastLon != null) {
                val dist = Math.sqrt(Math.pow(lat - lastLat, 2.0) + Math.pow(lon - lastLon, 2.0))
                if (dist < 0.05) {
                    return
                }
            }

            lastWeatherLat = lat
            lastWeatherLon = lon

            viewModelScope.launch {
                val info = MapNetworkConfig.fetchOpenMeteoWeather(lat, lon)
                if (info != null) {
                    _uiState.update {
                        it.copy(
                            weatherTemperature = info.temperature,
                            weatherDescription = info.description,
                            weatherIconEmoji = info.iconEmoji
                        )
                    }
                }
            }
        }
    }

    fun toggleOfflineMode() {
        _uiState.update {
            val nextState = !it.isOfflineMode
            sharedPreferences?.edit()?.putBoolean("is_offline_mode", nextState)?.apply()
            it.copy(isOfflineMode = nextState)
        }
    }

    fun downloadMapSector(sectorId: String) {
        val currentDownloads = _uiState.value.activeDownloads
        if (currentDownloads.containsKey(sectorId)) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloadingSector = sectorId,
                    activeDownloads = it.activeDownloads + (sectorId to 0.0f)
                )
            }
            // Simulate progression
            val steps = 20
            for (i in 1..steps) {
                kotlinx.coroutines.delay(80)
                val progress = i.toFloat() / steps
                _uiState.update {
                    it.copy(activeDownloads = it.activeDownloads + (sectorId to progress))
                }
            }
            _uiState.update {
                val updatedSectors = it.downloadedMapSectors + sectorId
                sharedPreferences?.edit()?.putStringSet("downloaded_map_sectors", updatedSectors)?.apply()
                it.copy(
                    downloadedMapSectors = updatedSectors,
                    activeDownloads = it.activeDownloads - sectorId,
                    isDownloadingSector = if (it.isDownloadingSector == sectorId) null else it.isDownloadingSector
                )
            }
        }
    }

    fun deleteMapSector(sectorId: String) {
        _uiState.update {
            val updatedSectors = it.downloadedMapSectors - sectorId
            sharedPreferences?.edit()?.putStringSet("downloaded_map_sectors", updatedSectors)?.apply()
            it.copy(
                downloadedMapSectors = updatedSectors,
                activeDownloads = it.activeDownloads - sectorId
            )
        }
    }

    // Static definition of supported local sectors for offline packaging
    object SectorsData {
        val PREDEFINED_SECTORS = listOf(
            OfflineSectorByCoord("sect_ggpark", "Golden Gate Park Core", 37.769, -122.486, radius = 0.035, sizeMb = 2.1),
            OfflineSectorByCoord("sect_downtown", "SF Downtown & Financial", 37.794, -122.408, radius = 0.035, sizeMb = 4.8),
            OfflineSectorByCoord("sect_presidio", "Presidio & Bridge Access", 37.800, -122.468, radius = 0.030, sizeMb = 1.8),
            OfflineSectorByCoord("sect_mission", "Mission & Potrero Grid", 37.755, -122.418, radius = 0.032, sizeMb = 3.2),
            OfflineSectorByCoord("sect_southsf", "South SF & Airport Access", 37.6547, -122.4077, radius = 0.045, sizeMb = 2.6)
        )

        fun getSectors(lat: Double, lon: Double, city: String): List<OfflineSectorByCoord> {
            val isNearSF = Math.abs(lat - 37.7749) < 1.0 && Math.abs(lon - (-122.4194)) < 1.0
            if (isNearSF) {
                return PREDEFINED_SECTORS
            } else {
                val cityName = if (city.isEmpty()) "Local" else city
                return listOf(
                    OfflineSectorByCoord("sect_local_city", "$cityName Center Grid", lat, lon, radius = 0.040, sizeMb = 3.5),
                    OfflineSectorByCoord("sect_local_north", "$cityName North District", lat + 0.025, lon + 0.015, radius = 0.035, sizeMb = 2.4),
                    OfflineSectorByCoord("sect_local_south", "$cityName South Quarter", lat - 0.025, lon - 0.015, radius = 0.035, sizeMb = 2.8),
                    OfflineSectorByCoord("sect_local_scenic", "$cityName Scenic Quarter", lat + 0.015, lon - 0.025, radius = 0.032, sizeMb = 1.9),
                    OfflineSectorByCoord("sect_local_airport", "$cityName Transit Axis", lat + 0.035, lon - 0.035, radius = 0.045, sizeMb = 3.1)
                )
            }
        }
    }

    fun getActiveCoordinateSector(lat: Double, lon: Double): OfflineSectorByCoord? {
        val userLoc = _uiState.value.userLocation
        val city = _uiState.value.userCity
        return SectorsData.getSectors(userLoc.first, userLoc.second, city).firstOrNull { sector ->
            val dist = Math.sqrt(Math.pow(lat - sector.centerLat, 2.0) + Math.pow(lon - sector.centerLon, 2.0))
            dist <= sector.radius
        }
    }

    fun getAvailableSectors(): List<OfflineSectorByCoord> {
        val loc = _uiState.value.userLocation
        return SectorsData.getSectors(loc.first, loc.second, _uiState.value.userCity)
    }

    fun generateOfflineRoute(
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double,
        travelMode: String,
        endName: String
    ): List<RouteOption> {
        val distanceDegrees = Math.sqrt(Math.pow(endLat - startLat, 2.0) + Math.pow(endLon - startLon, 2.0))
        val distanceMiles = distanceDegrees * 69.0
        val durationM = (distanceMiles * when (travelMode) {
            "DRIVING" -> 2.0
            "BICYCLING" -> 5.0
            else -> 15.0
        }).toInt().coerceAtLeast(1)

        // Generate grid-snapped street blocks for offline map routing
        val points = com.example.network.MapNetworkConfig.generateGridStreetCoordinates(startLat, startLon, endLat, endLon, skew = 0.0)

        val steps = listOf(
            NavigationStep("Depart using installed offline map package.", 0.0, startLat, startLon, "depart"),
            NavigationStep("Head northwest toward locally cached streets.", 120.0, startLat + (endLat - startLat) * 0.3, startLon + (endLon - startLon) * 0.3, "straight"),
            NavigationStep("Turn right using installed backup lane guidance.", 230.0, startLat + (endLat - startLat) * 0.6, startLon + (endLon - startLon) * 0.6, "right"),
            NavigationStep("Arrive at $endName (Offline Local Mode).", 0.0, endLat, endLon, "arrive")
        )

        val co2Val1 = distanceMiles * 0.44
        val gasVal1 = distanceMiles * 0.18

        return listOf(
            RouteOption(
                id = "offline-fastest",
                type = "FASTEST",
                title = "Offline Fast Path",
                description = "Offline fallback using downloaded sector. Auto-calibrated.",
                durationMin = durationM,
                distanceMiles = distanceMiles,
                coordinates = points,
                activeReview = "✨ Offline Routing Active. This path was computed entirely locally from your installed map database for $endName.",
                steps = steps
            ),
            RouteOption(
                id = "offline-scenic",
                type = "SCENIC",
                title = "Offline Backup Scenic",
                description = "Slightly longer bypass avoiding primary cached avenues.",
                durationMin = (durationM * 1.3).toInt(),
                distanceMiles = distanceMiles * 1.25,
                coordinates = points.map { Pair(it.first + 0.0015, it.second + 0.0015) },
                activeReview = "✨ Local Scenic Bypass. Navigating with fully offline-cached backup maps to dodge city bottlenecks.",
                steps = steps
            )
        )
    }

    fun addToSearchHistory(query: String) {
        if (query.trim().isEmpty() || !_uiState.value.isSearchHistoryEnabled) return
        val trimmed = query.trim()
        val currentHistory = _uiState.value.searchHistory.toMutableList()
        currentHistory.remove(trimmed)
        currentHistory.add(0, trimmed)
        val truncated = currentHistory.take(10)
        _uiState.update { it.copy(searchHistory = truncated) }
        sharedPreferences?.edit()?.putString("search_history", truncated.joinToString("|||"))?.apply()
    }

    fun removeFromSearchHistory(query: String) {
        val currentHistory = _uiState.value.searchHistory.toMutableList()
        currentHistory.remove(query)
        _uiState.update { it.copy(searchHistory = currentHistory) }
        sharedPreferences?.edit()?.putString("search_history", currentHistory.joinToString("|||"))?.apply()
    }

    fun clearSearchHistory() {
        _uiState.update { it.copy(searchHistory = emptyList()) }
        sharedPreferences?.edit()?.remove("search_history")?.apply()
    }

    fun savePlaceAsFavoriteWithGeocoding(
        name: String,
        address: String,
        category: String,
        onGeocodeComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val results = MapNetworkConfig.search(
                query = address,
                userLat = currentState.userLocation.first,
                userLon = currentState.userLocation.second,
                userCity = currentState.userCity
            )
            val bestResult = results.firstOrNull()
            val email = currentState.userEmail ?: "Guest Explorer"
            if (bestResult != null) {
                val place = SavedPlace(
                    name = name,
                    address = bestResult.address,
                    latitude = bestResult.latitude,
                    longitude = bestResult.longitude,
                    category = category,
                    userEmail = email
                )
                repository.insert(place)
                onGeocodeComplete(true)
            } else {
                // Coordinate Fallback based on user current location or general default if search yields nothing
                val userLoc = _uiState.value.userLocation
                val place = SavedPlace(
                    name = name,
                    address = address,
                    latitude = userLoc.first + (Math.random() * 0.02 - 0.01),
                    longitude = userLoc.second + (Math.random() * 0.02 - 0.01),
                    category = category,
                    userEmail = email
                )
                repository.insert(place)
                onGeocodeComplete(false)
            }
        }
    }

    fun updateMapCamera(lat: Double, lon: Double) {
        _uiState.update { 
            it.copy(
                mapCenterLat = lat, 
                mapCenterLon = lon,
                isCameraLockedToUser = false
            ) 
        }
        refreshWeather()
    }

    fun updateUserBearing(bearing: Float) {
        _uiState.update {
            it.copy(userBearing = bearing)
        }
    }

    private var lastGeocodedLoc: Pair<Double, Double>? = null
    private var hasLocalizedPresetPlaces = false

    private fun localizePresetPlaces(userLat: Double, userLon: Double, userCity: String) {
        if (hasLocalizedPresetPlaces) return
        
        // Only run if the user's current location is far away from Los Angeles (e.g., in Brazil)
        val distToLA = Math.sqrt(Math.pow(userLat - 34.0928, 2.0) + Math.pow(userLon - (-118.3287), 2.0))
        if (distToLA > 5.0) { // Far from LA
            hasLocalizedPresetPlaces = true
            viewModelScope.launch(Dispatchers.IO) {
                // Relocate preset places to coordinates near the user in Brazil
                val currentSaved = _uiState.value.savedPlaces
                currentSaved.forEach { place ->
                    val isOldDefault = when (place.name) {
                        "Home" -> Math.abs(place.latitude - 34.0928) < 0.1
                        "Work" -> Math.abs(place.latitude - 34.0522) < 0.1
                        "Central Park" -> Math.abs(place.latitude - 40.7851) < 0.1
                        "Brew & Bean" -> Math.abs(place.latitude - 34.0601) < 0.1
                        "Iron Paradise Gym" -> Math.abs(place.latitude - 34.0750) < 0.1
                        else -> false
                    }
                    if (isOldDefault) {
                        val newLat = when (place.name) {
                            "Home" -> userLat
                            "Work" -> userLat + 0.008
                            "Central Park" -> userLat - 0.012
                            "Brew & Bean" -> userLat + 0.004
                            "Iron Paradise Gym" -> userLat - 0.006
                            else -> userLat
                        }
                        val newLon = when (place.name) {
                            "Home" -> userLon
                            "Work" -> userLon - 0.006
                            "Central Park" -> userLon + 0.010
                            "Brew & Bean" -> userLon + 0.005
                            "Iron Paradise Gym" -> userLon - 0.004
                            else -> userLon
                        }
                        val cleanCity = if (userCity.isEmpty()) "Local" else userCity
                        val newAddress = when (place.name) {
                            "Home" -> "My Home, $cleanCity"
                            "Work" -> "Office Park, $cleanCity"
                            "Central Park" -> "$cleanCity Metropolitan Park"
                            "Brew & Bean" -> "Coffee Corner, $cleanCity"
                            "Iron Paradise Gym" -> "$cleanCity Fitness Gym"
                            else -> place.address
                        }
                        repository.insert(place.copy(latitude = newLat, longitude = newLon, address = newAddress))
                    }
                }
            }
        }
    }

    fun updateUserLocation(lat: Double, lon: Double, shouldCenter: Boolean = false) {
        _uiState.update {
            it.copy(
                userLocation = Pair(lat, lon),
                mapCenterLat = if (shouldCenter || it.isCameraLockedToUser) lat else it.mapCenterLat,
                mapCenterLon = if (shouldCenter || it.isCameraLockedToUser) lon else it.mapCenterLon
            )
        }
        refreshWeather()

        val state = _uiState.value
        if (state.isNavigationActive && !state.isSimulationRunning) {
            val activeRoute = state.computedRoutes.firstOrNull { it.id == state.selectedRouteId }
                ?: state.computedRoutes.firstOrNull()
            if (activeRoute != null && activeRoute.coordinates.isNotEmpty()) {
                var closestIndex = state.currentRoutePointIndex
                var minDistanceMiles = Double.MAX_VALUE
                for (i in 0 until activeRoute.coordinates.size) {
                    val coord = activeRoute.coordinates[i]
                    val dist = calculateHaversineDistance(lat, lon, coord.first, coord.second)
                    if (dist < minDistanceMiles) {
                        minDistanceMiles = dist
                        closestIndex = i
                    }
                }
                val minDistanceMeters = minDistanceMiles * 1609.34

                if (minDistanceMeters > 150.0 && !state.isRoutingLoading) {
                    triggerReroutingFromDeviatedLocation()
                } else {
                    val progress = closestIndex.toFloat() / activeRoute.coordinates.size.toFloat()
                    val remDist = maxOf(0.0, (1f - progress) * (activeRoute.distanceMiles * 1609.34))
                    val remTime = maxOf(0.0, (1f - progress) * (activeRoute.durationMin * 60.0))

                    var currentStepIdx = state.currentNavigationStepIndex
                    val steps = state.navigationSteps
                    if (steps.isNotEmpty()) {
                        val safeStepIdx = currentStepIdx.coerceIn(0, steps.size - 1)
                        if (safeStepIdx < steps.size - 1) {
                            val nextStep = steps[safeStepIdx + 1]
                            val dToNextStep = calculateHaversineDistance(
                                lat, lon,
                                nextStep.latitude, nextStep.longitude
                            ) * 1609.34
                            if (dToNextStep < 80.0) {
                                currentStepIdx++
                            }
                        }
                    }

                    _uiState.update {
                        it.copy(
                            currentRoutePointIndex = closestIndex,
                            currentNavigationStepIndex = currentStepIdx,
                            navigationRemainingDistanceMeters = remDist,
                            navigationRemainingDurationSeconds = remTime,
                            navigationProgressFraction = progress
                        )
                    }
                }
            }
        }
        
        val lastLoc = lastGeocodedLoc
        if (lastLoc == null || Math.abs(lastLoc.first - lat) > 0.05 || Math.abs(lastLoc.second - lon) > 0.05) {
            lastGeocodedLoc = Pair(lat, lon)
            viewModelScope.launch {
                try {
                    val cityCountry = MapNetworkConfig.reverseGeocode(lat, lon)
                    _uiState.update {
                        it.copy(
                            userCity = cityCountry.first,
                            userCountry = cityCountry.second
                        )
                    }
                    localizePresetPlaces(lat, lon, cityCountry.first)
                } catch (e: Exception) {
                    // Fail over silently
                }
            }
        }
    }

    // --- AILA AI CHAT CONVERSATION ---

    fun toggleChatDialog(open: Boolean) {
        _uiState.update { it.copy(isChatOpen = open) }
    }

    fun sendAilaChatMessage(text: String) {
        if (text.trim().isEmpty()) return
        
        val userMsg = ChatMessage(text = text, sender = "USER")
        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMsg,
                chatLoading = true
            )
        }

        viewModelScope.launch {
            val state = _uiState.value
            val contextInfo = """
                City: ${state.userCity}
                Country: ${state.userCountry}
                User Location Latitude: ${state.userLocation.first}, Longitude: ${state.userLocation.second}
                Current Map Zoom: ${state.mapZoom}
                Active Selected Dest: ${state.activeDestination?.name ?: "No destination selected"}
                Saved favorite places count: ${state.savedPlaces.size}
                Saved Places List Details: ${state.savedPlaces.joinToString { "${it.name} (${it.address})" }}
            """.trimIndent()

            val responseText = MapNetworkConfig.sendOpenRouterChat(
                messages = _uiState.value.chatMessages,
                contextInfo = contextInfo
            )

            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessage(text = responseText, sender = "AI"),
                    chatLoading = false
                )
            }
        }
    }

    fun zoomIn() {
        _uiState.update { it.copy(mapZoom = (it.mapZoom + 0.5).coerceAtMost(18.0)) }
    }

    fun zoomOut() {
        _uiState.update { it.copy(mapZoom = (it.mapZoom - 0.5).coerceAtLeast(10.0)) }
    }

    fun centerOnUser() {
        val userLoc = _uiState.value.userLocation
        val defaultZoom = when (_uiState.value.travelMode) {
            TravelMode.DRIVING -> 17.5
            TravelMode.BICYCLING -> 17.0
            TravelMode.WALKING -> 16.5
        }
        _uiState.update {
            it.copy(
                mapCenterLat = userLoc.first,
                mapCenterLon = userLoc.second,
                mapZoom = defaultZoom,
                isCameraLockedToUser = true,
                isBirdsEyeView = false
            )
        }
    }

    fun savePlaceAsFavorite(place: SavedPlace) {
        viewModelScope.launch {
            val email = _uiState.value.userEmail ?: "Guest Explorer"
            repository.insert(place.copy(userEmail = email))
        }
    }

    fun removeFavoritePlace(place: SavedPlace) {
        viewModelScope.launch {
            repository.delete(place)
        }
    }

    fun selectRoute(routeId: String) {
        _uiState.update { it.copy(selectedRouteId = routeId) }
    }

    fun setScreen(screen: ActiveScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    // --- AUTHENTICATION ---

    private fun saveLoginSession(email: String, loginType: String, rememberMe: Boolean) {
        sharedPreferences?.edit()?.apply {
            putBoolean("is_remembered", rememberMe)
            putString("remembered_email", email)
            putString("remembered_login_type", loginType)
            apply()
        }
    }

    private fun clearLoginSession() {
        sharedPreferences?.edit()?.apply {
            putBoolean("is_remembered", false)
            remove("remembered_email")
            remove("remembered_login_type")
            apply()
        }
    }

    fun loginWithFirebase(
        email: String,
        passwordHash: String,
        isSignUp: Boolean,
        rememberMe: Boolean = true,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val isSuccess = if (isSignUp) {
                com.example.network.SupabaseManager.signUpSupabase(email, passwordHash)
            } else {
                com.example.network.SupabaseManager.loginSupabase(email, passwordHash)
            }

            if (isSuccess) {
                _uiState.update {
                    it.copy(
                        isAuthenticated = true,
                        userEmail = email,
                        profileName = getDisplayNameFromEmail(email),
                        currentScreen = ActiveScreen.EXPLORE
                    )
                }
                if (rememberMe) {
                    saveLoginSession(email, "SUPABASE", true)
                } else {
                    clearLoginSession()
                }
                onResult(true, null)
            } else {
                val errorMsg = if (isSignUp) "Email already exists on Supabase." else "Invalid password. Please double check credentials."
                onResult(false, errorMsg)
            }
        }
    }

    fun loginWithGoogleSimulation(emailAddress: String, rememberMe: Boolean = true) {
        _uiState.update {
            it.copy(
                isAuthenticated = true,
                userEmail = emailAddress,
                profileName = getDisplayNameFromEmail(emailAddress),
                currentScreen = ActiveScreen.EXPLORE
            )
        }
        if (rememberMe) {
            saveLoginSession(emailAddress, "GOOGLE", true)
        } else {
            clearLoginSession()
        }
    }

    fun continueAsGuest(rememberMe: Boolean = true) {
        _uiState.update {
            it.copy(
                isAuthenticated = true,
                userEmail = "Guest Explorer",
                profileName = "Guest",
                currentScreen = ActiveScreen.EXPLORE
            )
        }
        if (rememberMe) {
            saveLoginSession("Guest Explorer", "GUEST", true)
        } else {
            clearLoginSession()
        }
    }

    fun logout() {
        simulationJob?.cancel()
        clearLoginSession()
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                userEmail = null,
                currentScreen = ActiveScreen.LOGIN,
                searchQuery = "",
                activeDestination = null,
                computedRoutes = emptyList(),
                isNavigationActive = false,
                isSimulationRunning = false
            )
        }
    }

    private var simulationJob: kotlinx.coroutines.Job? = null

    fun calculateRouteMidpointAndZoom(coords: List<Pair<Double, Double>>): Triple<Double, Double, Double> {
        if (coords.isEmpty()) {
            return Triple(37.7694, -122.4862, 14.5)
        }
        var minLat = 90.0
        var maxLat = -90.0
        var minLon = 180.0
        var maxLon = -180.0

        for (coord in coords) {
            val lat = coord.first
            val lon = coord.second
            if (lat < minLat) minLat = lat
            if (lat > maxLat) maxLat = lat
            if (lon < minLon) minLon = lon
            if (lon > maxLon) maxLon = lon
        }

        val centerLat = (minLat + maxLat) / 2.0
        val centerLon = (minLon + maxLon) / 2.0
        val dLat = maxLat - minLat
        val dLon = maxLon - minLon
        val maxSpan = maxOf(dLat, dLon)

        val zoom = when {
            maxSpan < 0.003 -> 16.5
            maxSpan < 0.008 -> 15.5
            maxSpan < 0.018 -> 14.5
            maxSpan < 0.040 -> 13.5
            maxSpan < 0.080 -> 12.5
            maxSpan < 0.180 -> 11.5
            else -> 10.5
        }
        return Triple(centerLat, centerLon, zoom)
    }

    fun toggleBirdsEyeView() {
        val state = _uiState.value
        val activeRoute = state.computedRoutes.firstOrNull { it.id == state.selectedRouteId }
            ?: state.computedRoutes.firstOrNull()

        val nextBirdsEye = !state.isBirdsEyeView

        if (nextBirdsEye && activeRoute != null && activeRoute.coordinates.isNotEmpty()) {
            val (birdLat, birdLon, birdZoom) = calculateRouteMidpointAndZoom(activeRoute.coordinates)
            _uiState.update {
                it.copy(
                    isBirdsEyeView = true,
                    isCameraLockedToUser = false,
                    mapCenterLat = birdLat,
                    mapCenterLon = birdLon,
                    mapZoom = birdZoom
                )
            }
        } else {
            val defaultZoom = when (state.travelMode) {
                TravelMode.DRIVING -> 17.5
                TravelMode.BICYCLING -> 17.0
                TravelMode.WALKING -> 16.5
            }
            _uiState.update {
                it.copy(
                    isBirdsEyeView = false,
                    isCameraLockedToUser = true,
                    mapCenterLat = it.userLocation.first,
                    mapCenterLon = it.userLocation.second,
                    mapZoom = defaultZoom
                )
            }
        }
    }

    fun startNavigation() {
        val activeRoute = _uiState.value.computedRoutes.firstOrNull { it.id == _uiState.value.selectedRouteId }
            ?: _uiState.value.computedRoutes.firstOrNull() ?: return

        val (birdLat, birdLon, birdZoom) = calculateRouteMidpointAndZoom(activeRoute.coordinates)

        _uiState.update {
            it.copy(
                isNavigationActive = true,
                navigationSteps = activeRoute.steps,
                currentNavigationStepIndex = 0,
                currentRoutePointIndex = 0,
                navigationRemainingDistanceMeters = activeRoute.distanceMiles * 1609.34,
                navigationRemainingDurationSeconds = activeRoute.durationMin * 60.0,
                navigationProgressFraction = 0f,
                isSimulationRunning = false,
                isCameraLockedToUser = false,
                isBirdsEyeView = true,
                userLocation = activeRoute.coordinates.firstOrNull() ?: it.userLocation,
                mapCenterLat = birdLat,
                mapCenterLon = birdLon,
                mapZoom = birdZoom
            )
        }
    }

    fun stopNavigation() {
        simulationJob?.cancel()
        _uiState.update {
            it.copy(
                isNavigationActive = false,
                isSimulationRunning = false,
                navigationSteps = emptyList(),
                currentNavigationStepIndex = 0,
                currentRoutePointIndex = 0,
                navigationProgressFraction = 0f,
                navigationRemainingDistanceMeters = 0.0,
                navigationRemainingDurationSeconds = 0.0
            )
        }
    }

    fun toggleSimulationRunning() {
        val isRunning = _uiState.value.isSimulationRunning
        if (isRunning) {
            simulationJob?.cancel()
            _uiState.update { it.copy(isSimulationRunning = false) }
        } else {
            _uiState.update { it.copy(isSimulationRunning = true) }
            startSimulationTicker()
        }
    }

    fun toggleCameraLock() {
        _uiState.update { it.copy(isCameraLockedToUser = !it.isCameraLockedToUser) }
    }

    private fun startSimulationTicker() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1200L)
                val state = _uiState.value
                val activeRoute = state.computedRoutes.firstOrNull { it.id == state.selectedRouteId }
                    ?: state.computedRoutes.firstOrNull()

                if (activeRoute == null || activeRoute.coordinates.isEmpty() || !state.isSimulationRunning) {
                    break
                }

                val nextPointIndex = state.currentRoutePointIndex + 1
                if (nextPointIndex >= activeRoute.coordinates.size) {
                    _uiState.update {
                        it.copy(
                            userLocation = activeRoute.coordinates.last(),
                            currentRoutePointIndex = activeRoute.coordinates.size - 1,
                            currentNavigationStepIndex = state.navigationSteps.size - 1,
                            navigationRemainingDistanceMeters = 0.0,
                            navigationRemainingDurationSeconds = 0.0,
                            navigationProgressFraction = 1f,
                            isSimulationRunning = false
                        )
                    }
                    if (state.isCameraLockedToUser) {
                        _uiState.update {
                            it.copy(
                                mapCenterLat = activeRoute.coordinates.last().first,
                                mapCenterLon = activeRoute.coordinates.last().second
                            )
                        }
                    }
                    break
                }

                val nextCoords = activeRoute.coordinates[nextPointIndex]
                val progress = nextPointIndex.toFloat() / activeRoute.coordinates.size.toFloat()

                val remDist = maxOf(0.0, (1f - progress) * (activeRoute.distanceMiles * 1609.34))
                val remTime = maxOf(0.0, (1f - progress) * (activeRoute.durationMin * 60.0))

                var currentStepIdx = state.currentNavigationStepIndex
                val steps = state.navigationSteps
                if (steps.isNotEmpty() && currentStepIdx < steps.size - 1) {
                    val nextStep = steps[currentStepIdx + 1]
                    val dToNextStep = calculateHaversineDistance(
                        nextCoords.first, nextCoords.second,
                        nextStep.latitude, nextStep.longitude
                    ) * 1609.34
                    
                    if (dToNextStep < 80.0) {
                        currentStepIdx++
                    }
                }

                _uiState.update {
                    it.copy(
                        userLocation = nextCoords,
                        currentRoutePointIndex = nextPointIndex,
                        currentNavigationStepIndex = currentStepIdx,
                        navigationRemainingDistanceMeters = remDist,
                        navigationRemainingDurationSeconds = remTime,
                        navigationProgressFraction = progress,
                        mapCenterLat = if (it.isCameraLockedToUser) nextCoords.first else it.mapCenterLat,
                        mapCenterLon = if (it.isCameraLockedToUser) nextCoords.second else it.mapCenterLon
                    )
                }
            }
        }
    }

    fun simulateDeviation() {
        val currentLoc = _uiState.value.userLocation
        val deviatedLoc = Pair(currentLoc.first + 0.0035, currentLoc.second - 0.0035)
        _uiState.update {
            it.copy(
                userLocation = deviatedLoc,
                isSimulationRunning = false
            )
        }
        
        val destination = _uiState.value.activeDestination
        if (destination != null) {
            triggerReroutingFromDeviatedLocation()
        }
    }

    private fun triggerReroutingFromDeviatedLocation() {
        val destination = _uiState.value.activeDestination ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRoutingLoading = true) }
            val currentLoc = _uiState.value.userLocation
            try {
                val routes = if (_uiState.value.isOfflineMode) {
                    generateOfflineRoute(
                        startLat = currentLoc.first, startLon = currentLoc.second,
                        endLat = destination.latitude, endLon = destination.longitude,
                        travelMode = _uiState.value.travelMode.name,
                        endName = destination.name
                    )
                } else {
                    MapNetworkConfig.fetchRoutes(
                        startLat = currentLoc.first, startLon = currentLoc.second,
                        endLat = destination.latitude, endLon = destination.longitude,
                        travelMode = _uiState.value.travelMode.name,
                        endName = destination.name,
                        isAiEnabled = _uiState.value.isAiFeaturesEnabled
                    )
                }
                val alertMsg = ChatMessage(
                    text = if (_uiState.value.isOfflineMode) 
                        "🔄 [OFFLINE Mode] Deviation alert! Recalculated offline path back to ${destination.name} using downloaded sector..."
                        else "🔄 Off-path alert! Recalculating route back to ${destination.name} with optimized street lanes...",
                    sender = "AI"
                )
                _uiState.update {
                    it.copy(
                        computedRoutes = routes,
                        selectedRouteId = routes.firstOrNull()?.id,
                        navigationSteps = routes.firstOrNull()?.steps ?: emptyList(),
                        currentNavigationStepIndex = 0,
                        currentRoutePointIndex = 0,
                        navigationRemainingDistanceMeters = (routes.firstOrNull()?.distanceMiles ?: 0.0) * 1609.34,
                        navigationRemainingDurationSeconds = (routes.firstOrNull()?.durationMin ?: 0) * 60.0,
                        navigationProgressFraction = 0f,
                        isRoutingLoading = false,
                        chatMessages = it.chatMessages + alertMsg,
                        isSimulationRunning = false,
                        mapCenterLat = currentLoc.first,
                        mapCenterLon = currentLoc.second
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRoutingLoading = false) }
            }
        }
    }

    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 3958.8
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}

// Custom ViewModel Factory supporting Room repository dependency injection cleanly
class AilaViewModelFactory(
    private val repository: SavedPlacesRepository,
    private val sharedPreferences: android.content.SharedPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AilaMapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AilaMapViewModel(repository, sharedPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
