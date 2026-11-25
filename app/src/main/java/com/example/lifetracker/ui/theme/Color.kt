package com.example.lifetracker.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta kolorów aplikacji (motyw pomarańczowy).
// Kolor zapisujemy jako 0xFF + kod koloru w formacie HEX (taki sam jak w Paincie/Photoshopie).
// "FF" na początku oznacza pełną nieprzezroczystość.

// ----- Jasny motyw (dzień) -----
val OrangePrimary = Color(0xFFD9480F)          // główny pomarańczowy: przyciski, checkboxy, akcenty
val OrangeContainer = Color(0xFFFFDBC8)        // jasny pomarańcz: tło karty "Bilans dnia", przycisk +
val OnOrangeContainer = Color(0xFF3A1400)      // ciemny brąz: tekst na jasnym pomarańczu
val AmberSecondary = Color(0xFF8A5A00)         // dodatkowy kolor (bursztyn)
val AmberContainer = Color(0xFFFFE0B2)         // tło plakietki z punktami i zaznaczenia w dolnym pasku
val OnAmberContainer = Color(0xFF4E2A00)
val TealTertiary = Color(0xFF00838F)           // trzeci kolor dla kontrastu (morski)
val TealContainer = Color(0xFFB2EBF2)
val OnTealContainer = Color(0xFF002024)
val LightBackground = Color(0xFFFFF8F5)        // tło ekranu – ciepła, bardzo jasna biel
val LightOnBackground = Color(0xFF221A15)      // kolor zwykłego tekstu
val LightSurfaceVariant = Color(0xFFF4DED4)
val LightOnSurfaceVariant = Color(0xFF52443C)  // szary tekst (godzina, opisy)
val LightOutline = Color(0xFF85746B)
// Odcienie tła kart i dolnego paska (od najjaśniejszego do najciemniejszego)
val LightContainerLowest = Color(0xFFFFFFFF)
val LightContainerLow = Color(0xFFFFF1EA)
val LightContainer = Color(0xFFFCEAE1)
val LightContainerHigh = Color(0xFFF6E5DB)
val LightContainerHighest = Color(0xFFF0DFD6)

// ----- Ciemny motyw (noc) -----
val DarkOrangePrimary = Color(0xFFFFB68F)
val DarkOnOrangePrimary = Color(0xFF552100)
val DarkOrangeContainer = Color(0xFF7A3000)
val DarkOnOrangeContainer = Color(0xFFFFDBC8)
val DarkAmberSecondary = Color(0xFFFFB951)
val DarkOnAmberSecondary = Color(0xFF462A00)
val DarkAmberContainer = Color(0xFF5E3E00)
val DarkOnAmberContainer = Color(0xFFFFDDB3)
val DarkTealTertiary = Color(0xFF4FD8EB)
val DarkOnTealTertiary = Color(0xFF00363D)
val DarkTealContainer = Color(0xFF004F58)
val DarkOnTealContainer = Color(0xFF97F0FF)
val DarkBackground = Color(0xFF1A120D)
val DarkOnBackground = Color(0xFFF0DFD6)
val DarkSurfaceVariant = Color(0xFF52443C)
val DarkOnSurfaceVariant = Color(0xFFD7C2B8)
val DarkOutline = Color(0xFFA08D84)
val DarkContainerLowest = Color(0xFF140D08)
val DarkContainerLow = Color(0xFF221A15)
val DarkContainer = Color(0xFF271E19)
val DarkContainerHigh = Color(0xFF322823)
val DarkContainerHighest = Color(0xFF3D332D)
