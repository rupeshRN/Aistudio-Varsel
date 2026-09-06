package com.varsel.expensetracker.data.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val BUDGET_PREFERENCES = "budget_display_preferences"

val Context.budgetDataStore by preferencesDataStore(name = BUDGET_PREFERENCES)

data class BudgetDisplayConfig(
    val hiddenBudgetIds: Set<Long> = emptySet(),
    val orderedBudgetIds: List<Long> = emptyList()
)

@Singleton
class BudgetDisplayRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val HIDDEN_BUDGETS = stringPreferencesKey("hidden_budgets")
        val ORDERED_BUDGETS = stringPreferencesKey("ordered_budgets")
    }

    val config: Flow<BudgetDisplayConfig> = context.budgetDataStore.data.map { prefs ->
        val hiddenStr = prefs[Keys.HIDDEN_BUDGETS] ?: ""
        val hiddenSet = if (hiddenStr.isBlank()) emptySet() else {
            hiddenStr.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
        }

        val orderStr = prefs[Keys.ORDERED_BUDGETS] ?: ""
        val orderList = if (orderStr.isBlank()) emptyList() else {
            orderStr.split(",").mapNotNull { it.trim().toLongOrNull() }
        }

        BudgetDisplayConfig(hiddenBudgetIds = hiddenSet, orderedBudgetIds = orderList)
    }

    suspend fun toggleHideBudget(budgetId: Long) {
        context.budgetDataStore.edit { prefs ->
            val hiddenStr = prefs[Keys.HIDDEN_BUDGETS] ?: ""
            val current = if (hiddenStr.isBlank()) mutableSetOf() else {
                hiddenStr.split(",").mapNotNull { it.trim().toLongOrNull() }.toMutableSet()
            }
            if (current.contains(budgetId)) {
                current.remove(budgetId)
            } else {
                current.add(budgetId)
            }
            prefs[Keys.HIDDEN_BUDGETS] = current.joinToString(",")
        }
    }

    suspend fun saveOrder(order: List<Long>) {
        context.budgetDataStore.edit { prefs ->
            prefs[Keys.ORDERED_BUDGETS] = order.joinToString(",")
        }
    }
}
