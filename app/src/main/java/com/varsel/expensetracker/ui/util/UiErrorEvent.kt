package com.varsel.expensetracker.ui.util

import com.varsel.expensetracker.util.AppError
import java.util.UUID

/**
 * One-time UI message/error event dispatched from ViewModels.
 * Using a unique ID prevents event duplication across recompositions and configuration changes.
 */
data class UiErrorEvent(
    val error: AppError,
    val id: String = UUID.randomUUID().toString(),
    val isDismissible: Boolean = true,
    val retryAction: (() -> Unit)? = null
)
