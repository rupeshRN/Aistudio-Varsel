package com.varsel.expensetracker.util

import com.varsel.expensetracker.category.CustomRuleEngine
import com.varsel.expensetracker.data.repository.CustomRuleRepository
import com.varsel.expensetracker.developer.ParserDiagnosticsCollector
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.parser.BankDetector
import com.varsel.expensetracker.parser.HdfcBankParser
import com.varsel.expensetracker.parser.IciciBankParser
import com.varsel.expensetracker.parser.IndianBankParser
import com.varsel.expensetracker.parser.ReconciliationEngine
import com.varsel.expensetracker.parser.StatementImportResult
import com.varsel.expensetracker.parser.StatementSummary
import com.varsel.expensetracker.parser.StatementSummaryExtractor
import com.varsel.expensetracker.parser.TextNormalizer
import javax.inject.Inject
import com.varsel.expensetracker.parser.TransactionFingerprintGenerator
import com.varsel.expensetracker.parser.AccountDetailsExtractor
import com.varsel.expensetracker.parser.AccountIdentityGenerator


/**
 * Central orchestration engine for importing bank statements.
 *
 * This class coordinates the complete import pipeline but intentionally
 * contains very little business logic itself.
 *
 * Pipeline
 * ----------------------------------------------------
 *
 * Raw PDF / OCR Text
 *          │
 *          ▼
 * TextNormalizer
 *          │
 *          ▼
 * Load Learning Cache
 *          │
 *          ▼
 * BankDetector
 *          │
 *          ▼
 * Bank-specific Parser
 *          │
 *          ▼
 * Apply Learning Engine
 *          │
 *          ▼
 * Statement Summary
 *          │
 *          ▼
 * Reconciliation
 *          │
 *          ▼
 * Import Preview
 *
 * Responsibilities
 * ----------------------------------------------------
 * ✓ Load user-learned knowledge once.
 * ✓ Normalize statement text.
 * ✓ Detect the correct bank parser.
 * ✓ Parse transactions.
 * ✓ Apply learned descriptions/categories.
 * ✓ Build statement summary.
 * ✓ Perform reconciliation.
 * ✓ Produce a single StatementImportResult.
 *
 * This class intentionally does NOT:
 * • parse bank formats itself
 * • categorize transactions
 * • maintain learning rules
 * • access Room directly
 * • update UI
 *
 * Every stage is delegated to a dedicated component.
 */
