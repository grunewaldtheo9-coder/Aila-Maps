package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Geometric Balance Color Palette Design Tokens
val SilkBackground = Color(0xFFF3F4F9) // Cool light gray-blue background
val SilkSurface = Color(0xFFFFFFFF)    // Clear crisp geometric white
val SilkPrimary = Color(0xFF6750A4)    // Classic MD3 Deep Purple
val SilkTertiary = Color(0xFF381E72)   // Deep indigo/purple text & profile accents
val SilkSecondary = Color(0xFFB3261E)  // Contrast balance Red (Saved tags)

// Dark accents for text and contrast
val SilkOnSurface = Color(0xFF1C1B1F)  // Primary deep text
val SilkOnSurfaceVariant = Color(0xFF49454F) // Medium secondary contrast text
val SilkOutline = Color(0xFFCAC4D0)    // Middle border gray
val SilkOutlineVariant = Color(0xFFE7E0EC) // Whispering divider border gray

// Subtle elegant drop shadows instead of neomorphic bevel contrast highlights
val SilkShadowDark = Color(0xFF1C1B1F).copy(alpha = 0.08f)
val SilkShadowLight = Color(0xFFFFFFFF)

// Core M3 color mappings
val PrimaryLight = Color(0xFF6750A4)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE8DEF8)
val OnPrimaryContainerLight = Color(0xFF1D192B)

val SecondaryLight = Color(0xFFB3261E)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD0BCFF)
val OnSecondaryContainerLight = Color(0xFF381E72)

val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)
