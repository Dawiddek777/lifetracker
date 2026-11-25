package com.example.lifetracker

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// Jedna kategoria tak, jak widzi ją ekran:
//  name          – nazwa (zapisywana przy zadaniu w bazie),
//  icon, color   – ikonka i kolor na karcie zadania,
//  defaultPoints – ile punktów wpisać automatycznie po wybraniu kategorii (null = zostaw puste pole),
//  entity        – dla kategorii dodanej przez użytkownika: jej wiersz z bazy (potrzebny do usunięcia);
//                  dla wbudowanych kategorii null.
data class Category(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val defaultPoints: Int?,
    val entity: CategoryEntity? = null
)

// Wbudowane kategorie – są zawsze i nie da się ich usunąć.
// Nazw nie zmieniaj, bo są zapisane przy zadaniach w bazie (stare zadania straciłyby kolor).
val builtInCategories = listOf(
    Category("Ogólne", Icons.Default.Star, Color(0xFFF57C00), null),          // pomarańczowy, bez domyślnych punktów
    Category("Trening", Icons.Default.FitnessCenter, Color(0xFFE53935), 20),  // czerwony
    Category("Dieta", Icons.Default.Restaurant, Color(0xFF43A047), 30),       // zielony
    Category("Nauka", Icons.Default.School, Color(0xFF1E88E5), 20),           // niebieski
    Category("Praca", Icons.Default.Work, Color(0xFF8E24AA), 10)              // fioletowy
)

// Kolory do wyboru przy dodawaniu własnej kategorii.
val categoryColors = listOf(
    0xFFE53935, // czerwony
    0xFFD81B60, // różowy
    0xFF8E24AA, // fioletowy
    0xFF3949AB, // granatowy
    0xFF1E88E5, // niebieski
    0xFF00897B, // morski
    0xFF43A047, // zielony
    0xFF6D4C41  // brązowy
)

// Szary kolor dla zadań, których kategoria została usunięta.
val deletedCategoryColor = Color(0xFF9E9E9E)

// Skleja wbudowane kategorie z tymi dodanymi przez użytkownika (z bazy) w jedną listę.
// Własne kategorie dostają ikonkę zakładki i nie mają domyślnych punktów.
fun allCategories(custom: List<CategoryEntity>): List<Category> {
    val fromDatabase = custom.map { entity ->
        Category(
            name = entity.name,
            icon = Icons.Default.Bookmark,
            color = Color(entity.color),
            defaultPoints = null,
            entity = entity
        )
    }
    return builtInCategories + fromDatabase
}

// Znajdź kategorię po nazwie. Jeśli jej nie ma (bo ktoś ją usunął),
// zwracamy szarą kategorię z tą samą nazwą – zadanie dalej się wyświetla, tylko na szaro.
fun categoryFor(name: String, all: List<Category>): Category {
    return all.find { it.name == name }
        ?: Category(name, Icons.Default.Bookmark, deletedCategoryColor, null)
}
