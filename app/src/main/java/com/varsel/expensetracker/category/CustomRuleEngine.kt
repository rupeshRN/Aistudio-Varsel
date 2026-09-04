package com.varsel.expensetracker.category

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central learning engine responsible for applying user-learned knowledge
 * during statement import.
 *
 * Responsibilities:
 *
 * • Maintains an in-memory cache of all learned rules.
 * • Performs fast lookups without querying Room for every transaction.
 * • Normalizes incoming descriptions before matching.
 * • Returns the learned display description and category when a match exists.
 *
 * Learning Flow
 * ----------------------------------------------------
 *
 * Room Database
 *        │
 *        ▼
 * CustomRuleRepository.loadRuleCache()
 *        │
 *        ▼
 * loadCache()
 *        │
 *        ▼
 * findKnowledge()
 *        │
 *        ├── Exact Match
 *        ├── Longest Contains Match
 *        └── No Match
 *        │
 *        ▼
 * CategoryRuleEngine
 *        │
 *        ▼
 * Import Preview
 *
 * This class intentionally contains only lookup logic.
 *
 * It never:
 * • writes to Room
 * • performs parsing
 * • decides categories itself
 * • performs UI updates
 */
@Singleton
class CustomRuleEngine @Inject constructor(

        /**
     * Canonical merchant description normalizer.
     * Ensures every lookup uses the exact same
     * normalization pipeline across the application.
     */
    private val descriptionNormalizer: DescriptionNormalizer

) {

    //--------------------------------------------------
    // In-memory learned knowledge
    //
    // Key   = normalized merchant pattern
    // Value = learned description + category
    //--------------------------------------------------

    private var cache:
        Map<String, KnowledgeRecord> = emptyMap()

    //--------------------------------------------------
    // Loads all learned rules once before statement
    // parsing begins.
    //
    // Every lookup afterwards is performed entirely
    // in memory for maximum performance.
    //--------------------------------------------------

    fun loadCache(

        rules: Map<String, KnowledgeRecord>

    ) {

        cache = rules

    }

    //--------------------------------------------------
    // Searches for previously learned knowledge.
    //
    // Matching strategy:
    //
    // 1. Exact normalized match
    // 2. Longest "contains" match
    // 3. No match
    //
    // The longest match prevents generic merchants
    // from overriding more specific learned rules.
    //--------------------------------------------------

    private fun isRecordCompatible(record: KnowledgeRecord, isIncome: Boolean?): Boolean {
        if (isIncome == null) return true
        val target = record.targetType.trim().uppercase()
        if (target.isBlank() || target == "BOTH") return true
        return if (isIncome) target == "INCOME" else target == "EXPENSE"
    }

    fun findKnowledge(
        description: String,
        isIncome: Boolean? = null
    ): KnowledgeRecord? {
        val normalized =
            descriptionNormalizer.normalize(description)

        val lower = description.trim().lowercase()

        // 1. Exact match on normalized
        if (normalized.isNotBlank()) {
            cache[normalized]?.takeIf { isRecordCompatible(it, isIncome) }?.let { return it }
        }

        // 2. Exact match on lowercase
        if (lower.isNotBlank()) {
            cache[lower]?.takeIf { isRecordCompatible(it, isIncome) }?.let { return it }
        }

        // 3. Word token matching (e.g., matching keywords inside long transaction descriptions)
        val tokens = lower.split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }
        for (token in tokens) {
            cache[token]?.takeIf { isRecordCompatible(it, isIncome) }?.let { return it }
        }

        val normTokens = normalized.split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }
        for (token in normTokens) {
            cache[token]?.takeIf { isRecordCompatible(it, isIncome) }?.let { return it }
        }

        // 4. Longest contains match
        return cache.entries
            .filter { entry ->
                val key = entry.key
                isRecordCompatible(entry.value, isIncome) &&
                key.length >= 3 && (
                    (normalized.isNotBlank() && (normalized.contains(key) || key.contains(normalized))) ||
                    (lower.isNotBlank() && lower.contains(key))
                )
            }
            .maxByOrNull {
                it.key.length
            }
            ?.value
    }

}
