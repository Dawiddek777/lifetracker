package com.example.lifetracker

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// AndroidViewModel daje dostęp do Application (a więc do kontekstu potrzebnego bazie)
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).taskDao()

    // Flow z bazy zamieniony na StateFlow, który Compose potrafi obserwować
    val tasks: StateFlow<List<TaskEntity>> = dao.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Wybrany dzień i okres. mutableStateOf sprawia, że po zmianie
    // wartości Compose sam odświeża ekran. ViewModel przeżywa obrót telefonu.
    var selectedDate by mutableStateOf(LocalDate.now())
    var selectedPeriod by mutableStateOf("DZIEN")

    // direction = 1 (do przodu) albo -1 (do tyłu); krok zależy od wybranego okresu
    fun shiftDate(direction: Int) {
        val n = direction.toLong()
        selectedDate = when (selectedPeriod) {
            "DZIEN" -> selectedDate.plusDays(n)
            "TYDZIEN" -> selectedDate.plusWeeks(n)
            "MIESIAC" -> selectedDate.plusMonths(n)
            else -> selectedDate.plusYears(n)
        }
    }

    // Nowe zadanie dostaje datę i okres, które są aktualnie wybrane na ekranie
    fun addTask(title: String, time: String, points: Int, category: String) {
        viewModelScope.launch {
            dao.insert(
                TaskEntity(
                    title = title.trim(),
                    time = time.trim(),
                    points = points,
                    category = category,
                    date = selectedDate.toString(), // format "2026-11-20"
                    period = selectedPeriod
                )
            )
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