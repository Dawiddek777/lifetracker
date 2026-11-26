package com.example.lifetracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
@Dao
interface TaskDao {

     @Query("SELECT * FROM tasks ORDER BY id ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    // suspend znaczy, że to dzieje się w tle i nie zamraża ekranu.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)
    @Update
    suspend fun update(task: TaskEntity)
    @Delete
    suspend fun delete(task: TaskEntity)
}
