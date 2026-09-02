package com.varsel.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "statement_snapshots")
data class StatementSnapshotEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /**
     * Stable internal identifier for the bank account.
     */
    val accountId: String? = null,

    /**
     * Last four digits for safe UI display.
     */
    val accountLast4: String? = null,

    val statementStartDate: Long? = null,

    val statementEndDate: Long? = null,

    val openingBalance: Double? = null,

    val totalCredits: Double? = null,

    val totalDebits: Double? = null,

    val endingBalance: Double? = null,

    /**
     * Time when Varsel successfully processed
     * this statement.
     */
    val importedAt: Long,

    /**
     * Name of the bank associated with this statement (e.g. "Indian Bank").
     */
    val bankName: String? = null,

    /**
     * Account or branch IFSC code extracted from the statement header.
     */
    val ifscCode: String? = null
)
