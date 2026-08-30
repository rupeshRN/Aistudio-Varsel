package com.varsel.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val budgetLimit: Double = 0.0,
    val keywords: String = "",
    val type: String = "EXPENSE" // "EXPENSE", "INCOME", "BOTH"
)
