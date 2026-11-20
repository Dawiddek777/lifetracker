package com.example.lifetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

// Encja = jedna tabela "tasks" w bazie. Każde pole to kolumna.
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) // id nadaje baza automatycznie
    val id: Int = 0,
    val title: String,               // nazwa zadania
    val time: String = "",           // godzina, pusty tekst = zadanie na cały dzień
    val points: Int,                 // punkty za wykonanie
    val isCompleted: Boolean = false, // czy zadanie odhaczone
    val date: String = "",           // data zadania jako tekst, np. "2026-11-20"
    val category: String = "Ogólne", // kategoria, np. "Trening"
    val period: String = "DZIEN"    // okres: DZIEN, TYDZIEN, MIESIAC albo ROK
)