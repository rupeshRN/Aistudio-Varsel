package com.varsel.expensetracker.ui.mapper

import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.ui.model.TransactionUiModel
import com.varsel.expensetracker.util.BankInfoHelper
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
            amountText = "₹%,.2f".format(transaction.amount),
            dateText = dateText,
            isIncome = transaction.type == TransactionType.INCOME,
            accountInfoText = accountInfoText,
            isTransfer = isTransfer,
            isEventLinked = isEventLinked
        )
    }
}
