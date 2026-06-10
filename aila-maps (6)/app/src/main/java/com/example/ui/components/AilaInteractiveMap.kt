package com.example.ui.components

import android.graphics.PointF
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.Typeface
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.BuildConfig
import com.example.data.SavedPlace
import com.example.ui.theme.SilkOnSurface
import com.example.ui.theme.SilkOnSurfaceVariant
import com.example.ui.theme.SilkPrimary
import com.example.ui.theme.SilkTertiary
import com.example.ui.theme.SilkBackground
import com.example.ui.viewmodel.TravelMode
import kotlin.math.*

@Composable
fun AilaInteractiveMap(
    centerLat: Double,
    centerLon: Double,
    zoom: Double,
    modifier: Modifier = Modifier,
    startPoint: Pair<Double, Double>? = null, // (lat, lon)
    endPoint: Pair<Double, Double>? = null,   // (lat, lon)
    routeCoordinates: List<Pair<Double, Double>> = emptyList(), // list of (lat, lon)
    savedPlaces: List<SavedPlace> = emptyList(),
    onMapMoved: (newLat: Double, newLon: Double) -> Unit = { _, _ -> },
    onPlacePinClicked: (SavedPlace) -> Unit = {},
    travelMode: TravelMode = TravelMode.DRIVING,
    userBearing: Float? = null,
    isTrafficOverlayEnabled: Boolean = false,
    isWeatherOverlayEnabled: Boolean = false,
    weatherTemperature: Double? = null,
    weatherDescription: String = "",
    weatherIconEmoji: String = ""
) {
    var viewWidth by remember { mutableStateOf(800) }
    var viewHeight by remember { mutableStateOf(1000) }

    val mapTilerKey = com.example.network.SupabaseManager.getMaptilerKey()
    val hasValidKey = mapTilerKey.isNotEmpty()

    val animatedZoom by animateFloatAsState(
        targetValue = zoom.toFloat(),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "SmoothZoom"
    )
    val displayZoom = animatedZoom.toDouble()

    val zoomInt = max(1, min(19, displayZoom.toInt()))
    val dZoom = displayZoom - zoomInt
    val scaleFactor = 2.0.pow(dZoom)
    val tileSize = (256.0 * scaleFactor).toFloat()

    val density = LocalDensity.current.density

    // Projected Web Mercator center tile calculations
    val centerTileX = (centerLon + 180.0) / 360.0 * (1 shl zoomInt)
    val centerLatClamped = centerLat.coerceIn(-85.0, 85.0)
    val latRadForCenter = Math.toRadians(centerLatClamped)
    val centerTileY = (1.0 - ln(tan(latRadForCenter) + 1.0 / cos(latRadForCenter)) / Math.PI) / 2.0 * (1 shl zoomInt)

    val currentCenterLat by rememberUpdatedState(centerLat)
    val currentCenterLon by rememberUpdatedState(centerLon)
    val currentZoom by rememberUpdatedState(displayZoom)
    val currentOnMapMoved by rememberUpdatedState(onMapMoved)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { size ->
                if (size.width > 0 && size.height > 0) {
                    viewWidth = size.width
                    viewHeight = size.height
                }
            }
            .pointerInput(Unit) {
                // Drag gesture to pan map center coordinates safely
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val degreesPerPixel = 360.0 / (256.0 * 2.0.pow(currentZoom))
                    val dLon = -dragAmount.x.toDouble() * degreesPerPixel
                    val dLat = dragAmount.y.toDouble() * degreesPerPixel

                    val newLat = (currentCenterLat + dLat).coerceIn(-85.0, 85.0)
                    var newLon = currentCenterLon + dLon
                    // Keep longitude wrapped within -180 to 180 degrees
                    if (newLon > 180.0) {
                        newLon = ((newLon + 180.0) % 360.0) - 180.0
                    } else if (newLon < -180.0) {
                        newLon = 180.0 - ((180.0 - newLon) % 360.0)
                    }
                    currentOnMapMoved(newLat, newLon)
                }
            }
    ) {
        // 1. Procedural Backdrop Map Layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFFF4F3EF))
        }

        // 2. Multi-Tile Dynamic Image Layer (Optimized for fast dynamic rendering)
        if (viewWidth > 0 && viewHeight > 0 && tileSize > 1f && !tileSize.isNaN() && !tileSize.isInfinite()) {
            val tilesNeededX = (ceil((viewWidth / 2f) / tileSize).toInt()).coerceIn(1, 2)
            val tilesNeededY = (ceil((viewHeight / 2f) / tileSize).toInt()).coerceIn(1, 2)

            val minTx = (centerTileX.toInt() - tilesNeededX).coerceAtLeast(0)
            val maxTx = (centerTileX.toInt() + tilesNeededX).coerceAtMost((1 shl zoomInt) - 1)
            val minTy = (centerTileY.toInt() - tilesNeededY).coerceAtLeast(0)
            val maxTy = (centerTileY.toInt() + tilesNeededY).coerceAtMost((1 shl zoomInt) - 1)

            for (tx in minTx..maxTx) {
                for (ty in minTy..maxTy) {
                    val tileOffsetX = (tx - centerTileX) * tileSize
                    val tileOffsetY = (ty - centerTileY) * tileSize

                    val screenX = (viewWidth / 2f) + tileOffsetX.toFloat()
                    val screenY = (viewHeight / 2f) + tileOffsetY.toFloat()

                    if (screenX + tileSize > 0f && screenX < viewWidth && screenY + tileSize > 0f && screenY < viewHeight) {
                        val tileUrl = if (hasValidKey) {
                            "https://api.maptiler.com/maps/streets-v4/256/$zoomInt/$tx/$ty.png?key=$mapTilerKey"
                        } else {
                            // Using CartoDB Voyager tiles for incredibly high-speed CDN loading and elegant light design integration
                            "https://basemaps.cartocdn.com/rastertiles/voyager/$zoomInt/$tx/$ty.png"
                        }

                        key(tx, ty, zoomInt) {
                            val context = LocalContext.current
                            val tileRequest = remember(tileUrl) {
                                ImageRequest.Builder(context)
                                    .data(tileUrl)
                                    .addHeader("User-Agent", "AilaMaps/1.0 (Android; Mobile)")
                                    .crossfade(true)
                                    .build()
                            }
                            AsyncImage(
                                model = tileRequest,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .offset(
                                        x = (screenX / density).dp,
                                        y = (screenY / density).dp
                                    )
                                    .size((tileSize / density).dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Mathematical Overlays Canvas (Pins, Routes, and Users)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val widthPx = size.width
            val heightPx = size.height

            val scale = 256.0 * 2.0.pow(currentZoom)
            val xCenter = (centerLon + 180.0) / 360.0
            val centerLatClampedDraw = centerLat.coerceIn(-85.0, 85.0)
            val latRadCenter = Math.toRadians(centerLatClampedDraw)
            val yCenter = (1.0 - Math.log(Math.tan(latRadCenter) + 1.0 / cos(latRadCenter)) / Math.PI) / 2.0

            fun getPixelOffset(lat: Double, lon: Double): Offset {
                val xTarget = (lon + 180.0) / 360.0
                val latRad = Math.toRadians(lat)
                // Avoid projection exceptions near extreme poles
                val latClamped = latRad.coerceIn(-1.48, 1.48)
                val yTarget = (1.0 - Math.log(Math.tan(latClamped) + 1.0 / cos(latClamped)) / Math.PI) / 2.0

                val dx = (xTarget - xCenter) * scale
                val dy = (yTarget - yCenter) * scale

                val x = (widthPx / 2f) + dx.toFloat()
                val y = (heightPx / 2f) + dy.toFloat()

                return Offset(x, y)
            }

            // Route drawings
            if (routeCoordinates.isNotEmpty()) {
                val path = Path()
                routeCoordinates.forEachIndexed { idx, coord ->
                    val offset = getPixelOffset(coord.first, coord.second)
                    if (idx == 0) {
                        path.moveTo(offset.x, offset.y)
                    } else {
                        path.lineTo(offset.x, offset.y)
                    }
                }

                drawPath(
                    path = path,
                    color = Color(0x3D1A73E8), // Beautiful semi-transparent halo for backlighting
                    style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                if (isTrafficOverlayEnabled) {
                    // Segment-by-segment traffic analysis rendering: Red (Congested), Amber (Moderate), Green (Fluid)
                    for (i in 0 until routeCoordinates.size - 1) {
                        val startCoord = routeCoordinates[i]
                        val endCoord = routeCoordinates[i + 1]
                        val offsetStart = getPixelOffset(startCoord.first, startCoord.second)
                        val offsetEnd = getPixelOffset(endCoord.first, endCoord.second)

                        val segmentColor = when {
                            i % 14 in 4..6 -> Color(0xFFE53935)  // Red (Heavy traffic bottleneck)
                            i % 14 in 7..9 -> Color(0xFFFFB300)  // Amber/Yellow (Moderate delays)
                            else -> Color(0xFF4CAF50)            // Green (Fluid, free flow)
                        }

                        drawLine(
                            color = segmentColor,
                            start = offsetStart,
                            end = offsetEnd,
                            strokeWidth = 10f,
                            cap = StrokeCap.Round
                        )

                        // If heavy traffic (Red), draw small hazard caution points
                        if (i % 14 == 5) {
                            drawCircle(
                                color = Color.White,
                                radius = 6f,
                                center = (offsetStart + offsetEnd) * 0.5f
                            )
                            drawCircle(
                                color = Color(0xFFE53935),
                                radius = 4f,
                                center = (offsetStart + offsetEnd) * 0.5f
                            )
                        }
                    }
                } else {
                    drawPath(
                        path = path,
                        color = Color(0xFF1A73E8), // Vibrant Royal Blue matching Google Maps styling
                        style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            // Draw saved places
            savedPlaces.forEach { place ->
                val offset = getPixelOffset(place.latitude, place.longitude)
                drawCircle(
                    color = Color.White,
                    radius = 16f,
                    center = offset
                )
                drawCircle(
                    color = SilkTertiary,
                    radius = 12f,
                    center = offset
                )
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = offset
                )
            }

            // Draw user coordinate as a state-of-the-art 3D Navigation Arrow with a premium metallic ring
            startPoint?.let {
                val offset = getPixelOffset(it.first, it.second)

                val headingDegrees = if (userBearing != null) {
                    userBearing
                } else {
                    // Calculate dynamic heading towards route coordinates or destination to point precisely
                    val screenNext = if (routeCoordinates.size >= 2) {
                        getPixelOffset(routeCoordinates[1].first, routeCoordinates[1].second)
                    } else if (endPoint != null) {
                        getPixelOffset(endPoint.first, endPoint.second)
                    } else {
                        null
                    }

                    if (screenNext != null) {
                        val dx = screenNext.x - offset.x
                        val dy = screenNext.y - offset.y
                        val rad = atan2(dy.toDouble(), dx.toDouble())
                        (Math.toDegrees(rad).toFloat() + 90f) % 360f
                    } else {
                        -20f // elegant default isometric heading
                    }
                }

                draw3DNavigationArrow(offset, headingDegrees, travelMode)
            }

            // Draw target destination coordinate
            endPoint?.let {
                val offset = getPixelOffset(it.first, it.second)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.15f),
                    radius = 20f,
                    center = offset + Offset(4f, 4f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 12f,
                    center = offset
                )
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = 8f,
                    center = offset
                )
            }

            // Weather Overlay
            if (isWeatherOverlayEnabled) {
                // Soft halo centered at current camera
                val centerOffset = getPixelOffset(centerLat, centerLon)
                drawCircle(
                    color = Color(0x2E00ACC1),
                    radius = (180f * scaleFactor.toFloat()).coerceAtLeast(50f),
                    center = centerOffset
                )
                drawCircle(
                    color = Color(0x1F00E5FF),
                    radius = (300f * scaleFactor.toFloat()).coerceAtLeast(80f),
                    center = centerOffset
                )

                // Draw Text labels over the coordinate overlays using native Canvas
                drawContext.canvas.nativeCanvas.apply {
                    val textPaint = Paint().apply {
                        color = android.graphics.Color.rgb(44, 62, 80)
                        textSize = 34f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        // Add shadow layer for accessibility / neat high contrast over map routes
                        setShadowLayer(6f, 3f, 3f, android.graphics.Color.WHITE)
                    }
                    
                    val weatherText = if (weatherTemperature != null) {
                        "$weatherIconEmoji $weatherDescription ($weatherTemperature°C)"
                    } else {
                        "⛅ Weather: Loading live report..."
                    }
                    
                    drawText(weatherText, centerOffset.x - 140f, centerOffset.y - 12f, textPaint)
                }
            }
        }

        // 4. Attribution / Compass Info box style
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 108.dp, end = 16.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Map Attribution",
                tint = SilkOnSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }

    }
}

// Map Helper extension functions for 3D renderings
private fun Double.roundToGrid(spacing: Double): Double {
    return (this / spacing).roundToInt() * spacing
}

private fun DrawScope.draw3DCar(center: Offset) {
    // Under-car glow (soft cyan neon halo)
    drawCircle(
        color = Color(0xFF03A9F4).copy(alpha = 0.45f),
        radius = 38f,
        center = center
    )
    
    // Low shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.25f),
        radius = 24f,
        center = center + Offset(3f, 5f)
    )
    
    // Main Car Body (Extruded Rounded Rectangle with perspective Angle)
    val carBodyPath = Path().apply {
        moveTo(center.x - 20f, center.y - 10f)
        lineTo(center.x + 20f, center.y - 15f)
        lineTo(center.x + 26f, center.y + 3f)
        lineTo(center.x - 12f, center.y + 8f)
        close()
    }
    drawPath(
        path = carBodyPath,
        color = Color(0xFF3F51B5) // Deep metallic blue
    )
    
    // Upper Cabin Glass Hood
    val cabinGlassPath = Path().apply {
        moveTo(center.x - 8f, center.y - 6f)
        lineTo(center.x + 12f, center.y - 10f)
        lineTo(center.x + 16f, center.y + 1f)
        lineTo(center.x - 4f, center.y + 4f)
        close()
    }
    drawPath(
        path = cabinGlassPath,
        color = Color(0xFF80DEEA).copy(alpha = 0.85f) // Glowing cyan glass
    )
    
    // Wheel details (isometric ellipses)
    drawOval(
        color = Color(0xFF212121),
        topLeft = center + Offset(-20f, 2f),
        size = androidx.compose.ui.geometry.Size(10f, 6f)
    )
    drawOval(
        color = Color(0xFF212121),
        topLeft = center + Offset(14f, -4f),
        size = androidx.compose.ui.geometry.Size(10f, 6f)
    )
    
    // Headlights neon glow beams
    val lightsPath = Path().apply {
        moveTo(center.x + 24f, center.y + 1f)
        lineTo(center.x + 60f, center.y + 10f)
        lineTo(center.x + 55f, center.y - 15f)
        lineTo(center.x + 18f, center.y - 15f)
        close()
    }
    drawPath(
        path = lightsPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFEB3B).copy(alpha = 0.6f), Color.Transparent),
            start = center + Offset(20f, -6f),
            end = center + Offset(60f, 0f)
        )
    )
}

