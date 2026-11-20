package com.example.lifetracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Główna klasa bazy. version = 1 (przy zmianie struktury tabel trzeba ją zwiększyć).
@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    // Singleton: w całej aplikacji istnieje tylko jedna instancja bazy
    companion object {
        @Volatile // zmiana widoczna od razu we wszystkich wątkach
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Jeśli baza już istnieje, zwracamy ją; jeśli nie, tworzymy (synchronized = bezpiecznie)
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifetracker_database" // nazwa pliku bazy w telefonie
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}