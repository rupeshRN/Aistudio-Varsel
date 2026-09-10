package com.varsel.expensetracker.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.domain.engine.AutoTransferReconciliationEngine
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.domain.usecase.AddManualTransactionUseCase
import com.varsel.expensetracker.ui.model.TransactionUiMapper
import com.varsel.expensetracker.ui.transaction.model.AccountOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val transactionUiMapper: TransactionUiMapper,
    private val autoTransferReconciliationEngine: AutoTransferReconciliationEngine,
    private val addManualTransactionUseCase: AddManualTransactionUseCase,
    private val statementSnapshotRepository: StatementSnapshotRepository,
    private val categoryDao: CategoryDao
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableAccounts: StateFlow<List<AccountOption>> = statementSnapshotRepository.observeAllSnapshots()
        .map { snapshots ->
            val cashOption = AccountOption(
                accountId = null,
                accountLast4 = null,
                bankName = "Cash",
                displayName = "Cash / General"
            )
            val bankAccounts = snapshots.mapNotNull { snap ->
                val id = snap.accountId ?: return@mapNotNull null
                val last4 = snap.accountLast4 ?: "••••"
                val name = snap.bankName ?: "Bank Account"
                AccountOption(
                    accountId = id,
                    accountLast4 = last4,
                    bankName = name,
                    displayName = "$name (•••• $last4)"
                )
            }.distinctBy { it.accountId }
            listOf(cashOption) + bankAccounts
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            listOf(
                AccountOption(
                    accountId = null,
                    accountLast4 = null,
                    bankName = "Cash",
                    displayName = "Cash / General"
                )
            )
        )

    private val _uiState =
        MutableStateFlow(
            TransactionUiState()
        )

    val uiState: StateFlow<TransactionUiState> =
        _uiState.asStateFlow()

    private var allTransactions: List<Transaction> =
        emptyList()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                autoTransferReconciliationEngine.reconcileTransfers()
            } catch (_: Exception) {}
        }
        loadTransactions()
    }

    private fun loadTransactions() {

        viewModelScope.launch {

            repository
                .getAllTransactions()
                .collectLatest { transactions ->

                    allTransactions = transactions

                    recalculateUi()

                }

        }

    }

    private fun recalculateUi() {

    val state = _uiState.value

    val months = allTransactions
        .map {

            YearMonth.from(

                Instant.ofEpochMilli(
                    it.dateTimestamp
                ).atZone(
                    ZoneId.systemDefault()
                )

            )

        }
        .distinct()
        .sortedDescending()

    val availableMonths =
        months.map { yearMonth ->

            TransactionMonth(

                yearMonth = yearMonth,

                displayName =

    buildString {

        append(

            yearMonth.month.name
                .lowercase()
                .replaceFirstChar {

                    it.uppercase()

                }
                .take(3)

        )

        if (

            yearMonth.year !=
            YearMonth.now().year

        ) {

            append(" ")

            append(yearMonth.year)

        }

    }

            )

        }

    val selectedMonth =

    availableMonths.firstOrNull {

        it.yearMonth == state.selectedMonth?.yearMonth

    }

    ?: availableMonths.firstOrNull()

    val monthTransactions =
        allTransactions.filter {

            isInSelectedMonth(

                it,

                selectedMonth

            )

        }

    val searchTransactions =
        if (state.searchQuery.isBlank()) {

            monthTransactions

        } else {

            val query =
                state.searchQuery
                    .trim()
                    .lowercase()

            val cleanAmountQuery = query.replace("₹", "").replace(",", "").trim()
            val amountDouble = cleanAmountQuery.toDoubleOrNull()

            monthTransactions.filter { transaction ->
                val amountFormatted = "%.2f".format(transaction.amount)
                val amountIntStr = transaction.amount.toLong().toString()
                val amountStr = transaction.amount.toString()

                transaction.description
                    .lowercase()
                    .contains(query)
                    ||
                    transaction.category
                        .lowercase()
                        .contains(query)
                    ||
                    (transaction.referenceNumber
                        ?.lowercase()
                        ?.contains(query) == true)
                    ||
                    (cleanAmountQuery.isNotEmpty() && (
                        amountFormatted.contains(cleanAmountQuery) ||
                        amountIntStr.contains(cleanAmountQuery) ||
                        amountStr.contains(cleanAmountQuery) ||
                        (amountDouble != null && kotlin.math.abs(transaction.amount - amountDouble) < 0.01)
                    ))
            }

        }

    val finalTransactions =
        when (state.selectedFilter) {

            TransactionFilter.All ->

                searchTransactions

            TransactionFilter.Income ->

                searchTransactions.filter {

                    it.type ==
                            TransactionType.INCOME

                }

            TransactionFilter.Expense ->

                searchTransactions.filter {

                    it.type ==
                            TransactionType.EXPENSE

                }

        }

    val income =
        finalTransactions
            .filter {

                it.type ==
                        TransactionType.INCOME

            }
            .sumOf {

                it.amount

            }

    val expense =
        finalTransactions
            .filter {

                it.type ==
                        TransactionType.EXPENSE

            }
            .sumOf {

                it.amount

            }

    _uiState.update {

        it.copy(

            transactions =
                transactionUiMapper.map(
                    finalTransactions
                ),

            availableMonths =
                availableMonths,

            selectedMonth =
                selectedMonth,

            monthlyIncome =
                income,

            monthlyExpense =
                expense,

            isLoading = false

        )

    }

}

