package com.varsel.expensetracker.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.dao.CustomRuleDao
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.data.local.entity.CustomRuleEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val customRules: List<CustomRuleEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryDao: CategoryDao,
    private val customRuleDao: CustomRuleDao
) : ViewModel() {

    val uiState: StateFlow<CategoryUiState> = combine(
        categoryDao.getAllCategories(),
        customRuleDao.getAllRules()
    ) { categories, rules ->
        CategoryUiState(
            categories = categories,
            customRules = rules,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryUiState(isLoading = true)
    )

    fun addCategory(
        name: String,
        colorHex: String,
        iconName: String,
        budgetLimit: Double = 0.0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newCategory = CategoryEntity(
                name = name.trim(),
                colorHex = colorHex,
                iconName = iconName,
                budgetLimit = budgetLimit
            )
            categoryDao.insertCategory(newCategory)
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryDao.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryDao.deleteCategory(category)
        }
    }

fun addCustomRule(

    merchantPattern: String,

    displayDescription: String,

    categoryName: String

) {

    viewModelScope.launch(Dispatchers.IO) {

        val rule = CustomRuleEntity(

            pattern = merchantPattern.trim().uppercase(),

            displayDescription = displayDescription.trim(),

            categoryName = categoryName

        )

        customRuleDao.insertCustomRule(rule)

    }

}

    fun deleteCustomRule(rule: CustomRuleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            customRuleDao.deleteRule(rule)
        }
    }
}
