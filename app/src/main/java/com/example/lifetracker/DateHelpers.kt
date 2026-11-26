package com.example.lifetracker

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// W tym pliku są małe funkcje-pomocnicy do dat. Trzymamy je osobno,
// żeby PlannerScreen i ViewModel nie były zbyt długie.

// Okresy: pierwszy napis to "klucz" zapisywany w bazie, drugi to tekst na ekranie.
val periods = listOf(
    "DZIEN" to "Dzień",
    "TYDZIEN" to "Tydzień",
    "MIESIAC" to "Miesiąc",
    "ROK" to "Rok"
)

// Skróty dni tygodnia do wyboru przy powtarzaniu. Numer 1 = poniedziałek, 7 = niedziela
// (tak samo numeruje dni sama Java: DayOfWeek.MONDAY.value == 1).
val weekDays = listOf(
    1 to "Pn", 2 to "Wt", 3 to "Śr", 4 to "Cz", 5 to "Pt", 6 to "So", 7 to "Nd"
)

// Zwraca pierwszy dzień okresu, w którym leży podana data.
// Przykład dla 26.11.2026 (czwartek):
//   DZIEN   -> 26.11.2026 (ten sam dzień)
//   TYDZIEN -> 23.11.2026 (poniedziałek tego tygodnia)
//   MIESIAC -> 01.11.2026
//   ROK     -> 01.01.2026
// Dzięki temu łatwo sprawdzić, czy dwie daty są w tym samym tygodniu/miesiącu/roku:
// wystarczy porównać ich "początki".
fun periodStart(date: LocalDate, period: String): LocalDate {
    return when (period) {
        "DZIEN" -> date
        "TYDZIEN" -> date.with(DayOfWeek.MONDAY)
        "MIESIAC" -> date.withDayOfMonth(1)
        else -> date.withDayOfYear(1)
    }
}

// Zamienia zapisany w bazie tekst "1,3,5" na zbiór liczb {1, 3, 5}.
fun parseRepeatDays(text: String): Set<Int> {
    return text.split(",").mapNotNull { it.toIntOrNull() }.toSet()
}

// Zamienia tekst "2026-11-23,2026-11-24" na listę napisów (puste kawałki wyrzucamy).
fun parseCompletedDates(text: String): List<String> {
    return text.split(",").filter { it.isNotEmpty() }
}

// Czy zadanie ma się pokazać na ekranie dla wybranej daty i wybranego okresu?
fun isVisible(task: TaskEntity, selected: LocalDate, period: String): Boolean {
    // Zadanie dzienne nie pokazuje się w widoku "Tydzień" i odwrotnie.
    if (task.period != period) return false

    val taskStart = periodStart(LocalDate.parse(task.date), period) // kiedy zadanie dodano
    val current = periodStart(selected, period)                      // co teraz oglądamy

    // Zadanie jednorazowe: pokazujemy je tylko w swoim dniu / tygodniu / miesiącu / roku.
    if (!task.isRepeating) return taskStart == current

    // Zadanie powtarzalne:
    // 1) nie pokazujemy go przed dniem, w którym je dodano,
    if (current < taskStart) return false
    // 2) nie pokazujemy go od dnia, w którym je "zakończono" (jeśli zakończono),
    if (task.endDate.isNotEmpty() && current >= LocalDate.parse(task.endDate)) return false
    // 3) zadanie dzienne pokazujemy tylko w zaznaczone dni tygodnia.
    if (period == "DZIEN") {
        return selected.dayOfWeek.value in parseRepeatDays(task.repeatDays)
    }
    // Cel powtarzalny (tydzień/miesiąc/rok) pokazuje się w każdym kolejnym okresie.
    return true
}

// Czy zadanie jest odhaczone dla oglądanego dnia/okresu?
fun isDone(task: TaskEntity, selected: LocalDate): Boolean {
    return if (task.isRepeating) {
        // Powtarzalne: sprawdzamy, czy ten dzień jest na liście wykonanych dni.
        val key = periodStart(selected, task.period).toString()
        key in parseCompletedDates(task.completedDates)
    } else {
        // Jednorazowe: wystarczy zwykłe pole isCompleted.
        task.isCompleted
    }
}

// Ile razy cel okresu jest większy od celu dziennego.
// Tydzień = 7 dni, miesiąc liczymy zawsze jako 30 dni, rok jako 365 dni.
fun goalMultiplier(period: String): Int {
    return when (period) {
        "DZIEN" -> 1
        "TYDZIEN" -> 7
        "MIESIAC" -> 30
        else -> 365
    }
}

// Lista wszystkich dni w oglądanym okresie, np. dla tygodnia: poniedziałek, wtorek, ..., niedziela.
fun daysInPeriod(date: LocalDate, period: String): List<LocalDate> {
    val start = periodStart(date, period)
    val count = when (period) {
        "DZIEN" -> 1
        "TYDZIEN" -> 7
        "MIESIAC" -> start.lengthOfMonth()  // 28, 29, 30 albo 31 – ile faktycznie ma ten miesiąc
        else -> start.lengthOfYear()        // 365 albo 366
    }
    // Od pierwszego dnia dodajemy po kolei 0, 1, 2... dni.
    return (0 until count).map { start.plusDays(it.toLong()) }
}

// Suma punktów ze wszystkich ODHACZONYCH zadań dziennych w oglądanym okresie.
// Przechodzimy dzień po dniu i dla każdego dnia sprawdzamy każde zadanie dzienne:
// czy pokazuje się w tym dniu i czy jest w tym dniu odhaczone.
// (Dla widoku "Dzień" to po prostu punkty z tego jednego dnia.)
fun dailyPointsInPeriod(allTasks: List<TaskEntity>, date: LocalDate, period: String): Int {
    var sum = 0
    for (day in daysInPeriod(date, period)) {
        for (task in allTasks) {
            if (isVisible(task, day, "DZIEN") && isDone(task, day)) {
                sum += task.points
            }
        }
    }
    return sum
}

// Krótki opis powtarzania wyświetlany na karcie zadania, np. "Codziennie" albo "Pn, Śr, Pt".
fun repeatLabel(task: TaskEntity): String {
    return when (task.period) {
        "DZIEN" -> {
            val days = parseRepeatDays(task.repeatDays)
            if (days.size == 7) "Codziennie"
            else weekDays.filter { it.first in days }.joinToString(", ") { it.second }
        }
        "TYDZIEN" -> "Co tydzień"
        "MIESIAC" -> "Co miesiąc"
        else -> "Co rok"
    }
}

// Napis między strzałkami na górze ekranu (np. "środa, 25.11.2026").
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

// "tego dnia" / "tego tygodnia" itd. – do okienka przy usuwaniu zadania powtarzalnego.
fun thisPeriodText(period: String): String {
    return when (period) {
        "DZIEN" -> "tego dnia"
        "TYDZIEN" -> "tego tygodnia"
        "MIESIAC" -> "tego miesiąca"
        else -> "tego roku"
    }
}

fun greeting(): String {
    return "Cześć!"
}
