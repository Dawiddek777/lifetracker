package com.example.lifetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AssistChip
import androidx.compose.material3.TimeInput
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

// ====================================================================
//  GŁÓWNY EKRAN PLANERA
// ====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(viewModel: TaskViewModel = viewModel()) {

    // "Podpinamy się" pod listę zadań z bazy. Gdy coś się w bazie zmieni
    // (dodasz, odhaczysz, usuniesz zadanie), ta zmienna dostanie nową listę,
    // a ekran sam się przerysuje. Nie trzeba niczego odświeżać ręcznie.
    val allTasks by viewModel.tasks.collectAsState()

    // To samo z własnymi kategoriami z bazy. Sklejamy je z wbudowanymi w jedną listę.
    val customCategories by viewModel.customCategories.collectAsState()
    val categoryList = allCategories(customCategories)

    // Zmienne, które pamiętają, jakie okienko jest teraz otwarte.
    // "remember" = zapamiętaj wartość między kolejnymi rysowaniami ekranu.
    var showAddDialog by remember { mutableStateOf(false) }               // okno "Nowe zadanie"
    var showDatePicker by remember { mutableStateOf(false) }              // kalendarz
    var showGoalDialog by remember { mutableStateOf(false) }              // okno "Dzienny cel punktów"
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }      // które zadanie edytujemy (null = żadne)
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }    // pytanie przy usuwaniu powtarzalnego

    // Snackbar to ten mały pasek na dole ekranu z napisem "Usunięto zadanie – COFNIJ".
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope() // potrzebne, bo pokazanie paska trwa chwilę (czeka na kliknięcie)

    // Pokazuje pasek z przyciskiem "Cofnij". Jeśli go klikniesz, przywracamy starą wersję zadania.
    val showUndo: (TaskEntity, String) -> Unit = { oldTask, message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss() // schowaj poprzedni pasek, jeśli jeszcze wisi
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Cofnij",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreTask(oldTask)
            }
        }
    }

    val date = viewModel.selectedDate
    val period = viewModel.selectedPeriod

    // Z wszystkich zadań zostawiamy tylko te, które pasują do oglądanego dnia/okresu,
    // i układamy je wg godziny. Zadania bez godziny lądują na końcu listy.
    // (Godziny mają format "08:00", "18:30", więc zwykłe porównanie tekstów układa je dobrze.)
    val tasks = allTasks
        .filter { isVisible(it, date, period) }
        .sortedWith(compareBy({ it.time.isEmpty() }, { it.time }))

    // Odhaczone pozycje z listy i suma ich punktów.
    val completedTasks = tasks.filter { isDone(it, date) }
    val listPoints = completedTasks.sumOf { it.points }

    // Punkty liczymy tak:
    //  - widok "Dzień": punkty z odhaczonych zadań tego dnia,
    //  - widok "Tydzień/Miesiąc/Rok": punkty ze WSZYSTKICH zadań dziennych z tego okresu
    //    + punkty z odhaczonych celów tego okresu (to, co widać na liście).
    val dailyPoints = if (period == "DZIEN") listPoints else dailyPointsInPeriod(allTasks, date, period)
    val goalsPoints = if (period == "DZIEN") 0 else listPoints
    val totalPoints = dailyPoints + goalsPoints

    // Scaffold to "szkielet" ekranu: ma miejsce na treść, przycisk + i pasek Snackbar.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // marginesy systemowe ogarnia już ekran główny
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
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
            // Nagłówek: nazwa aplikacji, powitanie i przycisk "Dziś".
            Header(
                showTodayButton = !viewModel.isShowingToday(),
                onTodayClick = { viewModel.goToToday() }
            )

            // Przełącznik okresu: Dzień / Tydzień / Miesiąc / Rok.
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                periods.forEachIndexed { index, (key, label) ->
                    SegmentedButton(
                        selected = period == key,
                        onClick = { viewModel.selectedPeriod = key },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size)
                    ) {
                        Text(label)
                    }
                }
            }

            // Strzałki i data. Kliknięcie w datę otwiera kalendarz.
            DateNavigator(
                label = periodLabel(date, period),
                onPrevious = { viewModel.shiftDate(-1) },
                onNext = { viewModel.shiftDate(1) },
                onPick = { showDatePicker = true }
            )

            // Karta z punktami, celem i paskiem postępu.
            SummaryCard(
                period = period,
                points = totalPoints,
                goal = viewModel.goalForSelectedPeriod(),
                dailyPoints = dailyPoints,
                goalsPoints = goalsPoints,
                done = completedTasks.size,
                all = tasks.size,
                onEditGoal = { showGoalDialog = true }
            )

            Spacer(modifier = Modifier.size(16.dp))

            if (tasks.isEmpty()) {
                // Pusta lista – zamiast niej pokazujemy napis na środku.
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (period == "DZIEN")
                            "Brak zadań w tym dniu.\nDodaj pierwsze zadanie przyciskiem +"
                        else
                            "Brak celów na ten okres.\nDodaj pierwszy cel przyciskiem +",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // LazyColumn = lista, która rysuje tylko to, co widać na ekranie.
                // Przy 5 zadaniach to bez różnicy, ale przy 500 telefon by się męczył bez tego.
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp) // zostawiamy miejsce, żeby + nie zasłaniał ostatniej karty
                ) {
                    // key = id: dzięki temu Compose wie, która karta jest która, i nie miesza ich.
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            category = categoryFor(task.category, categoryList),
                            done = isDone(task, date),
                            onToggle = { viewModel.toggleTask(task) },
                            onClick = { taskToEdit = task },
                            onDelete = {
                                if (task.isRepeating) {
                                    // Powtarzalne: najpierw pytamy, jak usunąć.
                                    taskToDelete = task
                                } else {
                                    // Jednorazowe: usuwamy od razu i dajemy szansę na "Cofnij".
                                    viewModel.deleteTask(task)
                                    showUndo(task, "Usunięto zadanie „${task.title}”")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ----- Okienka. Każde rysuje się tylko wtedy, gdy jego zmienna mówi "pokaż". -----

    // Dodawanie nowego zadania.
    if (showAddDialog) {
        TaskDialog(
            period = period,
            existingTask = null,
            categoryList = categoryList,
            onAddCategory = { name, color -> viewModel.addCategory(name, color) },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onDismiss = { showAddDialog = false },
            onConfirm = { title, time, points, category, isRepeating, repeatDays ->
                viewModel.addTask(title, time, points, category, isRepeating, repeatDays)
                showAddDialog = false
            }
        )
    }

    // Edycja istniejącego zadania (to samo okienko, tylko z wypełnionymi polami).
    // "?.let" = wykonaj to tylko, jeśli taskToEdit nie jest null.
    taskToEdit?.let { task ->
        TaskDialog(
            period = task.period,
            existingTask = task,
            categoryList = categoryList,
            onAddCategory = { name, color -> viewModel.addCategory(name, color) },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onDismiss = { taskToEdit = null },
            onConfirm = { title, time, points, category, isRepeating, repeatDays ->
                viewModel.editTask(task, title, time, points, category, isRepeating, repeatDays)
                taskToEdit = null
            }
        )
    }

    // Pytanie przy usuwaniu zadania powtarzalnego.
    taskToDelete?.let { task ->
        DeleteRepeatingDialog(
            task = task,
            onDismiss = { taskToDelete = null },
            onDeleteFromHere = {
                viewModel.endTaskFromSelectedDate(task)
                showUndo(task, "Zakończono zadanie „${task.title}”")
                taskToDelete = null
            },
            onDeleteEverywhere = {
                viewModel.deleteTask(task)
                showUndo(task, "Usunięto zadanie „${task.title}”")
                taskToDelete = null
            }
        )
    }

    // Ustawianie dziennego celu punktów (ołówek na karcie bilansu).
    if (showGoalDialog) {
        GoalDialog(
            currentGoal = viewModel.dailyGoal,
            onDismiss = { showGoalDialog = false },
            onSave = { newGoal ->
                viewModel.changeDailyGoal(newGoal)
                showGoalDialog = false
            }
        )
    }

    // Kalendarz: po wybraniu dnia przeskakujemy na niego.
    if (showDatePicker) {
        CalendarDialog(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onDateSelected = { picked ->
                viewModel.selectedDate = picked
                showDatePicker = false
            }
        )
    }
}

//  NAGŁÓWEK: "LifeTracker" + powitanie + przycisk "Dziś"
@Composable
fun Header(showTodayButton: Boolean, onTodayClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // weight(1f) = "zajmij całe wolne miejsce", więc przycisk "Dziś" ląduje przy prawej krawędzi.
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "LifeTracker",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        // Przycisk "Dziś" pokazujemy tylko wtedy, gdy oglądamy inny dzień niż dzisiejszy.
        if (showTodayButton) {
            FilledTonalButton(onClick = onTodayClick) {
                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dziś")
            }
        }
    }
}

// ====================================================================
//  WIERSZ Z DATĄ:  <   środa, 25.11.2026   >
// ====================================================================
@Composable
fun DateNavigator(label: String, onPrevious: () -> Unit, onNext: () -> Unit, onPick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // strzałki do krawędzi, data na środku
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

// ====================================================================
//  KALENDARZ (okienko z wyborem daty)
// ====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDialog(initialDate: LocalDate, onDismiss: () -> Unit, onDateSelected: (LocalDate) -> Unit) {
    // Kalendarz z Material 3 liczy daty w milisekundach (od 1970 roku, strefa UTC),
    // a my używamy LocalDate – dlatego zamieniamy w jedną i w drugą stronę.
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

// ====================================================================
//  KARTA "BILANS" – punkty względem celu + pasek postępu
// ====================================================================
// points      – ile punktów zdobyto w oglądanym okresie (razem)
// goal        – cel na ten okres (dzień = cel dzienny, tydzień = 7 x, miesiąc = 30 x, rok = 365 x)
// dailyPoints – część punktów z zadań dziennych, goalsPoints – część z celów okresu
// done / all  – ile pozycji z listy poniżej jest odhaczonych
@Composable
fun SummaryCard(
    period: String,
    points: Int,
    goal: Int,
    dailyPoints: Int,
    goalsPoints: Int,
    done: Int,
    all: Int,
    onEditGoal: () -> Unit
) {
    val title = when (period) {
        "DZIEN" -> "Bilans dnia"
        "TYDZIEN" -> "Bilans tygodnia"
        "MIESIAC" -> "Bilans miesiąca"
        else -> "Bilans roku"
    }
    // Ułamek od 0.0 do 1.0 do paska postępu (np. 45 z 100 pkt = 0.45).
    // coerceAtMost(1f) = nie więcej niż 1, żeby pasek nie "wyjechał", gdy przebijesz cel.
    // Gdy cel to 0, dajemy 0, żeby nie dzielić przez zero.
    val progress = if (goal == 0) 0f else (points.toFloat() / goal).coerceAtMost(1f)
    val percent = if (goal == 0) 0 else points * 100 / goal // tu może być ponad 100%
    val goalReached = goal > 0 && points >= goal

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Górny wiersz: tytuł po lewej, ołówek (zmiana celu) po prawej.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditGoal) {
                    Icon(Icons.Default.Edit, contentDescription = "Zmień dzienny cel punktów")
                }
            }

            // Duża liczba punktów i mniejszy cel obok, np. "45 / 100 pkt".
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$points",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " / $goal pkt",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Pasek postępu. Podajemy go jako { progress }, bo tak chce nowa wersja Material 3.
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (goalReached) "Cel osiągnięty!" else "Postęp celu",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (goalReached) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                Text(text = "$percent%", style = MaterialTheme.typography.bodyMedium)
            }

            // Ile pozycji z listy odhaczono. W widoku dnia to zadania, w pozostałych – cele.
            Text(
                text = if (period == "DZIEN") "Wykonane zadania: $done z $all"
                else "Wykonane cele: $done z $all",
                style = MaterialTheme.typography.bodyMedium
            )
            // W tygodniu/miesiącu/roku pokazujemy, skąd wzięły się punkty.
            if (period != "DZIEN") {
                Text(
                    text = "Z zadań dziennych: $dailyPoints pkt • Z celów: $goalsPoints pkt",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ====================================================================
//  OKIENKO "DZIENNY CEL PUNKTÓW"
// ====================================================================
@Composable
fun GoalDialog(currentGoal: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    // Pole tekstowe startuje od obecnego celu (np. "100").
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    val goal = goalText.toIntOrNull() // null, gdy pole jest puste

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Flag, contentDescription = null) },
        title = { Text("Dzienny cel punktów") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { newValue ->
                        // tylko cyfry, maksymalnie 5 (do 99999 pkt dziennie)
                        if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                            goalText = newValue
                        }
                    },
                    label = { Text("Punkty na dzień") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                // Podgląd, jak cel dzienny przekłada się na dłuższe okresy.
                val d = goal ?: 0
                Text(
                    text = "Tydzień: ${d * 7} pkt\nMiesiąc: ${d * 30} pkt\nRok: ${d * 365} pkt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (goal != null) onSave(goal) },
                enabled = goal != null && goal > 0 // cel musi być większy od zera
            ) { Text("Zapisz") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}

//  KARTA JEDNEGO ZADANIA
// done = czy zadanie jest odhaczone w oglądanym dniu (liczymy to wyżej, bo przy
// zadaniach powtarzalnych zależy to od dnia).
// category = kategoria zadania z kolorem i ikonką (szara, jeśli ktoś ją usunął).
@Composable
fun TaskCard(
    task: TaskEntity,
    category: Category,
    done: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    // Card z onClick = cała karta jest klikalna (kliknięcie otwiera edycję).
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox w kolorze kategorii.
            // Droga kliknięcia: onToggle -> ViewModel -> zapis w bazie -> Flow wysyła nową listę
            // -> ekran się przerysowuje (i licznik punktów na górze też).
            Checkbox(
                checked = done,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = category.color)
            )

            Column(modifier = Modifier.weight(1f)) {
                // Nazwa zadania. Jeśli zrobione – przekreślona i szara.
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    color = if (done)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                // Kolorowa "plakietka" kategorii z ikonką.
                CategoryLabel(category)

                // Godzina i powtarzanie – pokazujemy tylko to, co jest ustawione.
                if (task.time.isNotBlank() || task.isRepeating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (task.time.isNotBlank()) {
                            SmallInfo(Icons.Default.AccessTime, task.time)
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        if (task.isRepeating) {
                            SmallInfo(Icons.Default.Repeat, repeatLabel(task))
                        }
                    }
                }
            }

            // Plakietka z punktami.
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
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Usuń zadanie",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Mała kolorowa plakietka kategorii, np. [🏋 Trening] na czerwonawym tle.
@Composable
fun CategoryLabel(category: Category) {
    Surface(
        shape = RoundedCornerShape(50),
        // copy(alpha = 0.15f) = ten sam kolor, ale prawie przezroczysty (delikatne tło)
        color = category.color.copy(alpha = 0.15f),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(category.name, style = MaterialTheme.typography.labelMedium, color = category.color)
        }
    }
}

// Mała szara ikonka + tekst (np. zegar + "16:30").
@Composable
fun SmallInfo(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ====================================================================
//  OKIENKO DODAWANIA / EDYCJI ZADANIA
// ====================================================================
// existingTask = null    -> dodajemy nowe zadanie (puste pola)
// existingTask = zadanie -> edytujemy je (pola wypełnione jego danymi)
// categoryList           -> wszystkie kategorie (wbudowane + własne z bazy)
// onAddCategory / onDeleteCategory -> co zrobić po dodaniu / usunięciu własnej kategorii
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    period: String,
    existingTask: TaskEntity?,
    categoryList: List<Category>,
    onAddCategory: (name: String, color: Long) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (title: String, time: String, points: Int, category: String, isRepeating: Boolean, repeatDays: String) -> Unit
) {
    // Wartości pól formularza. Przy edycji startujemy od danych zadania,
    // przy dodawaniu – od wartości domyślnych. "?:" znaczy "a jeśli null, to weź to".
    val firstCategory = categoryList[0] // "Ogólne"
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var time by remember { mutableStateOf(existingTask?.time ?: "") }     // pusty = bez godziny
    var category by remember { mutableStateOf(existingTask?.category ?: firstCategory.name) }
    // Punkty: przy edycji – punkty zadania; przy nowym zadaniu – domyślne punkty kategorii "Ogólne"
    // (a że "Ogólne" ich nie ma, pole startuje puste).
    var pointsText by remember {
        mutableStateOf(existingTask?.points?.toString() ?: (firstCategory.defaultPoints?.toString() ?: ""))
    }
    var isRepeating by remember { mutableStateOf(existingTask?.isRepeating ?: false) }
    // Zaznaczone dni tygodnia jako zbiór liczb. Na start wszystkie 7 (= codziennie).
    var selectedDays by remember {
        mutableStateOf(
            if (existingTask != null && existingTask.repeatDays.isNotEmpty())
                parseRepeatDays(existingTask.repeatDays)
            else
                (1..7).toSet()
        )
    }
    var showTimeInput by remember { mutableStateOf(false) }                       // okienko z godziną
    var showAddCategory by remember { mutableStateOf(false) }                     // okienko "Nowa kategoria"
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }          // pytanie "usunąć kategorię?"

    // Tekst z pola "Punkty" zamieniamy na liczbę. Jak się nie da (np. puste pole) – wychodzi null.
    val points = pointsText.toIntOrNull()

    // Czy można zapisać? Musi być nazwa, punkty > 0, a przy powtarzaniu codziennym – choć jeden dzień.
    val canSave = title.isNotBlank() && points != null && points > 0 &&
            !(isRepeating && period == "DZIEN" && selectedDays.isEmpty())

    // Tekst przy przełączniku powtarzania zależy od okresu.
    val repeatText = when (period) {
        "DZIEN" -> "Powtarzaj"
        "TYDZIEN" -> "Powtarzaj co tydzień"
        "MIESIAC" -> "Powtarzaj co miesiąc"
        else -> "Powtarzaj co rok"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val what = if (period == "DZIEN") "zadanie" else "cel"
            Text(if (existingTask == null) "Nowe $what" else "Edytuj $what")
        },
        text = {
            // verticalScroll – gdyby okienko nie zmieściło się na małym ekranie, da się je przewinąć.
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

                // Godzinę wybieramy tylko przy zadaniach dziennych (cel na cały miesiąc nie ma godziny).
                if (period == "DZIEN") {
                    OutlinedButton(
                        onClick = { showTimeInput = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (time.isEmpty()) "Ustaw godzinę (opcjonalnie)" else time)
                    }
                    if (time.isNotEmpty()) {
                        TextButton(onClick = { time = "" }) { Text("Usuń godzinę") }
                    }
                }

                // Kategorie jako "chipy" w wierszu, który da się przesuwać palcem w bok.
                Text("Kategoria:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryList.forEach { cat ->
                        CategoryChip(
                            category = cat,
                            selected = category == cat.name,
                            onClick = {
                                category = cat.name
                                // Po wybraniu kategorii wpisujemy jej domyślne punkty
                                // (np. Trening = 20). Jeśli kategoria ich nie ma – czyścimy pole.
                                // Użytkownik i tak może potem wpisać własną liczbę.
                                pointsText = cat.defaultPoints?.toString() ?: ""
                            },
                            // Przytrzymanie działa tylko na własnych kategoriach (tych z bazy).
                            onLongClick = if (cat.entity != null) {
                                { categoryToDelete = cat }
                            } else null
                        )
                    }
                    // Ostatni chip: dodawanie nowej kategorii.
                    AssistChip(
                        onClick = { showAddCategory = true },
                        label = { Text("Dodaj kategorię") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
                // Podpowiedź pokazujemy tylko, gdy są jakieś własne kategorie.
                if (categoryList.any { it.entity != null }) {
                    Text(
                        text = "Przytrzymaj własną kategorię, aby ją usunąć",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Pole na punkty – tylko cyfry, maksymalnie 4 (czyli do 9999 pkt).
                OutlinedTextField(
                    value = pointsText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                            pointsText = newValue
                        }
                    },
                    label = { Text("Punkty") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) // klawiatura z cyframi
                )

                // Przełącznik "Powtarzaj".
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(repeatText, modifier = Modifier.weight(1f))
                    Switch(checked = isRepeating, onCheckedChange = { isRepeating = it })
                }

                // Przy zadaniach dziennych z powtarzaniem: wybór dni tygodnia.
                if (isRepeating && period == "DZIEN") {
                    WeekDayPicker(
                        selected = selectedDays,
                        onToggle = { day ->
                            // Kliknięty dzień: jeśli był zaznaczony – odznacz, jeśli nie – zaznacz.
                            selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                        }
                    )
                    Text(
                        text = when (selectedDays.size) {
                            7 -> "Codziennie"
                            0 -> "Zaznacz przynajmniej jeden dzień"
                            else -> "Wybrane dni tygodnia"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedDays.isEmpty())
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (points != null) {
                        // Dni zapisujemy jako tekst, np. "1,3,5". Dla celów i zadań bez powtarzania – pusty.
                        val daysText = if (isRepeating && period == "DZIEN")
                            selectedDays.sorted().joinToString(",")
                        else
                            ""
                        onConfirm(title, time, points, category, isRepeating, daysText)
                    }
                },
                enabled = canSave // przycisk jest wyszarzony, dopóki formularz nie jest poprawny
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )

    // ----- Okienka otwierane z wnętrza tego okienka (pokazują się nad nim) -----

    // Wpisywanie godziny cyframi: dwa pola, godzina i minuty.
    if (showTimeInput) {
        // Startujemy od godziny już ustawionej w zadaniu, a jak jej nie ma – od aktualnej godziny.
        val now = LocalTime.now()
        val timeState = rememberTimePickerState(
            initialHour = time.substringBefore(":").toIntOrNull() ?: now.hour,
            initialMinute = time.substringAfter(":", "").toIntOrNull() ?: now.minute,
            is24Hour = true // format 24-godzinny, bez AM/PM
        )
        AlertDialog(
            onDismissRequest = { showTimeInput = false },
            title = { Text("Godzina zadania") },
            // TimeInput = gotowe pola "GG : MM" z Material 3, wpisywane z klawiatury.
            text = { TimeInput(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    // %02d dokleja zero z przodu, żeby było "09:05", a nie "9:5".
                    time = "%02d:%02d".format(timeState.hour, timeState.minute)
                    showTimeInput = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimeInput = false }) { Text("Anuluj") }
            }
        )
    }

    // Dodawanie własnej kategorii.
    if (showAddCategory) {
        AddCategoryDialog(
            existingNames = categoryList.map { it.name },
            onDismiss = { showAddCategory = false },
            onSave = { name, color ->
                onAddCategory(name, color)
                // Od razu zaznaczamy nową kategorię. Nie ma ona domyślnych punktów, więc czyścimy pole.
                category = name.trim()
                pointsText = ""
                showAddCategory = false
            }
        )
    }

    // Pytanie przed usunięciem własnej kategorii.
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Usunąć kategorię „${cat.name}”?") },
            text = { Text("Zadania z tą kategorią zostaną, ale będą wyświetlane na szaro.") },
            confirmButton = {
                TextButton(onClick = {
                    cat.entity?.let { onDeleteCategory(it) }
                    // Jeśli właśnie ta kategoria była zaznaczona – wracamy do "Ogólne".
                    if (category == cat.name) {
                        category = firstCategory.name
                        pointsText = firstCategory.defaultPoints?.toString() ?: ""
                    }
                    categoryToDelete = null
                }) { Text("Usuń") }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("Anuluj") }
            }
        )
    }
}

