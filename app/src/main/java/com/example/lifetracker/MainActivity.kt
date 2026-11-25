package com.example.lifetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.lifetracker.ui.theme.LifetrackerTheme
import androidx.compose.ui.tooling.preview.Preview

// Opis jednej zakładki w dolnym pasku: "adres" ekranu (route), podpis i ikonka.
// Route to po prostu nazwa, po której nawigacja wie, który ekran pokazać.
data class Tab(val route: String, val label: String, val icon: ImageVector)

// Nasze 4 zakładki. Kolejność tutaj = kolejność w dolnym pasku.
val tabs = listOf(
    Tab("planer", "Planer", Icons.Default.CheckCircle),
    Tab("trening", "Trening", Icons.Default.FitnessCenter),
    Tab("dieta", "Dieta", Icons.Default.Restaurant),
    Tab("statystyki", "Statystyki", Icons.Default.BarChart)
)

// MainActivity to "drzwi wejściowe" aplikacji – Android uruchamia ją jako pierwszą.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // aplikacja rysuje się na całym ekranie, też pod paskiem z godziną
        // setContent = "od teraz ekran rysuje Compose". Wszystko owijamy w nasz motyw (kolory).
        setContent {
            LifetrackerTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    // NavController to "kierowca" – pamięta, na którym ekranie jesteśmy, i przełącza między nimi.
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route // nazwa ekranu, który jest teraz otwarty

    // Scaffold = szkielet ekranu. Tu dajemy mu dolny pasek, a w środek wkładamy aktualny ekran.
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route, // podświetl zakładkę, jeśli to ten ekran
                        onClick = {
                            navController.navigate(tab.route) {
                                // Nie układamy ekranów jeden na drugim w nieskończoność –
                                // przy każdym kliknięciu "cofamy się" do Planera i dopiero otwieramy nową zakładkę.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true // zapamiętaj, jak wyglądał zostawiany ekran
                                }
                                launchSingleTop = true // klik w tę samą zakładkę nie otwiera jej drugi raz
                                restoreState = true // wracając na zakładkę, przywróć jej poprzedni wygląd
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // NavHost to "ramka", w której pokazuje się aktualny ekran.
        // Poniżej przypisujemy: nazwa (route) -> jaki ekran narysować.
        NavHost(
            navController = navController,
            startDestination = "planer",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("planer") { PlannerScreen() }
            composable("trening") {
                PlaceholderScreen(
                    Icons.Default.FitnessCenter, "Trening",
                    "Moduł treningowy"
                )
            }
            composable("dieta") {
                PlaceholderScreen(
                    Icons.Default.Restaurant, "Dieta",
                    "Moduł diety i nawodnienia"
                )
            }
            composable("statystyki") {
                PlaceholderScreen(
                    Icons.Default.BarChart, "Statystyki",
                    "Statystyki i system passy (Streaks)"
                )
            }
        }
    }
}