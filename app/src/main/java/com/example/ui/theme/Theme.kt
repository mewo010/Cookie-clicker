package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SugarAmber,
    secondary = BiscuitCream,
    tertiary = DoughGold,
    background = ChocolateDarkBg,
    surface = CardSlateBg,
    onPrimary = DarkChocolate,
    onSecondary = DarkChocolate,
    onTertiary = DarkChocolate,
    onBackground = BiscuitCream,
    onSurface = BiscuitCream
)

private val LightColorScheme = lightColorScheme(
    primary = WarmChocolate,
    secondary = DarkChocolate,
    tertiary = DoughGold,
    background = BiscuitCream,
    surface = BiscuitCream,
    onPrimary = BiscuitCream,
    onSecondary = BiscuitCream,
    onBackground = DarkChocolate,
    onSurface = DarkChocolate
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force our beautiful dark cookie theme for premium game vibes!
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
