package com.varsel.expensetracker.parser

import javax.inject.Inject
import javax.inject.Singleton

data class RegisteredBankParser(
    val bankId: String,
    val displayName: String,
    val parser: StatementParser
)

@Singleton
open class BankParserRegistry(
    private val parsersProvider: () -> List<RegisteredBankParser>
) {
    @Inject
    constructor(
        indianBankParser: IndianBankParser,
        iciciBankParser: IciciBankParser,
        hdfcBankParser: HdfcBankParser
    ) : this({
        listOf(
            RegisteredBankParser(
                bankId = "indian_bank",
                displayName = "Indian Bank",
                parser = indianBankParser
            ),
            RegisteredBankParser(
                bankId = "icici_bank",
                displayName = "ICICI Bank",
                parser = iciciBankParser
            ),
            RegisteredBankParser(
                bankId = "hdfc_bank",
                displayName = "HDFC Bank",
                parser = hdfcBankParser
            )
        )
    })

    open fun all(): List<RegisteredBankParser> = parsersProvider()
}
