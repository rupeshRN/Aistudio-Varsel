package com.varsel.expensetracker.parser

import javax.inject.Inject

class ParserRuleEngine @Inject constructor() {

    fun evaluate(
        fields: TransactionFields
    ): RuleReport {

        val rules =
            mutableListOf<ParserRule>()

        //-----------------------------------------
        // Merchant Rules
        //-----------------------------------------

        val merchant =
            fields.merchant

        if (merchant.isNullOrBlank()) {

            rules += ParserRule(
                "MERCHANT_MISSING",
                "Merchant not detected.",
                RuleSeverity.WARNING
            )

        } else {

            if (merchant.contains("@")) {

                rules += ParserRule(
                    "MERCHANT_LOOKS_UPI",
                    "Merchant resembles a UPI handle.",
                    RuleSeverity.WARNING
                )
            }

            if (merchant.any { it.isDigit() }) {

                rules += ParserRule(
                    "MERCHANT_HAS_DIGITS",
                    "Merchant contains numeric characters.",
                    RuleSeverity.INFO
                )
            }

            if (merchant.length < 3) {

                rules += ParserRule(
                    "MERCHANT_TOO_SHORT",
                    "Merchant name is unusually short.",
                    RuleSeverity.INFO
                )
            }
        }

        //-----------------------------------------
        // Purpose Rules
        //-----------------------------------------

        val purpose =
            fields.purpose

        if (purpose.isNullOrBlank()) {

            rules += ParserRule(
                "PURPOSE_MISSING",
                "Purpose not detected.",
                RuleSeverity.WARNING
            )

        } else {

            if (purpose.any(Char::isDigit)) {

                rules += ParserRule(
                    "PURPOSE_HAS_DIGITS",
                    "Purpose contains numbers.",
                    RuleSeverity.INFO
                )
            }

            if (purpose.contains("@")) {

                rules += ParserRule(
                    "PURPOSE_LOOKS_UPI",
                    "Purpose resembles a UPI handle.",
                    RuleSeverity.WARNING
                )
            }

            val words =
                purpose.split("\\s+".toRegex())

            if (words.size >= 2 &&
                words.first().length in 3..12 &&
                words.first().all(Char::isLetter)
            ) {

                rules += ParserRule(
                    "PURPOSE_STARTS_WITH_NAME",
                    "Purpose may start with a person's name.",
                    RuleSeverity.INFO
                )
            }
        }

        //-----------------------------------------
        // UPI
        //-----------------------------------------

        if (fields.upiId == null) {

            rules += ParserRule(
                "UPI_NOT_FOUND",
                "No UPI ID detected.",
                RuleSeverity.INFO
            )
        }

        //-----------------------------------------
        // IFSC
        //-----------------------------------------

        if (fields.ifsc == null) {

            rules += ParserRule(
                "IFSC_NOT_FOUND",
                "No IFSC detected.",
                RuleSeverity.INFO
            )
        }

        //-----------------------------------------
        // Reference
        //-----------------------------------------

        if (fields.reference == null) {

            rules += ParserRule(
                "REFERENCE_NOT_FOUND",
                "Reference number missing.",
                RuleSeverity.INFO
            )
        }

        return RuleReport(rules)
    }
}
