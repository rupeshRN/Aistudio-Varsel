package com.varsel.expensetracker.parser

import javax.inject.Inject

class FieldInterpreter @Inject constructor() {

    private val ifscRegex =
        Regex("^[A-Z]{4}0[A-Z0-9]{6}$")

    private val maskedAccountRegex =
        Regex("^X{3,}\\d*$", RegexOption.IGNORE_CASE)

    private val upiRegex =
        Regex(".+@.+", RegexOption.IGNORE_CASE)

    private val referenceRegex =
        Regex("^(?:\\d{10,18}|(?=[A-Za-z0-9]*\\d)[A-Za-z0-9]{10,22})$")

    private val paymentChannels = setOf(
        "UPI",
        "IMPS",
        "NEFT",
        "RTGS",
        "ACH",
        "ECS",
        "ATM",
        "POS",
        "CARD"
    )

    private val genericWords = setOf(
        "PAY",
        "TO",
        "PAYMENT",
        "TRANSFER",
        "UPI",
        "IMPS",
        "NEFT",
        "RTGS",
        "ACH",
        "ECS"
    )

    fun interpret(
        fields: List<String>
    ): TransactionFields {

        var ifsc: String? = null
        var account: String? = null
        var upiId: String? = null
        var reference: String? = null
        var channel: String? = null

        val remaining = mutableListOf<String>()

        for (raw in fields) {

            val field = raw.trim()

            if (field.isBlank())
                continue

            when {

                ifsc == null &&
                        ifscRegex.matches(field) -> {

                    ifsc = field
                }

                account == null &&
                        maskedAccountRegex.matches(field) -> {

                    account = field
                }

                upiId == null &&
                        upiRegex.matches(field) -> {

                    upiId = field
                }

                reference == null &&
                        referenceRegex.matches(field) -> {

                    reference = field
                }

                field.uppercase() in paymentChannels -> {

                    channel = field.uppercase()
                }

                isNoise(field) -> {
                    // Ignore OCR garbage
                }

                else -> {

                    remaining.add(field)
                }
            }
        }

        //--------------------------------------------------------
        // Merchant
        //--------------------------------------------------------

        val merchant = remaining.firstOrNull {

            val upper = it.uppercase()

            upper !in genericWords &&
                    !isNoise(it)
        }

        //--------------------------------------------------------
        // Purpose
        //--------------------------------------------------------

      var purpose: String? = null

for (i in remaining.indices.reversed()) {

    val value = remaining[i]

    if (isNoise(value))
        continue

    if (isStatementMetadata(value))
        continue

    if (!value.any { ch -> ch.isLetter() })
        continue

    purpose = value

    break

}

        return TransactionFields(
            ifsc = ifsc,
            account = account,
            upiId = upiId,
            reference = reference,
            channel = channel,
            merchant = merchant,
            purpose = purpose,
            unknown = remaining
        )
    }

    //--------------------------------------------------------
    // OCR Noise Detector
    //--------------------------------------------------------

    private fun isNoise(
        value: String
    ): Boolean {

        val text = value.trim()

        if (text.isBlank())
            return true

        // Small numeric fragments
        if (text.matches(Regex("^\\d{1,7}$")))
            return true

        // Only punctuation
        if (text.matches(Regex("^[^A-Za-z0-9]+$")))
            return true

        // XXXXXX
        if (text.matches(Regex("^X+$", RegexOption.IGNORE_CASE)))
            return true

        return false
    }

    private fun isStatementMetadata(

    value: String

): Boolean {

    val text = value.uppercase().trim()

    val metadata = listOf(

        "BRANCH",
        "ATM SERVICE BRANCH",
        "ACCOUNT",
        "ACCOUNT NO",
        "ACCOUNT NUMBER",
        "CUSTOMER ID",
        "IFSC",
        "MICR",
        "AVAILABLE BALANCE",
        "CLOSING BALANCE",
        "OPENING BALANCE",
        "STATEMENT",
        "PAGE"

    )

    return metadata.any {

        text.contains(it)

    }

}
}
