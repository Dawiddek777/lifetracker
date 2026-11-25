package com.example.lifetracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// DAO to taka "lista poleceń", które możemy wydać bazie danych.
// My tylko piszemy, CO chcemy zrobić, a Room sam dopisze kod, który to robi.
@Dao
interface TaskDao {

    // Pobierz wszystkie zadania z tabeli.
    // Zwracamy Flow, czyli coś w rodzaju "subskrypcji": za każdym razem, gdy w tabeli
    // coś się zmieni, Room sam wyśle nam świeżą listę. Nie musimy pytać bazy w kółko.
    @Query("SELECT * FROM tasks ORDER BY id ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    // Dodaj zadanie do bazy.
    // REPLACE = jeśli zadanie o takim id już istnieje, to je nadpisz.
    // Dzięki temu ta sama funkcja przydaje się przy przycisku "Cofnij" po usunięciu:
    // wkładamy z powrotem starą wersję zadania (z tym samym id).
    // "suspend" znaczy, że to dzieje się w tle i nie zamraża ekranu.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    // Zmień istniejące zadanie (Room szuka go po id). Używamy przy odhaczaniu i edycji.
    @Update
    suspend fun update(task: TaskEntity)

    // Usuń zadanie z bazy na dobre.
    @Delete
    suspend fun delete(task: TaskEntity)
}
