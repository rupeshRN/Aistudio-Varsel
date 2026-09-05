package com.varsel.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val categoryName: String,
    val amount: Double,
    val period: String = "MONTHLY", // "MONTHLY", "WEEKLY", "YEARLY"
    val startDayOfMonth: Int = 1,
    val limitTotalType: String = "CONTRIBUTED", // "CONTRIBUTED" or "REMAINING"
    val spendingLimitType: String = "FIXED", // "FIXED" or "PERCENT"
    val budgetType: String = "EXPENSE", // "EXPENSE" or "SAVINGS"
    val colorHex: String? = null,
    val iconName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
