package com.varsel.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing Category entities in Room DB.
 * Provides reactive Flow streams for UI observation and one-shot functions for background engines.
 */
@Dao
interface CategoryDao {

    /**
     * Retrieves all spending/income categories observed as a reactive Flow.
     * Keeps the UI automatically updated in real-time whenever categories are added or modified.
     */
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    /**
     * Retrieves categories matching a specific type ("EXPENSE", "INCOME") or "BOTH".
     */
    @Query("SELECT * FROM categories WHERE UPPER(type) = UPPER(:type) OR UPPER(type) = 'BOTH' ORDER BY name ASC")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>

    /**
     * Retrieves only expense categories (and universal categories).
     */
    @Query("SELECT * FROM categories WHERE UPPER(type) = 'EXPENSE' OR UPPER(type) = 'BOTH' ORDER BY name ASC")
    fun getExpenseCategories(): Flow<List<CategoryEntity>>

    /**
     * Retrieves only income categories (and universal categories).
     */
    @Query("SELECT * FROM categories WHERE UPPER(type) = 'INCOME' OR UPPER(type) = 'BOTH' ORDER BY name ASC")
    fun getIncomeCategories(): Flow<List<CategoryEntity>>

    /**
     * Synchronous snapshot fetch of all categories used by SmartCategorizerEngine 
     * during background statement parsing and transaction auto-categorization.
     */
    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesSnapshot(): List<CategoryEntity>

    /**
     * Fetches a single category by its unique category name string.
     */
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    /**
     * Inserts a single category into the database. 
     * Ignores conflicts if a category with the same unique name already exists.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    /**
     * Batch inserts a default or custom set of categories into the database.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    /**
     * Updates an existing category (e.g., updating user keywords, icon, or color).
     */
    @Update
    suspend fun updateCategory(category: CategoryEntity)

    /**
     * Removes a category from the database.
     */
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}
