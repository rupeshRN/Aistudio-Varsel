package com.varsel.expensetracker.ui.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.preference.BiometricTimeout
import com.varsel.expensetracker.data.preference.GeneralConfig
import com.varsel.expensetracker.data.preference.GeneralPreferencesRepository
import com.varsel.expensetracker.data.preference.HomeSection
import com.varsel.expensetracker.security.BiometricAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val repository: GeneralPreferencesRepository,
    val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    val generalConfig: StateFlow<GeneralConfig> = repository.generalConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GeneralConfig()
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
}
