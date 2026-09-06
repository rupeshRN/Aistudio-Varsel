package com.varsel.expensetracker.data.preference

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val GENERAL_SETTINGS_DATASTORE = "general_settings_prefs"

val Context.generalDataStore by preferencesDataStore(
    name = GENERAL_SETTINGS_DATASTORE
)

enum class BiometricTimeout(val label: String, val timeoutMillis: Long) {
    OFF("Off", -1L),
    IMMEDIATELY("Immediately", 0L),
    AFTER_1_MIN("After 1 Minute", 60_000L),
    AFTER_5_MIN("After 5 Minutes", 300_000L),
    AFTER_15_MIN("After 15 Minutes", 900_000L),
    AFTER_30_MIN("After 30 Minutes", 1_800_000L);

    companion object {
        fun fromName(name: String?): BiometricTimeout {
            return entries.firstOrNull { it.name == name } ?: AFTER_5_MIN
        }
    }
}

enum class HomeSection(val id: String, val displayName: String, val description: String) {
    BANNER("BANNER", "Homepage Banner", "Greeting and financial status banner"),
    NET_WORTH("NET_WORTH", "Net Worth", "Overall net worth and balance card"),
    QUICK_ACTIONS("QUICK_ACTIONS", "Quick Actions", "Import, manual entry and analytics shortcuts"),
    TRANSACTIONS("TRANSACTIONS", "Transactions List", "Recent transaction history with quick view"),
    INSIGHTS("INSIGHTS", "Actionable Insights", "Smart spending flags and advisory tips"),
    LOANS("LOANS", "Loans & Liabilities", "Active loans and upcoming EMI schedule"),
    BUDGETS("BUDGETS", "Budgets", "Monthly category and total spending caps progress"),
    INCOME_EXPENSE("INCOME_EXPENSE", "Income & Expenses", "Comparative cashflow summary"),
    TRENDS_GRAPH("TRENDS_GRAPH", "Line Graph", "Monthly spending trends visualization"),
    ACCOUNTS_LIST("ACCOUNTS_LIST", "Accounts List", "Overview of cash, bank and card accounts"),
    GOALS("GOALS", "Goals", "Savings targets and milestones tracker");

    companion object {
        val DEFAULT_ACTIVE = listOf(
            BANNER.id,
            NET_WORTH.id,
            QUICK_ACTIONS.id,
            INSIGHTS.id,
            LOANS.id,
            TRANSACTIONS.id
        )

        val ALL_SECTIONS = entries.map { it.id }

        fun findById(id: String): HomeSection? {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
        }
    }
}

data class GeneralConfig(
    val biometricTimeout: BiometricTimeout = BiometricTimeout.AFTER_5_MIN,
    val lastActiveTimestamp: Long = 0L,
    val activeHomeSections: List<String> = HomeSection.DEFAULT_ACTIVE,
    val navigationTabs: List<String> = listOf("home", "transactions", "reports", "more"),
    val floatingNavBar: Boolean = false,
    val showNavLabels: Boolean = true,
    val netWorthWidgetPeriod: String = "All Time",
    val incomeExpenseWidgetPeriod: String = "This Month",
    val budgetWidgetCategory: String = "Food"
)

object GeneralPreferenceKeys {
    val BIOMETRIC_TIMEOUT = stringPreferencesKey("biometric_timeout")
    val LAST_ACTIVE_TIMESTAMP = longPreferencesKey("last_active_timestamp")
    val ACTIVE_HOME_SECTIONS = stringPreferencesKey("active_home_sections")
    val NAVIGATION_TABS = stringPreferencesKey("navigation_tabs")
    val FLOATING_NAV_BAR = booleanPreferencesKey("floating_nav_bar")
    val SHOW_NAV_LABELS = booleanPreferencesKey("show_nav_labels")
    val NET_WORTH_WIDGET_PERIOD = stringPreferencesKey("net_worth_widget_period")
    val INCOME_EXPENSE_WIDGET_PERIOD = stringPreferencesKey("income_expense_widget_period")
    val BUDGET_WIDGET_CATEGORY = stringPreferencesKey("budget_widget_category")
}

@Singleton
class GeneralPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val generalConfig: Flow<GeneralConfig> = context.generalDataStore.data.map { prefs ->
        val timeoutStr = prefs[GeneralPreferenceKeys.BIOMETRIC_TIMEOUT] ?: BiometricTimeout.AFTER_5_MIN.name
        val timeout = BiometricTimeout.fromName(timeoutStr)

