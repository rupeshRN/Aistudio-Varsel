package com.varsel.expensetracker.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.entity.BudgetEntity
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.domain.repository.BudgetRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.ui.budget.model.BudgetHistoryUiModel
import com.varsel.expensetracker.ui.budget.model.BudgetUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetScreenState(
    val budgets: List<BudgetUiModel> = emptyList(),
    val totalBudgetLimit: Double = 0.0,
    val totalAmountSpent: Double = 0.0,
    val totalAmountLeft: Double = 0.0,
    val isLoading: Boolean = false,
    val availableCategories: List<CategoryEntity> = emptyList()
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryDao: CategoryDao
) : ViewModel() {

    val uiState: StateFlow<BudgetScreenState> = combine(
        budgetRepository.getAllBudgets(),
        transactionRepository.getAllTransactions(),
        categoryDao.getAllCategories()
    ) { rawBudgets, transactions, categories ->
        val budgetUiModels = rawBudgets.map { budget ->
            BudgetCalculator.computeBudgetUiModel(
                budget = budget,
                transactions = transactions
            )
        }

        val totalLimit = budgetUiModels.sumOf { it.budget.amount }
        val totalSpent = budgetUiModels.sumOf { it.amountSpent }
        val totalLeft = (totalLimit - totalSpent).coerceAtLeast(0.0)

        BudgetScreenState(
            budgets = budgetUiModels,
            totalBudgetLimit = totalLimit,
            totalAmountSpent = totalSpent,
            totalAmountLeft = totalLeft,
            isLoading = false,
            availableCategories = categories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetScreenState(isLoading = true)
    )

    private val detailFlows = mutableMapOf<Long, StateFlow<BudgetUiModel?>>()
    private val historyFlows = mutableMapOf<Long, StateFlow<BudgetHistoryUiModel?>>()

    fun getBudgetDetail(budgetId: Long): StateFlow<BudgetUiModel?> {
        return detailFlows.getOrPut(budgetId) {
            combine(
                budgetRepository.getBudgetById(budgetId),
                transactionRepository.getAllTransactions()
            ) { budget, transactions ->
                if (budget != null) {
                    BudgetCalculator.computeBudgetUiModel(budget, transactions)
                } else null
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
        }
    }

    fun getBudgetHistory(budgetId: Long): StateFlow<BudgetHistoryUiModel?> {
        return historyFlows.getOrPut(budgetId) {
            combine(
                budgetRepository.getBudgetById(budgetId),
                transactionRepository.getAllTransactions()
            ) { budget, transactions ->
                if (budget != null) {
                    BudgetCalculator.computeBudgetHistory(budget, transactions)
                } else null
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
        }
    }

    fun createBudget(
        name: String,
        categoryName: String,
        amount: Double,
        period: String = "MONTHLY",
        startDayOfMonth: Int = 1,
        limitTotalType: String = "CONTRIBUTED",
        spendingLimitType: String = "FIXED",
        budgetType: String = "EXPENSE",
        colorHex: String? = null,
        iconName: String? = null
    ) {
        viewModelScope.launch {
            val budget = BudgetEntity(
                name = name.ifBlank { categoryName },
                categoryName = categoryName,
                amount = amount,
                period = period,
                startDayOfMonth = startDayOfMonth,
                limitTotalType = limitTotalType,
                spendingLimitType = spendingLimitType,
                budgetType = budgetType,
                colorHex = colorHex,
                iconName = iconName
            )
            budgetRepository.insertBudget(budget)
        }
    }

    fun updateBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            budgetRepository.updateBudget(budget)
        }
    }

    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch {
            budgetRepository.deleteBudgetById(budgetId)
        }
    }
}
