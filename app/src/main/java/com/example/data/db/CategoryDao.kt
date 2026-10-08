package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for local [Category] records in Room.
 * Handles database operations for inserting, retrieving, updating, and deleting category records.
 */
@Dao
interface CategoryDao {

    // --- Retrieving Operations ---

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun queryAllCategories(): Flow<List<Category>> = getAllCategories()

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getCategoriesList(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    fun getCategoryById(id: Long): Flow<Category?>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun findCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    fun getCategoryByName(name: String): Flow<Category?>

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findCategoryByName(name: String): Category?

    @Query("SELECT COUNT(*) FROM categories")
    fun getCategoryCount(): Flow<Int>

    // --- Inserting Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long = insertCategory(category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg categories: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>) = insertCategories(categories)

    // --- Updating Operations ---

    @Update
    suspend fun updateCategory(category: Category)

    @Update
    suspend fun update(category: Category) = updateCategory(category)

    // --- Deleting Operations ---

    @Delete
    suspend fun deleteCategory(category: Category)

    @Delete
    suspend fun delete(category: Category) = deleteCategory(category)

    @Delete
    suspend fun deleteCategories(categories: List<Category>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long) = deleteCategoryById(id)

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories() = clearAllCategories()

    @Query("DELETE FROM categories")
    suspend fun deleteAll() = clearAllCategories()
}
