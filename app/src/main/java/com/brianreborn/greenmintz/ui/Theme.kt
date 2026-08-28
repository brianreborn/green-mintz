package com.brianreborn.greenmintz.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF0B1220)
val Navy2 = Color(0xFF121A2A)
val Navy3 = Color(0xFF1A2434)
val Cream = Color(0xFFF4F1EA)
val Teal = Color(0xFF1F6F66)
val Gold = Color(0xFFC4A574)
val Stop = Color(0xFF9F1239)
val Muted = Color(0xFF8A857A)
val WarnBg = Color(0xFF3A2A12)
val Ok = Color(0xFF7DCEA0)

private val Scheme = darkColorScheme(
    primary = Teal,
    onPrimary = Cream,
    secondary = Gold,
    onSecondary = Navy,
    background = Navy,
    onBackground = Cream,
    surface = Navy2,
    onSurface = Cream,
    surfaceVariant = Navy3,
    onSurfaceVariant = Muted,
    error = Stop,
    onError = Cream,
)

@Composable
fun MintzTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
