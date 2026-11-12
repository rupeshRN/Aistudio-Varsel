package com.varsel.expensetracker.ui.quality

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.data.repository.DataQualityRepository
import com.varsel.expensetracker.domain.model.quality.DataQualityReport
import com.varsel.expensetracker.domain.model.quality.QualityActionType
import com.varsel.expensetracker.domain.model.quality.QualityCategory
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
                issue.entityId?.let { allocId ->
                    viewModelScope.launch {
                        repository.deleteOrphanAllocation(allocId)
                        _userMessage.emit("Cleaned orphan allocation")
                    }
                }
            }
            else -> {
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

    fun restoreIssue(issueId: String) {
        viewModelScope.launch {
            repository.restoreDismissedIssue(issueId)
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
