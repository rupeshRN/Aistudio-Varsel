package com.varsel.expensetracker.ui.model

import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class TransactionUiMapper @Inject constructor() {

    private val dateFormatter =
        SimpleDateFormat(
            "dd MMM yyyy",
            Locale.ENGLISH
        )

    fun map(
        transaction: Transaction
    ): TransactionUiModel {

        return TransactionUiModel(

            id = transaction.id,

            title = transaction.description,

            subtitle = null,

            category = transaction.category,

            amountText = formatAmount(
                transaction.amount
            ),

            dateText =
                dateFormatter.format(
                    Date(transaction.dateTimestamp)
                ),

            isIncome =
                transaction.type ==
                        TransactionType.INCOME
        )
    }

    fun map(
        transactions: List<Transaction>
    ): List<TransactionUiModel> {

        return transactions.map(::map)
    }

    private fun formatAmount(
        amount: Double
    ): String {

        return "₹%.2f".format(amount)
    }
}
