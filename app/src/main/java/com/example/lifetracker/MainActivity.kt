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

// Opis jednej zakładki: adres (route), podpis i ikona
data class Tab(val route: String, val label: String, val icon: ImageVector)

val tabs = listOf(
    Tab("planer", "Planer", Icons.Default.CheckCircle),
    Tab("trening", "Trening", Icons.Default.FitnessCenter),
    Tab("dieta", "Dieta", Icons.Default.Restaurant),
    Tab("statystyki", "Statystyki", Icons.Default.BarChart)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifetrackerTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    // NavController zarządza przechodzeniem między ekranami
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route // aktualnie otwarta zakładka

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                // wracamy do ekranu startowego, żeby stos nie rósł w nieskończoność
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true // nie otwieraj drugi raz tego samego ekranu
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // NavHost wyświetla ekran odpowiadający aktualnej trasie
        NavHost(
            navController = navController,
            startDestination = "planer",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("planer") { PlannerScreen() }
            composable("trening") {
                PlaceholderScreen(
                    Icons.Default.FitnessCenter, "Trening",
                    "Moduł treningowy – w przygotowaniu na Checkpoint 2"
                )
            }
            composable("dieta") {
                PlaceholderScreen(
                    Icons.Default.Restaurant, "Dieta",
                    "Moduł diety i nawodnienia – w przygotowaniu na Checkpoint 2"
                )
            }
            composable("statystyki") {
                PlaceholderScreen(
                    Icons.Default.BarChart, "Statystyki",
                    "Statystyki i system passy (Streaks) – w przygotowaniu na Checkpoint 2"
                )
            }
        }
    }
}