package com.varsel.expensetracker.domain.model.recurring

enum class RecurringType(val displayName: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    SUBSCRIPTION("Subscription")
}

enum class RecurringFrequency(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    SEMI_ANNUALLY("Semi-Annually"),
    YEARLY("Yearly")
}

data class RecurringItem(
    val id: Long = 0,
    val title: String,
    val notes: String? = null,
    val amount: Double,
    val isVariableAmount: Boolean = false,
    val type: RecurringType = RecurringType.EXPENSE,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val startDateTimestamp: Long,
    val nextOccurrenceTimestamp: Long,
    val endDateTimestamp: Long? = null,
    val isActive: Boolean = true,
    val accountId: String? = null,
    val accountLast4: String? = null,
    val bankName: String? = null,
    val category: String = "Other",
    val lastGeneratedTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
