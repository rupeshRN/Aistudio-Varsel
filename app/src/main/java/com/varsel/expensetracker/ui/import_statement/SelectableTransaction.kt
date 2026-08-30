package com.varsel.expensetracker.ui.import_statement

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.varsel.expensetracker.domain.model.Transaction

class SelectableTransaction(

    val transaction: Transaction,

    selected: Boolean = true,

    val isDuplicate: Boolean = false

) {

    var selected by mutableStateOf(selected)

}
