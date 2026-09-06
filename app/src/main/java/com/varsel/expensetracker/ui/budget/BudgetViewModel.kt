package com.varsel.expensetracker.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.entity.BudgetEntity
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.data.preference.BudgetDisplayRepository
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
    val allBudgets: List<BudgetUiModel> = emptyList(),
    val hiddenBudgetIds: Set<Long> = emptySet(),
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
    private val categoryDao: CategoryDao,
    private val budgetDisplayRepository: BudgetDisplayRepository
) : ViewModel() {

    val uiState: StateFlow<BudgetScreenState> = combine(
        budgetRepository.getAllBudgets(),
        transactionRepository.getAllTransactions(),
        categoryDao.getAllCategories(),
        budgetDisplayRepository.config
    ) { rawBudgets, transactions, categories, displayConfig ->
        val orderMap = displayConfig.orderedBudgetIds.mapIndexed { idx, id -> id to idx }.toMap()
        val sortedRaw = rawBudgets.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE }

        val allBudgetUiModels = sortedRaw.map { budget ->
            BudgetCalculator.computeBudgetUiModel(
                budget = budget,
                transactions = transactions
            )
        }

        val visibleBudgets = allBudgetUiModels.filter { it.budget.id !in displayConfig.hiddenBudgetIds }

        val totalLimit = visibleBudgets.sumOf { it.budget.amount }
        val totalSpent = visibleBudgets.sumOf { it.amountSpent }
        val totalLeft = (totalLimit - totalSpent).coerceAtLeast(0.0)

        BudgetScreenState(
            budgets = visibleBudgets,
            allBudgets = allBudgetUiModels,
            hiddenBudgetIds = displayConfig.hiddenBudgetIds,
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

    private val detailFlows = mutableMapOf<Pair<Long, Long>, StateFlow<BudgetUiModel?>>()
    private val historyFlows = mutableMapOf<Long, StateFlow<BudgetHistoryUiModel?>>()

    fun getBudgetDetail(budgetId: Long, referenceTime: Long = System.currentTimeMillis()): StateFlow<BudgetUiModel?> {
        val key = Pair(budgetId, referenceTime)
        return detailFlows.getOrPut(key) {
            combine(
                budgetRepository.getBudgetById(budgetId),
                transactionRepository.getAllTransactions()
            ) { budget, transactions ->
                if (budget != null) {
                    BudgetCalculator.computeBudgetUiModel(budget, transactions, referenceTime = referenceTime)
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

    fun toggleHideBudget(budgetId: Long) {
        viewModelScope.launch {
            budgetDisplayRepository.toggleHideBudget(budgetId)
        }
    }

    fun moveBudgetUp(budgetId: Long) {
        val currentOrder = uiState.value.allBudgets.map { it.budget.id }.toMutableList()
        val index = currentOrder.indexOf(budgetId)
        if (index > 0) {
            currentOrder.removeAt(index)
            currentOrder.add(index - 1, budgetId)
            viewModelScope.launch {
                budgetDisplayRepository.saveOrder(currentOrder)
            }
        }
    }

    fun moveBudgetDown(budgetId: Long) {
        val currentOrder = uiState.value.allBudgets.map { it.budget.id }.toMutableList()
        val index = currentOrder.indexOf(budgetId)
        if (index in 0 until currentOrder.size - 1) {
            currentOrder.removeAt(index)
            currentOrder.add(index + 1, budgetId)
            viewModelScope.launch {
                budgetDisplayRepository.saveOrder(currentOrder)
            }
        }
    }
}
