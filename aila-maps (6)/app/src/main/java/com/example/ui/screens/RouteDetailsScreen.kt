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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.NeomorphicButton
import com.example.ui.components.NeomorphicCard
import com.example.ui.components.NeomorphicIconButton
import com.example.ui.components.neomorphicInset
import com.example.ui.components.neomorphicRaised
import com.example.ui.components.AilaInteractiveMap
import com.example.ui.theme.*
import com.example.ui.viewmodel.ActiveScreen
import com.example.ui.viewmodel.AilaMapViewModel
import com.example.ui.viewmodel.TravelMode

@Composable
fun RouteDetailsScreen(
    viewModel: AilaMapViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SilkBackground)
            .testTag("route_details_screen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // 1. Header Toolbar
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NeomorphicIconButton(
                        onClick = { viewModel.setScreen(ActiveScreen.EXPLORE) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back back to map",
                                tint = SilkPrimary
                            )
                        },
                        testTag = "route_details_back_btn"
                    )

                    Text(
                        text = "Route Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = SilkOnSurface
                        )
                    )
                }

                // AI Active soft UI tag badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .neomorphicInset(12.dp, 2.dp, SilkBackground)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "AI Powered active",
                        tint = SilkPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            color = SilkPrimary
                        )
                    )
                }
            }

            // Travel Mode Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
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
                            .padding(vertical = 10.dp)
                            .testTag("route_details_travel_mode_${mode.name}"),
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

            // 2. Main Vertical Scroll containing lists and diagnostics
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Aila AI Routing intro block (Lavendar highlighted Geometric Balance style)
                item {
                    NeomorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFE8DEF8)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .neomorphicInset(22.dp, 2.dp, Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "AI Assistant Logo",
                                    tint = SilkPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = SilkPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "AI Recommendation",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = SilkPrimary,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Aila Smart Routing",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SilkTertiary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "I've analyzed real-time traffic, terrain elevation, and your previous preferences to rank the best paths to your destination.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SilkOnSurfaceVariant,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // Dynamic Route Comparison Dashboard (Conditional upon state configuration)
                if (uiState.isDynamicRouteComparisonEnabled && uiState.computedRoutes.size >= 2) {
                    item {
                        NeomorphicCard(
                            modifier = Modifier.fillMaxWidth().testTag("route_comparison_card"),
                            backgroundColor = SilkSurface
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CompareArrows,
                                        contentDescription = "Compare",
                                        tint = SilkPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "📊 Route Comparison",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = SilkOnSurface
                                        )
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    uiState.computedRoutes.forEach { route ->
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
                                                        .size(8.dp)
                                                        .background(
                                                            color = when (route.type) {
                                                                "FASTEST" -> Color(0xFF4CAF50)
                                                                "SCENIC" -> Color(0xFF1E88E5)
                                                                else -> Color(0xFFFF9800)
                                                            },
                                                            shape = CircleShape
                                                        )
                                                )
                                                Text(
                                                    text = route.type,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = SilkOnSurface
                                                    )
                                                )
                                            }
                                            
                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "${route.durationMin}m",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SilkPrimary
                                                    )
                                                    Text(
                                                        text = "Time",
                                                        fontSize = 8.sp,
                                                        color = SilkOnSurfaceVariant
                                                    )
                                                }
                                                
                                                Column(horizontalAlignment = Alignment.End) {
                                                    val isEco = uiState.travelMode == TravelMode.WALKING || uiState.travelMode == TravelMode.BICYCLING
                                                    val co2Val = if (isEco) 0.0 else route.distanceMiles * 0.44
                                                    Text(
                                                        text = if (isEco) "0kg" else "${(co2Val + 0.5).toInt()}kg",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF388E3C)
                                                    )
                                                    Text(
                                                        text = "CO₂ Est",
                                                        fontSize = 8.sp,
                                                        color = SilkOnSurfaceVariant
                                                    )
                                                }
                                                
                                                Column(horizontalAlignment = Alignment.End) {
                                                    val isEco = uiState.travelMode == TravelMode.WALKING || uiState.travelMode == TravelMode.BICYCLING
                                                    val gasVal = if (isEco) 0.0 else route.distanceMiles * 0.18
                                                    Text(
                                                        text = String.format("$%.2f", gasVal),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1976D2)
                                                    )
                                                    Text(
                                                        text = "Gas Est",
                                                        fontSize = 8.sp,
                                                        color = SilkOnSurfaceVariant
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

                // Calculated routing choices mapping
                items(uiState.computedRoutes, key = { it.id }) { option ->
                    val isSelected = uiState.selectedRouteId == option.id

                    NeomorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectRoute(option.id) }
                            .testTag("route_card_${option.type}"),
                        backgroundColor = if (isSelected) Color(0xFFE8DEF8) else SilkSurface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Row details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .neomorphicInset(10.dp, 2.dp, if (isSelected) Color.White else SilkBackground)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = option.type,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = when (option.type) {
                                                    "FASTEST" -> SilkPrimary
                                                    "SCENIC" -> SilkTertiary
                                                    else -> SilkOnSurfaceVariant
                                                }
                                            )
                                        }

                                        Text(
                                            text = option.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = SilkOnSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Time",
                                                tint = SilkPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "${option.durationMin} min",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = SilkPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = "Distance",
                                                tint = SilkOnSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "${option.distanceMiles} miles",
                                                style = MaterialTheme.typography.bodySmall.copy(color = SilkOnSurfaceVariant)
                                            )
                                        }
                                    }
                                }

                                // Selector button
                                NeomorphicButton(
                                    onClick = { viewModel.selectRoute(option.id) },
                                    cornerRadius = 10.dp,
                                    elevation = 4.dp,
                                    backgroundColor = if (isSelected) SilkPrimary.copy(alpha = 0.08f) else SilkBackground
                                ) {
                                    Text(
                                        text = if (isSelected) "Active" else "Select",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) SilkPrimary else SilkOnSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            // Horizontal divider
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(SilkOutlineVariant.copy(alpha = 0.3f))
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // AI Review statement
                            Text(
                                text = "\"${option.activeReview}\"",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = SilkOnSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }

                // Visual live diagnostics reports
                item {
                    Text(
                        text = "Visual Corridor Overview",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SilkOnSurface
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Asymmetric visual grid map overview + Side by side diagnostic indicators
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Live interactive preview map layer representing real-time Aila Smart Routing
                        NeomorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                val selectedRoute = uiState.computedRoutes.firstOrNull { it.id == uiState.selectedRouteId }
                                    ?: uiState.computedRoutes.firstOrNull()
                                
                                val (previewLat, previewLon, previewZoom) = remember(selectedRoute) {
                                    val coords = selectedRoute?.coordinates ?: emptyList()
                                    if (coords.isNotEmpty()) {
                                        viewModel.calculateRouteMidpointAndZoom(coords)
                                    } else {
                                        Triple(
                                            uiState.activeDestination?.latitude ?: uiState.userLocation.first,
                                            uiState.activeDestination?.longitude ?: uiState.userLocation.second,
                                            13.0
                                        )
                                    }
                                }
                                
                                AilaInteractiveMap(
                                    centerLat = previewLat,
                                    centerLon = previewLon,
                                    zoom = previewZoom,
                                    startPoint = uiState.userLocation,
                                    endPoint = uiState.activeDestination?.let { Pair(it.latitude, it.longitude) },
                                    routeCoordinates = selectedRoute?.coordinates ?: emptyList(),
                                    savedPlaces = uiState.savedPlaces,
                                    modifier = Modifier.fillMaxSize(),
                                    travelMode = uiState.travelMode,
                                    userBearing = uiState.userBearing,
                                    isTrafficOverlayEnabled = uiState.isTrafficOverlayEnabled
                                )

                                // Live traffic indicator feed overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp)
                                        .neomorphicRaised(8.dp, 3.dp, SilkBackground)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "LIVE TRAFFIC FEED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = SilkPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Side by side diagnostic widget row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Weather block
                            NeomorphicCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.22f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text = uiState.weatherIconEmoji,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (uiState.weatherTemperature != null) {
                                            "${uiState.weatherTemperature}°C"
                                        } else {
                                            "14.5°C"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = SilkOnSurface
                                        )
                                    )
                                    Text(
                                        text = uiState.weatherDescription,
                                        style = MaterialTheme.typography.labelSmall.copy(color = SilkOnSurfaceVariant)
                                    )
                                }
                            }

                            // Road Incident reports block
                            NeomorphicCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.22f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Hazards",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Pothole Alert",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = SilkOnSurface
                                        )
                                    )
                                    Text(
                                        text = "2 reports nearby",
                                        style = MaterialTheme.typography.labelSmall.copy(color = SilkOnSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Wide Back/Close Button directly navigation to Explore map
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                NeomorphicButton(
                    onClick = {
                        viewModel.startNavigation()
                        viewModel.setScreen(ActiveScreen.EXPLORE)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    testTag = "route_details_confirm_btn"
                ) {
                    Text(
                        text = "Accept Route & Navigate",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = SilkPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Text(
                text = "Made by Aila Company Solution",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SilkOnSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )
        }

        // Circular progress indicator overlay when route calculations are loading
        if (uiState.isRoutingLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                NeomorphicCard(
                    backgroundColor = SilkSurface,
                    elevation = 8.dp,
                    cornerRadius = 16.dp
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = SilkPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
        }
    }
}