private fun isInSelectedMonth(

    transaction: Transaction,

    selectedMonth: TransactionMonth?

): Boolean {

    if (selectedMonth == null) {

        return true

    }

    val transactionMonth =
        YearMonth.from(

            Instant.ofEpochMilli(
                transaction.dateTimestamp
            ).atZone(
                ZoneId.systemDefault()
            )

        )

    return transactionMonth ==
            selectedMonth.yearMonth

}

    fun updateSearchQuery(query: String) {

        _uiState.update {

            it.copy(

                searchQuery = query

            )

        }

        recalculateUi()

    }

    fun updateSelectedMonth(

        month: TransactionMonth

    ) {

        _uiState.update {

            it.copy(

                selectedMonth = month

            )

        }

        recalculateUi()

    }

    fun updateFilter(

        filter: TransactionFilter

    ) {

        _uiState.update {

            it.copy(

                selectedFilter = filter

            )

        }

        recalculateUi()

    }

    fun addTransaction(
        amount: Double,
        type: TransactionType,
        description: String,
        category: String,
        dateTimestamp: Long,
        referenceNumber: String?,
        accountId: String? = null,
        accountLast4: String? = null,
        bankName: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = addManualTransactionUseCase.addTransaction(
                amount = amount,
                type = type,
                description = description,
                category = category,
                dateTimestamp = dateTimestamp,
                referenceNumber = referenceNumber,
                accountId = accountId,
                accountLast4 = accountLast4,
                bankName = bankName
            )
            onComplete?.invoke(result.isSuccess)
        }
    }

    fun addTransfer(
        amount: Double,
        description: String,
        dateTimestamp: Long,
        fromAccountId: String?,
        fromAccountLast4: String?,
        fromBankName: String?,
        toAccountId: String?,
        toAccountLast4: String?,
        toBankName: String?,
        referenceNumber: String?,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = addManualTransactionUseCase.addTransfer(
                amount = amount,
                description = description,
                dateTimestamp = dateTimestamp,
                fromAccountId = fromAccountId,
                fromAccountLast4 = fromAccountLast4,
                fromBankName = fromBankName,
                toAccountId = toAccountId,
                toAccountLast4 = toAccountLast4,
                toBankName = toBankName,
                referenceNumber = referenceNumber
            )
            onComplete?.invoke(result.isSuccess)
        }
    }

    fun createCategory(name: String, isIncome: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = name.trim()
            if (trimmed.isBlank()) return@launch
            val iconKey = com.varsel.expensetracker.category.CategoryIconCatalog.iconKeyForCategory(trimmed, isIncome)
            val colorHex = if (isIncome) "#4CAF50" else "#E91E63"
            val entity = CategoryEntity(
                name = trimmed,
                type = if (isIncome) "INCOME" else "EXPENSE",
                iconName = iconKey,
                colorHex = colorHex
            )
            categoryDao.insertCategory(entity)
        }
    }
}
