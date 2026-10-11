package com.lcktechnologies.budgetplanner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

data class AccentTheme(val id: String, val label: String, val accent: Long, val hero: Long)

val THEMES = listOf(
    AccentTheme("green", "Forest", 0xFF0F5B47L, 0xFF0F4A3BL),
    AccentTheme("teal", "Teal", 0xFF0E7C86L, 0xFF0B5C63L),
    AccentTheme("blue", "Blue", 0xFF2A5FD9L, 0xFF1E3E8FL),
    AccentTheme("purple", "Purple", 0xFF7B4FD1L, 0xFF4E2F8CL),
    AccentTheme("orange", "Amber", 0xFFC4711AL, 0xFF8A4B10L),
    AccentTheme("red", "Rose", 0xFFC4302BL, 0xFF8C201CL)
)

/** Colour of the big summary card at the top of screens. */
val LocalHero = compositionLocalOf { Color(0xFF0F4A3B) }

/** Colour used for income / positive amounts. */
val LocalPositive = compositionLocalOf { Color(0xFF0E7A4F) }

@Composable
fun BudgetTheme(dark: Boolean, accent: AccentTheme, content: @Composable () -> Unit) {
    val accentColor = Color(accent.accent)
    val scheme = if (dark) {
        darkColorScheme(
            primary = lerp(accentColor, Color.White, 0.45f),
            onPrimary = Color(0xFF062018),
            background = Color(0xFF0E1512),
            onBackground = Color(0xFFE7EFEA),
            surface = Color(0xFF17211C),
            onSurface = Color(0xFFE7EFEA),
            surfaceVariant = Color(0xFF26332C),
            onSurfaceVariant = Color(0xFF93A59B),
            outlineVariant = Color(0xFF26332C),
            error = Color(0xFFFF7A70),
            surfaceContainer = Color(0xFF17211C),
            surfaceContainerLow = Color(0xFF17211C),
            surfaceContainerHigh = Color(0xFF1E2B25),
            surfaceContainerHighest = Color(0xFF26332C)
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            background = Color.White,
            onBackground = Color(0xFF14261F),
            surface = Color.White,
            onSurface = Color(0xFF14261F),
            surfaceVariant = Color(0xFFEEF1EF),
            onSurfaceVariant = Color(0xFF5C6E66),
            outlineVariant = Color(0xFFE3E7E4),
            error = Color(0xFFC4302B),
            surfaceContainer = Color.White,
            surfaceContainerLow = Color.White,
            surfaceContainerHigh = Color(0xFFF6F8F7),
            surfaceContainerHighest = Color(0xFFEEF1EF)
        )
    }
    CompositionLocalProvider(
        LocalHero provides Color(accent.hero),
        LocalPositive provides (if (dark) Color(0xFF4CD9A8) else Color(0xFF0E7A4F))
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
