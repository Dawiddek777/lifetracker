# LifeTracker

Aplikacja mobilna na system Android napisana w Kotlinie i Jetpack Compose (Material 3). Łączy planer zadań i celów okresowych z modułem treningowym, dietetycznym, kontrolą czasu przed ekranem oraz systemem punktacji motywującym do regularności.

Projekt zaliczeniowy z przedmiotu *Tworzenie aplikacji mobilnych* (Politechnika Lubelska).

## Funkcjonalności

### Zrealizowane (Etap 1 – Planer i warstwa danych)

**Planer zadań i celów**
- **Planer wielookresowy:** zadania dzienne oraz cele tygodniowe, miesięczne i roczne (`DZIEN`, `TYDZIEN`, `MIESIAC`, `ROK`), przełączane przyciskami `SingleChoiceSegmentedButtonRow`.
- **Nawigacja po kalendarzu:** przechodzenie strzałkami o dzień/tydzień/miesiąc/rok, wybór dowolnej daty w `DatePickerDialog` oraz przycisk **„Dziś”** do szybkiego powrotu do bieżącej daty.
- **Dodawanie i edycja zadań:** wspólny formularz (`AlertDialog`) do tworzenia nowych i edytowania istniejących zadań (kliknięcie w kartę zadania). Pola: nazwa, opcjonalna godzina, kategoria, punkty, powtarzanie.
- **Wybór godziny:** wpisywanie godziny cyframi w polach „GG : MM” (`TimeInput`, format 24-godzinny).
- **Zadania powtarzalne:** zadania dzienne powtarzane codziennie lub w wybrane dni tygodnia (Pn–Nd) oraz cele powtarzane co tydzień, co miesiąc lub co rok. Wykonanie jest zapisywane osobno dla każdego dnia/okresu.
- **Sortowanie:** zadania są układane według godziny; zadania bez godziny trafiają na koniec listy.
- **Usuwanie z możliwością cofnięcia:** po usunięciu zadania pojawia się `Snackbar` z przyciskiem **„Cofnij”**. Przy zadaniach powtarzalnych aplikacja pyta, czy usunąć zadanie od wybranego dnia w przód (historia i punkty zostają), czy wszędzie.

**Kategorie**
- Wbudowane kategorie z własnym kolorem i ikoną: Ogólne, Trening, Dieta, Nauka, Praca.
- **Domyślne punkty kategorii:** po wybraniu kategorii pole punktów wypełnia się automatycznie (Trening 20, Dieta 30, Nauka 20, Praca 10; Ogólne – bez wartości domyślnej). Wartość można zmienić ręcznie.
- **Własne kategorie:** przycisk „Dodaj kategorię” pozwala utworzyć kategorię z nazwą i kolorem (zapis w bazie Room). Przytrzymanie własnej kategorii pozwala ją usunąć – zadania z tą kategorią pozostają i są wyświetlane na szaro.

**Punkty i cele punktowe**
- **Dzienny cel punktowy** (domyślnie 100 pkt), ustawiany ikoną ołówka na karcie bilansu i zapisywany w `SharedPreferences`.
- Cele dłuższych okresów wyliczane z celu dziennego: **tydzień = 7×, miesiąc = 30×, rok = 365×** cel dzienny.
- **Karta bilansu (`SummaryCard`):** zdobyte punkty względem celu (np. „45 / 100 pkt”), pasek postępu (`LinearProgressIndicator`), procent realizacji, komunikat „Cel osiągnięty!” oraz liczba wykonanych zadań.
- W widokach tygodnia, miesiąca i roku do bilansu liczą się punkty ze wszystkich zadań dziennych z danego okresu oraz punkty z celów tego okresu (z podziałem na oba źródła).

**Wygląd i nawigacja**
- Własny pomarańczowy motyw Material 3 w wersji jasnej i ciemnej (kolory dynamiczne wyłączone, aby aplikacja wyglądała tak samo na każdym urządzeniu).
- Nagłówek z powitaniem zależnym od pory dnia.
- Kolorowe plakietki kategorii, ikony godziny i powtarzania na kartach zadań, przekreślenie wykonanych zadań.
- Dolny pasek `NavigationBar` z zakładkami `Planer`, `Trening`, `Dieta` i `Statystyki` (trzy ostatnie jako ekrany zastępcze do kolejnych etapów).

