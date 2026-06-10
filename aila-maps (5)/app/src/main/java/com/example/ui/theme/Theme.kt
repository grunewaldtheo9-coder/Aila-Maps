package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SilkColorScheme = lightColorScheme(
    primary = SilkPrimary,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = SilkTertiary,
    background = SilkBackground,
    onBackground = SilkOnSurface,
    surface = SilkSurface,
    onSurface = SilkOnSurface,
    surfaceVariant = SilkSurface,
    onSurfaceVariant = SilkOnSurfaceVariant,
    outline = SilkOutline,
    outlineVariant = SilkOutlineVariant,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SilkColorScheme,
        typography = Typography,
        content = content
    )
}
