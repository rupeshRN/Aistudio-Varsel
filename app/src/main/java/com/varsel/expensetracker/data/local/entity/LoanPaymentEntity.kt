package com.varsel.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "loan_payments",
    indices = [
        Index("loanId"),
        Index("linkedTransactionId")
    ]
)
data class LoanPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val loanId: Long,
    val paymentDateTimestamp: Long,
    val amount: Double,
    val principalComponent: Double,
    val interestComponent: Double,
    val paymentType: String = "REGULAR_EMI",
    val linkedTransactionId: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
