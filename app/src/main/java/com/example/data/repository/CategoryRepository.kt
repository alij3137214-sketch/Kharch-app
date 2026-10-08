package com.example.data.repository

import com.example.data.db.CategoryDao
import com.example.data.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Repository pattern implementation abstracting access to [Category] data.
 */
class CategoryRepository(private val categoryDao: CategoryDao) {

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getCategoriesList(): List<Category> = categoryDao.getCategoriesList()

    fun getCategoryById(id: Long): Flow<Category?> = categoryDao.getCategoryById(id)

    suspend fun findCategoryById(id: Long): Category? = categoryDao.findCategoryById(id)

    suspend fun findCategoryByName(name: String): Category? = categoryDao.findCategoryByName(name)

    suspend fun insert(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun insertCategories(categories: List<Category>) = categoryDao.insertCategories(categories)

    suspend fun update(category: Category) = categoryDao.updateCategory(category)

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    suspend fun delete(category: Category) = categoryDao.deleteCategory(category)

    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    suspend fun deleteById(id: Long) = categoryDao.deleteCategoryById(id)

    suspend fun clearAll() = categoryDao.clearAllCategories()
}
