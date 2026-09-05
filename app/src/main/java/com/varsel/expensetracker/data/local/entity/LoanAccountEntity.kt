package com.varsel.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loan_accounts")
data class LoanAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val loanType: String,
    val principal: Double,
    val annualInterestRate: Double,
    val emiAmount: Double,
    val totalTenureMonths: Int,
    val startDateTimestamp: Long,
    val collateralOrNotes: String? = null,
    val status: String = "ACTIVE",
    val linkedBankAccountId: String? = null,
    val bankAccountLast4: String? = null,
    val lenderName: String? = null,
    val loanAccountNumber: String? = null,
    val interestType: String = "FIXED",
    val benchmarkRate: Double? = null,
    val spreadRate: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
