package com.varsel.expensetracker.parser

import java.security.MessageDigest
import javax.inject.Inject

data class AccountIdentity(
    val accountId: String,
    val accountLast4: String
)

class AccountIdentityGenerator @Inject constructor() {

    fun generate(
        accountNumber: String
    ): AccountIdentity {

        val normalizedAccountNumber =
            accountNumber
                .filter { it.isLetterOrDigit() }

        require(normalizedAccountNumber.isNotBlank()) {
            "Account number is empty."
        }

        val last4 =
            normalizedAccountNumber
                .takeLast(4)

        return AccountIdentity(
            accountId = sha256(
                normalizedAccountNumber
            ),
            accountLast4 = last4
        )
    }

    private fun sha256(
        value: String
    ): String {

        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )

        val hash =
            digest.digest(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )

        return hash.joinToString("") {
            "%02x".format(it)
        }
    }
}
