package com.varsel.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.varsel.expensetracker.data.local.entity.CustomRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRuleDao {

    @Query("SELECT * FROM custom_rules")
    fun getAllRules(): Flow<List<CustomRuleEntity>>

    @Query("""
SELECT *
FROM custom_rules
WHERE pattern = :pattern
LIMIT 1
""")
suspend fun findRuleByPattern(
    pattern: String
): CustomRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomRule(rule: CustomRuleEntity)

    @Delete
    suspend fun deleteRule(rule: CustomRuleEntity)
}
