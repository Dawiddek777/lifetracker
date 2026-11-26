package com.example.lifetracker

import android.app.Application
import android.content.Context
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

// ViewModel to "mózg" ekranu Planer. Ekran (PlannerScreen) tylko rysuje i zbiera kliknięcia,
// a ViewModel decyduje, co z nimi zrobić, i rozmawia z bazą danych.
// Ważna zaleta: ViewModel nie znika przy obrocie telefonu, więc nie tracimy wybranej daty.
// AndroidViewModel (a nie zwykły ViewModel), bo potrzebujemy "application", żeby otworzyć bazę.
class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).taskDao()
    private val categoryDao = AppDatabase.getDatabase(application).categoryDao()

    private val prefs = application.getSharedPreferences("lifetracker_settings", Context.MODE_PRIVATE)

    // Dzienny cel punktów. Czytamy go z notesika; jak jeszcze go nie ma – bierzemy 100.
    // "private set" = zmienić go można tylko przez funkcję setDailyGoal poniżej.
    var dailyGoal by mutableStateOf(prefs.getInt("daily_goal", 100))
        private set

    fun changeDailyGoal(value: Int) {
        dailyGoal = value
        prefs.edit().putInt("daily_goal", value).apply()
    }

    // Cel dla oglądanego okresu: dzień = cel, tydzień = 7 x cel, miesiąc = 30 x cel, rok = 365 x cel.
    fun goalForSelectedPeriod(): Int {
        return dailyGoal * goalMultiplier(selectedPeriod)
    }

    // Lista kategorii dodanych przez użytkownika – tak samo "na żywo" z bazy jak zadania.
    val customCategories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dodanie własnej kategorii do bazy.
    fun addCategory(name: String, color: Long) {
        viewModelScope.launch {
            categoryDao.insert(CategoryEntity(name = name.trim(), color = color))
        }
    }

    // Usunięcie własnej kategorii. Zadania z tą kategorią zostają (z tą samą nazwą),
    // tylko wyświetlają się na szaro, bo kategoria nie ma już koloru.
    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryDao.delete(category)
        }
    }

    // Lista WSZYSTKICH zadań z bazy, która sama się aktualizuje.
    // stateIn zamienia Flow z bazy na StateFlow – to forma, którą Compose umie łatwo obserwować.
    // Na start (zanim baza odpowie) lista jest pusta.
    val tasks: StateFlow<List<TaskEntity>> = dao.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Który dzień i jaki okres oglądamy. mutableStateOf działa jak "dzwonek":
    // gdy zmienimy wartość, Compose sam przerysuje ekran.
    var selectedDate by mutableStateOf(LocalDate.now())
    var selectedPeriod by mutableStateOf("DZIEN")

    // Strzałki w lewo/prawo. direction = 1 (do przodu) albo -1 (do tyłu).
    // O ile skaczemy, zależy od okresu: o dzień, tydzień, miesiąc albo rok.
    fun shiftDate(direction: Int) {
        val n = direction.toLong()
        selectedDate = when (selectedPeriod) {
            "DZIEN" -> selectedDate.plusDays(n)
            "TYDZIEN" -> selectedDate.plusWeeks(n)
            "MIESIAC" -> selectedDate.plusMonths(n)
            else -> selectedDate.plusYears(n)
        }
    }

    // Przycisk "Dziś" – wracamy do dzisiejszej daty.
    fun goToToday() {
        selectedDate = LocalDate.now()
    }

    // Czy oglądamy właśnie dzisiejszy dzień (albo bieżący tydzień/miesiąc/rok)?
    // Jeśli tak, to przycisk "Dziś" jest niepotrzebny i go chowamy.
    fun isShowingToday(): Boolean {
        return periodStart(selectedDate, selectedPeriod) == periodStart(LocalDate.now(), selectedPeriod)
    }

    // Dodawanie nowego zadania. Zadanie dostaje datę i okres, które są teraz wybrane na ekranie.
    // viewModelScope.launch = "zrób to w tle", żeby zapis do bazy nie zamroził ekranu.
    fun addTask(
        title: String, time: String, points: Int, category: String,
        isRepeating: Boolean, repeatDays: String
    ) {
        viewModelScope.launch {
            dao.insert(
                TaskEntity(
                    title = title.trim(),
                    time = time,
                    points = points,
                    category = category,
                    date = selectedDate.toString(), // np. "2026-11-25"
                    period = selectedPeriod,
                    isRepeating = isRepeating,
                    repeatDays = repeatDays
                )
            )
        }
    }

    // Edycja zadania. copy() robi kopię zadania z podmienionymi polami (reszta zostaje bez zmian),
    // a update zapisuje ją w bazie w miejscu starej wersji (Room rozpoznaje zadanie po id).
    fun editTask(
        task: TaskEntity, title: String, time: String, points: Int, category: String,
        isRepeating: Boolean, repeatDays: String
    ) {
        viewModelScope.launch {
            dao.update(
                task.copy(
                    title = title.trim(),
                    time = time,
                    points = points,
                    category = category,
                    isRepeating = isRepeating,
                    repeatDays = repeatDays
                )
            )
        }
    }

    // Kliknięcie w Checkbox: odhacz albo cofnij odhaczenie.
    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            if (task.isRepeating) {
                // Zadanie powtarzalne: dopisujemy albo wykreślamy oglądany dzień z listy wykonanych dni.
                val key = periodStart(selectedDate, task.period).toString()
                val dates = parseCompletedDates(task.completedDates).toMutableList()
                if (key in dates) dates.remove(key) else dates.add(key)
                dao.update(task.copy(completedDates = dates.joinToString(",")))
            } else {
                // Zadanie jednorazowe: po prostu odwracamy true <-> false.
                dao.update(task.copy(isCompleted = !task.isCompleted))
            }
        }
    }

    // Usunięcie zadania z bazy na dobre (ze wszystkich dni).
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            dao.delete(task)
        }
    }

    // Zadanie powtarzalne: "usuń tylko od tego dnia".
    // Ustawiamy datę końca – od niej w przód zadanie przestaje się pokazywać,
    // ale w starszych dniach zostaje (razem z punktami, które tam zdobyłeś).
    fun endTaskFromSelectedDate(task: TaskEntity) {
        viewModelScope.launch {
            val end = periodStart(selectedDate, task.period)
            val start = periodStart(LocalDate.parse(task.date), task.period)
            if (end <= start) {
                // Jeśli "kończymy" je w dniu, w którym zaczęło (albo wcześniej),
                // to nie zostałoby nigdzie – więc po prostu je usuwamy.
                dao.delete(task)
            } else {
                dao.update(task.copy(endDate = end.toString()))
            }
        }
    }

    // Przycisk "Cofnij": wkładamy do bazy starą wersję zadania (sprzed usunięcia).
    // Dzięki REPLACE w DAO działa to w obu przypadkach – i gdy zadanie zostało usunięte,
    // i gdy tylko dostało datę końca.
    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch {
            dao.insert(task)
        }
    }
}
