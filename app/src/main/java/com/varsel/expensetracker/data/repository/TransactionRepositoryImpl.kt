package com.varsel.expensetracker.data.repository

import com.varsel.expensetracker.data.local.dao.TransactionDao
import com.varsel.expensetracker.data.local.entity.TransactionEntity
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionRole
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.domain.repository.TransferLinkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

class TransactionRepositoryImpl @Inject constructor(

    private val transactionDao:
        TransactionDao,

    private val financialEventAllocationRepository:
        FinancialEventAllocationRepository,

    private val descriptionNormalizer:
        com.varsel.expensetracker.category.DescriptionNormalizer

) : TransactionRepository {

    //--------------------------------------------------
    // Observe all transactions
    //--------------------------------------------------

    override fun getAllTransactions():
        Flow<List<Transaction>> {

        return transactionDao
            .getAllTransactions()
            .map { entities ->

                entities.map {
                    it.toDomain()
                }
            }
    }

    //--------------------------------------------------
    // Insert multiple transactions
    //--------------------------------------------------

    override suspend fun insertTransactions(
        transactions: List<Transaction>
    ) {

        if (transactions.isEmpty()) {
            return
        }

        transactionDao
            .insertTransactions(
                transactions.map {
                    it.toEntity()
                }
            )
    }

    //--------------------------------------------------
    // Insert single transaction
    //--------------------------------------------------

    override suspend fun insertTransaction(
        transaction: Transaction
    ) {

        transactionDao
            .insertTransaction(
                transaction.toEntity()
            )
    }

    //--------------------------------------------------
    // Update transaction
    //--------------------------------------------------

    override suspend fun updateTransaction(
        transaction: Transaction
    ) {

        transactionDao
            .updateTransaction(
                transaction.toEntity()
            )
    }

    //--------------------------------------------------
    // Delete transaction
    //--------------------------------------------------

    override suspend fun deleteTransaction(
        transaction: Transaction
    ) {

        /*
         * Remove Financial Event allocations first.
         *
         * The transaction itself is then deleted.
         */
        financialEventAllocationRepository
            .deleteAllocationsForTransaction(
                transaction.id
            )

        transactionDao
            .deleteTransaction(
                transaction.toEntity()
            )
    }

    //--------------------------------------------------
    // Get transaction by ID
    //--------------------------------------------------

    override suspend fun getTransactionById(
        id: Long
    ): Transaction? {

        return transactionDao
            .getTransactionById(id)
            ?.toDomain()
    }

    //--------------------------------------------------
    // Existing fingerprints
    //--------------------------------------------------

    override suspend fun findExistingFingerprints(
        fingerprints: List<String>
    ): Set<String> {

        if (fingerprints.isEmpty()) {
            return emptySet()
        }

        return transactionDao
            .findExistingFingerprints(
                fingerprints
            )
            .toSet()
    }

    //--------------------------------------------------
    // Financial Event linking
    //--------------------------------------------------
    //
    // IMPORTANT:
    //
    // The legacy transactionLinkId is intentionally
    // still maintained for backward compatibility.
    //
    // In addition, every transaction linked through
    // this method now receives a Financial Event
    // allocation equal to the full transaction amount.
    //
    // This means existing UI behaviour remains:
    //
    //     transaction -> one Financial Event
    //
    // while the database now also supports:
    //
    //     transaction -> multiple allocations
    //
    //--------------------------------------------------

    override suspend fun linkTransactions(

        transactionIds:
            List<Long>,

        transactionLinkId:
            String

    ) {

        if (transactionIds.isEmpty()) {
            return
        }

        /*
         * Process each transaction individually.
         *
         * We intentionally use the existing repository
         * operation rather than changing the existing UI.
         */
        transactionIds
            .distinct()
            .forEach { transactionId ->

                val transaction =
                    transactionDao
                        .getTransactionById(
                            transactionId
                        )
                        ?.toDomain()
                        ?: return@forEach

                /*
                 * Existing UI only presents completely
                 * unlinked transactions.
                 *
                 * Preserve that rule here as well.
                 *
                 * This prevents accidentally replacing
                 * an existing Financial Event relationship.
                 */
                if (
                    transaction.transactionLinkId !=
                        null
                ) {
                    return@forEach
                }

                /*
                 * Full allocation for the existing
                 * one-to-one Financial Event behaviour.
                 *
                 * Example:
                 *
                 * Transaction = ₹1,000
                 *
                 * Existing Financial Event UI:
                 *     Event A = ₹1,000
                 *
                 * New allocation model:
                 *     Event A = ₹1,000
                 */
                financialEventAllocationRepository
                    .insertAllocation(
                        transactionId =
                            transaction.id,

                        transactionLinkId =
                            transactionLinkId,

                        allocatedAmount =
                            abs(
                                transaction.amount
                            )
                    )

                /*
                 * Keep the legacy relationship.
                 *
                 * This is still required by the current
                 * Financial Event UI and other existing
                 * code paths.
                 */
                transactionDao
                    .linkTransactions(
                        transactionIds =
                            listOf(
                                transaction.id
                            ),

                        transactionLinkId =
                            transactionLinkId
                    )
            }
    }

    //--------------------------------------------------
    // Financial Event unlink
    //--------------------------------------------------

    override suspend fun unlinkTransaction(
        transactionId: Long
    ) {

        /*
         * Current UI semantics are:
         *
         *     remove transaction from Financial Event
         *
         * Because the current UI has one legacy
         * transactionLinkId, unlinking currently means
         * removing all Financial Event allocations for
         * this transaction.
         *
         * This is safe for the current one-to-one UI.
         *
         * When the multi-event allocation editor is
         * introduced, this method will be replaced by
         * an allocation-specific unlink operation.
         */
        financialEventAllocationRepository
            .deleteAllocationsForTransaction(
                transactionId
            )

        transactionDao
            .unlinkTransaction(
                transactionId
            )
    }

    //--------------------------------------------------
    // Transfer linking
    //--------------------------------------------------
    //
    // A transfer is valid only when:
    //
    //     TRANSFER_OUT
    //          +
    //     TRANSFER_IN
    //
    // and both amounts are exactly equal.
    //
    //--------------------------------------------------

    override suspend fun linkTransfer(

        transferOutTransactionId:
            Long,

        transferInTransactionId:
            Long

    ): TransferLinkResult {

        //--------------------------------------------------
        // Same transaction cannot be both sides.
        //--------------------------------------------------

        if (
            transferOutTransactionId ==
                transferInTransactionId
        ) {
            return TransferLinkResult
                .InvalidTransactionPair
        }

        //--------------------------------------------------
        // Load both transactions.
        //--------------------------------------------------

        val transferOut =
            transactionDao
                .getTransactionById(
                    transferOutTransactionId
                )
                ?.toDomain()

        val transferIn =
            transactionDao
                .getTransactionById(
                    transferInTransactionId
                )
                ?.toDomain()

        //--------------------------------------------------
        // Transaction existence validation.
        //--------------------------------------------------

        if (
            transferOut == null ||
            transferIn == null
        ) {

            return TransferLinkResult
                .TransactionNotFound
        }

        //--------------------------------------------------
        // Validate transaction types / roles.
        //--------------------------------------------------

        if (
            transferOut.role !=
                TransactionRole.TRANSFER_OUT ||

            transferIn.role !=
                TransactionRole.TRANSFER_IN
        ) {

            return TransferLinkResult
                .InvalidTransactionPair
        }

        //--------------------------------------------------
        // Validate income / expense types.
        //--------------------------------------------------

        if (
            transferOut.type !=
                TransactionType.EXPENSE ||

            transferIn.type !=
                TransactionType.INCOME
        ) {

            return TransferLinkResult
                .InvalidTransactionPair
        }

        //--------------------------------------------------
        // Exact amount validation.
        //--------------------------------------------------

        if (
            transferOut.amount !=
                transferIn.amount
        ) {

            return TransferLinkResult
                .AmountMismatch(

                    transferOutAmount =
                        transferOut.amount,

                    transferInAmount =
                        transferIn.amount
                )
        }

        //--------------------------------------------------
        // Existing transfer validation.
        //--------------------------------------------------

        if (
            transferOut.transferLinkId !=
                null ||

            transferIn.transferLinkId !=
                null
        ) {

            return TransferLinkResult
                .AlreadyLinked
        }

        //--------------------------------------------------
        // Create shared transfer ID.
        //--------------------------------------------------

        val transferLinkId =
            UUID.randomUUID()
                .toString()

        //--------------------------------------------------
        // Persist transfer only after validation.
        //--------------------------------------------------

        transactionDao
            .linkTransferTransactions(

                transferOutTransactionId =
                    transferOutTransactionId,

                transferInTransactionId =
                    transferInTransactionId,

                transferLinkId =
                    transferLinkId
            )

        return TransferLinkResult.Success
    }

    //--------------------------------------------------
    // Transfer unlink
    //--------------------------------------------------

    override suspend fun unlinkTransfer(
        transactionId: Long
    ) {

        transactionDao
            .unlinkTransfer(
                transactionId
            )
    }

    //--------------------------------------------------
    // Get paired transfer
    //--------------------------------------------------

    override suspend fun getLinkedTransferTransactions(
        transferLinkId: String
    ): List<Transaction> {
        return emptyList()
    }

    override suspend fun updateTransactions(transactions: List<Transaction>) {
        if (transactions.isEmpty()) return
        transactionDao.updateTransactions(transactions.map { it.toEntity() })
    }

    override suspend fun findSimilarTransactions(
        excludeId: Long,
        pattern: String,
        isIncome: Boolean,
        sinceTimestamp: Long
    ): List<Transaction> {
        val cleanPattern = pattern.trim().lowercase()
        val normalizedPattern = descriptionNormalizer.normalize(pattern).trim()

        val noiseWords = setOf("upi", "pos", "inb", "neft", "rtgs", "ach", "trf", "dr", "cr", "xx", "xxx", "the", "and", "for", "to")
        val patternTokens = normalizedPattern.split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length >= 3 && !noiseWords.contains(it) }

        val candidates = transactionDao.getTransactionsSince(excludeId, sinceTimestamp)

        return candidates.filter { entity ->
            val entityIsIncome = entity.type.equals("INCOME", ignoreCase = true) || entity.type.equals("CREDIT", ignoreCase = true)
            if (entityIsIncome != isIncome) return@filter false

            val entityDesc = entity.description.trim().lowercase()
            val entityNorm = descriptionNormalizer.normalize(entity.description).trim()

            when {
                // Exact description match
                entityDesc == cleanPattern -> true
                // Exact normalized description match
                normalizedPattern.isNotBlank() && entityNorm == normalizedPattern -> true
                // Substring containment on normalized forms
                normalizedPattern.length >= 4 && (entityNorm.contains(normalizedPattern) || normalizedPattern.contains(entityNorm)) -> true
                // Substring containment on raw descriptions
                cleanPattern.length >= 4 && (entityDesc.contains(cleanPattern) || cleanPattern.contains(entityDesc)) -> true
                // Matching token overlap (e.g. key merchant name present in both)
                patternTokens.isNotEmpty() && patternTokens.any { token ->
                    entityNorm.contains(token) || entityDesc.contains(token)
                } -> true
                else -> false
            }
        }.map { it.toDomain() }
    }
}

