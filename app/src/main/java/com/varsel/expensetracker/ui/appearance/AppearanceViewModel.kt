package com.varsel.expensetracker.ui.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.preference.AccentScheme
import com.varsel.expensetracker.data.preference.AppearanceConfig
import com.varsel.expensetracker.data.preference.AppearanceRepository
import com.varsel.expensetracker.data.preference.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val repository: AppearanceRepository
) : ViewModel() {

    val appearanceConfig: StateFlow<AppearanceConfig> = repository.appearanceConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppearanceConfig()
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDynamicColor(enabled)
        }
    }

    fun setAccentScheme(scheme: AccentScheme) {
        viewModelScope.launch {
            repository.setAccentScheme(scheme)
        }
    }

    fun setAmoledDark(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAmoledDark(enabled)
        }
    }
}
