package com.varsel.expensetracker.parser

import com.varsel.expensetracker.domain.model.TransactionType
import javax.inject.Inject

class AmountInterpreter @Inject constructor() {

    private val inrAmountRegex =
        Regex("INR\\s*([\\d,]+\\.\\d{2})")

    private val plainAmountRegex =
        Regex("""(?<![.\d])([0-9]{1,3}(?:,[0-9]{3})*\.\d{2}|\d+\.\d{2})(?![.\d])""")

    fun parse(firstLine: String): ParsedAmount? {

        var matches = inrAmountRegex.findAll(firstLine).toList()

        if (matches.size < 2) {
            matches = plainAmountRegex.findAll(firstLine).toList()
        }

        if (matches.isEmpty()) {
            return null
        }

        val firstAmount =
            matches[0]
                .groupValues[1]
                .replace(",", "")
                .toDoubleOrNull()
                ?: return null

        val balance =
            if (matches.size >= 2) {
                matches[1]
                    .groupValues[1]
                    .replace(",", "")
                    .toDoubleOrNull()
                    ?: 0.0
            } else {
                0.0
            }

        // If only 1 amount found, determine type by line markers
        if (matches.size == 1) {
            val upper = firstLine.uppercase()
            val isCredit = upper.contains(" CR") || upper.endsWith("CR") || upper.contains("CREDIT") || upper.contains("DEPOSIT") || upper.contains("BY ")
            return ParsedAmount(
                amount = firstAmount,
                balance = balance,
                type = if (isCredit) TransactionType.INCOME else TransactionType.EXPENSE
            )
        }

        //----------------------------------------------------
        // Determine whether first amount is Debit or Credit
        //----------------------------------------------------

        val beforeFirstAmount =
            firstLine.substring(
                0,
                matches[0].range.first
            ).trimEnd()

        val betweenAmounts =
            firstLine.substring(
                matches[0].range.last + 1,
                matches[1].range.first
            ).trim()

        //----------------------------------------------------
        // Credit marker
        //
        // IMPORTANT:
        // The dash must be a standalone token.
        //
        // This prevents descriptions such as:
        //
        // SMS_CHGS_MARCH-
        //
        // from being mistaken for a credit marker.
        //----------------------------------------------------

        val hasStandaloneCreditDash =
            Regex("(^|\\s)-\\s*$")
                .containsMatchIn(beforeFirstAmount)

        return when {

            //------------------------------------------------
            // Credit
            //
            // Example:
            // DESCRIPTION - INR 1774.00 INR 3298.59
            //------------------------------------------------

            hasStandaloneCreditDash -> {

                ParsedAmount(
                    amount = firstAmount,
                    balance = balance,
                    type = TransactionType.INCOME
                )
            }

            //------------------------------------------------
            // Debit
            //
            // Example:
            // DESCRIPTION INR 70.00 - INR 4070.10
            //------------------------------------------------

            betweenAmounts == "-" -> {

                ParsedAmount(
                    amount = firstAmount,
                    balance = balance,
                    type = TransactionType.EXPENSE
                )
            }

            // Explicit Credit indicator on the line (e.g. CR, CREDIT)
            firstLine.uppercase().contains(" CR") ||
            firstLine.uppercase().endsWith("CR") ||
            firstLine.uppercase().contains("CREDIT") -> {

                ParsedAmount(
                    amount = firstAmount,
                    balance = balance,
                    type = TransactionType.INCOME
                )
            }

            //------------------------------------------------
            // Fallback
            //------------------------------------------------

            else -> {

                ParsedAmount(
                    amount = firstAmount,
                    balance = balance,
                    type = TransactionType.EXPENSE
                )
            }
        }
    }
}
