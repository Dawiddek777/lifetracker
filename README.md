# LifeTracker

Aplikacja mobilna na system Android napisana w Kotlinie i Jetpack Compose. Łączy planer zadań i celów okresowych z modułem treningowym, dietetycznym, kontrolą czasu przed ekranem oraz prostym systemem punktacji.

## Funkcjonalności

### Zrealizowane (Etap 1 – Planer i warstwa danych)
- **Planer wielookresowy (`PlannerScreen`):** obsługa zadań dziennych oraz celów tygodniowych, miesięcznych i rocznych (`DZIEN`, `TYDZIEN`, `MIESIAC`, `ROK`) z filtrowaniem po wybranej dacie.
- **Nawigacja po kalendarzu:** przełączanie okresów strzałkami oraz wybór konkretnego dnia z poziomu `DatePickerDialog`.
- **Tworzenie i zarządzanie zadaniami:** formularz (`AlertDialog`) z wyborem kategorii (`FilterChip`), opcjonalnym ustawieniem godziny dla zadań dziennych (`TimePicker`), przypisaniem własnej liczby punktów oraz oznaczaniem statusu wykonania (`Checkbox`).
- **Podsumowanie postępów (`SummaryCard`):** obliczanie na żywo sumy zdobytych punktów oraz liczby ukończonych zadań w wybranym okresie.
- **Lokalna baza danych (Room):** trwały zapis rekordów w SQLite z reaktywną aktualizacją interfejsu przez `Flow` i `StateFlow`.
- **Nawigacja główna:** dolny pasek `NavigationBar` z przełączaniem widoków między zakładkami `Planer`, `Trening`, `Dieta` oraz `Statystyki` (obecnie jako ekrany zastępcze).

### Planowane (Etap 2 – Trening, dieta i system passy)
- **Moduł treningowy:** lokalna baza podstawowych ćwiczeń siłowych z możliwością dodawania własnych pozycji, zapis serii i powtórzeń oraz rejestrowanie czasu rozgrzewki, rozciągania, cardio i dziennej liczby kroków.
- **Moduł diety i nawodnienia:** definiowanie dziennych celów oraz logowanie spożytych kalorii (kcal), makroskładników (białko, tłuszcze, węglowodany) i wypitej wody.
- **System passy (Streaks):** zliczanie kolejnych dni realizacji celów i przyznawanie premii punktowych za utrzymanie regularności (3, 7 oraz 30 dni).

### Planowane (Etap 3 – Integracje systemowe)
- **Monitorowanie czasu przed ekranem (`UsageStatsManager`):** zliczanie czasu spędzonego w wybranych aplikacjach i przyznawanie punktów premiowych za nieprzekroczenie dziennego limitu (z trybem ręcznego potwierdzenia jako fallback).
- **Harmonogram powiadomień (`AlarmManager` / `WorkManager`):** lokalne przypomnienia wyzwalane o określonej godzinie (np. o treningu, nawodnieniu lub suplementacji).

## Architektura i struktura projektu

Aplikacja korzysta ze wzorca MVVM z reaktywnym przepływem danych (`Room DAO -> Flow -> StateFlow -> Compose UI`). Stan filtrów daty, wybranego okresu oraz operacje asynchroniczne na bazie obsługuje `TaskViewModel` (`AndroidViewModel`), dzięki czemu stan ekranu jest zachowywany przy zmianie konfiguracji urządzenia.

```text
com.example.lifetracker/
├── MainActivity.kt          # Punkt wejścia aplikacji, konfiguracja NavHost i dolnego NavigationBar
├── PlannerScreen.kt         # Widok planera, filtrowanie po okresach, DatePicker, TimePicker i dialogi
├── PlaceHolderScreens.kt    # Ekrany zastępcze dla modułów przygotowywanych w kolejnych etapach
├── TaskViewModel.kt         # Zarządzanie stanem UI (StateFlow, LocalDate) oraz operacjami CRUD
├── AppDatabase.kt           # Konfiguracja instancji lokalnej bazy danych Room (Singleton)
├── TaskDao.kt               # Interfejs dostępu do danych (zapytania SQL i operacje suspend)
├── TaskEntity.kt            # Model encji reprezentujący pojedyncze zadanie lub cel w tabeli tasks
└── ui/theme/                # Definicje kolorów, typografii i motywu Material Design 3
