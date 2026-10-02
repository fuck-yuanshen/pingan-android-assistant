package com.example.pingan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val PinganColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandSurface,
    primaryContainer = BrandOrange,
    onPrimaryContainer = BrandSurface,
    secondary = BrandTextSecondary,
    background = BrandBackground,
    onBackground = BrandText,
    surface = BrandSurface,
    onSurface = BrandText,
    surfaceVariant = BrandBackground,
    outline = BrandBorder,
)

@Composable
fun PinganTheme(content: @Composable () -> Unit) {
    val typography = MaterialTheme.typography.copy(
        bodyLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 16.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 12.sp,
        ),
    )

    MaterialTheme(
        colorScheme = PinganColorScheme,
        typography = typography,
        content = content,
    )
}
