package com.example.lifetracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PlannerScreen(viewModel: TaskViewModel = viewModel()) {

    // collectAsState: każda nowa lista z bazy (przez Flow) odświeża ekran automatycznie
    val tasks by viewModel.tasks.collectAsState()

    // Stan okna dialogowego: czy jest otwarte
    var showDialog by remember { mutableStateOf(false) }

    // Obliczenia na podstawie aktualnej listy. Przeliczają się przy każdej zmianie listy.
    val completedTasks = tasks.filter { it.isCompleted }
    val totalPoints = completedTasks.sumOf { it.points }

    // Wewnętrzny Scaffold tylko po to, żeby dodać przycisk FAB na tym ekranie
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
            Spacer(modifier = Modifier.size(16.dp))

            // Górna karta: Bilans dnia
            SummaryCard(
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
                        text = "Brak zadań na dziś.\nDodaj pierwsze zadanie przyciskiem +",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // LazyColumn rysuje tylko widoczne elementy (wydajna lista)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp) // miejsce na FAB
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

    // Dialog jest rysowany tylko, gdy showDialog == true
    if (showDialog) {
        AddTaskDialog(
            onDismiss = { showDialog = false },
            onConfirm = { title, time, points ->
                viewModel.addTask(title, time, points)
                showDialog = false
            }
        )
    }
}

// Karta z podsumowaniem punktów i postępu
@Composable
fun SummaryCard(points: Int, done: Int, all: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Bilans dnia",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "$points pkt",
                style = MaterialTheme.typography.displaySmall
            )
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
                // Godzinę pokazujemy tylko, jeśli została podana
                if (task.time.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                    }
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

// Okno dialogowe dodawania zadania
@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onConfirm: (String, String, Int) -> Unit) {
    // Stan pól formularza zapamiętywany podczas wyświetlania dialogu
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var points by remember { mutableIntStateOf(10) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowe zadanie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nazwa zadania") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Godzina (opcjonalnie, np. 18:00)") },
                    singleLine = true
                )
                Text("Punkty:", style = MaterialTheme.typography.labelLarge)
                // Trzy chipy do wyboru liczby punktów
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 20, 50).forEach { value ->
                        FilterChip(
                            selected = points == value,
                            onClick = { points = value },
                            label = { Text("$value pkt") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, time, points) },
                enabled = title.isNotBlank() // pusty tytuł = przycisk nieaktywny
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}