**Warstwa danych**
- Lokalna baza **Room** (SQLite) z reaktywną aktualizacją interfejsu (`Flow` → `StateFlow` → `collectAsState`).
- Dwie tabele: `tasks` (zadania i cele) oraz `categories` (własne kategorie użytkownika).
- **Migracje bazy** (`Migration 2→3`, `Migration 3→4`) – aktualizacja aplikacji nie usuwa zapisanych zadań.

### Planowane (Etap 2 – Trening, dieta i system passy)
- **Moduł treningowy:** lokalna baza podstawowych ćwiczeń siłowych z możliwością dodawania własnych pozycji, zapis serii i powtórzeń oraz rejestrowanie czasu rozgrzewki, rozciągania, cardio i dziennej liczby kroków.
- **Moduł diety i nawodnienia:** definiowanie dziennych celów oraz logowanie spożytych kalorii (kcal), makroskładników (białko, tłuszcze, węglowodany) i wypitej wody.
- **System passy (Streaks):** zliczanie kolejnych dni realizacji celów i przyznawanie premii punktowych za utrzymanie regularności (3, 7 oraz 30 dni).

### Planowane (Etap 3 – Integracje systemowe)
- **Monitorowanie czasu przed ekranem (`UsageStatsManager`):** zliczanie czasu spędzonego w wybranych aplikacjach i przyznawanie punktów premiowych za nieprzekroczenie dziennego limitu (z trybem ręcznego potwierdzenia jako fallback).
- **Harmonogram powiadomień (`AlarmManager` / `WorkManager`):** lokalne przypomnienia wyzwalane o określonej godzinie (np. o treningu, nawodnieniu lub suplementacji).

## Architektura i struktura projektu

Aplikacja korzysta z prostego wzorca MVVM z reaktywnym przepływem danych:

```text
Room DAO -> Flow -> StateFlow (TaskViewModel) -> collectAsState -> Compose UI
```

`TaskViewModel` (`AndroidViewModel`) przechowuje stan ekranu (wybrana data, okres, cel punktowy) i wykonuje operacje na bazie w `viewModelScope`, dzięki czemu stan jest zachowywany przy obrocie ekranu. Projekt celowo nie używa bibliotek do wstrzykiwania zależności (Hilt/Koin) ani wielowarstwowej Clean Architecture – kod ma być prosty i czytelny.

```text
com.example.lifetracker/
├── MainActivity.kt          # Punkt wejścia aplikacji, NavHost i dolny NavigationBar
├── PlannerScreen.kt         # Ekran planera: nagłówek, bilans, lista zadań, dialogi (zadanie, cel, kategoria, usuwanie)
├── PlaceHolderScreens.kt    # Ekrany zastępcze dla modułów z kolejnych etapów
├── TaskViewModel.kt         # Stan UI, cel punktowy (SharedPreferences) i operacje na zadaniach/kategoriach
├── DateHelpers.kt           # Funkcje pomocnicze: okresy, widoczność zadań, powtarzanie, sumy punktów
├── Categories.kt            # Wbudowane kategorie (kolor, ikona, domyślne punkty) i paleta kolorów
├── AppDatabase.kt           # Baza Room (Singleton), wersja 4, migracje 2→3 i 3→4
├── TaskEntity.kt            # Encja tabeli tasks (zadanie lub cel)
├── TaskDao.kt               # Zapytania do tabeli tasks
├── CategoryEntity.kt        # Encja tabeli categories (własne kategorie)
├── CategoryDao.kt           # Zapytania do tabeli categories
└── ui/theme/                # Kolory (motyw pomarańczowy), typografia i motyw Material 3
```

## Technologie

- Kotlin, Jetpack Compose, Material 3 (+ `material-icons-extended`)
- Room (`room-runtime`, `room-ktx`, KSP)
- Navigation Compose
- Lifecycle ViewModel Compose
- Minimalna wersja systemu: Android 11 (API 30)

## Uruchomienie

1. Sklonuj repozytorium:
   ```bash
   git clone https://github.com/Dawiddek777/lifetracker.git
   ```
2. Otwórz projekt w Android Studio i poczekaj na synchronizację Gradle.
3. Uruchom aplikację na emulatorze lub telefonie z Androidem 11 lub nowszym.