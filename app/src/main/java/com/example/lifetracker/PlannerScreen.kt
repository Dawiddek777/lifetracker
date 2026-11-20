package com.example.lifetracker

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Okresy: pierwszy element to klucz zapisywany w bazie, drugi to napis na ekranie
val periods = listOf(
    "DZIEN" to "Dzień",
    "TYDZIEN" to "Tydzień",
    "MIESIAC" to "Miesiąc",
    "ROK" to "Rok"
)

// Dostępne kategorie zadań
val categories = listOf("Ogólne", "Trening", "Dieta", "Nauka", "Praca", "Inne")

// Czy zadanie należy do okresu, który aktualnie oglądamy?
fun isInPeriod(task: TaskEntity, selected: LocalDate, period: String): Boolean {
    if (task.period != period) return false
    val taskDate = LocalDate.parse(task.date)
    return when (period) {
        "DZIEN" -> taskDate == selected
        // tydzień liczymy od poniedziałku: porównujemy poniedziałki obu dat
        "TYDZIEN" -> taskDate.with(DayOfWeek.MONDAY) == selected.with(DayOfWeek.MONDAY)
        "MIESIAC" -> taskDate.year == selected.year && taskDate.month == selected.month
        else -> taskDate.year == selected.year
    }
}

// Napis wyświetlany między strzałkami, zależny od okresu
fun periodLabel(date: LocalDate, period: String): String {
    val pl = Locale.forLanguageTag("pl-PL")
    return when (period) {
        "DZIEN" -> date.format(DateTimeFormatter.ofPattern("EEEE, d.MM.yyyy", pl))
        "TYDZIEN" -> {
            val monday = date.with(DayOfWeek.MONDAY)
            val sunday = monday.plusDays(6)
            monday.format(DateTimeFormatter.ofPattern("d.MM", pl)) + " – " +
                    sunday.format(DateTimeFormatter.ofPattern("d.MM.yyyy", pl))
        }
        "MIESIAC" -> date.month.getDisplayName(TextStyle.FULL_STANDALONE, pl)
            .replaceFirstChar { it.uppercase() } + " " + date.year
        else -> date.year.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(viewModel: TaskViewModel = viewModel()) {

    // collectAsState: każda nowa lista z bazy (przez Flow) odświeża ekran automatycznie
    val allTasks by viewModel.tasks.collectAsState()

    var showDialog by remember { mutableStateOf(false) }      // okno dodawania zadania
    var showDatePicker by remember { mutableStateOf(false) }  // okno kalendarza

    // Z wszystkich zadań w bazie zostawiamy tylko te z wybranego dnia/tygodnia/miesiąca/roku
    val tasks = allTasks.filter {
        isInPeriod(it, viewModel.selectedDate, viewModel.selectedPeriod)
    }
    val completedTasks = tasks.filter { it.isCompleted }
    val totalPoints = completedTasks.sumOf { it.points }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // odstępy systemowe obsługuje zewnętrzny Scaffold
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj zadanie")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.size(8.dp))

            // Przełącznik okresu: Dzień / Tydzień / Miesiąc / Rok
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                periods.forEachIndexed { index, (key, label) ->
                    SegmentedButton(
                        selected = viewModel.selectedPeriod == key,
                        onClick = { viewModel.selectedPeriod = key },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size)
                    ) {
                        Text(label)
                    }
                }
            }

            // Strzałki i napis z datą (kliknięcie napisu otwiera kalendarz)
            DateNavigator(
                label = periodLabel(viewModel.selectedDate, viewModel.selectedPeriod),
                onPrevious = { viewModel.shiftDate(-1) },
                onNext = { viewModel.shiftDate(1) },
                onPick = { showDatePicker = true }
            )

            // Karta z podsumowaniem punktów i postępu
            SummaryCard(
                period = viewModel.selectedPeriod,
                points = totalPoints,
                done = completedTasks.size,
                all = tasks.size
            )

            Spacer(modifier = Modifier.size(16.dp))

            if (tasks.isEmpty()) {
                // Pusta lista: komunikat na środku ekranu
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (viewModel.selectedPeriod == "DZIEN")
                            "Brak zadań w tym dniu.\nDodaj pierwsze zadanie przyciskiem +"
                        else
                            "Brak celów na ten okres.\nDodaj pierwszy cel przyciskiem +",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // LazyColumn rysuje tylko widoczne elementy (wydajna lista)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp) // miejsce na FAB
                ) {
                    // key = id pozwala Compose rozpoznać, który element się zmienił
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggle = { viewModel.toggleTask(task) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    // Dialog dodawania jest rysowany tylko, gdy showDialog == true
    if (showDialog) {
        AddTaskDialog(
            period = viewModel.selectedPeriod,
            onDismiss = { showDialog = false },
            onConfirm = { title, time, points, category ->
                viewModel.addTask(title, time, points, category)
                showDialog = false
            }
        )
    }

    // Kalendarz: po wybraniu dnia ustawiamy go w ViewModelu
    if (showDatePicker) {
        CalendarDialog(
            initialDate = viewModel.selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                viewModel.selectedDate = date
                showDatePicker = false
            }
        )
    }
}

// Wiersz: strzałka wstecz, napis z datą (przycisk kalendarza), strzałka dalej
@Composable
fun DateNavigator(label: String, onPrevious: () -> Unit, onNext: () -> Unit, onPick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Wstecz")
        }
        TextButton(onClick = onPick) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Dalej")
        }
    }
}