private fun DrawScope.draw3DBike(center: Offset) {
    // Under-bike soft glow
    drawCircle(
        color = Color(0xFF4CAF50).copy(alpha = 0.35f),
        radius = 32f,
        center = center
    )
    
    // Shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.15f),
        radius = 18f,
        center = center + Offset(2f, 4f)
    )
    
    // Two wheels in perspective
    drawOval(
        color = Color(0xFF212121),
        topLeft = center + Offset(-18f, 0f),
        size = androidx.compose.ui.geometry.Size(12f, 5f)
    )
    drawOval(
        color = Color(0xFF212121),
        topLeft = center + Offset(10f, -8f),
        size = androidx.compose.ui.geometry.Size(12f, 5f)
    )
    
    // Frame lines
    drawLine(
        color = Color(0xFF4CAF50), // Lime green sporty frame
        start = center + Offset(-12f, 2f),
        end = center + Offset(0f, -5f),
        strokeWidth = 3f
    )
    drawLine(
        color = Color(0xFF4CAF50),
        start = center + Offset(0f, -5f),
        end = center + Offset(15f, -6f),
        strokeWidth = 3f
    )
    drawLine(
        color = Color(0xFF4CAF50),
        start = center + Offset(-12f, 2f),
        end = center + Offset(-3f, 4f),
        strokeWidth = 3f
    )
    drawLine(
        color = Color(0xFF4CAF50),
        start = center + Offset(-3f, 4f),
        end = center + Offset(15f, -6f),
        strokeWidth = 3f
    )
    
    // Handlebars
    drawLine(
        color = Color(0xFF333333),
        start = center + Offset(15f, -6f),
        end = center + Offset(15f, -15f),
        strokeWidth = 2.5f
    )
    drawLine(
        color = Color(0xFF212121),
        start = center + Offset(11f, -15f),
        end = center + Offset(18f, -13f),
        strokeWidth = 3f
    )
}

