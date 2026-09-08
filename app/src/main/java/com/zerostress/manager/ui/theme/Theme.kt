package com.zerostress.manager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---- ZS palette aliases (used by activity screens) ----
val ZSBackground   = Color(0xFF0F0C29)
val ZSCard         = Color(0xFF1A1A2E)
val ZSCardElevated = Color(0xFF16213E)
val ZSInput        = Color(0xFF1A1A2E)
val ZSPrimary      = Color(0xFF667EEA)
val ZSPrimaryDark  = Color(0xFF764BA2)
val ZSAccent       = Color(0xFF38EF7D)
val ZSText         = Color(0xFFFFFFFF)
val ZSTextMuted    = Color(0xFF718096)
val ZSTextSecondary = Color(0xFFA0AEC0)
val ZSBorder       = Color(0xFF4A5568)
val ZSDanger       = Color(0xFFFC4A1A)
val ZSSuccess      = Color(0xFF11998E)
val ZSGreen        = Color(0xFF10B981)
val ZSCyan         = Color(0xFF38BDF8)
val ZSInfo         = Color(0xFF00D2FF)
val ZSWarning      = Color(0xFFF7971E)
val ZSGold         = Color(0xFFFFD200)
val ZSPurple       = Color(0xFFA855F7)

private val ZeroStressColorScheme = darkColorScheme(
    primary = ZSPrimary,
    onPrimary = Color.White,
    secondary = ZSAccent,
    onSecondary = Color(0xFF090D16),
    tertiary = ZSCyan,
    background = ZSBackground,
    onBackground = ZSText,
    surface = ZSCard,
    onSurface = ZSText,
    surfaceVariant = ZSCardElevated,
    onSurfaceVariant = ZSTextSecondary,
    error = ZSDanger,
    onError = Color.White,
    outline = ZSBorder
)

private val ZeroStressTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp)
)

@Composable
fun ZeroStressTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZeroStressColorScheme,
        typography = ZeroStressTypography,
        content = content
    )
}
