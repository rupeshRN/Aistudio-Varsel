package com.varsel.expensetracker.data.repository

import com.varsel.expensetracker.data.local.dao.BudgetDao
import com.varsel.expensetracker.data.local.entity.BudgetEntity
import com.varsel.expensetracker.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getAllBudgets(): Flow<List<BudgetEntity>> =
        budgetDao.getAllBudgets()

    override fun getBudgetById(id: Long): Flow<BudgetEntity?> =
        budgetDao.getBudgetById(id)

    override suspend fun getBudgetByIdSnapshot(id: Long): BudgetEntity? =
        budgetDao.getBudgetByIdSnapshot(id)

    override suspend fun insertBudget(budget: BudgetEntity): Long =
        budgetDao.insertBudget(budget)

    override suspend fun insertBudgets(budgets: List<BudgetEntity>) =
        budgetDao.insertBudgets(budgets)

    override suspend fun updateBudget(budget: BudgetEntity) =
        budgetDao.updateBudget(budget)

    override suspend fun deleteBudget(budget: BudgetEntity) =
        budgetDao.deleteBudget(budget)

    override suspend fun deleteBudgetById(id: Long) =
        budgetDao.deleteBudgetById(id)
}
