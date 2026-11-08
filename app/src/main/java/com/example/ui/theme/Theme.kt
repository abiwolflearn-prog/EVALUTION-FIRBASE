package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WcuDarkColorScheme = darkColorScheme(
  primary = WcuBlue500,
  onPrimary = WcuWhite,
  primaryContainer = WcuNavy700,
  onPrimaryContainer = WcuBlue100,
  secondary = WcuGold500,
  onSecondary = WcuNavy950,
  background = WcuNavy950,
  onBackground = WcuSlate50,
  surface = WcuNavy900,
  onSurface = WcuSlate50,
  surfaceVariant = WcuNavy800,
  onSurfaceVariant = WcuSlate300,
)

private val WcuLightColorScheme = lightColorScheme(
  primary = WcuBlue700,
  onPrimary = WcuWhite,
  primaryContainer = WcuBlue50,
  onPrimaryContainer = WcuNavy900,
  secondary = WcuNavy800,
  onSecondary = WcuWhite,
  background = WcuSlate50,
  onBackground = WcuNavy900,
  surface = WcuWhite,
  onSurface = WcuNavy900,
  surfaceVariant = WcuSlate100,
  onSurfaceVariant = WcuSlate600,
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) WcuDarkColorScheme else WcuLightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