class StatementParserEngine @Inject constructor(

    /**
     * Selects the correct parser implementation
     * based on statement content.
     */
    private val bankDetector: BankDetector,

    /**
     * Cleans raw OCR/PDF text before parsing.
     */
    private val textNormalizer: TextNormalizer,

    /**
     * Extracts opening/closing balances,
     * statement dates and totals.
     */
    private val statementSummaryExtractor: StatementSummaryExtractor,

    /**
     * Verifies parser output against
     * statement summary values.
     */
    private val reconciliationEngine: ReconciliationEngine,
    
    /**
     * Create SHA code for duplicate identification.
     */
    private val transactionFingerprintGenerator:
    TransactionFingerprintGenerator,

    /**
     * Loads persisted learning rules.
     */
    private val customRuleRepository: CustomRuleRepository,

    /**
     * Performs fast in-memory learned lookups.
     */
    private val customRuleEngine: CustomRuleEngine,

    private val accountDetailsExtractor: AccountDetailsExtractor,

    private val accountIdentityGenerator: AccountIdentityGenerator,

/**
 * Collects parser diagnostics during the import pipeline.
 *
 * This component isolates all developer-only diagnostic updates
 * from the production parsing logic.
 *
 * StatementParserEngine reports parsing events through this
 * collector instead of writing directly to ParserDiagnosticsManager,
 * keeping the parser focused solely on business logic.
 */
    private val diagnosticsCollector: ParserDiagnosticsCollector

) {

    /**
     * Executes the complete import pipeline.
     */
    suspend fun parseStatement(

        rawText: String

    ): StatementImportResult {

        diagnosticsCollector.reset()

        //--------------------------------------------------
        // Stage 1
        //
        // Load learned knowledge once.
        //
        // Every transaction lookup afterwards happens
        // entirely from memory.
        //--------------------------------------------------

        customRuleEngine.loadCache(

            customRuleRepository.loadRuleCache()

        )

        //--------------------------------------------------
        // Stage 2
        //
        // Normalize statement text before parsing.
        //--------------------------------------------------

        val normalizedText = textNormalizer.normalize(rawText)

        //--------------------------------------------------
        // Diagnostics
        //--------------------------------------------------

        diagnosticsCollector.recordNormalization(
            rawText,
            normalizedText
        )

        diagnosticsCollector.recordDetectedDates(
            normalizedText
        )

        //--------------------------------------------------
        // Stage 3
        //
        // Extract statement-level metadata.
        //--------------------------------------------------

        val defaultSummary =
            statementSummaryExtractor.extract(
                rawText
            )

        //--------------------------------------------------
        // Stage 4
        //
        // Detect bank and execute the correct parser.
        //--------------------------------------------------

        val parser =
            bankDetector.detect(rawText)

        val parsedTransactions =
            parser.parse(normalizedText).ifEmpty {
                parser.parse(rawText)
            }

        val accountNumber =
            accountDetailsExtractor.extractAccountNumber(
                rawText
            )
        
        val accountIdentity =
            accountNumber?.let {
                accountIdentityGenerator.generate(it)
            }

        //--------------------------------------------------
        // Establish transaction identity BEFORE applying
        // learned descriptions/categories.
        //
        // IMPORTANT:
        // The fingerprint represents the original statement
        // transaction and must not change when the user or
        // learning engine changes the description/category.
        //--------------------------------------------------

        val fingerprintedTransactions =
            parsedTransactions.map { transaction ->

                transaction.copy(
                    transactionFingerprint =
                        transactionFingerprintGenerator.generate(
                            transaction
                        ),

                    accountId =
                        accountIdentity?.accountId,

                    accountLast4 =
                        accountIdentity?.accountLast4
                )
            }

        //--------------------------------------------------
        // Stage 5
        //
        // Apply user-learned description/category.
        //--------------------------------------------------

        val transactions =
            applyLearning(fingerprintedTransactions)

        //--------------------------------------------------
        // Diagnostics
        //--------------------------------------------------

        diagnosticsCollector.recordTransactions(
            transactionCount = fingerprintedTransactions.size,
            lastTimestamp =
                fingerprintedTransactions
                    .maxByOrNull {
                        it.dateTimestamp
                    }
                    ?.dateTimestamp
        )

        //--------------------------------------------------
        // Stage 6
        //
        // Verify parsed data against statement totals.
        //--------------------------------------------------

        val parserSummary = parser.extractSummary(rawText, fingerprintedTransactions)

        val computedCredits = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val computedDebits = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val computedStart = transactions.minOfOrNull { it.dateTimestamp }
        val computedEnd = transactions.maxOfOrNull { it.dateTimestamp }

        val resolvedSummary = StatementSummary(
            statementStartDate = parserSummary?.statementStartDate ?: defaultSummary.statementStartDate ?: computedStart,
            statementEndDate = parserSummary?.statementEndDate ?: defaultSummary.statementEndDate ?: computedEnd,
            totalCredits = parserSummary?.totalCredits ?: defaultSummary.totalCredits ?: if (computedCredits > 0.0) computedCredits else null,
            totalDebits = parserSummary?.totalDebits ?: defaultSummary.totalDebits ?: if (computedDebits > 0.0) computedDebits else null,
            openingBalance = parserSummary?.openingBalance ?: defaultSummary.openingBalance,
            endingBalance = parserSummary?.endingBalance ?: defaultSummary.endingBalance ?: if (defaultSummary.openingBalance != null) {
                defaultSummary.openingBalance + (defaultSummary.totalCredits ?: computedCredits) - (defaultSummary.totalDebits ?: computedDebits)
            } else null
        )

        val reconciliation =
            reconciliationEngine.reconcile(
                resolvedSummary,
                fingerprintedTransactions
            )

        //--------------------------------------------------
        // Reconciliation diagnostics
        //--------------------------------------------------

        diagnosticsCollector.recordReconciliation(
            reconciliation = reconciliation,
            statementCredits = resolvedSummary.totalCredits,
            statementDebits = resolvedSummary.totalDebits
        )

        val bankName = when (parser) {
            is IciciBankParser -> "ICICI Bank"
            is IndianBankParser -> "Indian Bank"
            is HdfcBankParser -> "HDFC Bank"
            else -> "Bank Statement"
        }

        val ifscCode = accountDetailsExtractor.extractIfscCode(rawText)

        val taggedTransactions = transactions.map { tx ->
            if (tx.bankName.isNullOrBlank()) tx.copy(bankName = bankName) else tx
        }

        //--------------------------------------------------
        // Final result returned to ImportViewModel.
        //--------------------------------------------------

        return StatementImportResult(
            summary = resolvedSummary,
            reconciliation = reconciliation,
            transactions = taggedTransactions,
            bankName = bankName,
            accountId = accountIdentity?.accountId,
            accountLast4 = accountIdentity?.accountLast4,
            ifscCode = ifscCode
        )

    }

    //--------------------------------------------------
    // Applies learned merchant knowledge.
    //
    // A learned rule can replace:
    // • Display Description
    // • Category
    //
    // Amount, date and reference remain unchanged.
    //--------------------------------------------------

    private fun applyLearning(

        transactions: List<Transaction>

    ): List<Transaction> {

        return transactions.map { transaction ->

            val knowledge =

                customRuleEngine.findKnowledge(

                    transaction.description

                )

            if (knowledge == null) {

                transaction

            } else {

                transaction.copy(

                    description =

                        if (knowledge.displayDescription.isNotBlank()) knowledge.displayDescription else transaction.description,

                    category =

                        knowledge.categoryName

                )

            }

        }

    }

}
