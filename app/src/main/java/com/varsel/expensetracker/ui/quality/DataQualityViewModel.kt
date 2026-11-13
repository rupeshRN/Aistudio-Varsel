package com.varsel.expensetracker.ui.quality

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.data.repository.DataQualityRepository
import com.varsel.expensetracker.domain.model.quality.DataQualityReport
import com.varsel.expensetracker.domain.model.quality.QualityActionType
import com.varsel.expensetracker.domain.model.quality.QualityCategory
import com.varsel.expensetracker.domain.model.quality.QualityCheckType
import com.varsel.expensetracker.domain.model.quality.QualityIssue
import com.varsel.expensetracker.domain.model.quality.QualitySeverity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class QuickFixSheetState {
    object None : QuickFixSheetState()
    data class Categorize(val issue: QualityIssue) : QuickFixSheetState()
    data class DuplicateMerge(val issue: QualityIssue) : QuickFixSheetState()
    data class AdjustDate(val issue: QualityIssue) : QuickFixSheetState()
}

sealed class QualityNavEvent {
    data class OpenTransaction(val transactionId: Long) : QualityNavEvent()
    data class OpenLoan(val loanId: Long) : QualityNavEvent()
    object OpenLoans : QualityNavEvent()
    data class OpenBudget(val budgetId: Long) : QualityNavEvent()
    object OpenRecurring : QualityNavEvent()
    object OpenImport : QualityNavEvent()
}

data class DataQualityUiState(
    val isLoading: Boolean = false,
    val report: DataQualityReport? = null,
    val selectedCategory: QualityCategory = QualityCategory.ALL,
    val selectedSeverityFilter: QualitySeverity? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val quickFixSheetState: QuickFixSheetState = QuickFixSheetState.None
) {
    val filteredIssues: List<QualityIssue>
        get() {
            val baseList = report?.issues ?: emptyList()
            val categoryFiltered = if (selectedCategory == QualityCategory.ALL) {
                baseList
            } else {
                baseList.filter { it.category == selectedCategory }
            }
            return if (selectedSeverityFilter == null) {
                categoryFiltered
            } else {
                categoryFiltered.filter { it.severity == selectedSeverityFilter }
            }
        }
}

@HiltViewModel
class DataQualityViewModel @Inject constructor(
    private val repository: DataQualityRepository,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(QualityCategory.ALL)
    private val _selectedSeverity = MutableStateFlow<QualitySeverity?>(null)
    private val _quickFixState = MutableStateFlow<QuickFixSheetState>(QuickFixSheetState.None)

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _navEvents = MutableSharedFlow<QualityNavEvent>()
    val navEvents: SharedFlow<QualityNavEvent> = _navEvents.asSharedFlow()

    val uiState: StateFlow<DataQualityUiState> = combine(
        repository.dataQualityReport,
        _selectedCategory,
        _selectedSeverity,
        categoryDao.getAllCategories(),
        _quickFixState
    ) { report, category, severity, categories, quickFix ->
        DataQualityUiState(
            isLoading = false,
            report = report,
            selectedCategory = category,
            selectedSeverityFilter = severity,
            categories = categories,
            quickFixSheetState = quickFix
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DataQualityUiState(isLoading = true)
    )

    fun selectCategory(category: QualityCategory) {
        _selectedCategory.value = category
    }

    fun selectSeverity(severity: QualitySeverity?) {
        _selectedSeverity.value = severity
    }

    fun openQuickFix(issue: QualityIssue) {
        when (issue.actionType) {
            QualityActionType.CATEGORIZE -> {
                _quickFixState.value = QuickFixSheetState.Categorize(issue)
            }
            QualityActionType.DELETE_DUPLICATE -> {
                _quickFixState.value = QuickFixSheetState.DuplicateMerge(issue)
            }
            QualityActionType.ADJUST_DATE -> {
                _quickFixState.value = QuickFixSheetState.AdjustDate(issue)
            }
            QualityActionType.CLEAN_ORPHAN -> {
                viewModelScope.launch {
                    val cleaned = repository.cleanOrphanIssue(issue)
                    if (cleaned) {
                        _userMessage.emit("Cleaned orphan reference")
                    }
                }
            }
            QualityActionType.LINK_TRANSFER,
            QualityActionType.VIEW_TRANSACTION -> {
                val txId = issue.entityId
                if (txId != null) {
                    viewModelScope.launch {
                        _navEvents.emit(QualityNavEvent.OpenTransaction(txId))
                    }
                } else {
                    dismissIssue(issue)
                }
            }
            QualityActionType.VIEW_LOAN -> {
                viewModelScope.launch {
                    if (issue.type == QualityCheckType.OVERDUE_LOAN_EMI && issue.entityId != null) {
                        _navEvents.emit(QualityNavEvent.OpenLoan(issue.entityId))
                    } else if (issue.entityId != null && issue.type == QualityCheckType.UNLINKED_LOAN_PAYMENT) {
                        _navEvents.emit(QualityNavEvent.OpenTransaction(issue.entityId))
                    } else {
                        _navEvents.emit(QualityNavEvent.OpenLoans)
                    }
                }
            }
            QualityActionType.VIEW_BUDGET -> {
                val budgetId = issue.entityId
                if (budgetId != null) {
                    viewModelScope.launch {
                        _navEvents.emit(QualityNavEvent.OpenBudget(budgetId))
                    }
                }
            }
            QualityActionType.RECORD_RECURRING -> {
                viewModelScope.launch {
                    _navEvents.emit(QualityNavEvent.OpenRecurring)
                }
            }
            QualityActionType.IMPORT_STATEMENT -> {
                viewModelScope.launch {
                    _navEvents.emit(QualityNavEvent.OpenImport)
                }
            }
            QualityActionType.DISMISS -> {
                dismissIssue(issue)
            }
        }
    }

    fun closeQuickFix() {
        _quickFixState.value = QuickFixSheetState.None
    }

    fun dismissIssue(issue: QualityIssue) {
        viewModelScope.launch {
            repository.dismissIssue(issue.id)
            _userMessage.emit("Issue dismissed from audit")
        }
    }

    fun restoreAllDismissed() {
        viewModelScope.launch {
            repository.clearAllDismissed()
            _userMessage.emit("Dismissed issues restored")
        }
    }

    fun applyCategorize(transactionId: Long, categoryName: String) {
        viewModelScope.launch {
            val success = repository.categorizeTransaction(transactionId, categoryName)
            if (success) {
                _userMessage.emit("Categorised as $categoryName")
            }
            closeQuickFix()
        }
    }

    fun applyDeleteDuplicate(transactionId: Long) {
        viewModelScope.launch {
            val success = repository.deleteTransaction(transactionId)
            if (success) {
                _userMessage.emit("Duplicate transaction deleted")
            }
            closeQuickFix()
        }
    }

    fun applyAdjustDate(transactionId: Long, newTimestamp: Long) {
        viewModelScope.launch {
            val success = repository.adjustTransactionDate(transactionId, newTimestamp)
            if (success) {
                _userMessage.emit("Transaction date updated")
            }
            closeQuickFix()
        }
    }
}
