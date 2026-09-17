package com.varsel.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.varsel.expensetracker.domain.model.recurring.RecurringFrequency
import com.varsel.expensetracker.domain.model.recurring.RecurringItem
import com.varsel.expensetracker.domain.model.recurring.RecurringType

@Entity(
    tableName = "recurring_items",
    indices = [
        Index(value = ["nextOccurrenceTimestamp"]),
        Index(value = ["isActive"]),
        Index(value = ["type"])
    ]
)
data class RecurringItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String? = null,
    val amount: Double,
    val isVariableAmount: Boolean = false,
    val type: String, // EXPENSE, INCOME, SUBSCRIPTION
    val frequency: String, // DAILY, WEEKLY, MONTHLY, QUARTERLY, SEMI_ANNUALLY, YEARLY
    val startDateTimestamp: Long,
    val nextOccurrenceTimestamp: Long,
    val endDateTimestamp: Long? = null,
    val isActive: Boolean = true,
    val accountId: String? = null,
    val accountLast4: String? = null,
    val bankName: String? = null,
    val category: String = "Other",
    val lastGeneratedTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): RecurringItem {
        return RecurringItem(
            id = id,
            title = title,
            notes = notes,
            amount = amount,
            isVariableAmount = isVariableAmount,
            type = try { RecurringType.valueOf(type) } catch (e: Exception) { RecurringType.EXPENSE },
            frequency = try { RecurringFrequency.valueOf(frequency) } catch (e: Exception) { RecurringFrequency.MONTHLY },
            startDateTimestamp = startDateTimestamp,
            nextOccurrenceTimestamp = nextOccurrenceTimestamp,
            endDateTimestamp = endDateTimestamp,
            isActive = isActive,
            accountId = accountId,
            accountLast4 = accountLast4,
            bankName = bankName,
            category = category,
            lastGeneratedTimestamp = lastGeneratedTimestamp,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(item: RecurringItem): RecurringItemEntity {
            return RecurringItemEntity(
                id = item.id,
                title = item.title,
                notes = item.notes,
                amount = item.amount,
                isVariableAmount = item.isVariableAmount,
                type = item.type.name,
                frequency = item.frequency.name,
                startDateTimestamp = item.startDateTimestamp,
                nextOccurrenceTimestamp = item.nextOccurrenceTimestamp,
                endDateTimestamp = item.endDateTimestamp,
                isActive = item.isActive,
                accountId = item.accountId,
                accountLast4 = item.accountLast4,
                bankName = item.bankName,
                category = item.category,
                lastGeneratedTimestamp = item.lastGeneratedTimestamp,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt
            )
        }
    }
}
