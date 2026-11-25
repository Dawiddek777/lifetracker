package com.example.lifetracker

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// "Lista poleceń" dla tabeli z własnymi kategoriami. Działa dokładnie tak samo jak TaskDao.
@Dao
interface CategoryDao {

    // Wszystkie własne kategorie, w kolejności dodania. Flow = lista sama się odświeża,
    // więc nowa kategoria od razu pojawia się w okienku dodawania zadania.
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert
    suspend fun insert(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)
}
