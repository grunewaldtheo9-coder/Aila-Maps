package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedPlace
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ActiveScreen
import com.example.ui.viewmodel.AilaMapViewModel
import com.example.ui.viewmodel.TravelMode
import androidx.compose.material.icons.outlined.Star
import kotlin.math.max
import kotlin.math.roundToInt

private fun android.content.Context.findMainActivity(): com.example.MainActivity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is com.example.MainActivity) return ctx
        ctx = ctx.baseContext
    }
    return ctx as? com.example.MainActivity
}

@Composable
fun MapScreen(
    viewModel: AilaMapViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Detect and request location permissions on launch or update
    var hasLocationPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                      permissions.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false)
        hasLocationPermission = granted
        if (granted) {
            context.findMainActivity()?.startLocationUpdatesPublic()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            context.findMainActivity()?.startLocationUpdatesPublic()
        }
    }

    val routeCoordinates = uiState.computedRoutes.firstOrNull { it.id == uiState.selectedRouteId }?.coordinates
        ?: uiState.computedRoutes.firstOrNull()?.coordinates
        ?: emptyList()

    val isRoutingActive = routeCoordinates.isNotEmpty() && uiState.activeDestination != null

    var isLayersPanelOpen by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SilkBackground)
            .testTag("map_screen_container")
    ) {
        // 1. Fullscreen Map Canvas
        AilaInteractiveMap(
            centerLat = uiState.mapCenterLat,
            centerLon = uiState.mapCenterLon,
            zoom = uiState.mapZoom,
            startPoint = uiState.userLocation,
            endPoint = uiState.activeDestination?.let { Pair(it.latitude, it.longitude) },
            routeCoordinates = routeCoordinates,
            savedPlaces = if (uiState.isNavigationActive) emptyList() else uiState.savedPlaces,
            onMapMoved = { lat, lon ->
                viewModel.updateMapCamera(lat, lon)
            },
            travelMode = uiState.travelMode,
            userBearing = uiState.userBearing,
            isTrafficOverlayEnabled = uiState.isTrafficOverlayEnabled,
            isWeatherOverlayEnabled = uiState.isWeatherOverlayEnabled,
            weatherTemperature = uiState.weatherTemperature,
            weatherDescription = uiState.weatherDescription,
            weatherIconEmoji = uiState.weatherIconEmoji
        )

        // 2. Floating Header and Search Container
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.isOfflineMode && !uiState.isNavigationActive) {
                val currentSector = viewModel.getActiveCoordinateSector(uiState.mapCenterLat, uiState.mapCenterLon)
                val isDownloaded = currentSector != null && uiState.downloadedMapSectors.contains(currentSector.id)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offline_indicator_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDownloaded) Color(0xFF2E7D32) else Color(0xFFC62828)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Offline Mode Status",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isDownloaded) 
                                    "🔌 Offline Mode Active" 
                                    else "⚠️ Offline Mode (No Map Installed)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (isDownloaded)
                                    "Navigating locally using downloaded '${currentSector?.name}' package."
                                    else "Install cached local sectors below to enable offline fallback routing.",
                                fontSize = 9.5.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            if (uiState.isNavigationActive) {
                val steps = uiState.navigationSteps
                val currentStepIdx = uiState.currentNavigationStepIndex
                val currentStep = steps.getOrNull(currentStepIdx)
                val instructionText = currentStep?.instruction ?: "Proceed along route"
                val distanceMeters = currentStep?.distanceMeters ?: 150.0
                val distanceText = if (distanceMeters < 10.0) {
                    "Keep going"
                } else {
                    if (distanceMeters < 500) "${distanceMeters.toInt()} m" else "${String.format("%.1f", distanceMeters / 1000.0)} km"
                }
                val type = currentStep?.type ?: "continue"
                val navIcon = when {
                    type.contains("left", ignoreCase = true) -> Icons.Filled.ArrowBack
                    type.contains("right", ignoreCase = true) -> Icons.Filled.ArrowForward
                    type.contains("arrive", ignoreCase = true) -> Icons.Filled.LocationOn
                    type.contains("depart", ignoreCase = true) -> Icons.Filled.ArrowUpward
                    else -> Icons.Filled.ArrowUpward
                }

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("active_navigation_header_hud"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SilkPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = navIcon,
                                    contentDescription = "Active Step Instruction Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = instructionText,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "In $distanceText",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.5f))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "GPS STABLE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeomorphicButton(
                                    onClick = { viewModel.toggleSimulationRunning() },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    cornerRadius = 8.dp,
                                    elevation = 2.dp,
                                    backgroundColor = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isSimulationRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Toggle simulated movement ticker",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (uiState.isSimulationRunning) "Pause Sim" else "Play Sim",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }

                                NeomorphicButton(
                                    onClick = { viewModel.simulateDeviation() },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    cornerRadius = 8.dp,
                                    elevation = 2.dp,
                                    backgroundColor = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Simulate drift/deviation from route",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Deviate Path",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { viewModel.toggleCameraLock() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isCameraLockedToUser) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock map camera tracking user",
                                    tint = if (uiState.isCameraLockedToUser) Color.White else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            } else if (isRoutingActive) {
                // Previews the directions instructions or fallback mock
                val rawSteps = uiState.computedRoutes.firstOrNull { it.id == uiState.selectedRouteId }?.steps
                    ?: uiState.computedRoutes.firstOrNull()?.steps
                
                val steps = if (!rawSteps.isNullOrEmpty()) {
                    rawSteps.map { step ->
                        val instruction = step.instruction
                        val distText = if (step.distanceMeters < 500) "${step.distanceMeters.toInt()}m" else "${String.format("%.1f", step.distanceMeters/1000.0)}km"
                        val type = step.type
                        val icon = when {
                            type.contains("left", ignoreCase = true) -> Icons.Filled.ArrowBack
                            type.contains("right", ignoreCase = true) -> Icons.Filled.ArrowForward
                            type.contains("arrive", ignoreCase = true) -> Icons.Filled.LocationOn
                            else -> Icons.Filled.ArrowUpward
                        }
                        Triple(instruction, distText, icon)
                    }
                } else {
                    remember(uiState.travelMode) {
                        when (uiState.travelMode) {
                            TravelMode.DRIVING -> listOf(
                                Triple("Turn RIGHT onto Crossover Dr", "500 ft", Icons.Filled.ArrowForward),
                                Triple("Merge onto Golden Gate Parkway", "0.8 mi", Icons.Filled.ArrowUpward),
                                Triple("Turn LEFT onto John F Kennedy Dr", "300 ft", Icons.Filled.ArrowBack),
                                Triple("Arrive at Destination", "100 ft", Icons.Filled.LocationOn)
                            )
                            TravelMode.BICYCLING -> listOf(
                                Triple("Enter Bicycle Lane - Great Highway Path", "200 ft", Icons.Filled.ArrowUpward),
                                Triple("Slight LEFT onto Bike Lane Trail", "0.6 mi", Icons.Filled.ArrowBack),
                                Triple("Turn RIGHT toward Overpass Route", "0.2 mi", Icons.Filled.ArrowForward),
                                Triple("Destination reached via cycle lanes", "50 ft", Icons.Filled.LocationOn)
                            )
                            TravelMode.WALKING -> listOf(
                                Triple("Walk east on John F Kennedy Dr footpath", "150 ft", Icons.Filled.ArrowUpward),
                                Triple("Follow sidewalk path split LEFT", "0.3 mi", Icons.Filled.ArrowBack),
                                Triple("Cross Walkway intersection carefully", "400 ft", Icons.Filled.Warning),
                                Triple("Destination on the right inside park", "10 ft", Icons.Filled.LocationOn)
                            )
                        }
                    }
                }

                var currentStepIdx by remember(uiState.travelMode) { mutableStateOf(0) }
                val currentStep = steps.getOrElse(currentStepIdx) { steps[0] }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(when (uiState.travelMode) {
                                    TravelMode.DRIVING -> Color(0xFFE8EAF6)
                                    TravelMode.BICYCLING -> Color(0xFFE8F5E9)
                                    TravelMode.WALKING -> Color(0xFFE0F7FA)
                                }),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = currentStep.third,
                                contentDescription = "Step instruction",
                                tint = when (uiState.travelMode) {
                                    TravelMode.DRIVING -> Color(0xFF3F51B5)
                                    TravelMode.BICYCLING -> Color(0xFF4CAF50)
                                    TravelMode.WALKING -> Color(0xFF00B0FF)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentStep.first,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SilkOnSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "In ${currentStep.second}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SilkPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(SilkOnSurfaceVariant.copy(alpha = 0.4f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${uiState.travelMode.name} ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SilkOnSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                currentStepIdx = (currentStepIdx + 1) % steps.size
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = "Next direction instruction",
                                tint = SilkPrimary
                            )
                        }
                    }
                }
            } else {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeomorphicIconButton(
                        onClick = { /* Toggle Menu Drawer */ },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Main Menu",
                                tint = SilkPrimary
                            )
                        }
                    )

                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_aila_logo),
                        contentDescription = "Aila Maps",
                        modifier = Modifier.height(64.dp).padding(vertical = 4.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeomorphicIconButton(
                            onClick = { isLayersPanelOpen = !isLayersPanelOpen },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Layers,
                                    contentDescription = "Map Layers",
                                    tint = if (isLayersPanelOpen) Color(0xFFC2185B) else SilkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            cornerRadius = 10.dp,
                            elevation = 4.dp
                        )

                        // Mini circular user profile avatar with navigation link to Profile Screen
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.setScreen(ActiveScreen.PROFILE) },
                            contentAlignment = Alignment.Center
                        ) {
                            NeomorphicIconButton(
                                onClick = { viewModel.setScreen(ActiveScreen.PROFILE) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Profile Screen",
                                        tint = SilkPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                cornerRadius = 10.dp,
                                elevation = 4.dp
                            )
                        }
                    }
                }
            }

            if (!uiState.isNavigationActive) {
                // Search Bar
                NeomorphicTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = "Search places...",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search Icon",
                            tint = SilkPrimary
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search",
                                    tint = SilkOnSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    },
                    cornerRadius = 16.dp,
                    testTag = "place_search_input",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = {
                            viewModel.executePlacedQuery(uiState.searchQuery)
                            focusManager.clearFocus()
                        }
                    ),
                    onFocusChanged = { isSearchFocused = it }
                )

                // Indication of loading search results
                if (uiState.isLoadingSearch) {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .height(4.dp),
                        color = SilkPrimary,
                        trackColor = Color.Transparent
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Dynamic Travel Mode Selector Row (Driving, Bicycling, Walking)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val travelModes = listOf(
                        Triple(TravelMode.DRIVING, Icons.Default.DirectionsCar, "Car"),
                        Triple(TravelMode.BICYCLING, Icons.Default.DirectionsBike, "Bike"),
                        Triple(TravelMode.WALKING, Icons.Default.DirectionsWalk, "Walk")
                    )
                    
                    travelModes.forEach { (mode, icon, label) ->
                        val isSelected = uiState.travelMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFE8DEF8) else SilkSurface)
                                .clickable { viewModel.setTravelMode(mode) }
                                .padding(vertical = 8.dp)
                                .testTag("travel_mode_${mode.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) SilkPrimary else SilkOnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) SilkPrimary else SilkOnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3b. Quick Map Enhancements Settings (Row 1 & Row 2)
            AnimatedVisibility(
                visible = isLayersPanelOpen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SilkSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // ✨ AI Features
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (uiState.isAiFeaturesEnabled) Color(0xFFE8DEF8) else SilkBackground)
                                .clickable { viewModel.toggleAiFeatures() }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✨ AI Features",
                                fontSize = 10.5.sp,
                                fontWeight = if (uiState.isAiFeaturesEnabled) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.isAiFeaturesEnabled) SilkPrimary else SilkOnSurfaceVariant
                            )
                        }

                        // 🚗 Add Traffic Overlay
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (uiState.isTrafficOverlayEnabled) Color(0xFFC8E6C9) else SilkBackground)
                                .clickable { viewModel.toggleTrafficOverlay() }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🚗 Traffic Overlay",
                                fontSize = 10.5.sp,
                                fontWeight = if (uiState.isTrafficOverlayEnabled) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.isTrafficOverlayEnabled) Color(0xFF2E7D32) else SilkOnSurfaceVariant
                            )
                        }

                        // 🎤 Voice Route Guidance
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (uiState.isVoiceGuidanceEnabled) Color(0xFFFFCDD2) else SilkBackground)
                                .clickable { viewModel.toggleVoiceGuidance() }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎤 Voice Route",
                                fontSize = 10.5.sp,
                                fontWeight = if (uiState.isVoiceGuidanceEnabled) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.isVoiceGuidanceEnabled) Color(0xFFC62828) else SilkOnSurfaceVariant
                            )
                        }
                    }

                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 📊 Dynamic Route Comparison
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (uiState.isDynamicRouteComparisonEnabled) Color(0xFFBBDEFB) else SilkBackground)
                                .clickable { viewModel.toggleDynamicRouteComparison() }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📊 Route Comparison",
                                fontSize = 10.5.sp,
                                fontWeight = if (uiState.isDynamicRouteComparisonEnabled) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.isDynamicRouteComparisonEnabled) Color(0xFF1565C0) else SilkOnSurfaceVariant
                            )
                        }

                        // 🕒 Add Search History
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (uiState.isSearchHistoryEnabled) Color(0xFFFFF9C4) else SilkBackground)
                                .clickable { viewModel.toggleSearchHistory() }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🕒 Search History",
                                fontSize = 10.5.sp,
                                fontWeight = if (uiState.isSearchHistoryEnabled) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.isSearchHistoryEnabled) Color(0xFFF57F17) else SilkOnSurfaceVariant
                            )
                        }
                    }

                    // Row 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // ⛅ Weather Overlay
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (uiState.isWeatherOverlayEnabled) Color(0xFFE1F5FE) else SilkBackground)
                                .clickable { viewModel.toggleWeatherOverlay() }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⛅ Weather Overlay",
                                fontSize = 10.5.sp,
                                fontWeight = if (uiState.isWeatherOverlayEnabled) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.isWeatherOverlayEnabled) Color(0xFF0288D1) else SilkOnSurfaceVariant
                            )
                        }

                        // 🔌 Offline Mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (uiState.isOfflineMode) Color(0xFFFFECE0) else SilkBackground)
                                .clickable { viewModel.toggleOfflineMode() }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔌 Offline Mode",
                                fontSize = 10.5.sp,
                                fontWeight = if (uiState.isOfflineMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.isOfflineMode) Color(0xFFE65100) else SilkOnSurfaceVariant
                            )
                        }
                    }

                    // Divider and Offline Map Sectors Installer
                    HorizontalDivider(color = SilkOutlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                    
                    var isSectorsExpanded by remember { mutableStateOf(false) }
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSectorsExpanded = !isSectorsExpanded }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Offline Cache",
                                    tint = SilkPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🗺️ Local Offline map packages",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SilkOnSurface
                                    )
                                )
                            }
                            Icon(
                                imageVector = if (isSectorsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand map packages list",
                                tint = SilkOnSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                                    .testTag("expand_sectors_btn")
                            )
                        }
                        
                        AnimatedVisibility(visible = isSectorsExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                viewModel.getAvailableSectors().forEach { sector ->
                                    val isDownloaded = uiState.downloadedMapSectors.contains(sector.id)
                                    val downloadProgress = uiState.activeDownloads[sector.id]
                                    val isDownloading = downloadProgress != null

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = SilkBackground),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = sector.name,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = SilkOnSurface
                                                        )
                                                    )
                                                    Text(
                                                        text = "${sector.sizeMb} MB • Sector Boundary",
                                                        fontSize = 9.sp,
                                                        color = SilkOnSurfaceVariant
                                                    )
                                                }
                                                
                                                when {
                                                    isDownloading -> {
                                                        Box(
                                                            modifier = Modifier.size(24.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            CircularProgressIndicator(
                                                                progress = { downloadProgress ?: 0.0f },
                                                                modifier = Modifier.size(18.dp),
                                                                strokeWidth = 2.dp,
                                                                color = SilkPrimary
                                                            )
                                                        }
                                                    }
                                                    isDownloaded -> {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Installed",
                                                                tint = Color(0xFF2E7D32),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            IconButton(
                                                                onClick = { viewModel.deleteMapSector(sector.id) },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "Remove Map Package",
                                                                    tint = Color(0xFFD32F2F),
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    else -> {
                                                        NeomorphicIconButton(
                                                            onClick = { viewModel.downloadMapSector(sector.id) },
                                                            icon = {
                                                                Icon(
                                                                    imageVector = Icons.Default.Download,
                                                                    contentDescription = "Download Package",
                                                                    tint = SilkPrimary,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            },
                                                            cornerRadius = 6.dp,
                                                            elevation = 1.dp
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            // Progress bar indicating download state
                                            if (isDownloading) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LinearProgressIndicator(
                                                    progress = { downloadProgress ?: 0.0f },
                                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                                    color = SilkPrimary,
                                                    trackColor = SilkOutlineVariant.copy(alpha = 0.3f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }

            // Persistence Search History Dropdown Overlay
            AnimatedVisibility(
                visible = isSearchFocused && uiState.isSearchHistoryEnabled && uiState.searchQuery.isEmpty() && uiState.searchHistory.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .heightIn(max = 240.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SilkBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Search History Icon",
                                    tint = SilkPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RECENT SEARCHES",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = SilkPrimary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                            Text(
                                text = "Clear All",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFD32F2F),
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.clickable { viewModel.clearSearchHistory() }
                            )
                        }
                        
                        HorizontalDivider(color = SilkOutlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                        
                        LazyColumn {
                            items(uiState.searchHistory) { historyQuery ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onSearchQueryChange(historyQuery)
                                            viewModel.executePlacedQuery(historyQuery)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "Recent Search Item",
                                            tint = SilkOnSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = historyQuery,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = SilkOnSurface,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeFromSearchHistory(historyQuery) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete Search Item",
                                            tint = SilkOnSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Searching Autocomplete Dropdown overlay
            AnimatedVisibility(
                visible = uiState.isSearching && uiState.searchResults.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .heightIn(max = 280.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SilkBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    val cityVal = uiState.userCity
                    val countryVal = uiState.userCountry

                    val cityResults = uiState.searchResults.filter {
                        it.address.contains(cityVal, ignoreCase = true) || it.name.contains(cityVal, ignoreCase = true)
                    }
                    val countryResults = uiState.searchResults.filter {
                        (it.address.contains(countryVal, ignoreCase = true) || it.name.contains(countryVal, ignoreCase = true)) &&
                        !cityResults.contains(it)
                    }
                    val otherResults = uiState.searchResults.filter {
                        !cityResults.contains(it) && !countryResults.contains(it)
                    }

                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        // 1. City Results
                        if (cityResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "📍 Na sua Cidade ($cityVal)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = SilkPrimary,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(cityResults) { item ->
                                SearchResultItemRow(item, focusManager, viewModel)
                            }
                        }

                        // 2. Country Results
                        if (countryResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "🗺️ No seu País ($countryVal)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = SilkOnSurfaceVariant,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(countryResults) { item ->
                                SearchResultItemRow(item, focusManager, viewModel)
                            }
                        }

                        // 3. Global Results
                        if (otherResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "🌐 No resto do Mundo",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = SilkOnSurfaceVariant.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(otherResults) { item ->
                                SearchResultItemRow(item, focusManager, viewModel)
                            }
                        }
                    }
                }
            }
        }

        // 5. Active Selected Place Banner Overlay / Custom Turn-by-Turn HUD Overlay
        if (uiState.isNavigationActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("active_navigation_bottom_hud")
            ) {
                val distanceMeters = uiState.navigationRemainingDistanceMeters
                val distanceText = if (distanceMeters < 100) {
                    "Arriving soon"
                } else {
                    val distanceKm = distanceMeters / 1000.0
                    val distanceMiles = distanceMeters / 1609.34
                    "${String.format("%.1f", distanceKm)} km (${String.format("%.1f", distanceMiles)} mi)"
                }

                val durationSeconds = uiState.navigationRemainingDurationSeconds
                val durationMin = max(1, (durationSeconds / 60.0).roundToInt())
                val durationText = if (durationMin > 60) {
                    "${durationMin / 60}h ${durationMin % 60}m"
                } else {
                    "$durationMin min"
                }

                val etaCalendar = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.SECOND, durationSeconds.toInt())
                }
                val etaHour = etaCalendar.get(java.util.Calendar.HOUR_OF_DAY)
                val etaMinute = etaCalendar.get(java.util.Calendar.MINUTE)
                val etaText = String.format("%02d:%02d", etaHour, etaMinute)

                NeomorphicCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00B0FF))
                                )
                                Text(
                                    text = "NAVIGATING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SilkPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = durationText,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color(0xFFE91E63),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                    Text(
                                        text = "ETA $etaText",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = SilkOnSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(32.dp)
                                        .background(SilkOutlineVariant)
                                )

                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = distanceText,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = SilkOnSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "remaining",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = SilkOnSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { uiState.navigationProgressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SilkSecondary,
                                trackColor = SilkOutlineVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Start",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SilkOnSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = uiState.activeDestination?.name ?: "Destination",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SilkOnSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        NeomorphicButton(
                            onClick = { viewModel.stopNavigation() },
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 12.dp,
                            backgroundColor = Color(0xFFD32F2F)
                        ) {
                            Text(
                                text = "End Navigation",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        } else if (uiState.activeDestination != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                val destination = uiState.activeDestination!!
                val isSaved = uiState.savedPlaces.any { it.name == destination.name || (it.latitude == destination.latitude && it.longitude == destination.longitude) }

                // Calculate real distance
                val distanceMiles = com.example.network.MapNetworkConfig.calculateHaversineDistance(
                    uiState.userLocation.first, uiState.userLocation.second,
                    destination.latitude, destination.longitude
                ) * 1.25 // simulated street winding factor
                
                val distanceText = if (distanceMiles < 0.1) {
                    "Nearby"
                } else {
                    val distanceKm = distanceMiles * 1.60934
                    if (distanceKm < 1.0) "${(distanceKm * 1000).roundToInt()} m" else "${String.format("%.1f", distanceKm)} km"
                }

                // Dynamic speed configuration
                val speedMps = when (uiState.travelMode) {
                    TravelMode.BICYCLING -> 4.5  // bicycling at ~10 mph
                    TravelMode.WALKING -> 1.3    // walking at ~3 mph
                    else -> 15.0                // driving at ~34 mph
                }

                val totalMeters = distanceMiles * 1609.34
                val durationSec = totalMeters / speedMps
                val durationMin = max(2, (durationSec / 60.0).roundToInt())
                val durationText = if (durationMin > 60) {
                    val hrs = durationMin / 60
                    val mins = durationMin % 60
                    if (mins > 0) "${hrs}h ${mins}m" else "${hrs}h"
                } else {
                    "$durationMin mins"
                }

                val routeTypeText = when (uiState.travelMode) {
                    TravelMode.BICYCLING -> "Cycleways & bike lanes"
                    TravelMode.WALKING -> "Continuous sidewalks"
                    else -> "Fastest route via highway"
                }

                NeomorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_place_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    // Launch comprehensive routes details flow
                                    viewModel.triggerRouteCalculation(destination)
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .neomorphicInset(12.dp, 3.dp, SilkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                val navigationIcon = when (uiState.travelMode) {
                                    TravelMode.BICYCLING -> Icons.Filled.DirectionsBike
                                    TravelMode.WALKING -> Icons.Filled.DirectionsWalk
                                    else -> Icons.Filled.DirectionsCar
                                }
                                Icon(
                                    imageVector = navigationIcon,
                                    contentDescription = "Travel Mode Type Icon",
                                    tint = SilkTertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = destination.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = SilkOnSurface
                                    )
                                )
                                Text(
                                    text = routeTypeText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SilkOnSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        // Star Favorite Quick Action on the active selection banner
                        IconButton(
                            onClick = {
                                if (isSaved) {
                                    val savedItem = uiState.savedPlaces.find { it.name == destination.name || (it.latitude == destination.latitude && it.longitude == destination.longitude) }
                                    if (savedItem != null) {
                                        viewModel.removeFavoritePlace(savedItem)
                                    }
                                } else {
                                    viewModel.savePlaceAsFavorite(destination)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp).testTag("favorite_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "Toggle favorite place status",
                                tint = if (isSaved) Color(0xFFFFB300) else SilkOnSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Separation Divider line
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(SilkOutlineVariant)
                            )

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = distanceText,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = SilkPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = durationText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SilkOnSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Floating Action Camera Control Buttons and AI Assistant Trigger
        // Vertically centered on the right side of the screen with zIndex(5f) so they are always accessible, legible, and never overlap elements
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .zIndex(5f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // PULSATING NEOMORPHIC AILA AI CHAT TRIGGER BUTTON
            NeomorphicIconButton(
                onClick = { viewModel.toggleChatDialog(true) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = "Aila AI Chat assistant",
                        tint = Color(0xFFC2185B),
                        modifier = Modifier.size(22.dp)
                    )
                },
                testTag = "aila_ai_chat_btn",
                cornerRadius = 14.dp,
                elevation = 6.dp
            )

            NeomorphicIconButton(
                onClick = { viewModel.zoomIn() },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Zoom In", tint = SilkPrimary) },
                cornerRadius = 12.dp
            )
            
            // Fixed correct negative Zoom icon to standard minus symbol
            NeomorphicIconButton(
                onClick = { viewModel.zoomOut() },
                icon = {
                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        Text("-", style = MaterialTheme.typography.headlineMedium.copy(color = SilkPrimary, fontWeight = FontWeight.Bold))
                    }
                },
                cornerRadius = 12.dp
            )

            if (uiState.isNavigationActive || uiState.computedRoutes.isNotEmpty()) {
                NeomorphicIconButton(
                    onClick = { viewModel.toggleBirdsEyeView() },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Map,
                            contentDescription = "Toggle Bird's Eye View",
                            tint = if (uiState.isBirdsEyeView) Color(0xFF1976D2) else SilkPrimary
                        )
                    },
                    testTag = "birds_eye_view_toggle",
                    cornerRadius = 12.dp
                )
            }
            
            NeomorphicIconButton(
                onClick = { viewModel.centerOnUser() },
                icon = { Icon(Icons.Filled.LocationOn, contentDescription = "My Location", tint = SilkPrimary) },
                testTag = "my_location_button",
                cornerRadius = 12.dp
            )
        }

        // 6. Navigation Bottom Bar (Geometric Balance MD3 Style)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(80.dp)
                .background(SilkBackground)
                .drawBehind {
                    // Thin top border line matching #E7E0EC
                    drawLine(
                        color = Color(0xFFE7E0EC),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Explore Tab (ACTIVE)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { /* Already here */ }
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFE8DEF8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Explore",
                            tint = Color(0xFF1D192B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Explore",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF1D192B),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Favorites Tab (Click navigates to list layout)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setScreen(ActiveScreen.FAVORITES) }
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Favorites",
                            tint = SilkOnSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Favorites",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SilkOnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Profile Tab (Navigates to profile display)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setScreen(ActiveScreen.PROFILE) }
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = SilkOnSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SilkOnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // Brand Footnote Signature centring above the Bottom Tab Bar
        Text(
            text = "Made by Aila Company Solution",
            style = MaterialTheme.typography.labelSmall.copy(
                color = SilkOnSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 82.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // 7. Aila Conversational AI assistant pop-up card
        if (uiState.isChatOpen) {
            AilaChatDialog(
                messages = uiState.chatMessages,
                isLoading = uiState.chatLoading,
                onDismiss = { viewModel.toggleChatDialog(false) },
                onSendMessage = { text -> viewModel.sendAilaChatMessage(text) }
            )
        }
    }
}

@Composable
fun SearchResultItemRow(
    item: com.example.network.SearchResult,
    focusManager: FocusManager,
    viewModel: AilaMapViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                focusManager.clearFocus()
                viewModel.selectSearchResult(item)
            }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when(item.type) {
                "HOME" -> Icons.Default.Home
                "WORK" -> Icons.Default.Star
                "PARK" -> Icons.Default.Favorite
                else -> Icons.Default.LocationOn
            },
            contentDescription = "Pin type",
            tint = SilkPrimary,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = SilkOnSurface)
            )
            Text(
                text = item.address,
                style = MaterialTheme.typography.labelMedium.copy(color = SilkOnSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(color = SilkOutlineVariant.copy(alpha = 0.3f))
}

@Composable
fun AilaChatDialog(
    messages: List<com.example.ui.viewmodel.ChatMessage>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var textVal by remember { mutableStateOf("") }
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Auto-scroll to latest messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFE1FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = "AI Spark",
                            tint = Color(0xFFC2185B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Aila AI Assistant",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = SilkOnSurface,
                            fontSize = 18.sp
                        )
                    )
                }
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SilkOnSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                // Info banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(SilkPrimary.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Ask Aila AI directions, route specifics, distances, map specs, and coordinate information easily in real-time!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SilkPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }

                // Chat bubble list
                androidx.compose.foundation.lazy.LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(messages) { msg ->
                        val isUser = msg.sender == "USER"
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .background(
                                        color = if (isUser) Color(0xFFE8DEF8) else Color(0xFFF4F3F7),
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isUser) 16.dp else 0.dp,
                                            bottomEnd = if (isUser) 0.dp else 16.dp
                                        )
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isUser) Color(0xFF1D192B) else SilkOnSurface,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color(0xFFC2185B),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Aila is writing...",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SilkOnSurfaceVariant.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        NeomorphicTextField(
                            value = textVal,
                            onValueChange = { textVal = it },
                            placeholder = "Pergunte algo para a Aila AI...",
                            cornerRadius = 12.dp
                        )
                    }

                    NeomorphicIconButton(
                        onClick = {
                            if (textVal.trim().isNotEmpty()) {
                                onSendMessage(textVal.trim())
                                textVal = ""
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = SilkPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        cornerRadius = 12.dp,
                        elevation = 4.dp
                    )
                }
            }
        },
        confirmButton = {},
        containerColor = SilkBackground,
        shape = RoundedCornerShape(24.dp)
    )
}
