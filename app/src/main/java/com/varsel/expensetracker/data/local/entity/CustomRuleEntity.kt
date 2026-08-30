package com.varsel.expensetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores a learned knowledge record.
 *
 * pattern             -> Raw description extracted from the bank statement
 * displayDescription  -> User's preferred display description
 * categoryName        -> User's preferred category
 */
@Entity(
    tableName = "custom_rules",
    indices = [
        Index(
            value = ["pattern"],
            unique = true
        )
    ]
)
data class CustomRuleEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "pattern")
    val pattern: String,

    @ColumnInfo(name = "displayDescription")
    val displayDescription: String,

    @ColumnInfo(name = "categoryName")
    val categoryName: String

)
