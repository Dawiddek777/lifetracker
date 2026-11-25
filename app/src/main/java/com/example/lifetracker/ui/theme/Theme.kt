package com.example.lifetracker.ui.theme

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

// Tu przypisujemy kolory z Color.kt do "ról" w Material 3.
// Np. "primary" to kolor przycisków i akcentów, "primaryContainer" to tło karty bilansu itd.
// Dzięki temu w reszcie kodu piszemy MaterialTheme.colorScheme.primary
// i nie musimy pamiętać kodów kolorów.

// Kolory na jasny motyw telefonu.
private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,               // "on" = kolor tekstu, który leży NA danym kolorze
    primaryContainer = OrangeContainer,
    onPrimaryContainer = OnOrangeContainer,
    secondary = AmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = OnAmberContainer,
    tertiary = TealTertiary,
    onTertiary = Color.White,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = OnTealContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightBackground,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    surfaceContainerLowest = LightContainerLowest,
    surfaceContainerLow = LightContainerLow,
    surfaceContainer = LightContainer,             // tło dolnego paska nawigacji
    surfaceContainerHigh = LightContainerHigh,     // tło okienek (dialogów)
    surfaceContainerHighest = LightContainerHighest // tło kart z zadaniami
)

// Kolory na ciemny motyw telefonu.
private val DarkColorScheme = darkColorScheme(
    primary = DarkOrangePrimary,
    onPrimary = DarkOnOrangePrimary,
    primaryContainer = DarkOrangeContainer,
    onPrimaryContainer = DarkOnOrangeContainer,
    secondary = DarkAmberSecondary,
    onSecondary = DarkOnAmberSecondary,
    secondaryContainer = DarkAmberContainer,
    onSecondaryContainer = DarkOnAmberContainer,
    tertiary = DarkTealTertiary,
    onTertiary = DarkOnTealTertiary,
    tertiaryContainer = DarkTealContainer,
    onTertiaryContainer = DarkOnTealContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkBackground,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    surfaceContainerLowest = DarkContainerLowest,
    surfaceContainerLow = DarkContainerLow,
    surfaceContainer = DarkContainer,
    surfaceContainerHigh = DarkContainerHigh,
    surfaceContainerHighest = DarkContainerHighest
)

@Composable
fun LifetrackerTheme(
    // Czy telefon ma włączony tryb ciemny? Sprawdzamy to automatycznie.
    darkTheme: Boolean = isSystemInDarkTheme(),
    // "Kolory z tapety" (działają od Androida 12). Wyłączone (false), żeby aplikacja
    // na każdym telefonie i emulatorze wyglądała tak samo – po pomarańczowemu.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // MaterialTheme "rozsyła" kolory i czcionki do wszystkich elementów w środku.
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
