package com.varsel.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.varsel.expensetracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    //--------------------------------------------------
    // Observe all transactions
    //--------------------------------------------------

    @Query(
        """
        SELECT *
        FROM transactions
        ORDER BY dateTimestamp DESC
        """
    )
    fun getAllTransactions():
        Flow<List<TransactionEntity>>

    //--------------------------------------------------
    // Insert transactions
    //--------------------------------------------------

    @Transaction
    @Insert(
        onConflict =
            OnConflictStrategy.REPLACE
    )
    suspend fun insertTransactions(
        transactions:
            List<TransactionEntity>
    )

    @Insert(
        onConflict =
            OnConflictStrategy.REPLACE
    )
    suspend fun insertTransaction(
        transaction:
            TransactionEntity
    )

    //--------------------------------------------------
    // Update transaction
    //--------------------------------------------------

    @Update
    suspend fun updateTransaction(
        transaction:
            TransactionEntity
    )

    //--------------------------------------------------
    // Delete transaction
    //--------------------------------------------------

    @Delete
    suspend fun deleteTransaction(
        transaction:
            TransactionEntity
    )

    //--------------------------------------------------
    // Get transaction
    //--------------------------------------------------

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE id = :id
        """
    )
    suspend fun getTransactionById(
        id: Long
    ):
        TransactionEntity?

    //--------------------------------------------------
    // Existing fingerprints
    //--------------------------------------------------

    @Query(
        """
        SELECT transactionFingerprint
        FROM transactions
        WHERE transactionFingerprint IN (:fingerprints)
        AND transactionFingerprint IS NOT NULL
        """
    )
    suspend fun findExistingFingerprints(
        fingerprints:
            List<String>
    ):
        List<String>

    //--------------------------------------------------
    // Financial Event linking
    //
    // Expense  -> LENT
    // Income   -> REIMBURSEMENT
    //--------------------------------------------------

    @Query(
        """
        UPDATE transactions
        SET
            transactionLinkId =
                :transactionLinkId,

            role =
                CASE
                    WHEN type = 'EXPENSE'
                        THEN 'LENT'

                    WHEN type = 'INCOME'
                        THEN 'REIMBURSEMENT'

                    ELSE role
                END
        WHERE id IN (:transactionIds)
        """
    )
    suspend fun linkTransactions(
        transactionIds:
            List<Long>,

        transactionLinkId:
            String
    )

    //--------------------------------------------------
    // Transfer linking
    //
    // Transfer Out:
    //     TRANSFER_OUT
    //
    // Transfer In:
    //     TRANSFER_IN
    //
    // A transfer relationship is completely separate
    // from Financial Events.
    //--------------------------------------------------

    @Query(
        """
        UPDATE transactions
        SET
            transferLinkId =
                :transferLinkId,

            transactionLinkId =
                NULL,

            role =
                CASE
                    WHEN id =
                        :transferOutTransactionId
                        THEN 'TRANSFER_OUT'

                    WHEN id =
                        :transferInTransactionId
                        THEN 'TRANSFER_IN'

                    ELSE role
                END,

            category = 'Transfer'
        WHERE id IN (
            :transferOutTransactionId,
            :transferInTransactionId
        )
        """
    )
    suspend fun linkTransferTransactions(

        transferOutTransactionId:
            Long,

        transferInTransactionId:
            Long,

        transferLinkId:
            String
    )

    //--------------------------------------------------
    // Remove Financial Event relationship
    //
    // IMPORTANT:
    //
    // This is for Financial Events only.
    // Financial Event unlinking resets the role.
    //--------------------------------------------------

    @Query(
        """
        UPDATE transactions
        SET
            transactionLinkId = NULL,
            role = 'NORMAL'
        WHERE id = :transactionId
        """
    )
    suspend fun unlinkTransaction(
        transactionId:
            Long
    )

    //--------------------------------------------------
    // Remove transfer relationship
    //
    // IMPORTANT:
    //
    // A transfer consists of TWO transactions sharing
    // the same transferLinkId.
    //
    // When either side is unlinked, BOTH sides must
    // lose the transferLinkId.
    //
    // We intentionally DO NOT change the roles.
    //
    // Example:
    //
    // Before:
    //
    // A -> TRANSFER_OUT + ABC
    // B -> TRANSFER_IN  + ABC
    //
    // After:
    //
    // A -> TRANSFER_OUT + NULL
    // B -> TRANSFER_IN  + NULL
    //
    // This allows the SAME pair to be linked again.
    //--------------------------------------------------

    @Query(
        """
        UPDATE transactions
        SET
            transferLinkId = NULL
        WHERE transferLinkId = (
            SELECT transferLinkId
            FROM transactions
            WHERE id = :transactionId
              AND transferLinkId IS NOT NULL
        )
        AND transferLinkId IS NOT NULL
        """
    )
    suspend fun unlinkTransfer(
        transactionId:
            Long
    )

    //--------------------------------------------------
    // Get linked transfer
    //
    // Returns the opposite side of the transfer.
    //--------------------------------------------------

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE transferLinkId = :transferLinkId
        AND id != :currentTransactionId
        LIMIT 1
        """
    )
    suspend fun getLinkedTransfer(

        transferLinkId:
            String,

        currentTransactionId:
            Long
    ):
        TransactionEntity?

    //--------------------------------------------------
    // Get transactions belonging to a Financial Event
    //--------------------------------------------------

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE transactionLinkId = :transactionLinkId
        ORDER BY dateTimestamp ASC
        """
    )
    suspend fun getLinkedTransactions(
        transactionLinkId:
            String
    ):
        List<TransactionEntity>

    //--------------------------------------------------
    // Get unlinked reimbursement transactions
    //
    // Kept for existing Financial Event functionality.
    //--------------------------------------------------

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE type = 'INCOME'
        AND role = 'REIMBURSEMENT'
        AND transactionLinkId IS NULL
        AND id != :currentTransactionId
        ORDER BY dateTimestamp DESC
        """
    )
    suspend fun getUnlinkedReimbursements(
        currentTransactionId:
            Long
    ):
        List<TransactionEntity>

    //--------------------------------------------------
    // Bulk update transactions
    //--------------------------------------------------

    @Update
    suspend fun updateTransactions(
        transactions: List<TransactionEntity>
    )

    //--------------------------------------------------
    // Similarity search query
    //--------------------------------------------------

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE id != :excludeId
        AND dateTimestamp >= :sinceTimestamp
        ORDER BY dateTimestamp DESC
        """
    )
    suspend fun getTransactionsSince(
        excludeId: Long,
        sinceTimestamp: Long
    ): List<TransactionEntity>

    //--------------------------------------------------
    // Transfer Candidate Queries
    //--------------------------------------------------

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE transferLinkId IS NULL
        AND type = :type
        AND amount = :amount
        AND dateTimestamp BETWEEN :minDate AND :maxDate
        ORDER BY dateTimestamp ASC
        """
    )
    suspend fun findUnlinkedTransferCandidates(
        type: String,
        amount: Double,
        minDate: Long,
        maxDate: Long
    ): List<TransactionEntity>

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE transferLinkId IS NULL
        AND type = :type
        AND referenceNumber IS NOT NULL
        AND (
            referenceNumber = :referenceNumber
            OR UPPER(TRIM(referenceNumber)) = UPPER(TRIM(:referenceNumber))
            OR (length(:referenceNumber) >= 8 AND referenceNumber LIKE '%' || :referenceNumber || '%')
            OR (length(:referenceNumber) >= 8 AND :referenceNumber LIKE '%' || referenceNumber || '%')
        )
        LIMIT 10
        """
    )
    suspend fun findUnlinkedTransferCandidatesByReference(
        type: String,
        referenceNumber: String
    ): List<TransactionEntity>

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE id IN (:ids)
        """
    )
    suspend fun getTransactionsByIds(ids: List<Long>): List<TransactionEntity>

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE transferLinkId IS NULL
        AND dateTimestamp >= :minDateTimestamp
        ORDER BY dateTimestamp DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentUnlinkedTransactionsSince(
        minDateTimestamp: Long,
        limit: Int = 1000
    ): List<TransactionEntity>

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE transferLinkId IS NULL
        ORDER BY dateTimestamp DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentUnlinkedTransactions(limit: Int = 5000): List<TransactionEntity>

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE transferLinkId IS NULL
        ORDER BY dateTimestamp DESC
        """
    )
    suspend fun getAllUnlinkedTransactions(): List<TransactionEntity>
}