// Chip (plakietka do kliknięcia) z jedną kategorią.
// Robimy go sami zamiast gotowego FilterChip, bo gotowy nie obsługuje przytrzymania palcem.
// combinedClickable = obsługuje zwykłe kliknięcie (onClick) i przytrzymanie (onLongClick).
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        // Zaznaczony chip ma tło w kolorze kategorii, niezaznaczony – tylko ramkę.
        color = if (selected) category.color.copy(alpha = 0.2f) else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) category.color else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(category.name, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// Okienko "Nowa kategoria": nazwa + wybór koloru.
// existingNames – nazwy, które już są (żeby nie dało się dodać dwóch "Hobby").
@Composable
fun AddCategoryDialog(
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(categoryColors[0]) }

    // Czy taka nazwa już istnieje? ignoreCase = true -> "hobby" i "Hobby" to to samo.
    val nameTaken = existingNames.any { it.equals(name.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowa kategoria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it }, // max 20 znaków
                    label = { Text("Nazwa") },
                    singleLine = true,
                    isError = nameTaken, // czerwona ramka, gdy nazwa jest zajęta
                    supportingText = { if (nameTaken) Text("Taka kategoria już istnieje") }
                )
                Text("Kolor:", style = MaterialTheme.typography.labelLarge)
                // Kółka z kolorami. Wybrane ma grubą ramkę.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryColors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    width = if (c == color) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { color = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), color) },
                enabled = name.isNotBlank() && !nameTaken // nazwa wymagana i nie może się powtarzać
            ) { Text("Dodaj") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}

