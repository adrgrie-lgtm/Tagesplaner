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

private val FamilyLightColorScheme = lightColorScheme(
    primary = FamilyPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = FamilyPurpleContainer,
    onPrimaryContainer = FamilyPurpleOnContainer,
    secondary = PapaPrimary,
    onSecondary = Color.White,
    secondaryContainer = PapaContainer,
    onSecondaryContainer = PapaOnContainer,
    background = SoftCreamBackground,
    onBackground = DarkText,
    surface = SoftCreamSurface,
    onSurface = DarkText,
    surfaceVariant = SoftCreamSurfaceVariant,
    onSurfaceVariant = MediumText,
    outline = SubtleBorder
)

private val FamilyDarkColorScheme = darkColorScheme(
    primary = Color(0xFFCE93D8),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4A148C),
    onPrimaryContainer = Color(0xFFF3E5F5),
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF0D47A1),
    background = Color(0xFF1E1B18),
    onBackground = Color(0xFFF2ECE6),
    surface = Color(0xFF282420),
    onSurface = Color(0xFFF2ECE6),
    surfaceVariant = Color(0xFF38332E),
    onSurfaceVariant = Color(0xFFD7CCC8),
    outline = Color(0xFF554D46)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve carefully curated warm family palette
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FamilyDarkColorScheme
        else -> FamilyLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
