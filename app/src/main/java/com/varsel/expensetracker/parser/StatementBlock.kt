package com.varsel.expensetracker.parser

data class StatementBlock(

    val rawLines: List<String>

) {

    val text: String
        get() = rawLines.joinToString("\n")

}
