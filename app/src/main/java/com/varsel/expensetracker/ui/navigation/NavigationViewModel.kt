package com.varsel.expensetracker.ui.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor() : ViewModel() {

    private val _currentDestination =
        MutableStateFlow<AppDestination>(
            AppDestination.Home
        )

    val currentDestination: StateFlow<AppDestination> =
        _currentDestination.asStateFlow()

    fun navigateTo(
        destination: AppDestination
    ) {

        if (_currentDestination.value == destination)
            return

        _currentDestination.value = destination
    }
}
