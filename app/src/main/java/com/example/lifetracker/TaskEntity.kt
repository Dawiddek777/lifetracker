package com.example.lifetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

//Tabela nazywa się tasks a każde dodane zadanie to jeden nowy wiersz.
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val time: String = "",
    val points: Int,
    val isCompleted: Boolean = false,
    val date: String = "",
    val category: String = "Ogólne",
    val period: String = "DZIEN",      
    val isRepeating: Boolean = false,
    val repeatDays: String = "",
    val completedDates: String = "",
    val endDate: String = ""
)
