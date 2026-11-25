package com.example.lifetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

// To jest "przepis" na jedną tabelę w bazie danych. Tabela nazywa się "tasks".
// Wyobraź sobie arkusz w Excelu: każde pole poniżej to jedna kolumna,
// a każde dodane zadanie to jeden nowy wiersz.
@Entity(tableName = "tasks")
data class TaskEntity(
    // Numer zadania. Nie wymyślamy go sami – baza sama nada kolejny numer (1, 2, 3...).
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,                 // nazwa zadania, np. "Trening siłowy"
    val time: String = "",             // godzina, np. "16:30" (pusty tekst = bez godziny)
    val points: Int,                   // ile punktów dostajemy za wykonanie
    val isCompleted: Boolean = false,  // czy odhaczone (używane tylko przy zadaniach jednorazowych)
    val date: String = "",             // dzień, w którym zadanie dodano, np. "2026-11-20"
    val category: String = "Ogólne",   // kategoria, np. "Trening"
    val period: String = "DZIEN",      // czy to zadanie na dzień, czy cel na TYDZIEN / MIESIAC / ROK

    // ----- Pola dodane w wersji 3 bazy (zadania powtarzalne) -----

    // Czy zadanie ma się powtarzać (true = tak, false = jednorazowe).
    val isRepeating: Boolean = false,

    // W które dni tygodnia ma się pokazywać (tylko dla zadań dziennych).
    // Zapisujemy to jako zwykły tekst z numerami dni: 1 = poniedziałek ... 7 = niedziela.
    // Przykład: "1,3,5" = poniedziałek, środa i piątek. "1,2,3,4,5,6,7" = codziennie.
    val repeatDays: String = "",

    // Zadanie powtarzalne może być wykonane w poniedziałek, a w środę jeszcze nie.
    // Dlatego jedno "isCompleted" nie wystarczy – zapisujemy listę dni, w które je odhaczyliśmy.
    // Przykład: "2026-11-23,2026-11-24". Dla celów tygodniowych/miesięcznych/rocznych
    // zapisujemy pierwszy dzień danego tygodnia/miesiąca/roku.
    val completedDates: String = "",

    // Od kiedy zadanie powtarzalne ma przestać się pokazywać (pusty tekst = nigdy nie przestaje).
    // Ustawiamy to, gdy przy usuwaniu wybierzesz "tylko od tego dnia".
    val endDate: String = ""
)
