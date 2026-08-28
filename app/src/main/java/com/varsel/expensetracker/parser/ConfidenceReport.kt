package com.varsel.expensetracker.parser

data class ConfidenceReport(

    val fields: List<FieldConfidence>,

    val rules: RuleReport
) {

    val overallScore: Int
        get() {

            if (fields.isEmpty()) return 0

            return fields.sumOf {
                it.confidence
            } / fields.size
        }
}
