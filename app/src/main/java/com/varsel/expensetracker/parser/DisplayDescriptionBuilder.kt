package com.varsel.expensetracker.parser

import javax.inject.Inject

class DisplayDescriptionBuilder @Inject constructor() {

    fun build(
    fields: TransactionFields,
    fallback: String
): String {

    val merchant =
        fields.merchant
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    val purpose =
        fields.purpose
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    //----------------------------------------------------
    // Prefer a meaningful purpose
    //----------------------------------------------------

if (purpose != null) {

    val cleanedPurpose =
        purpose
            .replace(Regex("\\.+$"), "")
            .trim()

    if (!isGenericPurpose(cleanedPurpose)) {

        return cleanedPurpose
            .replaceFirstChar {

                if (it.isLowerCase())
                    it.titlecase()
                else
                    it.toString()
            }
    }
}

    //----------------------------------------------------
    // Otherwise use merchant
    //----------------------------------------------------

    if (merchant != null) {
        return merchant
    }

    //----------------------------------------------------
    // Otherwise use purpose (generic)
    //----------------------------------------------------

    if (purpose != null) {
        return purpose
    }

    //----------------------------------------------------
    // Fallback
    //----------------------------------------------------

    return fallback
    }

    //----------------------------------------------------

private fun isGenericPurpose(
    purpose: String
): Boolean {

    val text = purpose
        .lowercase()
        .trim()

    val genericPhrases = listOf(

        "upi",
        "payment",
        "pay",
        "transfer",
        "fund transfer",

        "pay for intent",
        "payment from phonepe",
        "pay to bharatpe",
        "pay to bharatpe merc",
        "gpay",
        "payment on cred",
        "paid via cred",

        "monthly autopay",
        "monthly autopay.",
        "autopay",
        "intent"

    )

    return genericPhrases.any {

        text.startsWith(it)

    }
}

    //----------------------------------------------------

    private fun looksLikePersonName(
        merchant: String
    ): Boolean {

        val words =
            merchant.split("\\s+".toRegex())

        if (words.isEmpty())
            return false

        if (words.size > 3)
            return false

        return words.all {

            it.length >= 2 &&
            it.all(Char::isLetter)
        }
    }
}
