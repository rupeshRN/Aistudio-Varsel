package com.varsel.expensetracker.data.repository

import com.varsel.expensetracker.category.KnowledgeRecord
import com.varsel.expensetracker.data.local.dao.CustomRuleDao
import com.varsel.expensetracker.data.local.entity.CustomRuleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomRuleRepository @Inject constructor(

    private val customRuleDao: CustomRuleDao

) {

    //--------------------------------------------------
    // Load all learned rules into memory
    //--------------------------------------------------

    suspend fun loadRuleCache(): Map<String, KnowledgeRecord> {

        return getAllRules()
            .first()
            .associate { rule ->

                rule.pattern.lowercase() to

                    KnowledgeRecord(

                        displayDescription =
                            rule.displayDescription,

                        categoryName =
                            rule.categoryName

                    )

            }

    }

    //--------------------------------------------------
    // Observe all rules
    //--------------------------------------------------

    fun getAllRules(): Flow<List<CustomRuleEntity>> {

        return customRuleDao.getAllRules()

    }

    //--------------------------------------------------
    // Lookup
    //--------------------------------------------------

    suspend fun findRule(

        pattern: String

    ): CustomRuleEntity? {

        return customRuleDao.findRuleByPattern(pattern)

    }

    //--------------------------------------------------
    // Save / Replace
    //--------------------------------------------------

    suspend fun saveRule(

        pattern: String,

        displayDescription: String,

        categoryName: String

    ) {

        customRuleDao.insertCustomRule(

            CustomRuleEntity(

                pattern = pattern,

                displayDescription = displayDescription,

                categoryName = categoryName

            )

        )

    }

    //--------------------------------------------------
    // Delete
    //--------------------------------------------------

    suspend fun deleteRule(

        rule: CustomRuleEntity

    ) {

        customRuleDao.deleteRule(rule)

    }

}
