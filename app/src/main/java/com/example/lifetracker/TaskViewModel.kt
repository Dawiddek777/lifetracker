package com.example.lifetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// AndroidViewModel daje dostęp do Application (a więc do kontekstu potrzebnego bazie)
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).taskDao()

    // Zamieniamy Flow z bazy na StateFlow, który Compose potrafi obserwować.
    // Początkowa wartość to pusta lista, dopóki baza nie zwróci danych.
    val tasks: StateFlow<List<TaskEntity>> = dao.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // viewModelScope.launch = uruchomienie operacji bazy w tle
    fun addTask(title: String, time: String, points: Int) {
        viewModelScope.launch {
            dao.insert(TaskEntity(title = title.trim(), time = time.trim(), points = points))
        }
    }

    // copy() tworzy kopię zadania ze zmienionym polem isCompleted
    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            dao.update(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            dao.delete(task)
        }
    }
}