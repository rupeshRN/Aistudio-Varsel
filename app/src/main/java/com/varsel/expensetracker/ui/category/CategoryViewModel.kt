package com.varsel.expensetracker.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.dao.CustomRuleDao
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.data.local.entity.CustomRuleEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CategoryTab {
    EXPENSES,
    INCOME,
    RULES
}

data class CategoryUiState(
    val allCategories: List<CategoryEntity> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val customRules: List<CustomRuleEntity> = emptyList(),
    val selectedTab: CategoryTab = CategoryTab.EXPENSES,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryDao: CategoryDao,
    private val customRuleDao: CustomRuleDao
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(CategoryTab.EXPENSES)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<CategoryUiState> = combine(
        categoryDao.getAllCategories(),
        customRuleDao.getAllRules(),
        _selectedTab,
        _searchQuery
    ) { categories, rules, selectedTab, searchQuery ->
        val query = searchQuery.trim().lowercase()

        val filteredCategories = if (query.isBlank()) {
            categories
        } else {
            categories.filter {
                it.name.lowercase().contains(query) || it.keywords.lowercase().contains(query)
            }
        }

        val expenses = filteredCategories.filter {
            it.type.equals("EXPENSE", ignoreCase = true) || it.type.equals("BOTH", ignoreCase = true)
        }

        val income = filteredCategories.filter {
            it.type.equals("INCOME", ignoreCase = true) || it.type.equals("BOTH", ignoreCase = true)
        }

        val filteredRules = if (query.isBlank()) {
            rules
        } else {
            rules.filter {
                it.pattern.lowercase().contains(query) ||
                it.displayDescription.lowercase().contains(query) ||
                it.categoryName.lowercase().contains(query)
            }
        }

        CategoryUiState(
            allCategories = categories,
            expenseCategories = expenses,
            incomeCategories = income,
            customRules = filteredRules,
            selectedTab = selectedTab,
            searchQuery = searchQuery,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryUiState(isLoading = true)
    )

    fun selectTab(tab: CategoryTab) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveCategory(
        id: Long = 0L,
        name: String,
        type: String = "EXPENSE",
        colorHex: String,
        iconName: String,
        budgetLimit: Double = 0.0,
        keywords: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val category = CategoryEntity(
                id = id,
                name = name.trim(),
                type = type.uppercase().trim(),
                colorHex = colorHex.trim(),
                iconName = iconName.trim(),
                budgetLimit = budgetLimit,
                keywords = keywords.trim()
            )
            if (id == 0L) {
                categoryDao.insertCategory(category)
            } else {
                categoryDao.updateCategory(category)
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryDao.deleteCategory(category)
        }
    }

    fun saveCustomRule(
        id: Long = 0L,
        merchantPattern: String,
        displayDescription: String,
        categoryName: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val rule = CustomRuleEntity(
                id = id,
                pattern = merchantPattern.trim().uppercase(),
                displayDescription = displayDescription.trim().ifBlank { merchantPattern.trim() },
                categoryName = categoryName.trim()
            )
            if (id == 0L) {
                customRuleDao.insertCustomRule(rule)
            } else {
                customRuleDao.updateRule(rule)
            }
        }
    }

    fun deleteCustomRule(rule: CustomRuleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            customRuleDao.deleteRule(rule)
        }
    }
}

