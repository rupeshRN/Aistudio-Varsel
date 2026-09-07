package com.varsel.expensetracker.ui.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.preference.BiometricTimeout
import com.varsel.expensetracker.data.preference.GeneralConfig
import com.varsel.expensetracker.data.preference.GeneralPreferencesRepository
import com.varsel.expensetracker.data.preference.HomeSection
import com.varsel.expensetracker.domain.repository.BudgetRepository
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.security.BiometricAuthManager
import com.varsel.expensetracker.ui.budget.BudgetCalculator
import com.varsel.expensetracker.ui.budget.model.BudgetUiModel
import com.varsel.expensetracker.util.BankInfoHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val repository: GeneralPreferencesRepository,
    val biometricAuthManager: BiometricAuthManager,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val statementSnapshotRepository: StatementSnapshotRepository
) : ViewModel() {

    val generalConfig: StateFlow<GeneralConfig> = repository.generalConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GeneralConfig()
        )

    val allBudgets: StateFlow<List<BudgetUiModel>> = combine(
        budgetRepository.getAllBudgets(),
        transactionRepository.getAllTransactions()
    ) { rawBudgets, transactions ->
        rawBudgets.map { budget ->
            BudgetCalculator.computeBudgetUiModel(
                budget = budget,
                transactions = transactions
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val availableAccounts: StateFlow<List<String>> = combine(
        transactionRepository.getAllTransactions(),
        repository.generalConfig
    ) { transactions, config ->
        val discovered = linkedSetOf<String>()
        // Defaults matching user's reference mockup
        discovered.add("IB")
        discovered.add("IC")
        discovered.add("IC Credit card")
        discovered.add("HD")

        // Include any currently pinned accounts
        discovered.addAll(config.pinnedAccounts)

        // Include accounts detected from transactions
        transactions.forEach { txn ->
            val detected = BankInfoHelper.detectBankForTransaction(txn)
            if (detected.isNotBlank() && detected != "Bank" && detected != "Bank Account") {
                val shortName = BankInfoHelper.getBankShortName(detected)
                if (shortName.isNotBlank()) discovered.add(shortName)
            }
        }

        // Try getting snapshots
        try {
            val snapshots = statementSnapshotRepository.getAllSnapshots()
            snapshots.forEach { s ->
                val name = s.bankName
                if (!name.isNullOrBlank() && name != "Bank Statement") {
                    discovered.add(BankInfoHelper.getBankShortName(name))
                }
            }
        } catch (_: Exception) {}

        discovered.toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("IB", "IC", "IC Credit card", "HD")
    )

    fun setBiometricTimeout(timeout: BiometricTimeout) {
        viewModelScope.launch {
            repository.setBiometricTimeout(timeout)
            if (timeout == BiometricTimeout.OFF) {
                biometricAuthManager.unlockManually()
            }
        }
    }

    fun addHomeSection(sectionId: String) {
        val current = generalConfig.value.activeHomeSections.toMutableList()
        if (!current.contains(sectionId)) {
            current.add(sectionId)
            viewModelScope.launch {
                repository.setActiveHomeSections(current)
            }
        }
    }

    fun removeHomeSection(sectionId: String) {
        val current = generalConfig.value.activeHomeSections.toMutableList()
        if (current.remove(sectionId)) {
            viewModelScope.launch {
                repository.setActiveHomeSections(current)
            }
        }
    }

    fun moveHomeSectionUp(index: Int) {
        if (index <= 0) return
        val current = generalConfig.value.activeHomeSections.toMutableList()
        if (index < current.size) {
            val item = current.removeAt(index)
            current.add(index - 1, item)
            viewModelScope.launch {
                repository.setActiveHomeSections(current)
            }
        }
    }

    fun moveHomeSectionDown(index: Int) {
        val current = generalConfig.value.activeHomeSections.toMutableList()
        if (index >= current.size - 1) return
        val item = current.removeAt(index)
        current.add(index + 1, item)
        viewModelScope.launch {
            repository.setActiveHomeSections(current)
        }
    }

    fun resetHomeSections() {
        viewModelScope.launch {
            repository.setActiveHomeSections(HomeSection.DEFAULT_ACTIVE)
        }
    }

    fun updateNavigationSlot(slotIndex: Int, newRoute: String) {
        val current = generalConfig.value.navigationTabs.toMutableList()
        if (slotIndex in 0 until 4) {
            val existingIndex = current.indexOf(newRoute)
            if (existingIndex != -1 && existingIndex != slotIndex) {
                // Swap the two slots
                val oldSlotRoute = current[slotIndex]
                current[slotIndex] = newRoute
                current[existingIndex] = oldSlotRoute
            } else {
                current[slotIndex] = newRoute
            }
            viewModelScope.launch {
                repository.setNavigationTabs(current)
            }
        }
    }

    fun setFloatingNavBar(enabled: Boolean) {
        viewModelScope.launch {
            repository.setFloatingNavBar(enabled)
        }
    }

    fun setShowNavLabels(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowNavLabels(enabled)
        }
    }

    fun setNetWorthWidgetPeriod(period: String) {
        viewModelScope.launch {
            repository.setNetWorthWidgetPeriod(period)
        }
    }

    fun setBudgetWidgetCategory(category: String) {
        viewModelScope.launch {
            repository.setBudgetWidgetCategory(category)
        }
    }

    fun setHomeBudgetsSelection(selection: String) {
        viewModelScope.launch {
            repository.setHomeBudgetsSelection(selection)
        }
    }

    fun setHomeGoalsSelection(selection: String) {
        viewModelScope.launch {
            repository.setHomeGoalsSelection(selection)
        }
    }

    fun setPrimaryAccount(account: String) {
        viewModelScope.launch {
            repository.setPrimaryAccount(account)
        }
    }

    fun setPinnedAccounts(accounts: List<String>) {
        viewModelScope.launch {
            repository.setPinnedAccounts(accounts)
        }
    }

    fun togglePinAccount(account: String) {
        val current = generalConfig.value.pinnedAccounts.toMutableList()
        if (current.contains(account)) {
            current.remove(account)
        } else {
            current.add(account)
        }
        viewModelScope.launch {
            repository.setPinnedAccounts(current)
        }
    }

    fun addCustomAccount(accountName: String) {
        val trimmed = accountName.trim()
        if (trimmed.isBlank()) return
        val current = generalConfig.value.pinnedAccounts.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            viewModelScope.launch {
                repository.setPinnedAccounts(current)
            }
        }
    }

    fun removePinnedAccount(account: String) {
        val current = generalConfig.value.pinnedAccounts.toMutableList()
        if (current.remove(account)) {
            viewModelScope.launch {
                repository.setPinnedAccounts(current)
            }
        }
    }

    fun movePinnedAccountUp(index: Int) {
        if (index <= 0) return
        val current = generalConfig.value.pinnedAccounts.toMutableList()
        if (index < current.size) {
            val item = current.removeAt(index)
            current.add(index - 1, item)
            viewModelScope.launch {
                repository.setPinnedAccounts(current)
            }
        }
    }

    fun movePinnedAccountDown(index: Int) {
        val current = generalConfig.value.pinnedAccounts.toMutableList()
        if (index >= current.size - 1) return
        val item = current.removeAt(index)
        current.add(index + 1, item)
        viewModelScope.launch {
            repository.setPinnedAccounts(current)
        }
    }

    fun setShowNetWorthBreakdown(show: Boolean) {
        viewModelScope.launch {
            repository.setShowNetWorthBreakdown(show)
        }
    }

    fun setHomeTransactionsCount(count: Int) {
        viewModelScope.launch {
            repository.setHomeTransactionsCount(count)
        }
    }

    fun setHomeTransactionsFilter(filter: String) {
        viewModelScope.launch {
            repository.setHomeTransactionsFilter(filter)
        }
    }

    fun setHomeBannerShowGreeting(show: Boolean) {
        viewModelScope.launch {
            repository.setHomeBannerShowGreeting(show)
        }
    }

    fun setHomeBannerShowStatus(show: Boolean) {
        viewModelScope.launch {
            repository.setHomeBannerShowStatus(show)
        }
    }

    fun setHomeLoansFilter(filter: String) {
        viewModelScope.launch {
            repository.setHomeLoansFilter(filter)
        }
    }
}
