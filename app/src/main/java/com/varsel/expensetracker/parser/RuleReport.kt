package com.varsel.expensetracker.parser

data class RuleReport(

    val rules: List<ParserRule>
) {

    fun hasErrors(): Boolean =
        rules.any {
            it.severity == RuleSeverity.ERROR
        }

    fun hasWarnings(): Boolean =
        rules.any {
            it.severity == RuleSeverity.WARNING
        }
}
