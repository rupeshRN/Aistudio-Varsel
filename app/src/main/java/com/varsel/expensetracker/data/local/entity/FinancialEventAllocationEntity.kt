package com.varsel.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Allocation of part or all of a transaction to a
 * Financial Event.
 *
 * A transaction may have multiple allocation rows.
 *
 * Example:
 *
 * Transaction ₹1,000
 *
 * Event A -> ₹600
 * Event B -> ₹400
 *
 * The total allocated amount may never exceed
 * the original transaction amount.
 */
@Entity(
    tableName = "financial_event_allocations",
    indices = [
        Index(
            value = [
                "transactionId"
            ]
        ),
        Index(
            value = [
                "transactionLinkId"
            ]
        ),
        Index(
            value = [
                "transactionId",
                "transactionLinkId"
            ],
            unique = true
        )
    ]
)
data class FinancialEventAllocationEntity(

    @PrimaryKey(
        autoGenerate = true
    )
    val id: Long = 0L,

    /**
     * Original transaction being allocated.
     */
    val transactionId: Long,

    /**
     * Financial Event receiving this allocation.
     *
     * This corresponds to TransactionLinkGroupEntity
     * / TransactionLinkGroup.transactionLinkId.
     */
    val transactionLinkId: String,

    /**
     * Amount of the transaction assigned to this
     * Financial Event.
     *
     * Must be:
     *
     *     > 0
     *
     * and the sum of allocations for one transaction
     * must never exceed transaction.amount.
     */
    val allocatedAmount: Double,

    /**
     * Timestamp when the allocation was created.
     */
    val createdAt: Long
)
