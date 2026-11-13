package com.varsel.expensetracker.domain.model.quality

enum class QualitySeverity {
    ERROR,
    WARNING,
    INFO
}

enum class QualityCategory(val displayName: String) {
    ALL("All Issues"),
    INTEGRITY("Ledger Integrity"),
    CATEGORIES_AND_BUDGETS("Categories & Budgets"),
    TRANSFERS_AND_ACCOUNTS("Transfers & Accounts"),
    RECURRING_AND_LOANS("Recurring & Loans"),
    SPLITS_AND_EVENTS("Events & Receivables")
}

enum class QualityActionType {
    DELETE_DUPLICATE,
    DISMISS,
    CATEGORIZE,
    ADJUST_DATE,
    LINK_TRANSFER,
    VIEW_TRANSACTION,
    RECORD_RECURRING,
    VIEW_LOAN,
    VIEW_BUDGET,
    IMPORT_STATEMENT,
    CLEAN_ORPHAN
}

enum class QualityCheckType(
    val title: String,
    val category: QualityCategory,
    val defaultSeverity: QualitySeverity,
    val penalty: Int
) {
    EXACT_DUPLICATE(
        "Exact Duplicate Entry",
        QualityCategory.INTEGRITY,
        QualitySeverity.ERROR,
        8
    ),
    POSSIBLE_DUPLICATE(
        "Possible Duplicate Entry",
        QualityCategory.INTEGRITY,
        QualitySeverity.WARNING,
        4
    ),
    ZERO_AMOUNT_TRANSACTION(
        "Zero Amount Transaction",
        QualityCategory.INTEGRITY,
        QualitySeverity.ERROR,
        5
    ),
    FUTURE_DATE_ANOMALY(
        "Future-Dated Entry",
        QualityCategory.INTEGRITY,
        QualitySeverity.WARNING,
        3
    ),
    BALANCE_INCONSISTENCY(
        "Statement Balance Inconsistency",
        QualityCategory.INTEGRITY,
        QualitySeverity.ERROR,
        8
    ),
    ROLE_TYPE_MISMATCH(
        "Role & Type Mismatch",
        QualityCategory.INTEGRITY,
        QualitySeverity.ERROR,
        6
    ),
    UNCATEGORISED_ITEM(
        "Uncategorised Transaction",
        QualityCategory.CATEGORIES_AND_BUDGETS,
        QualitySeverity.WARNING,
        2
    ),
    UNUSUAL_AMOUNT_SPIKE(
        "Unusual Spending Spike",
        QualityCategory.CATEGORIES_AND_BUDGETS,
        QualitySeverity.WARNING,
        3
    ),
    BUDGET_OVERRUN(
        "Budget Limit Exceeded",
        QualityCategory.CATEGORIES_AND_BUDGETS,
        QualitySeverity.WARNING,
        3
    ),
    UNMATCHED_TRANSFER(
        "Unmatched Transfer Leg",
        QualityCategory.TRANSFERS_AND_ACCOUNTS,
        QualitySeverity.WARNING,
        4
    ),
    STALE_ACCOUNT(
        "Stale Account Inactivity",
        QualityCategory.TRANSFERS_AND_ACCOUNTS,
        QualitySeverity.INFO,
        1
    ),
    MISSING_RECURRING_BILL(
        "Overdue Recurring Bill",
        QualityCategory.RECURRING_AND_LOANS,
        QualitySeverity.WARNING,
        4
    ),
    OVERDUE_LOAN_EMI(
        "Overdue Loan EMI",
        QualityCategory.RECURRING_AND_LOANS,
        QualitySeverity.ERROR,
        7
    ),
    UNLINKED_LOAN_PAYMENT(
        "Unlinked Loan Payment",
        QualityCategory.RECURRING_AND_LOANS,
        QualitySeverity.INFO,
        1
    ),
    OLD_RECEIVABLE(
        "Old Unsettled Receivable",
        QualityCategory.SPLITS_AND_EVENTS,
        QualitySeverity.WARNING,
        3
    ),
    OVER_ALLOCATION(
        "Allocation Exceeds Transaction",
        QualityCategory.SPLITS_AND_EVENTS,
        QualitySeverity.ERROR,
        6
    ),
    OVER_SETTLEMENT(
        "Event Over-Settlement",
        QualityCategory.SPLITS_AND_EVENTS,
        QualitySeverity.WARNING,
        4
    ),
    ORPHAN_MAPPING(
        "Orphan Ledger Mapping",
        QualityCategory.SPLITS_AND_EVENTS,
        QualitySeverity.ERROR,
        5
    )
}

data class QualityIssue(
    val id: String,
    val type: QualityCheckType,
    val category: QualityCategory,
    val severity: QualitySeverity,
    val title: String,
    val description: String,
    val details: String? = null,
    val entityId: Long? = null,
    val secondaryEntityId: Long? = null,
    val affectedAmount: Double? = null,
    val dateTimestamp: Long? = null,
    val accountName: String? = null,
    val actionType: QualityActionType = QualityActionType.DISMISS
)

data class DataQualityReport(
    val healthScore: Int,
    val totalIssues: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val issues: List<QualityIssue>,
    val passedChecksCount: Int,
    val totalChecksCount: Int = QualityCheckType.values().size,
    val dismissedCount: Int = 0
) {
    val isHealthy: Boolean get() = totalIssues == 0
}

data class QualitySummaryUiModel(
    val healthScore: Int,
    val issueCount: Int,
    val hasIssues: Boolean,
    val headline: String,
    val severity: QualitySeverity
)
