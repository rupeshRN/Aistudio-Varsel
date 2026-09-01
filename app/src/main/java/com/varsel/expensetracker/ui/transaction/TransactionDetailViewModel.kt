package com.varsel.expensetracker.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.category.CategoryMetadata
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.entity.FinancialEventAllocationEntity
import com.varsel.expensetracker.data.repository.CustomRuleRepository
import com.varsel.expensetracker.data.repository.FinancialEventAllocationRepository
import com.varsel.expensetracker.domain.model.FinancialEventAllocationResult
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionLinkGroup
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.repository.TransactionLinkGroupRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.domain.repository.TransferLinkResult
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
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val customRuleRepository: CustomRuleRepository,
    private val transactionLinkGroupRepository: TransactionLinkGroupRepository,
    private val categoryDao: CategoryDao,
    private val financialEventAllocationRepository: FinancialEventAllocationRepository,
    private val createFinancialEventAllocationUseCase: CreateFinancialEventAllocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransactionDetailUiState>(
        TransactionDetailUiState.Loading
    )
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    private val _saveCompleted = MutableStateFlow(false)
    val saveCompleted: StateFlow<Boolean> = _saveCompleted.asStateFlow()

    private var transactionObservationJob: Job? = null

    //--------------------------------------------------
    // Load transaction
    //--------------------------------------------------

    fun loadTransaction(transactionId: Long) {
        transactionObservationJob?.cancel()

        viewModelScope.launch {
            val transaction = transactionRepository.getTransactionById(transactionId)

            if (transaction == null) {
                _uiState.value = TransactionDetailUiState.Error("Transaction not found.")
                return@launch
            }

            val isIncome = transaction.type == TransactionType.INCOME || transaction.type == TransactionType.CREDIT
            val categories = loadCategories(isIncome)

            _uiState.value = TransactionDetailUiState.Loaded(
                transaction = transaction,
                editableDescription = transaction.description,
                selectedCategory = transaction.category,
                selectedRole = transaction.role,
                hasChanges = false,
                isSaving = false,
                categories = categories,
                allocations = emptyList(),
                totalAllocatedAmount = 0.0,
                remainingUnallocatedAmount = abs(transaction.amount),
                allAvailableEventGroups = emptyList(),
                showCreateGroupPrompt = false,
                showAllocateExistingPrompt = false,
                editingAllocation = null,
                allocationErrorMessage = null,
                linkedTransactions = emptyList(),
                isLinking = false,
                transactionLinkGroup = null,
                isSavingGroup = false,
                linkedTransfer = null,
                transferCandidates = emptyList(),
                isTransferLinking = false,
                transferErrorMessage = null
            )

            observeTransactions(transactionId)
        }
    }

    private fun loadCategories(isIncome: Boolean): List<String> {
        return CategoryMetadata.categoriesFor(isIncome)
            .map { it.id }
            .filter { it.isNotBlank() }
            .distinct()
    }

    //--------------------------------------------------
    // Observe transaction and allocation changes
    //--------------------------------------------------

    private fun observeTransactions(transactionId: Long) {
        transactionObservationJob = viewModelScope.launch {
            combine(
                transactionRepository.getAllTransactions(),
                transactionLinkGroupRepository.getAllGroups(),
                financialEventAllocationRepository.observeAllAllocations(),
                categoryDao.getAllCategories()
            ) { allTransactions, allGroups, allAllocations, dbCategories ->
                val currentTx = allTransactions.firstOrNull { it.id == transactionId }
                val isIncome = currentTx?.let { it.type == TransactionType.INCOME || it.type == TransactionType.CREDIT } ?: false
                val staticCategoryNames = CategoryMetadata.categoriesFor(isIncome).map { it.id }
                val filteredDbCategories = dbCategories
                    .filter { it.type == "BOTH" || (isIncome && it.type == "INCOME") || (!isIncome && it.type == "EXPENSE") }
                    .map { it.name }
                val categoryNames = (filteredDbCategories + staticCategoryNames).distinct()
                Quad(allTransactions, allGroups, allAllocations, categoryNames)
            }.collectLatest { (allTransactions, allGroups, allAllocations, categoryNames) ->
                updateTransactionDetailState(
                    transactionId = transactionId,
                    allTransactions = allTransactions,
                    allGroups = allGroups,
                    allAllocations = allAllocations,
                    categoryNames = categoryNames
                )
            }
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private suspend fun updateTransactionDetailState(
        transactionId: Long,
        allTransactions: List<Transaction>,
        allGroups: List<TransactionLinkGroup>,
        allAllocations: List<FinancialEventAllocationEntity>,
        categoryNames: List<String>
    ) {
        val currentState = _uiState.value as? TransactionDetailUiState.Loaded ?: return

        val currentTransaction = allTransactions.firstOrNull {
            it.id == transactionId
        } ?: currentState.transaction

        val txMagnitude = abs(currentTransaction.amount)

        //--------------------------------------------------
        // Multi-Event Allocations for this transaction
        //--------------------------------------------------

        val txAllocations = allAllocations.filter { it.transactionId == transactionId }

        val allocationUiModels = txAllocations.map { alloc ->
            val group = allGroups.firstOrNull { it.transactionLinkId == alloc.transactionLinkId }
            TransactionEventAllocationUiModel(
                allocationId = alloc.id,
                transactionLinkId = alloc.transactionLinkId,
                groupName = group?.groupName ?: "Financial Event",
                category = group?.category ?: "Other",
                allocatedAmount = alloc.allocatedAmount,
                totalTransactionAmount = txMagnitude
            )
        }.toMutableList()

        // Handle legacy link if no allocation row exists
        if (allocationUiModels.isEmpty() && currentTransaction.transactionLinkId != null) {
            val legacyGroup = allGroups.firstOrNull { it.transactionLinkId == currentTransaction.transactionLinkId }
            if (legacyGroup != null) {
                allocationUiModels.add(
                    TransactionEventAllocationUiModel(
                        allocationId = -currentTransaction.id,
                        transactionLinkId = currentTransaction.transactionLinkId,
                        groupName = legacyGroup.groupName,
                        category = legacyGroup.category,
                        allocatedAmount = txMagnitude,
                        totalTransactionAmount = txMagnitude
                    )
                )
            }
        }

        val totalAllocated = allocationUiModels.sumOf { it.allocatedAmount }
        val remainingUnallocated = maxOf(0.0, txMagnitude - totalAllocated)

        val availableGroups = allGroups.filter { group ->
            allocationUiModels.none { it.transactionLinkId == group.transactionLinkId }
        }

        val primaryGroup = allocationUiModels.firstOrNull()?.let { allocModel ->
            allGroups.firstOrNull { it.transactionLinkId == allocModel.transactionLinkId }
        } ?: currentTransaction.transactionLinkId?.let { linkId ->
            allGroups.firstOrNull { it.transactionLinkId == linkId }
        }

        val linkedTransactions = primaryGroup?.let { group ->
            allTransactions.filter { it.transactionLinkId == group.transactionLinkId }
                .sortedByDescending { it.dateTimestamp }
        }.orEmpty()

        //--------------------------------------------------
        // Transfer logic
        //--------------------------------------------------

        val transferLinkId = currentTransaction.transferLinkId

        val linkedTransfer = transferLinkId?.let { linkId ->
            allTransactions.firstOrNull {
                it.id != currentTransaction.id && it.transferLinkId == linkId
            }
        }

        val transferCandidates = if (transferLinkId == null) {
            when (currentTransaction.role) {
                TransactionRole.TRANSFER_OUT -> {
                    allTransactions.filter { candidate ->
                        candidate.id != currentTransaction.id &&
                            candidate.type == TransactionType.INCOME &&
                            candidate.role == TransactionRole.TRANSFER_IN &&
                            candidate.transferLinkId == null
                    }.sortedByDescending { it.dateTimestamp }
                }
                TransactionRole.TRANSFER_IN -> {
                    allTransactions.filter { candidate ->
                        candidate.id != currentTransaction.id &&
                            candidate.type == TransactionType.EXPENSE &&
                            candidate.role == TransactionRole.TRANSFER_OUT &&
                            candidate.transferLinkId == null
                    }.sortedByDescending { it.dateTimestamp }
                }
                else -> emptyList()
            }
        } else {
            emptyList()
        }

        _uiState.value = currentState.copy(
            transaction = currentTransaction,
            categories = categoryNames,
            allocations = allocationUiModels,
            totalAllocatedAmount = totalAllocated,
            remainingUnallocatedAmount = remainingUnallocated,
            allAvailableEventGroups = availableGroups,
            linkedTransactions = linkedTransactions,
            transactionLinkGroup = primaryGroup,
            showCreateGroupPrompt = if (currentState.isSavingGroup) false else currentState.showCreateGroupPrompt,
            isSavingGroup = false,
            linkedTransfer = linkedTransfer,
            transferCandidates = transferCandidates,
            isLinking = false,
            isTransferLinking = false
        )
    }

    //--------------------------------------------------
    // Multi-Event Allocation Actions
    //--------------------------------------------------

    fun showCreateGroupPrompt() {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        _uiState.value = current.copy(showCreateGroupPrompt = true, allocationErrorMessage = null)
    }

    fun dismissCreateGroupPrompt() {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        _uiState.value = current.copy(showCreateGroupPrompt = false)
    }

    fun showAllocateExistingPrompt() {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        _uiState.value = current.copy(showAllocateExistingPrompt = true, allocationErrorMessage = null)
    }

    fun dismissAllocateExistingPrompt() {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        _uiState.value = current.copy(showAllocateExistingPrompt = false)
    }

    fun startEditingAllocation(allocation: TransactionEventAllocationUiModel) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        _uiState.value = current.copy(editingAllocation = allocation, allocationErrorMessage = null)
    }

    fun dismissEditingAllocation() {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        _uiState.value = current.copy(editingAllocation = null)
    }

    fun clearAllocationError() {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        _uiState.value = current.copy(allocationErrorMessage = null)
    }

    fun createReportGroup(
        groupName: String,
        category: String,
        allocatedAmount: Double? = null
    ) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        if (current.isSavingGroup) return

        val cleanName = groupName.trim()
        val cleanCategory = category.trim()
        if (cleanName.isBlank() || cleanCategory.isBlank()) return

        val selectedCategory = current.categories.firstOrNull {
            it.equals(cleanCategory, ignoreCase = true)
        } ?: return

        viewModelScope.launch {
            _uiState.value = current.copy(isSavingGroup = true, allocationErrorMessage = null)

            val newLinkId = UUID.randomUUID().toString()

            val group = TransactionLinkGroup(
                transactionLinkId = newLinkId,
                groupName = cleanName,
                category = selectedCategory,
                createdAt = System.currentTimeMillis()
            )
            transactionLinkGroupRepository.saveGroup(group)

            val amountToAllocate = allocatedAmount ?: if (current.remainingUnallocatedAmount > 0.0) {
                current.remainingUnallocatedAmount
            } else {
                abs(current.transaction.amount)
            }

            val result = createFinancialEventAllocationUseCase(
                transactionId = current.transaction.id,
                transactionLinkId = newLinkId,
                allocatedAmount = amountToAllocate
            )

            when (result) {
                is FinancialEventAllocationResult.Success -> {
                    _uiState.value = current.copy(
                        showCreateGroupPrompt = false,
                        isSavingGroup = false,
                        allocationErrorMessage = null
                    )
                }
                is FinancialEventAllocationResult.ExceedsTransactionAmount -> {
                    _uiState.value = current.copy(
                        isSavingGroup = false,
                        allocationErrorMessage = "Amount exceeds available unallocated balance (₹%.2f)".format(result.remainingAmount)
                    )
                }
                is FinancialEventAllocationResult.InvalidAmount -> {
                    _uiState.value = current.copy(
                        isSavingGroup = false,
                        allocationErrorMessage = result.message
                    )
                }
            }
        }
    }

    fun allocateToExistingGroup(transactionLinkId: String, allocatedAmount: Double) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        if (current.isSavingGroup || allocatedAmount <= 0.0) return

        viewModelScope.launch {
            _uiState.value = current.copy(isSavingGroup = true, allocationErrorMessage = null)

            val result = createFinancialEventAllocationUseCase(
                transactionId = current.transaction.id,
                transactionLinkId = transactionLinkId,
                allocatedAmount = allocatedAmount
            )

            when (result) {
                is FinancialEventAllocationResult.Success -> {
                    _uiState.value = current.copy(
                        showAllocateExistingPrompt = false,
                        isSavingGroup = false,
                        allocationErrorMessage = null
                    )
                }
                is FinancialEventAllocationResult.ExceedsTransactionAmount -> {
                    _uiState.value = current.copy(
                        isSavingGroup = false,
                        allocationErrorMessage = "Amount exceeds available balance (Remaining: ₹%.2f)".format(result.remainingAmount)
                    )
                }
                is FinancialEventAllocationResult.InvalidAmount -> {
                    _uiState.value = current.copy(
                        isSavingGroup = false,
                        allocationErrorMessage = result.message
                    )
                }
            }
        }
    }

    fun updateAllocationAmount(transactionLinkId: String, newAmount: Double) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        if (newAmount <= 0.0) return

        // Verify that new amount does not exceed (txMagnitude - otherAllocations)
        val otherAllocationsSum = current.allocations
            .filter { it.transactionLinkId != transactionLinkId }
            .sumOf { it.allocatedAmount }
        val maxAllowed = abs(current.transaction.amount) - otherAllocationsSum

        if (newAmount > maxAllowed + 0.000001) {
            _uiState.value = current.copy(
                allocationErrorMessage = "Total allocation cannot exceed ₹%.2f (Maximum allowed: ₹%.2f)".format(abs(current.transaction.amount), maxAllowed)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(editingAllocation = null, allocationErrorMessage = null)
            financialEventAllocationRepository.updateAllocationAmount(
                transactionId = current.transaction.id,
                transactionLinkId = transactionLinkId,
                newAmount = newAmount
            )
        }
    }

    fun deleteAllocation(transactionLinkId: String) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return

        viewModelScope.launch {
            financialEventAllocationRepository.deleteAllocationForTransactionAndEvent(
                transactionId = current.transaction.id,
                transactionLinkId = transactionLinkId
            )

            if (current.transaction.transactionLinkId == transactionLinkId) {
                transactionRepository.unlinkTransaction(current.transaction.id)
            }

            val remainingAllocations = financialEventAllocationRepository.getAllocationsForFinancialEvent(transactionLinkId)
            val remainingLinkedTxs = transactionRepository.getAllTransactions().first().filter { it.transactionLinkId == transactionLinkId }
            if (remainingAllocations.isEmpty() && remainingLinkedTxs.isEmpty()) {
                transactionLinkGroupRepository.deleteGroup(transactionLinkId)
            }
        }
    }

    fun unlinkCurrentTransaction() {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return

        viewModelScope.launch {
            _uiState.value = current.copy(isLinking = true)

            val eventIdsToCheck = current.allocations.map { it.transactionLinkId }.toSet() +
                listOfNotNull(current.transaction.transactionLinkId)

            for (alloc in current.allocations) {
                financialEventAllocationRepository.deleteAllocationForTransactionAndEvent(
                    transactionId = current.transaction.id,
                    transactionLinkId = alloc.transactionLinkId
                )
            }

            if (current.transaction.transactionLinkId != null) {
                transactionRepository.unlinkTransaction(current.transaction.id)
            }

            for (linkId in eventIdsToCheck) {
                val remainingAllocations = financialEventAllocationRepository.getAllocationsForFinancialEvent(linkId)
                val remainingLinkedTxs = transactionRepository.getAllTransactions().first().filter { it.transactionLinkId == linkId }
                if (remainingAllocations.isEmpty() && remainingLinkedTxs.isEmpty()) {
                    transactionLinkGroupRepository.deleteGroup(linkId)
                }
            }
        }
    }

    fun deleteReportGroup() {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        val transactionLinkId = current.transaction.transactionLinkId ?: return

        viewModelScope.launch {
            transactionLinkGroupRepository.deleteGroup(transactionLinkId)
            _uiState.value = current.copy(transactionLinkGroup = null)
        }
    }

    //--------------------------------------------------
    // Description, Category, Role
    //--------------------------------------------------

    fun updateDescription(description: String) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        _uiState.value = current.copy(
            editableDescription = description,
            hasChanges = description != current.transaction.description ||
                current.selectedCategory != current.transaction.category ||
                current.selectedRole != current.transaction.role
        )
    }

    fun updateCategory(category: String) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        _uiState.value = current.copy(
            selectedCategory = category,
            hasChanges = category != current.transaction.category ||
                current.editableDescription != current.transaction.description ||
                current.selectedRole != current.transaction.role
        )
    }

    fun updateRole(role: TransactionRole) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        _uiState.value = current.copy(
            selectedRole = role,
            hasChanges = role != current.transaction.role ||
                current.editableDescription != current.transaction.description ||
                current.selectedCategory != current.transaction.category
        )
    }

    //--------------------------------------------------
    // Transfer linking
    //--------------------------------------------------

    fun linkTransfer(otherTransactionId: Long) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        if (current.isTransferLinking) return

        if (current.transaction.id == otherTransactionId) {
            _uiState.value = current.copy(
                transferErrorMessage = "Please choose a different transaction for the transfer."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isTransferLinking = true, transferErrorMessage = null)

            val otherTransaction = transactionRepository.getTransactionById(otherTransactionId)
            if (otherTransaction == null) {
                _uiState.value = current.copy(
                    isTransferLinking = false,
                    transferErrorMessage = "The selected transaction could not be found."
                )
                return@launch
            }

            val transferOutId: Long
            val transferInId: Long

            when {
                current.transaction.role == TransactionRole.TRANSFER_OUT &&
                    otherTransaction.role == TransactionRole.TRANSFER_IN -> {
                    transferOutId = current.transaction.id
                    transferInId = otherTransaction.id
                }
                current.transaction.role == TransactionRole.TRANSFER_IN &&
                    otherTransaction.role == TransactionRole.TRANSFER_OUT -> {
                    transferOutId = otherTransaction.id
                    transferInId = current.transaction.id
                }
                else -> {
                    _uiState.value = current.copy(
                        isTransferLinking = false,
                        transferErrorMessage = "Please select one Transfer In and one Transfer Out transaction."
                    )
                    return@launch
                }
            }

            when (val result = transactionRepository.linkTransfer(transferOutId, transferInId)) {
                TransferLinkResult.Success -> {
                    _uiState.value = current.copy(isTransferLinking = false, transferErrorMessage = null)
                }
                is TransferLinkResult.AmountMismatch -> {
                    _uiState.value = current.copy(
                        isTransferLinking = false,
                        transferErrorMessage = "The transfer amounts don't match. Transfer Out: ₹${result.transferOutAmount}, Transfer In: ₹${result.transferInAmount}."
                    )
                }
                TransferLinkResult.InvalidTransactionPair -> {
                    _uiState.value = current.copy(
                        isTransferLinking = false,
                        transferErrorMessage = "These transactions cannot be linked as a transfer."
                    )
                }
                TransferLinkResult.TransactionNotFound -> {
                    _uiState.value = current.copy(
                        isTransferLinking = false,
                        transferErrorMessage = "One of the selected transfer transactions could not be found."
                    )
                }
                TransferLinkResult.AlreadyLinked -> {
                    _uiState.value = current.copy(
                        isTransferLinking = false,
                        transferErrorMessage = "One of these transactions is already linked to another transfer."
                    )
                }
            }
        }
    }

    fun clearTransferError() {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        _uiState.value = current.copy(transferErrorMessage = null)
    }

    fun unlinkTransfer() {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        if (current.transaction.transferLinkId == null || current.isLinking) return

        viewModelScope.launch {
            _uiState.value = current.copy(isLinking = true, transferErrorMessage = null)
            transactionRepository.unlinkTransfer(current.transaction.id)
        }
    }

    //--------------------------------------------------
    // Smart Rule & Similar Transactions Management
    //--------------------------------------------------

    fun prepareSaveSmartRuleDialog() {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        loadSimilarTransactions(current.selectedPastTimeframe, current.customCutoffTimestamp)
    }

    fun setPastTimeframe(timeframe: PastTimeframe, customDate: Long? = null) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        _uiState.value = current.copy(
            selectedPastTimeframe = timeframe,
            customCutoffTimestamp = customDate ?: current.customCutoffTimestamp
        )
        loadSimilarTransactions(timeframe, customDate ?: current.customCutoffTimestamp)
    }

    fun setApplyToSimilar(enabled: Boolean) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        _uiState.value = current.copy(applyToSimilarTransactions = enabled)
        if (enabled && current.similarTransactions.isEmpty()) {
            loadSimilarTransactions(current.selectedPastTimeframe, current.customCutoffTimestamp)
        }
    }

    fun setUpdateDescriptionForSimilar(enabled: Boolean) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        _uiState.value = current.copy(updateDescriptionForSimilar = enabled)
    }

    fun loadSimilarTransactions(timeframe: PastTimeframe, customDate: Long? = null) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return

        val now = System.currentTimeMillis()
        val sinceTimestamp = when (timeframe) {
            PastTimeframe.ALL_TIME -> 0L
            PastTimeframe.LAST_30_DAYS -> now - (30L * 24 * 60 * 60 * 1000L)
            PastTimeframe.LAST_90_DAYS -> now - (90L * 24 * 60 * 60 * 1000L)
            PastTimeframe.LAST_180_DAYS -> now - (180L * 24 * 60 * 60 * 1000L)
            PastTimeframe.CUSTOM -> customDate ?: (now - 30L * 24 * 60 * 60 * 1000L)
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isLoadingSimilar = true)
            val isIncome = current.transaction.type == TransactionType.INCOME
            val patternToSearch = current.transaction.description
            val matches = transactionRepository.findSimilarTransactions(
                excludeId = current.transaction.id,
                pattern = patternToSearch,
                isIncome = isIncome,
                sinceTimestamp = sinceTimestamp
            )
            val latest = _uiState.value as? TransactionDetailUiState.Loaded ?: return@launch
            _uiState.value = latest.copy(
                similarTransactions = matches,
                similarTransactionsCount = matches.size,
                selectedPastTimeframe = timeframe,
                customCutoffTimestamp = if (timeframe == PastTimeframe.CUSTOM) (customDate ?: latest.customCutoffTimestamp) else latest.customCutoffTimestamp,
                isLoadingSimilar = false
            )
        }
    }

    //--------------------------------------------------
    // Save transaction changes
    //--------------------------------------------------

    fun saveChanges(
        createSmartRule: Boolean = true,
        applyToSimilar: Boolean = false,
        updateDescriptionForSimilar: Boolean = true
    ) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        if (current.isSaving) return

        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true)

            val updatedTransaction = current.transaction.copy(
                description = current.editableDescription,
                category = current.selectedCategory,
                role = current.selectedRole
            )

            if (createSmartRule && (
                current.transaction.description != current.editableDescription ||
                current.transaction.category != current.selectedCategory
            )) {
                customRuleRepository.saveRule(
                    pattern = current.transaction.description,
                    displayDescription = current.editableDescription,
                    categoryName = current.selectedCategory
                )
            }

            transactionRepository.updateTransaction(updatedTransaction)

            if (applyToSimilar && current.similarTransactions.isNotEmpty()) {
                val similarToUpdate = current.similarTransactions.map { sim ->
                    sim.copy(
                        category = current.selectedCategory,
                        description = if (updateDescriptionForSimilar && current.editableDescription.isNotBlank()) {
                            current.editableDescription
                        } else {
                            sim.description
                        }
                    )
                }
                transactionRepository.updateTransactions(similarToUpdate)
            }

            _saveCompleted.value = true
            _uiState.value = current.copy(
                transaction = updatedTransaction,
                selectedRole = updatedTransaction.role,
                hasChanges = false,
                isSaving = false,
                transferErrorMessage = null
            )
        }
    }

    fun createCategory(name: String, isIncome: Boolean) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val iconKey = com.varsel.expensetracker.category.CategoryIconCatalog.iconKeyForCategory(name, isIncome)
            val typeStr = if (isIncome) "INCOME" else "EXPENSE"
            val newCategory = com.varsel.expensetracker.data.local.entity.CategoryEntity(
                name = name.trim(),
                type = typeStr,
                colorHex = if (isIncome) "#4CAF50" else "#2196F3",
                iconName = iconKey,
                budgetLimit = 0.0,
                keywords = name.trim().uppercase()
            )
            categoryDao.insertCategory(newCategory)
            updateCategory(name.trim())
        }
    }

    fun deleteTransaction(onDeleted: () -> Unit) {
        val current = _uiState.value as? TransactionDetailUiState.Loaded ?: return
        if (current.transaction.isImported) return

        viewModelScope.launch {
            transactionRepository.deleteTransaction(current.transaction)
            onDeleted()
        }
    }

    fun consumeSaveCompleted() {
        _saveCompleted.value = false
    }
}
