package com.varsel.expensetracker.parser

import javax.inject.Inject

class ParserConfidenceEngine @Inject constructor(

    private val parserRuleEngine: ParserRuleEngine

) {

    fun evaluate(
        fields: TransactionFields
    ): ConfidenceReport {

        val ruleReport =
            parserRuleEngine.evaluate(fields)

        val result = mutableListOf<FieldConfidence>()

        result += FieldConfidence(
            field = "Merchant",
            value = fields.merchant,
            confidence = merchantScore(
                fields.merchant,
                ruleReport
            )
        )

        result += FieldConfidence(
            field = "Purpose",
            value = fields.purpose,
            confidence = purposeScore(
                fields.purpose,
                ruleReport
            )
        )

        result += FieldConfidence(
            field = "UPI",
            value = fields.upiId,
            confidence =
                if (fields.upiId != null) 100 else 0
        )

        result += FieldConfidence(
            field = "Reference",
            value = fields.reference,
            confidence =
                if (fields.reference != null) 100 else 0
        )

        result += FieldConfidence(
            field = "IFSC",
            value = fields.ifsc,
            confidence =
                if (fields.ifsc != null) 100 else 0
        )

        return ConfidenceReport(
            fields = result,
            rules = ruleReport
        )
    }

    //----------------------------------------------------
    // Merchant
    //----------------------------------------------------

    private fun merchantScore(
        merchant: String?,
        rules: RuleReport
    ): Int {

        if (merchant.isNullOrBlank())
            return 0

        var score = 100

        rules.rules.forEach {

            when (it.code) {

                "MERCHANT_LOOKS_UPI" ->
                    score -= 40

                "MERCHANT_HAS_DIGITS" ->
                    score -= 15

                "MERCHANT_TOO_SHORT" ->
                    score -= 20

                "MERCHANT_MISSING" ->
                    score = 0
            }
        }

        return score.coerceIn(0, 100)
    }

    //----------------------------------------------------
    // Purpose
    //----------------------------------------------------

    private fun purposeScore(
        purpose: String?,
        rules: RuleReport
    ): Int {

        if (purpose.isNullOrBlank())
            return 0

        var score = 100

        rules.rules.forEach {

            when (it.code) {

                "PURPOSE_LOOKS_UPI" ->
                    score -= 40

                "PURPOSE_HAS_DIGITS" ->
                    score -= 20

                "PURPOSE_STARTS_WITH_NAME" ->
                    score -= 15

                "PURPOSE_MISSING" ->
                    score = 0
            }
        }

        return score.coerceIn(0, 100)
    }
}