        val lastActive = prefs[GeneralPreferenceKeys.LAST_ACTIVE_TIMESTAMP] ?: 0L

        val sectionsRaw = prefs[GeneralPreferenceKeys.ACTIVE_HOME_SECTIONS]
        val activeSections = if (!sectionsRaw.isNullOrBlank()) {
            sectionsRaw.split(",").filter { it.isNotBlank() }
        } else {
            HomeSection.DEFAULT_ACTIVE
        }

        val navTabsRaw = prefs[GeneralPreferenceKeys.NAVIGATION_TABS]
        val navTabs = if (!navTabsRaw.isNullOrBlank()) {
            navTabsRaw.split(",").filter { it.isNotBlank() }.take(4)
        } else {
            listOf("home", "transactions", "reports", "more")
        }

        val floatingBar = prefs[GeneralPreferenceKeys.FLOATING_NAV_BAR] ?: false
        val showLabels = prefs[GeneralPreferenceKeys.SHOW_NAV_LABELS] ?: true
        val nwPeriod = prefs[GeneralPreferenceKeys.NET_WORTH_WIDGET_PERIOD] ?: "All Time"
        val iePeriod = prefs[GeneralPreferenceKeys.INCOME_EXPENSE_WIDGET_PERIOD] ?: "This Month"
        val budgetCat = prefs[GeneralPreferenceKeys.BUDGET_WIDGET_CATEGORY] ?: "Food"

        GeneralConfig(
            biometricTimeout = timeout,
            lastActiveTimestamp = lastActive,
            activeHomeSections = activeSections,
            navigationTabs = if (navTabs.size == 4) navTabs else listOf("home", "transactions", "reports", "more"),
            floatingNavBar = floatingBar,
            showNavLabels = showLabels,
            netWorthWidgetPeriod = nwPeriod,
            incomeExpenseWidgetPeriod = iePeriod,
            budgetWidgetCategory = budgetCat
        )
    }

    suspend fun setBiometricTimeout(timeout: BiometricTimeout) {
        context.generalDataStore.edit { prefs ->
            prefs[GeneralPreferenceKeys.BIOMETRIC_TIMEOUT] = timeout.name
        }
    }

    suspend fun updateLastActiveTimestamp(timestamp: Long = System.currentTimeMillis()) {
        context.generalDataStore.edit { prefs ->
            prefs[GeneralPreferenceKeys.LAST_ACTIVE_TIMESTAMP] = timestamp
        }
    }

    suspend fun setActiveHomeSections(sections: List<String>) {
        context.generalDataStore.edit { prefs ->
            prefs[GeneralPreferenceKeys.ACTIVE_HOME_SECTIONS] = sections.joinToString(",")
        }
    }

    suspend fun setNavigationTabs(tabs: List<String>) {
        // Enforce exactly 4 items
        val validTabs = tabs.distinct().take(4)
        context.generalDataStore.edit { prefs ->
            prefs[GeneralPreferenceKeys.NAVIGATION_TABS] = validTabs.joinToString(",")
        }
    }

    suspend fun setFloatingNavBar(enabled: Boolean) {
        context.generalDataStore.edit { prefs ->
            prefs[GeneralPreferenceKeys.FLOATING_NAV_BAR] = enabled
        }
    }

    suspend fun setShowNavLabels(enabled: Boolean) {
        context.generalDataStore.edit { prefs ->
            prefs[GeneralPreferenceKeys.SHOW_NAV_LABELS] = enabled
        }
    }

    suspend fun setNetWorthWidgetPeriod(period: String) {
        context.generalDataStore.edit { prefs ->
            prefs[GeneralPreferenceKeys.NET_WORTH_WIDGET_PERIOD] = period
        }
    }

    suspend fun setIncomeExpenseWidgetPeriod(period: String) {
        context.generalDataStore.edit { prefs ->
            prefs[GeneralPreferenceKeys.INCOME_EXPENSE_WIDGET_PERIOD] = period
        }
    }

    suspend fun setBudgetWidgetCategory(category: String) {
        context.generalDataStore.edit { prefs ->
            prefs[GeneralPreferenceKeys.BUDGET_WIDGET_CATEGORY] = category
        }
    }
}
