package com.example.lifetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

// Druga tabela w bazie: "categories". Trzymamy w niej TYLKO kategorie dodane przez użytkownika
// (przyciskiem "+ Dodaj kategorię"). Wbudowane kategorie (Ogólne, Trening, Dieta...)
// są wpisane na sztywno w pliku Categories.kt i nie trzeba ich zapisywać.
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,      // numer nadawany przez bazę
    val name: String,     // nazwa kategorii, np. "Hobby"
    // Kolor zapisany jako liczba, np. 0xFF1E88E5 (niebieski).
    // Long, a nie Int, bo taki kod koloru jest za duży na zwykłego Inta.
    val color: Long
)
