package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ActiveScreen
import com.example.ui.viewmodel.AilaMapViewModel

@Composable
fun ProfileScreen(
    viewModel: AilaMapViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showEditDialog by remember { mutableStateOf(false) }
    var tempSelectedAvatar by remember { mutableStateOf(uiState.profileAvatarUrl) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            tempSelectedAvatar = uri.toString()
        }
    }

    LaunchedEffect(showEditDialog) {
        if (showEditDialog) {
            tempSelectedAvatar = uiState.profileAvatarUrl
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SilkBackground)
            .testTag("profile_screen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 84.dp) // padding safe area for bottom navigation bar
        ) {
            // 1. Header Row
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
                        text = "My Profile",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = SilkOnSurface
                        )
                    )
                }
            }

            // 2. Scrollable Profile Contents
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Tactical Logo Display Card with Clay Neomorphic Touch
                NeomorphicCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(SilkBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .neomorphicInset(20.dp, 4.dp, SilkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                val isCustomAvatar = !uiState.profileAvatarUrl.startsWith("avatar_")
                                if (isCustomAvatar) {
                                    AsyncImage(
                                        model = uiState.profileAvatarUrl,
                                        contentDescription = "User Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    val avatarIcon = when (uiState.profileAvatarUrl) {
                                        "avatar_1" -> Icons.Default.Android
                                        "avatar_2" -> Icons.Default.LocationOn
                                        "avatar_3" -> Icons.Default.DirectionsBike
                                        "avatar_4" -> Icons.Default.DirectionsCar
                                        else -> Icons.Default.Person
                                    }
                                    Icon(
                                        imageVector = avatarIcon,
                                        contentDescription = "User Avatar",
                                        tint = SilkPrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val displayEmail = uiState.userEmail ?: "Guest Explorer"

                        Text(
                            text = uiState.profileName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = SilkOnSurface,
                                fontSize = 22.sp
                            )
                        )

                        Text(
                            text = uiState.profileBio,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = SilkOnSurfaceVariant,
                                fontWeight = FontWeight.Normal,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        Text(
                            text = displayEmail,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SilkOnSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .neomorphicInset(8.dp, 2.dp, SilkBackground)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (uiState.userEmail != null && uiState.userEmail != "Guest Explorer") "AUTHENTICATED USER" else "GUEST TRAVELLER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SilkPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        NeomorphicButton(
                            onClick = { showEditDialog = true },
                            cornerRadius = 10.dp,
                            backgroundColor = SilkBackground,
                            elevation = 3.dp,
                            modifier = Modifier.width(160.dp),
                            testTag = "profile_edit_dialog_trigger"
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = SilkPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edit Profile",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = SilkPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // PROFILE CUSTOMIZATION DIALOG
                        if (showEditDialog) {
                            var editName by remember { mutableStateOf(uiState.profileName) }
                            var editBio by remember { mutableStateOf(uiState.profileBio) }

                            AlertDialog(
                                onDismissRequest = { showEditDialog = false },
                                title = {
                                    Text(
                                        text = "Edit Profile details",
                                        fontWeight = FontWeight.Bold,
                                        color = SilkOnSurface
                                    )
                                },
                                text = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Choose Traveler Avatar",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = SilkOnSurfaceVariant
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceAround,
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val avatars = listOf(
                                                Pair("avatar_1", Icons.Default.Android),
                                                Pair("avatar_2", Icons.Default.LocationOn),
                                                Pair("avatar_3", Icons.Default.DirectionsBike),
                                                Pair("avatar_4", Icons.Default.DirectionsCar)
                                            )
                                            avatars.forEach { (id, icon) ->
                                                val isChosen = tempSelectedAvatar == id
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isChosen) Color(0xFFE8DEF8) else SilkBackground)
                                                        .clickable { tempSelectedAvatar = id }
                                                        .padding(4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = if (isChosen) SilkPrimary else SilkOnSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            // 5th custom avatar element
                                            val isCustomSelected = !tempSelectedAvatar.startsWith("avatar_")
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isCustomSelected) Color(0xFFE8DEF8) else SilkBackground)
                                                    .clickable {
                                                        photoPickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    }
                                                    .padding(4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isCustomSelected) {
                                                    AsyncImage(
                                                        model = tempSelectedAvatar,
                                                        contentDescription = "Custom Image",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(CircleShape)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Upload Custom Image",
                                                        tint = SilkPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Personal details",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = SilkOnSurfaceVariant
                                        )

                                        NeomorphicTextField(
                                            value = editName,
                                            onValueChange = { editName = it },
                                            placeholder = "Your Name"
                                        )

                                        NeomorphicTextField(
                                            value = editBio,
                                            onValueChange = { editBio = it },
                                            placeholder = "Profile Biography"
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            if (editName.isNotEmpty()) {
                                                viewModel.updateProfile(editName, editBio, tempSelectedAvatar)
                                                showEditDialog = false
                                            }
                                        }
                                    ) {
                                        Text("Save", color = SilkPrimary, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showEditDialog = false }) {
                                        Text("Cancel", color = SilkOnSurfaceVariant)
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                containerColor = SilkBackground
                            )
                        }
                    }
                }

                // Stats Dashboard Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Saved Places Card
                    NeomorphicCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Saved Count",
                                tint = SilkPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${uiState.savedPlaces.size}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SilkPrimary
                                )
                            )
                            Text(
                                text = "Saved Places",
                                style = MaterialTheme.typography.labelSmall.copy(color = SilkOnSurfaceVariant)
                            )
                        }
                    }

                    // Map System Status Card
                    NeomorphicCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Map Zoom",
                                tint = SilkTertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "%.1f".format(uiState.mapZoom),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SilkTertiary
                                )
                            )
                            Text(
                                text = "Current Zoom",
                                style = MaterialTheme.typography.labelSmall.copy(color = SilkOnSurfaceVariant)
                            )
                        }
                    }
                }

                // AI Features Card
                NeomorphicCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .neomorphicInset(10.dp, 2.dp, SilkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Aila AI Info",
                                    tint = SilkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Aila AI Assistant Enabled",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = SilkOnSurface
                                    )
                                )
                                Text(
                                    text = "Ready to talk about maps and routes",
                                    style = MaterialTheme.typography.bodySmall.copy(color = SilkOnSurfaceVariant)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "You can chat with Aila AI at any time using the floating AI assistant button on the explorer map. Ask about routes, traffic, or your saved locations!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SilkOnSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }

                // Large Clear tactile log out button
                NeomorphicButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    cornerRadius = 14.dp,
                    testTag = "profile_logout_btn"
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = SilkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out Account",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = SilkPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // 3. Navigation Bottom Bar (Profile Screen ACTIVE)
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

                // Favorites Tab
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

                // Profile Tab (ACTIVE)
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
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = Color(0xFF1D192B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF1D192B),
                            fontWeight = FontWeight.Bold
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
