package com.varsel.expensetracker.domain.model.loan

enum class InterestRateType(
    val displayName: String,
    val shortName: String,
    val description: String
) {
    FIXED(
        displayName = "Fixed Rate",
        shortName = "Fixed",
        description = "Interest rate remains constant throughout the loan tenure"
    ),
    FLOATING(
        displayName = "Floating Rate (Repo-Linked)",
        shortName = "Floating",
        description = "Interest rate is tied to RBI repo rate and changes when benchmark rates are revised"
    )
}
