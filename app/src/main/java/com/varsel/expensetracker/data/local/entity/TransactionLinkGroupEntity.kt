package com.varsel.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Optional reporting metadata for a manually linked financial event.
 *
 * The transactionLinkId connects the underlying transactions.
 * This entity only adds reporting information such as a user-facing
 * group name and category.
 *
 * A group is intentionally NOT created for every transaction link.
 */
@Entity(
    tableName = "transaction_link_groups"
)
data class TransactionLinkGroupEntity(

    /**
     * Same internal ID used by TransactionEntity.transactionLinkId.
     *
     * One report group belongs to one transaction link.
     */
    @PrimaryKey
    val transactionLinkId: String,

    /**
     * User-facing name of the financial event.
     *
     * Example:
     * "Goa Trip"
     * "Household Purchase"
     * "Wedding"
     */
    val groupName: String,

    /**
     * Report category for the grouped financial event.
     *
     * Example:
     * "Travel"
     * "Household"
     * "Family"
     */
    val category: String,

    /**
     * Time when the report group was created.
     */
    val createdAt: Long
)
