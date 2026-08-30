package com.varsel.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.varsel.expensetracker.domain.model.TransactionRole

@Entity(
    tableName = "transactions"
)
data class TransactionEntity(

    @PrimaryKey(
        autoGenerate = true
    )
    val id: Long = 0L,

    val amount: Double,

    val type: String,

    val description: String,

    val category: String,

    val dateTimestamp: Long,

    val referenceNumber: String? = null,

    val transactionFingerprint: String? = null,

    /**
     * Stable internal account identifier.
     *
     * Contains a SHA-256 hash of the full account number.
     */
    val accountId: String? = null,

    /**
     * Last four digits of the account number.
     */
    val accountLast4: String? = null,

    /**
     * Financial Event relationship.
     *
     * Used for:
     * Expense/Lent
     * Income/Reimbursement
     */
    val transactionLinkId: String? = null,

    /**
     * Transfer relationship.
     *
     * Used ONLY for:
     * Transfer Out <-> Transfer In
     *
     * This is deliberately separate from
     * transactionLinkId.
     */
    val transferLinkId: String? = null,

    /**
     * Transaction role.
     */
    val role:
        String =
            TransactionRole.NORMAL.name
)
