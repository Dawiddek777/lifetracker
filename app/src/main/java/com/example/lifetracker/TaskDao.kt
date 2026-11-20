package com.example.lifetracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// DAO = interfejs z operacjami na bazie. Room sam generuje jego implementację.
@Dao
interface TaskDao {

    // Zwraca Flow: za każdym razem, gdy tabela się zmieni,
    // Room automatycznie wysyła nową listę do wszystkich, którzy ją obserwują.
    @Query("SELECT * FROM tasks ORDER BY id ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    // suspend = wykonywane w tle (coroutine), nie blokuje interfejsu
    @Insert
    suspend fun insert(task: TaskEntity)

    // Aktualizuje wiersz o takim samym id (używamy do zmiany isCompleted)
    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)
}