package com.varsel.expensetracker.ui.model

import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.util.BankInfoHelper
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

        val bankDetected = BankInfoHelper.detectBankForTransaction(transaction)
        val bankShort = if (bankDetected.isNotBlank()) BankInfoHelper.getBankShortName(bankDetected) else "Bank"
        val accountInfoText = when {
            transaction.accountLast4 != null -> "$bankShort •••• ${transaction.accountLast4}"
            transaction.isImported -> bankShort
            else -> "Manual"
        }

        val isTransfer = transaction.isTransfer

        val isEventLinked = transaction.transactionLinkId != null

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
                        TransactionType.INCOME,
            accountInfoText = accountInfoText,
            isTransfer = isTransfer,
            isEventLinked = isEventLinked
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

