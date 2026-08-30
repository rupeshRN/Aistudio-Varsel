package com.varsel.expensetracker.developer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeveloperViewModel @Inject constructor(

    private val repository: DeveloperRepository

) : ViewModel() {

    val parserDiagnosticsEnabled =
        repository.parserDiagnosticsEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

    fun setParserDiagnostics(
        enabled: Boolean
    ) {

        viewModelScope.launch {

            repository.setParserDiagnostics(enabled)

        }
    }
}
