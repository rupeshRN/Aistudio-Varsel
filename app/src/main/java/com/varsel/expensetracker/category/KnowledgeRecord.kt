package com.varsel.expensetracker.category

/**
 * Represents a single piece of learned knowledge produced by Varsel.
 *
 * A KnowledgeRecord is created when the user manually corrects a transaction
 * and chooses to save those corrections.
 *
 * The record is later used during future imports to automatically:
 *
 * 1. Replace the parser-generated display description.
 * 2. Restore the user's preferred category.
 *
 * Example:
 *
 * Parser output:
 *     "NETFLIX AUTOPAY"
 *
 * User edits:
 *     Description -> "Netflix"
 *     Category    -> "Entertainment"
 *
 * Stored knowledge:
 *     Pattern              -> "netflix autopay"
 *     Display Description  -> "Netflix"
 *     Category             -> "Entertainment"
 *
 * Future imports matching the same normalized pattern will automatically
 * reuse this learned knowledge before the transaction is shown to the user.
 */
data class KnowledgeRecord(

    /**
     * User-friendly description that replaces the parser-generated text.
     *
     * Example:
     * "Netflix"
     */
    val displayDescription: String,

    /**
     * User-selected category remembered for future imports.
     *
     * Example:
     * "Entertainment"
     */
    val categoryName: String,

    /**
     * Target transaction type compatibility: "INCOME", "EXPENSE", or "BOTH".
     */
    val targetType: String = "BOTH"

)
