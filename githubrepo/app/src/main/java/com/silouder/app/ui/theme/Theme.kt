package com.silouder.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AegisDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF00363A),
    primaryContainer = CyberCyanContainer,
    onPrimaryContainer = Color(0xFFE0F7FA),
    
    secondary = TacticalEmerald,
    onSecondary = Color(0xFF003822),
    secondaryContainer = TacticalEmeraldContainer,
    onSecondaryContainer = Color(0xFFA7F3D0),
    
    tertiary = AmberAlert,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = AmberAlertContainer,
    onTertiaryContainer = Color(0xFFFDE68A),
    
    background = DarkBackground,
    onBackground = TextPrimary,
    
    surface = DarkSurface,
    onSurface = TextPrimary,
    
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    
    outline = DarkSurfaceHighlight,
    outlineVariant = Color(0xFF1E293B),
    
    error = SignalRed,
    onError = Color(0xFF450A0A),
    errorContainer = SignalRedContainer,
    onErrorContainer = Color(0xFFFECACA)
)

private val AegisLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF047857),
    
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFFB45309),
    
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark stealth theme for mesh tactical communicator
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AegisDarkColorScheme else AegisLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
