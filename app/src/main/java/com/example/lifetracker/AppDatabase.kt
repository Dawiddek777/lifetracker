package com.example.lifetracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Główna klasa bazy danych. Mówimy tu Roomowi, jakie mamy tabele:
// zadania (TaskEntity) i własne kategorie (CategoryEntity).
// version = 4 to numer wersji bazy. Za każdym razem, gdy zmieniamy tabele,
// trzeba ten numer podbić o 1 – inaczej aplikacja się wysypie.
// Historia: 1 – pierwsze zadania, 2 – data/kategoria/okres, 3 – zadania powtarzalne,
//           4 – tabela z własnymi kategoriami.
@Database(entities = [TaskEntity::class, CategoryEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Przez te funkcje dostajemy się do "list poleceń" (DAO) dla każdej tabeli.
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        // Tu trzymamy jedną, jedyną bazę dla całej aplikacji (tzw. Singleton).
        // Nie chcemy otwierać pliku bazy kilka razy naraz – to byłby bałagan.
        // @Volatile: gdy jeden wątek ustawi bazę, inne od razu to zobaczą.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migracja = instrukcja "jak przerobić starą bazę na nową",
        // żeby zadania, które już masz w telefonie, nie zniknęły.

        // Z wersji 2 na 3: dopisujemy do tabeli zadań 4 nowe kolumny (zadania powtarzalne).
        // Stare zadania dostaną wartości domyślne (0 = false, '' = pusty tekst).
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isRepeating INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeatDays TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tasks ADD COLUMN completedDates TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tasks ADD COLUMN endDate TEXT NOT NULL DEFAULT ''")
            }
        }

        // Z wersji 3 na 4:
        // 1) tworzymy nową, pustą tabelę na własne kategorie,
        // 2) kategorii "Inne" już nie ma, więc zadania, które ją miały, przenosimy do "Ogólne".
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS categories (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "color INTEGER NOT NULL)"
                )
                db.execSQL("UPDATE tasks SET category = 'Ogólne' WHERE category = 'Inne'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            // Jeśli baza już jest – oddaj ją. Jeśli nie – stwórz.
            // synchronized pilnuje, żeby dwa wątki naraz nie stworzyły dwóch baz.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifetracker_database" // tak nazywa się plik bazy w pamięci telefonu
                )
                    // Room sam wybierze potrzebne migracje: z wersji 2 zrobi 2->3->4, z wersji 3 tylko 3->4.
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    // Koło ratunkowe: gdyby w telefonie była bardzo stara baza (wersja 1),
                    // dla której nie mamy migracji, Room stworzy bazę od nowa zamiast się wysypać.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
