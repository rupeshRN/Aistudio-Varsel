package com.varsel.expensetracker.parser

import com.varsel.expensetracker.domain.model.Transaction
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject

class TransactionFingerprintGenerator @Inject constructor() {

    fun generate(transaction: Transaction): String {

        val reference = transaction.referenceNumber
            ?.trim()
            ?.uppercase(Locale.ENGLISH)

        val normalizedDescription =
            normalizeDescription(transaction.description)

        val identitySource =
            if (!reference.isNullOrBlank()) {
                buildString {
                    append("REF=")
                    append(reference)
                    append("|DATE=")
                    append(transaction.dateTimestamp)
                    append("|AMOUNT=")
                    append(formatAmount(transaction.amount))
                    append("|TYPE=")
                    append(transaction.type.name)
                }
            } else {
                buildString {
                    append("DATE=")
                    append(transaction.dateTimestamp)
                    append("|AMOUNT=")
                    append(formatAmount(transaction.amount))
                    append("|TYPE=")
                    append(transaction.type.name)
                    append("|DESC=")
                    append(normalizedDescription)
                }
            }

        return sha256(identitySource)
    }

    private fun normalizeDescription(
        description: String
    ): String {

        return description
            .trim()
            .uppercase(Locale.ENGLISH)
            .replace(Regex("\\s+"), " ")
    }

    private fun formatAmount(
        amount: Double
    ): String {

        return String.format(
            Locale.US,
            "%.2f",
            amount
        )
    }

    private fun sha256(
        value: String
    ): String {

        val digest =
            MessageDigest.getInstance("SHA-256")

        val hash =
            digest.digest(
                value.toByteArray(Charsets.UTF_8)
            )

        return hash.joinToString("") {
            "%02x".format(it)
        }
    }
}