//======================================================
// Entity -> Domain
//======================================================

fun TransactionEntity.toDomain():
    Transaction {

    return Transaction(

        id =
            id,

        amount =
            amount,

        type =
            if (
                type == "INCOME"
            ) {

                TransactionType.INCOME

            } else {

                TransactionType.EXPENSE
            },

        description =
            description,

        category =
            category,

        dateTimestamp =
            dateTimestamp,

        referenceNumber =
            referenceNumber,

        transactionFingerprint =
            transactionFingerprint,

        accountId =
            accountId,

        accountLast4 =
            accountLast4,

        transactionLinkId =
            transactionLinkId,

        transferLinkId =
            transferLinkId,

        role =
            try {

                TransactionRole.valueOf(
                    role
                )

            } catch (
                e:
                    IllegalArgumentException
            ) {

                TransactionRole.NORMAL
            },

        bankName = bankName,
        rawDescription = rawDescription
    )
}

//======================================================
// Domain -> Entity
//======================================================

fun Transaction.toEntity():
    TransactionEntity {

    return TransactionEntity(

        id =
            id,

        amount =
            amount,

        type =
            if (
                type ==
                    TransactionType.INCOME
            ) {

                "INCOME"

            } else {

                "EXPENSE"
            },

        description =
            description,

        category =
            category,

        dateTimestamp =
            dateTimestamp,

        referenceNumber =
            referenceNumber,

        transactionFingerprint =
            transactionFingerprint,

        accountId =
            accountId,

        accountLast4 =
            accountLast4,

        transactionLinkId =
            transactionLinkId,

        transferLinkId =
            transferLinkId,

        role =
            role.name,

        bankName =
            bankName,

        rawDescription =
            rawDescription
    )
}
