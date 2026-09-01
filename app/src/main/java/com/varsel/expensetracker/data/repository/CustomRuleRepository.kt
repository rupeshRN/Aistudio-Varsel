package com.varsel.expensetracker.data.repository

import com.varsel.expensetracker.category.DescriptionNormalizer
import com.varsel.expensetracker.category.KnowledgeRecord
import com.varsel.expensetracker.data.local.dao.CustomRuleDao
import com.varsel.expensetracker.data.local.entity.CustomRuleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomRuleRepository @Inject constructor(
    private val customRuleDao: CustomRuleDao,
    private val categoryDao: com.varsel.expensetracker.data.local.dao.CategoryDao,
    private val descriptionNormalizer: DescriptionNormalizer
) {

    //--------------------------------------------------
    // Load all learned rules into memory
    //--------------------------------------------------

    suspend fun loadRuleCache(): Map<String, KnowledgeRecord> {
        val cache = mutableMapOf<String, KnowledgeRecord>()

        // 1. Load Auto-match keywords from Category database
        try {
            val categories = categoryDao.getAllCategoriesSnapshot()
            categories.forEach { category ->
                if (category.keywords.isNotBlank()) {
                    val keywordsList = category.keywords.split(",", ";", "\n", " ")
                        .map { it.trim().lowercase() }
                        .filter { it.isNotBlank() }

                    for (kw in keywordsList) {
                        val record = KnowledgeRecord(
                            displayDescription = "",
                            categoryName = category.name,
                            targetType = category.type
                        )
                        cache[kw] = record
                        val normalizedKw = descriptionNormalizer.normalize(kw)
                        if (normalizedKw.isNotBlank()) {
                            cache[normalizedKw] = record
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Load explicit custom merchant rename/reclassification rules (takes priority)
        try {
            val rules = getAllRules().first()
            rules.forEach { rule ->
                val record = KnowledgeRecord(
                    displayDescription = rule.displayDescription,
                    categoryName = rule.categoryName
                )

                // Cache with canonical normalized pattern for resilient matching
                val normalized = descriptionNormalizer.normalize(rule.pattern)
                if (normalized.isNotBlank()) {
                    cache[normalized] = record
                }

                // Also cache exact trimmed lowercase pattern
                val lower = rule.pattern.trim().lowercase()
                if (lower.isNotBlank()) {
                    cache[lower] = record
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return cache
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
