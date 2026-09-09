package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SportsDarkColorScheme =
  darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF00363A),
    primaryContainer = ElectricCyanDark,
    onPrimaryContainer = Color.White,
    secondary = NeonGreen,
    onSecondary = Color(0xFF003822),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFF6EE7B7),
    tertiary = GoldCrown,
    onTertiary = Color(0xFF452E07),
    background = AthleticBackground,
    onBackground = TextPrimary,
    surface = AthleticSurface,
    onSurface = TextPrimary,
    surfaceVariant = AthleticSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = AthleticBorder,
    error = ErrorRed,
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = SportsDarkColorScheme,
    typography = Typography,
    content = content
  )
}
