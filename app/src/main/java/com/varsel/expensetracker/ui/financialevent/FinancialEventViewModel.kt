package com.varsel.expensetracker.ui.financialevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.category.CategoryMetadata
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.entity.FinancialEventAllocationEntity
import com.varsel.expensetracker.data.repository.FinancialEventAllocationRepository
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionLinkGroup
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.repository.TransactionLinkGroupRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.domain.usecase.CreateFinancialEventAllocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class FinancialEventViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transactionLinkGroupRepository: TransactionLinkGroupRepository,
    private val financialEventAllocationRepository: FinancialEventAllocationRepository,
    private val createFinancialEventAllocationUseCase: CreateFinancialEventAllocationUseCase,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<FinancialEventUiState>(FinancialEventUiState.Loading)
    val uiState: StateFlow<FinancialEventUiState> = _uiState.asStateFlow()

    private var currentTransactionLinkId: String? = null
    private var observeJob: Job? = null

    fun loadFinancialEvent(transactionLinkId: String) {
        if (currentTransactionLinkId == transactionLinkId && observeJob?.isActive == true) {
            return
        }

        currentTransactionLinkId = transactionLinkId
        observeJob?.cancel()

        observeJob = viewModelScope.launch {
            combine(
                transactionRepository.getAllTransactions(),
                financialEventAllocationRepository.observeAllAllocations(),
                categoryDao.getAllCategories()
            ) { transactions, allocations, dbCategories ->
                Triple(transactions, allocations, dbCategories)
            }.collectLatest { (transactions, allocations, dbCategories) ->
                rebuildState(
                    transactionLinkId = transactionLinkId,
                    transactions = transactions,
                    allocations = allocations,
                    dbCategories = dbCategories.map { it.name }
                )
            }
        }
    }

    private suspend fun rebuildState(
        transactionLinkId: String,
        transactions: List<Transaction>,
        allocations: List<FinancialEventAllocationEntity>,
        dbCategories: List<String> = emptyList()
    ) {
        val group = transactionLinkGroupRepository.getGroup(transactionLinkId)
        if (group == null) {
            _uiState.value = FinancialEventUiState.EventDeleted
            return
        }

        val eventAllocations = allocations.filter { it.transactionLinkId == transactionLinkId }

        // Mapped allocated expenses and reimbursements
        val allocatedExpenses = mutableListOf<FinancialEventItemUiModel>()
        val allocatedReimbursements = mutableListOf<FinancialEventItemUiModel>()

        for (alloc in eventAllocations) {
            val tx = transactions.firstOrNull { it.id == alloc.transactionId }
            if (tx != null) {
                val item = FinancialEventItemUiModel(
                    allocationId = alloc.id,
                    transaction = tx,
                    allocatedAmount = alloc.allocatedAmount,
                    totalTransactionAmount = abs(tx.amount)
                )
                if (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.DEBIT) {
                    allocatedExpenses.add(item)
                } else {
                    allocatedReimbursements.add(item)
                }
            }
        }

        // Also check if any legacy linked transactions exist that don't have allocation rows yet
        val legacyLinkedTxs = transactions.filter {
            it.transactionLinkId == transactionLinkId && eventAllocations.none { alloc -> alloc.transactionId == it.id }
        }
        for (tx in legacyLinkedTxs) {
            val item = FinancialEventItemUiModel(
                allocationId = -tx.id,
                transaction = tx,
                allocatedAmount = abs(tx.amount),
                totalTransactionAmount = abs(tx.amount)
            )
            if (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.DEBIT) {
                allocatedExpenses.add(item)
            } else {
                allocatedReimbursements.add(item)
            }
        }

        // Sort by timestamp descending
        allocatedExpenses.sortByDescending { it.transaction.dateTimestamp }
        allocatedReimbursements.sortByDescending { it.transaction.dateTimestamp }

        // Find available transactions for allocation
        val availableExpenses = mutableListOf<AvailableTransactionUiModel>()
        val availableReimbursements = mutableListOf<AvailableTransactionUiModel>()

        for (tx in transactions) {
            // Transfers (TRANSFER_IN, TRANSFER_OUT, transferLinkId, or Transfer category) are not eligible for Financial Events
            if (tx.isTransfer) continue

            // If already allocated to THIS event, skip from available list (they can edit amount directly)
            val alreadyInThisEvent = eventAllocations.any { it.transactionId == tx.id } || tx.transactionLinkId == transactionLinkId
            if (alreadyInThisEvent) continue

            val txMagnitude = abs(tx.amount)
            val totalAllocated = allocations.filter { it.transactionId == tx.id }.sumOf { it.allocatedAmount }
            val remaining = maxOf(0.0, txMagnitude - totalAllocated)

            if (remaining > 0.009) {
                val avail = AvailableTransactionUiModel(
                    transaction = tx,
                    remainingAmount = remaining,
                    totalAmount = txMagnitude
                )
                if (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.DEBIT) {
                    availableExpenses.add(avail)
                } else {
                    availableReimbursements.add(avail)
                }
            }
        }

        availableExpenses.sortByDescending { it.transaction.dateTimestamp }
        availableReimbursements.sortByDescending { it.transaction.dateTimestamp }

        val staticCategories = CategoryMetadata.all
            .map { it.id }
            .filter { it.isNotBlank() }

        val categories = (dbCategories + staticCategories)
            .filter { it.isNotBlank() }
            .distinct()

        val totalExpenses = allocatedExpenses.sumOf { it.allocatedAmount }
        val totalReimbursements = allocatedReimbursements.sumOf { it.allocatedAmount }

        val current = _uiState.value as? FinancialEventUiState.Loaded
        val isEditingGroup = current?.isEditingGroup ?: false
        val editingItem = current?.editingItem

        _uiState.value = FinancialEventUiState.Loaded(
            group = group,
            allocatedExpenses = allocatedExpenses,
            allocatedReimbursements = allocatedReimbursements,
            expenses = allocatedExpenses.map { it.transaction },
            reimbursements = allocatedReimbursements.map { it.transaction },
            availableExpenses = availableExpenses,
            availableReimbursements = availableReimbursements,
            categories = categories,
            totalExpenses = totalExpenses,
            totalReimbursements = totalReimbursements,
            isUpdating = false,
            isEditingGroup = isEditingGroup,
            editingItem = editingItem
        )
    }

    // Add expenses with custom allocated amounts
    fun addExpensesWithAmounts(allocationsMap: Map<Long, Double>) {
        val current = _uiState.value as? FinancialEventUiState.Loaded ?: return
        if (current.isUpdating || allocationsMap.isEmpty()) return

        val linkId = current.group.transactionLinkId
        viewModelScope.launch {
            _uiState.value = current.copy(isUpdating = true)
            for ((txId, amount) in allocationsMap) {
                if (amount > 0.0) {
                    createFinancialEventAllocationUseCase(
                        transactionId = txId,
                        transactionLinkId = linkId,
                        allocatedAmount = amount
                    )
                }
            }
        }
    }

    fun addExpenses(transactionIds: Set<Long>) {
        val current = _uiState.value as? FinancialEventUiState.Loaded ?: return
        if (current.isUpdating || transactionIds.isEmpty()) return

        val allocationsMap = mutableMapOf<Long, Double>()
        for (id in transactionIds) {
            val avail = current.availableExpenses.firstOrNull { it.transaction.id == id }
            if (avail != null) {
                allocationsMap[id] = avail.remainingAmount
            }
        }
        addExpensesWithAmounts(allocationsMap)
    }

    fun addExpense(transactionId: Long) {
        addExpenses(setOf(transactionId))
    }

    // Add reimbursements with custom allocated amounts
    fun addReimbursementsWithAmounts(allocationsMap: Map<Long, Double>) {
        val current = _uiState.value as? FinancialEventUiState.Loaded ?: return
        if (current.isUpdating || allocationsMap.isEmpty()) return

        val linkId = current.group.transactionLinkId
        viewModelScope.launch {
            _uiState.value = current.copy(isUpdating = true)
            for ((txId, amount) in allocationsMap) {
                if (amount > 0.0) {
                    createFinancialEventAllocationUseCase(
                        transactionId = txId,
                        transactionLinkId = linkId,
                        allocatedAmount = amount
                    )
                }
            }
        }
    }

    fun addReimbursements(transactionIds: Set<Long>) {
        val current = _uiState.value as? FinancialEventUiState.Loaded ?: return
        if (current.isUpdating || transactionIds.isEmpty()) return

        val allocationsMap = mutableMapOf<Long, Double>()
        for (id in transactionIds) {
            val avail = current.availableReimbursements.firstOrNull { it.transaction.id == id }
            if (avail != null) {
                allocationsMap[id] = avail.remainingAmount
            }
        }
        addReimbursementsWithAmounts(allocationsMap)
    }

    fun addReimbursement(transactionId: Long) {
        addReimbursements(setOf(transactionId))
    }

    fun startEditingItem(item: FinancialEventItemUiModel) {
        val current = _uiState.value as? FinancialEventUiState.Loaded ?: return
        _uiState.value = current.copy(editingItem = item)
    }

    fun cancelEditingItem() {
        val current = _uiState.value as? FinancialEventUiState.Loaded ?: return
        _uiState.value = current.copy(editingItem = null)
    }

    fun updateItemAllocationAmount(transactionId: Long, newAmount: Double) {
        val current = _uiState.value as? FinancialEventUiState.Loaded ?: return
        if (current.isUpdating || newAmount <= 0.0) return

        val linkId = current.group.transactionLinkId
        viewModelScope.launch {
            _uiState.value = current.copy(isUpdating = true, editingItem = null)
            financialEventAllocationRepository.updateAllocationAmount(
                transactionId = transactionId,
                transactionLinkId = linkId,
                newAmount = newAmount
            )
        }
    }

    fun removeTransaction(transactionId: Long) {
        val current = _uiState.value as? FinancialEventUiState.Loaded ?: return
        if (current.isUpdating) return

        val linkId = current.group.transactionLinkId
        viewModelScope.launch {
            _uiState.value = current.copy(isUpdating = true)
            // Delete allocation record
            financialEventAllocationRepository.deleteAllocationForTransactionAndEvent(
                transactionId = transactionId,
                transactionLinkId = linkId
            )
            // Also unlink if transaction's primary transactionLinkId matched
            val tx = transactionRepository.getTransactionById(transactionId)
            if (tx?.transactionLinkId == linkId) {
                transactionRepository.unlinkTransaction(transactionId)
            }

            // Check if any allocations or linked transactions remain for this event
            val remainingAllocations = financialEventAllocationRepository.getAllocationsForFinancialEvent(linkId)
            val remainingLinkedTxs = transactionRepository.getAllTransactions().first().filter { it.transactionLinkId == linkId }
            if (remainingAllocations.isEmpty() && remainingLinkedTxs.isEmpty()) {
                transactionLinkGroupRepository.deleteGroup(linkId)
                _uiState.value = FinancialEventUiState.EventDeleted
            }
        }
    }

    fun startEditingGroup() {
        val current = _uiState.value as? FinancialEventUiState.Loaded ?: return
        _uiState.value = current.copy(isEditingGroup = true)
    }

    fun cancelEditingGroup() {
        val current = _uiState.value as? FinancialEventUiState.Loaded ?: return
        _uiState.value = current.copy(isEditingGroup = false)
    }

    fun saveGroup(groupName: String, category: String) {
        val current = _uiState.value as? FinancialEventUiState.Loaded ?: return
        val cleanName = groupName.trim()
        val cleanCategory = category.trim()

        if (cleanName.isBlank() || cleanCategory.isBlank()) return

        val categoryExists = current.categories.any { it.equals(cleanCategory, ignoreCase = true) }
        if (!categoryExists) return

        val selectedCategory = current.categories.first { it.equals(cleanCategory, ignoreCase = true) }

        viewModelScope.launch {
            val updatedGroup = TransactionLinkGroup(
                transactionLinkId = current.group.transactionLinkId,
                groupName = cleanName,
                category = selectedCategory,
                createdAt = current.group.createdAt
            )

            transactionLinkGroupRepository.saveGroup(updatedGroup)

            _uiState.value = current.copy(
                group = updatedGroup,
                isEditingGroup = false
            )
        }
    }
}
