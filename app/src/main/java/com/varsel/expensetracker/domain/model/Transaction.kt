package com.varsel.expensetracker.domain.model

data class Transaction(

    val id: Long = 0L,

    val amount: Double,

    val type: TransactionType,

    val description: String,

    val category: String,

    val dateTimestamp: Long,

    val referenceNumber: String? = null,

    val transactionFingerprint: String? = null,

    /**
     * Stable internal identifier for the bank account.
     *
     * This is a SHA-256 hash of the full account number.
     * The actual account number is never stored here.
     */
    val accountId: String? = null,

    /**
     * Last four digits of the account number.
     *
     * Used only for safe user-facing display.
     */
    val accountLast4: String? = null,

    /**
     * Logical Financial Event link.
     *
     * Used for:
     * - Lent expenses
     * - Reimbursements
     *
     * This MUST NOT be used for account transfers.
     */
    val transactionLinkId: String? = null,

    /**
     * Logical transfer relationship.
     *
     * A transfer consists of exactly two transactions:
     *
     *     TRANSFER_OUT
     *          +
     *     TRANSFER_IN
     *
     * Both transactions share the same transferLinkId.
     *
     * This is intentionally separate from transactionLinkId
     * so transfers never become Financial Events.
     */
    val transferLinkId: String? = null,

    /**
     * Transaction classification.
     */
    val role:
        TransactionRole =
            TransactionRole.NORMAL,

    /**
     * Name of the issuing bank (e.g. "Indian Bank", "ICICI Bank").
     */
    val bankName: String? = null,

    /**
     * Original uncleaned transaction narration from statement or OCR.
     */
    val rawDescription: String? = null
) {
    val isImported: Boolean
        get() = !transactionFingerprint.isNullOrBlank() || !referenceNumber.isNullOrBlank() || !bankName.isNullOrBlank()

    val isTransfer: Boolean
        get() = role == TransactionRole.TRANSFER_IN ||
                role == TransactionRole.TRANSFER_OUT ||
                transferLinkId != null ||
                category.equals("Transfer", ignoreCase = true)
}

