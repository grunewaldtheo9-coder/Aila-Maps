package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

// --- GEOMETRIC BALANCE SHADOW & CONTAINER MODIFIERS ---

/**
 * Draws a beautiful, high-fidelity crisp raised element incorporating subtle card drop shadows and thin outline borders.
 */
fun Modifier.neomorphicRaised(
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 5.dp,
    backgroundColor: Color = SilkBackground
): Modifier = this.drawBehind {
    val cornerRadiusPx = cornerRadius.toPx()
    val elevationPx = elevation.toPx()

    // 1. Draw standard elegant card drop shadow
    if (elevationPx > 0f) {
        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint().apply {
                color = backgroundColor.toArgb()
                isAntiAlias = true
                setShadowLayer(
                    elevationPx * 1.8f,
                    0f,
                    elevationPx * 0.6f,
                    Color(0xFF1C1B1F).copy(alpha = 0.07f).toArgb()
                )
            }
            canvas.nativeCanvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                cornerRadiusPx, cornerRadiusPx,
                paint
            )
        }
    }

    // 2. Render container backdrop fill (e.g. Crisp white surface or cool light grey)
    drawRoundRect(
        color = backgroundColor,
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
    )

    // 3. Draw thin geometric outline border
    val raisedBorderColor = when (backgroundColor) {
        Color(0xFFE8DEF8) -> Color(0xFFD0BCFF) // soft lavender gets light lavender border
        Color(0xFFF3F4F9) -> Color(0xFFCAC4D0) // grey body gets distinct gray border
        else -> Color(0xFFE7E0EC)              // white gets standard whisper border
    }

    val borderPaint = Paint().apply {
        color = raisedBorderColor
        style = androidx.compose.ui.graphics.PaintingStyle.Stroke
        strokeWidth = 1.dp.toPx()
    }
    drawIntoCanvas { canvas ->
        canvas.drawRoundRect(
            left = 0.5f,
            top = 0.5f,
            right = size.width - 0.5f,
            bottom = size.height - 0.5f,
            radiusX = cornerRadiusPx,
            radiusY = cornerRadiusPx,
            paint = borderPaint
        )
    }
}

/**
 * Draws an inset-style flat element utilizing soft light background tints and a distinct border outline.
 */
fun Modifier.neomorphicInset(
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 4.dp,
    backgroundColor: Color = SilkBackground
): Modifier = this.drawBehind {
    val cornerRadiusPx = cornerRadius.toPx()

    // 1. Base background fill
    drawRoundRect(
        color = backgroundColor,
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
    )

    // 2. Draw distinct outline border
    val borderColor = when (backgroundColor) {
        Color(0xFFE8DEF8) -> Color(0xFFD0BCFF) // soft lavender container gets light lavender border
        Color.White -> Color(0xFFE7E0EC)       // pristine white gets whisper light border
        else -> Color(0xFFCAC4D0)              // standard fallback gray outline
    }

    val borderPaint = Paint().apply {
        color = borderColor
        style = androidx.compose.ui.graphics.PaintingStyle.Stroke
        strokeWidth = 1.dp.toPx()
    }
    drawIntoCanvas { canvas ->
        canvas.drawRoundRect(
            left = 0.5f,
            top = 0.5f,
            right = size.width - 0.5f,
            bottom = size.height - 0.5f,
            radiusX = cornerRadiusPx,
            radiusY = cornerRadiusPx,
            paint = borderPaint
        )
    }
}

// --- NEOMORPHIC COMPOSABLES ---

@Composable
fun NeomorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 5.dp,
    backgroundColor: Color = SilkSurface,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .neomorphicRaised(cornerRadius, elevation, backgroundColor)
            .padding(1.dp), // Tiny margin safe area
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun NeomorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 5.dp,
    testTag: String? = null,
    backgroundColor: Color = SilkBackground,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()

    val surfaceModifier = if (isPressed.value) {
        Modifier.neomorphicInset(cornerRadius, elevation / 2, backgroundColor)
    } else {
        Modifier.neomorphicRaised(cornerRadius, elevation, backgroundColor)
    }

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current, // Enable material feedback ripple
                onClick = onClick
            )
            .then(surfaceModifier)
            .padding(vertical = 12.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

@Composable
fun NeomorphicIconButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    elevation: Dp = 5.dp,
    testTag: String? = null,
    backgroundColor: Color = SilkBackground
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()

    val surfaceModifier = if (isPressed.value) {
        Modifier.neomorphicInset(cornerRadius, elevation / 2, backgroundColor)
    } else {
        Modifier.neomorphicRaised(cornerRadius, elevation, backgroundColor)
    }

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .then(surfaceModifier),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
fun NeomorphicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    cornerRadius: Dp = 14.dp,
    testTag: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    backgroundColor: Color = SilkBackground,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .neomorphicInset(cornerRadius, 4.dp, backgroundColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(10.dp))
            }

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = SilkOnSurfaceVariant.copy(alpha = 0.5f),
                        style = LocalTextStyle.current.copy(color = SilkOnSurfaceVariant.copy(alpha = 0.5f))
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = LocalTextStyle.current.copy(color = SilkOnSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { onFocusChanged(it.isFocused) },
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = true
                )
            }

            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(10.dp))
                trailingIcon()
            }
        }
    }
}
