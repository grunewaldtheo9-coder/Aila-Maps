package com.example.ui.screens

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.SavedPlace
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ActiveScreen
import com.example.ui.viewmodel.AilaMapViewModel

@Composable
fun FavoritesScreen(
    viewModel: AilaMapViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var localSearchText by remember { mutableStateOf("") }

    // Dialog state controllers
    var newName by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("HOME") }

    val filteredPlaces = if (localSearchText.isEmpty()) {
        uiState.savedPlaces
    } else {
        uiState.savedPlaces.filter {
            it.name.contains(localSearchText, ignoreCase = true) ||
            it.address.contains(localSearchText, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SilkBackground)
            .testTag("favorites_screen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 84.dp) // padding safe area for bottom navigation bar
        ) {
            // 1. Customized Header and Avatar Row
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NeomorphicIconButton(
                        onClick = { viewModel.setScreen(ActiveScreen.EXPLORE) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menu Drawer",
                                tint = SilkPrimary
                            )
                        }
                    )
                    Text(
                        text = "My Favorites",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = SilkOnSurface
                        )
                    )
                }

                // Small user profile avatar display
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SilkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    NeomorphicIconButton(
                        onClick = { viewModel.setScreen(ActiveScreen.PROFILE) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "My Profile Actions",
                                tint = SilkPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        cornerRadius = 10.dp,
                        elevation = 4.dp
                    )
                }
            }

            // 2. Inset search bar for local filtering
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                NeomorphicTextField(
                    value = localSearchText,
                    onValueChange = { localSearchText = it },
                    placeholder = "Search your saved places",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search Icon",
                            tint = SilkOnSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    cornerRadius = 14.dp,
                    testTag = "favorites_search_field"
                )
            }

            // 3. Scrollable list of favorites
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("favorites_list"),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(filteredPlaces, key = { it.id }) { place ->
                    val isParkWithImage = !place.imageUrl.isNullOrEmpty()

                    if (isParkWithImage) {
                        // Asymmetric premium card with dynamic image underlay matching Central Park Mockup 3
                        NeomorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .testTag("favorite_card_image_${place.name}")
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Background Landscape Image
                                AsyncImage(
                                    model = place.imageUrl,
                                    contentDescription = place.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Soft ambient dark glassmorphic gradient overlay under content
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    SilkBackground.copy(alpha = 0.95f)
                                                )
                                            )
                                        )
                                )

                                // Foreground card details
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .neomorphicInset(10.dp, 2.dp, SilkBackground),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Favorite,
                                                contentDescription = place.name,
                                                tint = SilkTertiary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = place.name,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = SilkOnSurface
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = place.address,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = SilkOnSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    NeomorphicButton(
                                        onClick = { viewModel.triggerRouteCalculation(place) },
                                        cornerRadius = 10.dp,
                                        elevation = 4.dp
                                    ) {
                                        Text(
                                            text = "Route",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = SilkPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }

                                // Delete Button on top right
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.removeFavoritePlace(place) },
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.8f), CircleShape)
                                            .size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete Location",
                                            tint = Color.Red.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard Raised Neomorphic Card for default items
                        NeomorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("favorite_card_standard_${place.name}")
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
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .neomorphicInset(12.dp, 3.dp, SilkBackground),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (place.category) {
                                                "HOME" -> Icons.Default.Home
                                                "WORK" -> Icons.Default.Star
                                                "CAFE" -> Icons.Default.Favorite
                                                "GYM" -> Icons.Default.PlayArrow
                                                else -> Icons.Default.LocationOn
                                            },
                                            contentDescription = place.name,
                                            tint = SilkPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = place.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = SilkOnSurface,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = place.address,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = SilkOnSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    NeomorphicButton(
                                        onClick = { viewModel.triggerRouteCalculation(place) },
                                        cornerRadius = 10.dp,
                                        elevation = 4.dp
                                    ) {
                                        Text(
                                            text = "Route",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = SilkPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    IconButton(onClick = { viewModel.removeFavoritePlace(place) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Remove Favourite",
                                            tint = SilkOnSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Large Call-to-Action Add New Place Tactile Button
                item {
                    NeomorphicButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        backgroundColor = SilkBackground,
                        testTag = "open_add_favorite_dialog_btn"
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Icon",
                            tint = SilkPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add New Place",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = SilkPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // 5. Add New Favorite Dialog Modal (Beautiful, minimal clay card overlay)
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = {
                    Text(
                        text = "Add New Favorite",
                        style = MaterialTheme.typography.headlineMedium.copy(color = SilkOnSurface, fontSize = 20.sp)
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        NeomorphicTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = "Place Name (e.g., Beach Cabin)"
                        )
                        NeomorphicTextField(
                            value = newAddress,
                            onValueChange = { newAddress = it },
                            placeholder = "Address or City Name"
                        )
                        
                        // Category Selectors Row
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val categories = listOf("HOME", "WORK", "CAFE", "GYM")
                            categories.forEach { cat ->
                                Button(
                                    onClick = { newCategory = cat },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (newCategory == cat) SilkPrimary else SilkBackground,
                                        contentColor = if (newCategory == cat) Color.White else SilkOnSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(2.dp).weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(text = cat, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newName.isNotEmpty() && newAddress.isNotEmpty()) {
                                viewModel.savePlaceAsFavoriteWithGeocoding(
                                    name = newName,
                                    address = newAddress,
                                    category = newCategory
                                )
                                showAddDialog = false
                                newName = ""
                                newAddress = ""
                            }
                        }
                    ) {
                        Text("Add", color = SilkPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = SilkOnSurfaceVariant)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = SilkBackground
            )
        }

        // 6. Navigation Bottom Bar (Favorites ACTIVE)
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
                // Explore Tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setScreen(ActiveScreen.EXPLORE) }
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Explore",
                            tint = SilkOnSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Explore",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SilkOnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Favorites Tab (ACTIVE)
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
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Favorites",
                            tint = Color(0xFF1D192B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Favorites",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF1D192B),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Profile Tab (Exits)
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

        // Brand Footnote Signature centering above the Bottom Tab Bar
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
    }
}