// Okno z kalendarzem (DatePicker). Zwraca wybraną datę jako LocalDate.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDialog(initialDate: LocalDate, onDismiss: () -> Unit, onDateSelected: (LocalDate) -> Unit) {
    // DatePicker pracuje na milisekundach (UTC), więc zamieniamy datę tam i z powrotem
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    ) {
        DatePicker(state = state)
    }
}

// Karta z podsumowaniem punktów i postępu
@Composable
fun SummaryCard(period: String, points: Int, done: Int, all: Int) {
    val title = when (period) {
        "DZIEN" -> "Bilans dnia"
        "TYDZIEN" -> "Bilans tygodnia"
        "MIESIAC" -> "Bilans miesiąca"
        else -> "Bilans roku"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = "$points pkt", style = MaterialTheme.typography.displaySmall)
            Text(
                text = "Wykonane zadania: $done z $all",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Karta pojedynczego zadania
@Composable
fun TaskCard(task: TaskEntity, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kliknięcie wywołuje onToggle -> ViewModel -> update w bazie -> Flow -> odświeżenie ekranu
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    // Wykonane zadanie: przekreślone i wyszarzone
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Godzinę pokazujemy tylko, jeśli została podana
                    if (task.time.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.time,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    // Kategoria zadania
                    Text(
                        text = task.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Plakietka z punktami
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "+${task.points} pkt",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Usuń zadanie")
            }
        }
    }
}

// Okno dialogowe dodawania zadania lub celu
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    period: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String) -> Unit
) {
    // Stan pól formularza zapamiętywany podczas wyświetlania dialogu
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }            // pusty tekst = cały dzień
    var category by remember { mutableStateOf(categories[0]) }
    var pointsText by remember { mutableStateOf("10") }    // punkty jako tekst z pola
    var showTimePicker by remember { mutableStateOf(false) }

    // Zamiana tekstu na liczbę; gdy pole puste, wynik to null
    val points = pointsText.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (period == "DZIEN") "Nowe zadanie" else "Nowy cel") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nazwa") },
                    singleLine = true
                )

                // Godzina ma sens tylko dla zadań dziennych
                if (period == "DZIEN") {
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (time.isEmpty()) "Wybierz godzinę (opcjonalnie)" else time)
                    }
                    if (time.isNotEmpty()) {
                        TextButton(onClick = { time = "" }) { Text("Usuń godzinę") }
                    }
                }

                // Kategorie jako chipy w wierszu, który można przewijać w bok
                Text("Kategoria:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { name ->
                        FilterChip(
                            selected = category == name,
                            onClick = { category = name },
                            label = { Text(name) }
                        )
                    }
                }

                // Własna liczba punktów: pole numeryczne
                OutlinedTextField(
                    value = pointsText,
                    onValueChange = { newValue ->
                        // przepuszczamy tylko cyfry, maksymalnie 4 znaki
                        if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                            pointsText = newValue
                        }
                    },
                    label = { Text("Punkty") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (points != null) onConfirm(title, time, points, category)
                },
                // Zapis możliwy tylko z nazwą i punktami większymi od zera
                enabled = title.isNotBlank() && points != null && points > 0
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )

    // Drugie okno (z zegarem) wyświetlane nad pierwszym
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = 12,
            initialMinute = 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    // %02d dopisuje zero z przodu, np. 9:5 -> "09:05"
                    time = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Anuluj") }
            }
        )
    }
}