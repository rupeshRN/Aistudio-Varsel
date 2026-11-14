package com.varsel.expensetracker.domain.model

data class TransactionLinkGroup(

    /**
     * Matches Transaction.transactionLinkId.
     */
    val transactionLinkId: String,

    /**
     * User-facing event name.
     */
    val groupName: String,

    /**
     * Report category.
     */
    val category: String,

    /**
     * Creation timestamp.
     */
    val createdAt: Long,

    /**
     * Manual override marking the event settled.
     */
    val isManuallySettled: Boolean = false,

    /**
     * Optional expected reimbursement target from roommates/others.
     */
    val expectedReimbursement: Double? = null
)
