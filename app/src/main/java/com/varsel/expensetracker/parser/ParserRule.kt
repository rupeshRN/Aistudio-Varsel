package com.varsel.expensetracker.parser

enum class RuleSeverity {
    INFO,
    WARNING,
    ERROR
}

data class ParserRule(

    val code: String,

    val message: String,

    val severity: RuleSeverity
)
