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
        transaction: Transaction,
        accountBankMap: Map<String, String> = emptyMap()
    ): TransactionUiModel {

        val bankShort = BankInfoHelper.resolveBankShortName(transaction, accountBankMap)
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
        transactions: List<Transaction>,
        extraAccountBankMap: Map<String, String> = emptyMap()
    ): List<TransactionUiModel> {
        val accountBankMap = mutableMapOf<String, String>()
        accountBankMap.putAll(extraAccountBankMap)

        transactions.forEach { tx ->
            val bank = tx.bankName?.takeIf {
                it.isNotBlank() &&
                    !it.equals("Bank Account", ignoreCase = true) &&
                    !it.equals("Bank Statement", ignoreCase = true)
            }
            if (bank != null) {
                tx.accountId?.takeIf { it.isNotBlank() }?.let { accountBankMap[it] = bank }
                tx.accountLast4?.takeIf { it.isNotBlank() }?.let { accountBankMap[it] = bank }
            }
        }

        return transactions.map { map(it, accountBankMap) }
    }

    private fun formatAmount(
        amount: Double
    ): String {

        return "₹%.2f".format(amount)
    }
}