// Siedem kółek z dniami tygodnia: Pn Wt Śr Cz Pt So Nd. Zaznaczone są pomarańczowe.
@Composable
fun WeekDayPicker(selected: Set<Int>, onToggle: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        weekDays.forEach { (number, label) ->
            val isOn = number in selected
            // weight(1f) – każde kółko dostaje tyle samo miejsca, więc 7 kółek zawsze się mieści.
            // aspectRatio(1f) – szerokość = wysokość, czyli wychodzi koło, a nie owal.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(
                        if (isOn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onToggle(number) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isOn) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ====================================================================
//  PYTANIE PRZY USUWANIU ZADANIA POWTARZALNEGO
// ====================================================================
@Composable
fun DeleteRepeatingDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onDeleteFromHere: () -> Unit,
    onDeleteEverywhere: () -> Unit
) {
    val fromHere = thisPeriodText(task.period) // "tego dnia", "tego tygodnia"...
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
        title = { Text("Usunąć „${task.title}”?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("To zadanie się powtarza. Co chcesz zrobić?")
                // Opcja 1: zadanie zostaje w przeszłości (z punktami), znika od oglądanego dnia w przód.
                OutlinedButton(onClick = onDeleteFromHere, modifier = Modifier.fillMaxWidth()) {
                    Text("Usuń od $fromHere")
                }
                // Opcja 2: zadanie znika wszędzie, także z historii.
                OutlinedButton(onClick = onDeleteEverywhere, modifier = Modifier.fillMaxWidth()) {
                    Text("Usuń wszędzie (też z historii)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}
