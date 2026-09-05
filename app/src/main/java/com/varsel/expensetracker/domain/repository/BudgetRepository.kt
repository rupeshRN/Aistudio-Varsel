package com.varsel.expensetracker.domain.repository

import com.varsel.expensetracker.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    fun getBudgetById(id: Long): Flow<BudgetEntity?>
    suspend fun getBudgetByIdSnapshot(id: Long): BudgetEntity?
    suspend fun insertBudget(budget: BudgetEntity): Long
    suspend fun insertBudgets(budgets: List<BudgetEntity>)
    suspend fun updateBudget(budget: BudgetEntity)
    suspend fun deleteBudget(budget: BudgetEntity)
    suspend fun deleteBudgetById(id: Long)
}