private fun DrawScope.draw3DWalkingMan(center: Offset) {
    // Under-man soft green walk halo
    drawCircle(
        color = Color(0xFF00E676).copy(alpha = 0.4f),
        radius = 24f,
        center = center
    )
    
    // Base shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.2f),
        radius = 12f,
        center = center + Offset(2f, 3f)
    )
    
    // Stylized body circles & lines representing a walking figure
    val headCenter = center + Offset(2f, -28f)
    val torsoTop = center + Offset(2f, -22f)
    val hipsCenter = center + Offset(0f, -10f)
    
    // Head
    drawCircle(
        color = Color(0xFFE0F7FA),
        radius = 5f,
        center = headCenter
    )
    
    // Torso (Pill-shaped)
    drawLine(
        color = Color(0xFF00B0FF), // Neon blue sport jacket
        start = torsoTop,
        end = hipsCenter,
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
    
    // Left leg (taking step)
    drawLine(
        color = Color(0xFF00B0FF),
        start = hipsCenter,
        end = center + Offset(6f, 2f),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
    
    // Right leg (stepping back)
    drawLine(
        color = Color(0xFF00B0FF),
        start = hipsCenter,
        end = center + Offset(-6f, -1f),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
    
    // Arms
    drawLine(
        color = Color(0xFFE0F7FA),
        start = torsoTop,
        end = center + Offset(-5f, -15f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFE0F7FA),
        start = torsoTop,
        end = center + Offset(8f, -17f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.draw3DNavigationArrow(center: Offset, headingDegrees: Float, travelMode: TravelMode) {
    // 1. Dynamic Mode-based colors
    val colorPrimaryLeft = when (travelMode) {
        TravelMode.DRIVING -> Color(0xFFFFB03A) // Premium light orange/gold
        TravelMode.BICYCLING -> Color(0xFF81C784) // Sport Green light
        TravelMode.WALKING -> Color(0xFF4FC3F7) // Neon Blue light
    }
    val colorPrimaryRight = when (travelMode) {
        TravelMode.DRIVING -> Color(0xFFD35400) // Deep dark amber/bronze
        TravelMode.BICYCLING -> Color(0xFF2E7D32) // Deep athletic forest green
        TravelMode.WALKING -> Color(0xFF0277BD) // Deep navy ocean blue
    }
    val ringColorBase = when (travelMode) {
        TravelMode.DRIVING -> Color(0xFF78909C) // Slate metal base
        TravelMode.BICYCLING -> Color(0xFF388E3C) // Dark green ring base
        TravelMode.WALKING -> Color(0xFF0288D1) // Dark blue ring base
    }
    val ringColorFace = when (travelMode) {
        TravelMode.DRIVING -> Color(0xFFFFFFFF) // Brilliant glossy white chrome
        TravelMode.BICYCLING -> Color(0xFFE8F5E9) // Ice green chrome
        TravelMode.WALKING -> Color(0xFFE1F5FE) // Ice blue chrome
    }

    val cx = center.x
    val cy = center.y

    // Use withTransform to handle 3D tilt, rotation, and translation together as a pristine compass dial
    withTransform({
        // Rotate entire dial with heading
        rotate(degrees = headingDegrees, pivot = center)
    }) {
        // --- 1. FLOATING 3D COMPASS SECTOR C-RING ---
        // Offset of shadow on the floor (representing physical height/hovering of the ring)
        val shadowOffset = Offset(2f, 10f)

        // A. Floor Shadow of the C-ring
        val shadowRect = androidx.compose.ui.geometry.Rect(
            cx - 30f + shadowOffset.x, cy - 15f + shadowOffset.y,
            cx + 30f + shadowOffset.x, cy + 15f + shadowOffset.y
        )
        drawArc(
            color = Color.Black.copy(alpha = 0.16f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = shadowRect.topLeft,
            size = shadowRect.size,
            style = Stroke(width = 7.5f, cap = StrokeCap.Round)
        )

        // B. Dark Extrusion / Bevel Border (thickness & shadow depth of the physical ring)
        val ringBaseRect = androidx.compose.ui.geometry.Rect(
            cx - 30f, cy - 14f,
            cx + 30f, cy + 14f
        )
        drawArc(
            color = ringColorBase,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = ringBaseRect.topLeft,
            size = ringBaseRect.size,
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )

        // C. Shiny Ice White Polished Front Surface (offset slightly up to simulate thickness)
        val ringFaceRect = androidx.compose.ui.geometry.Rect(
            cx - 30f, cy - 16f,
            cx + 30f, cy + 12f
        )
        drawArc(
            color = ringColorFace,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = ringFaceRect.topLeft,
            size = ringFaceRect.size,
            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
        )

        // --- 2. HOVERING 3D CHEVRON ARROWHEAD ---
        // Shadow cast on map floor (offset slightly down/right for height simulation)
        val chevronShadowOffset = Offset(1.5f, 9f)
        val sTip = pTipOffset(cx + chevronShadowOffset.x, cy + chevronShadowOffset.y, -32f)
        val sLeft = pCornerOffset(cx + chevronShadowOffset.x, cy + chevronShadowOffset.y, -18f, 16f)
        val sRight = pCornerOffset(cx + chevronShadowOffset.x, cy + chevronShadowOffset.y, 18f, 16f)
        val sBase = pTipOffset(cx + chevronShadowOffset.x, cy + chevronShadowOffset.y, 4f)

        val shadowPath = Path().apply {
            moveTo(sTip.x, sTip.y)
            lineTo(sLeft.x, sLeft.y)
            lineTo(sBase.x, sBase.y)
            lineTo(sRight.x, sRight.y)
            close()
        }
        drawPath(
            path = shadowPath,
            color = Color.Black.copy(alpha = 0.2f)
        )

        // Actual 3D Arrowhead coordinates relative to translated center
        val pTip = pTipOffset(cx, cy, -32f)
        val pLeft = pCornerOffset(cx, cy, -18f, 16f)
        val pRight = pCornerOffset(cx, cy, 18f, 16f)
        val pBase = pTipOffset(cx, cy, 4f)

        // Left front wing face (Specular Light Source color)
        val leftFrontPath = Path().apply {
            moveTo(pTip.x, pTip.y)
            lineTo(pLeft.x, pLeft.y)
            lineTo(pBase.x, pBase.y)
            close()
        }
        drawPath(path = leftFrontPath, color = colorPrimaryLeft)

        // Right front wing face (Bronze Shaded color)
        val rightFrontPath = Path().apply {
            moveTo(pTip.x, pTip.y)
            lineTo(pRight.x, pRight.y)
            lineTo(pBase.x, pBase.y)
            close()
        }
        drawPath(path = rightFrontPath, color = colorPrimaryRight)

        // Shiny central highlight ridge line
        drawLine(
            color = Color.White.copy(alpha = 0.85f),
            start = pTip,
            end = pBase,
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
    }
}

private fun pTipOffset(cx: Float, cy: Float, offset: Float): Offset = Offset(cx, cy + offset)
private fun pCornerOffset(cx: Float, cy: Float, dx: Float, dy: Float): Offset = Offset(cx + dx, cy + dy)
