package com.varsel.expensetracker.ui.mapper

import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.ui.model.TransactionUiModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class TransactionUiMapper @Inject constructor() {

    private val formatter =
        DateTimeFormatter.ofPattern(
            "dd MMM",
            Locale.ENGLISH
        )

    fun map(
        transaction: Transaction
    ): TransactionUiModel {

        val dateText = Instant
            .ofEpochMilli(transaction.dateTimestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)

        return TransactionUiModel(

            id = transaction.id,

            title = transaction.description,

            subtitle = null,

            category = transaction.category,

            amountText = "₹%,.2f".format(transaction.amount),

            dateText = dateText,

            isIncome =
                transaction.type ==
                        TransactionType.INCOME
        )
    }
}
