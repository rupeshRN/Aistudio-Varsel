package com.varsel.expensetracker.domain.engine

import com.varsel.expensetracker.data.local.dao.StatementSnapshotDao
import com.varsel.expensetracker.data.local.dao.TransactionDao
import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import com.varsel.expensetracker.data.local.entity.TransactionEntity
import com.varsel.expensetracker.util.BankInfoHelper
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class AutoTransferReconciliationEngine @Inject constructor(
    private val transactionDao: TransactionDao,
    private val statementSnapshotDao: StatementSnapshotDao
) {

    companion object {
        private const val TWO_DAYS_MS = 2 * 24 * 60 * 60 * 1000L
        private const val MIN_CONFIDENCE_THRESHOLD = 75

        // Regex to capture standard 11-character Indian Financial System Code (IFSC)
        private val IFSC_REGEX = Regex("""\b([A-Z]{4}0[A-Z0-9]{6})\b""", RegexOption.IGNORE_CASE)

        // Regex to capture masked account numbers, e.g. "XXXXXXXX9012", "XXXX9012", "/X*9012/"
        private val MASKED_ACCOUNT_REGEX = Regex("""(?:^|[/\s\-_,:]|\b)X+([0-9]{4})(?:[/\s\-_,:]|\b|$)""", RegexOption.IGNORE_CASE)

        // Regex to capture explicit account indications like "A/c 9012", "to A/c ...9012"
        private val EXPLICIT_ACCOUNT_REGEX = Regex("""(?:A/c|Acc|Account|to)\s*[:#\-]?\s*X*([0-9]{4})\b""", RegexOption.IGNORE_CASE)

        // Regex to capture 10 to 18-digit reference numbers (UTR, RRN, IMPS/UPI ref)
        private val REF_NUM_REGEX = Regex("""\b([0-9]{10,18})\b""")

        // Known IFSC bank code prefixes mapped to common bank names
        private val IFSC_PREFIX_MAP = mapOf(
            "IDIB" to "Indian Bank",
            "ICIC" to "ICICI Bank",
            "HDFC" to "HDFC Bank",
            "SBIN" to "State Bank of India",
            "UTIB" to "Axis Bank",
            "KKBK" to "Kotak Mahindra Bank",
            "PUNB" to "Punjab National Bank",
            "BARB" to "Bank of Baroda",
            "CNRB" to "Canara Bank",
            "UBIN" to "Union Bank of India",
            "SCBL" to "Standard Chartered",
            "IBKL" to "IDBI Bank",
            "FDRL" to "Federal Bank",
            "INDB" to "IndusInd Bank",
            "YESB" to "Yes Bank",
            "IDFB" to "IDFC FIRST Bank"
        )
    }

    /**
     * Reconciles unmatched debits and credits across user accounts.
     *
     * @param targetTransactionIds Optional list of newly added or specific transaction IDs to check.
     *                             If null or empty, checks recent unlinked transactions.
     * @return Number of transfer pairs automatically linked.
     */
    suspend fun reconcileTransfers(targetTransactionIds: List<Long>? = null): Int {
        val snapshots = statementSnapshotDao.getAllSnapshots()

        val candidates: List<TransactionEntity> = if (!targetTransactionIds.isNullOrEmpty()) {
            transactionDao.getTransactionsByIds(targetTransactionIds)
                .filter { it.transferLinkId == null }
        } else {
            transactionDao.getRecentUnlinkedTransactions(200)
        }

        if (candidates.isEmpty()) return 0

        val debits = candidates.filter { it.type == "EXPENSE" && it.transferLinkId == null }
        val credits = candidates.filter { it.type == "INCOME" && it.transferLinkId == null }

        val alreadyLinkedIds = mutableSetOf<Long>()
        var linkedCount = 0

        // Phase 1: For each debit candidate, find the best matching credit candidate
        for (debit in debits) {
            if (alreadyLinkedIds.contains(debit.id)) continue

            val potentialCredits = fetchCandidateCredits(debit)
                .filter { it.id != debit.id && it.transferLinkId == null && !alreadyLinkedIds.contains(it.id) }

            val match = findBestMatch(
                debit = debit,
                candidates = potentialCredits,
                snapshots = snapshots
            )

            if (match != null) {
                val transferLinkId = UUID.randomUUID().toString()
                transactionDao.linkTransferTransactions(
                    transferOutTransactionId = debit.id,
                    transferInTransactionId = match.id,
                    transferLinkId = transferLinkId
                )
                alreadyLinkedIds.add(debit.id)
                alreadyLinkedIds.add(match.id)
                linkedCount++
            }
        }

        // Phase 2: For any remaining credit candidates (e.g. if newly imported target was a credit)
        for (credit in credits) {
            if (alreadyLinkedIds.contains(credit.id)) continue

            val potentialDebits = fetchCandidateDebits(credit)
                .filter { it.id != credit.id && it.transferLinkId == null && !alreadyLinkedIds.contains(it.id) }

            val match = findBestDebitMatch(
                credit = credit,
                candidates = potentialDebits,
                snapshots = snapshots
            )

            if (match != null) {
                val transferLinkId = UUID.randomUUID().toString()
                transactionDao.linkTransferTransactions(
                    transferOutTransactionId = match.id,
                    transferInTransactionId = credit.id,
                    transferLinkId = transferLinkId
                )
                alreadyLinkedIds.add(match.id)
                alreadyLinkedIds.add(credit.id)
                linkedCount++
            }
        }

        return linkedCount
    }

    /**
     * Efficiently fetches candidate credits from the database for a given debit.
     */
    private suspend fun fetchCandidateCredits(debit: TransactionEntity): List<TransactionEntity> {
        val minDate = debit.dateTimestamp - TWO_DAYS_MS
        val maxDate = debit.dateTimestamp + TWO_DAYS_MS

        val listByAmount = transactionDao.findUnlinkedTransferCandidates(
            type = "INCOME",
            amount = debit.amount,
            minDate = minDate,
            maxDate = maxDate
        )

        val listByRef = debit.referenceNumber?.takeIf { it.isNotBlank() }?.let { ref ->
            transactionDao.findUnlinkedTransferCandidatesByReference(
                type = "INCOME",
                referenceNumber = ref.trim()
            )
        }.orEmpty()

        return (listByAmount + listByRef).distinctBy { it.id }
    }

    /**
     * Efficiently fetches candidate debits from the database for a given credit.
     */
    private suspend fun fetchCandidateDebits(credit: TransactionEntity): List<TransactionEntity> {
        val minDate = credit.dateTimestamp - TWO_DAYS_MS
        val maxDate = credit.dateTimestamp + TWO_DAYS_MS

        val listByAmount = transactionDao.findUnlinkedTransferCandidates(
            type = "EXPENSE",
            amount = credit.amount,
            minDate = minDate,
            maxDate = maxDate
        )

        val listByRef = credit.referenceNumber?.takeIf { it.isNotBlank() }?.let { ref ->
            transactionDao.findUnlinkedTransferCandidatesByReference(
                type = "EXPENSE",
                referenceNumber = ref.trim()
            )
        }.orEmpty()

        return (listByAmount + listByRef).distinctBy { it.id }
    }

    /**
     * Finds the credit candidate that best matches the given debit.
     */
    private fun findBestMatch(
        debit: TransactionEntity,
        candidates: List<TransactionEntity>,
        snapshots: List<StatementSnapshotEntity>
    ): TransactionEntity? {
        var bestCandidate: TransactionEntity? = null
        var highestScore = 0
        var lowestDateDiff = Long.MAX_VALUE

        for (candidate in candidates) {
            val score = scorePair(debit = debit, credit = candidate, snapshots = snapshots)
            if (score >= MIN_CONFIDENCE_THRESHOLD) {
                val dateDiff = abs(debit.dateTimestamp - candidate.dateTimestamp)
                if (score > highestScore || (score == highestScore && dateDiff < lowestDateDiff)) {
                    highestScore = score
                    lowestDateDiff = dateDiff
                    bestCandidate = candidate
                }
            }
        }

        return bestCandidate
    }

    /**
     * Finds the debit candidate that best matches the given credit.
     */
    private fun findBestDebitMatch(
        credit: TransactionEntity,
        candidates: List<TransactionEntity>,
        snapshots: List<StatementSnapshotEntity>
    ): TransactionEntity? {
        var bestCandidate: TransactionEntity? = null
        var highestScore = 0
        var lowestDateDiff = Long.MAX_VALUE

        for (candidate in candidates) {
            val score = scorePair(debit = candidate, credit = credit, snapshots = snapshots)
            if (score >= MIN_CONFIDENCE_THRESHOLD) {
                val dateDiff = abs(candidate.dateTimestamp - credit.dateTimestamp)
                if (score > highestScore || (score == highestScore && dateDiff < lowestDateDiff)) {
                    highestScore = score
                    lowestDateDiff = dateDiff
                    bestCandidate = candidate
                }
            }
        }

        return bestCandidate
    }

    /**
     * Evaluates confidence score (0 to 100) for a (debit, credit) candidate transfer pair.
     */
    fun scorePair(
        debit: TransactionEntity,
        credit: TransactionEntity,
        snapshots: List<StatementSnapshotEntity>
    ): Int {
        // Must be opposite transaction types
        if (debit.type != "EXPENSE" || credit.type != "INCOME") return 0

        // Amount must match
        if (abs(debit.amount - credit.amount) >= 0.01) return 0

        // Date must be within reasonable window (±2 days)
        val dateDiff = abs(debit.dateTimestamp - credit.dateTimestamp)
        if (dateDiff > TWO_DAYS_MS) return 0

        // Cannot be internal to the exact same account
        if (!debit.accountId.isNullOrBlank() && debit.accountId == credit.accountId) {
            return 0
        }
        if (!debit.accountLast4.isNullOrBlank() &&
            debit.accountLast4 == credit.accountLast4 &&
            debit.bankName == credit.bankName &&
            !debit.bankName.isNullOrBlank()
        ) {
            return 0
        }

        //-------------------------------------------------------------
        // Rule 1: Reference Number / UTR / RRN (Confidence: 100)
        //-------------------------------------------------------------
        val debitRef = debit.referenceNumber?.trim().orEmpty()
        val creditRef = credit.referenceNumber?.trim().orEmpty()

        if (debitRef.isNotBlank() && creditRef.isNotBlank()) {
            if (debitRef.equals(creditRef, ignoreCase = true)) {
                return 100
            }
            if (debitRef.length >= 8 && creditRef.length >= 8) {
                if (debitRef.contains(creditRef) || creditRef.contains(debitRef)) {
                    return 100
                }
            }
        }

        // Also check if extracted reference/UTR appears in both narrations
        val debitRefs = extractAllReferenceNumbers(debit)
        val creditRefs = extractAllReferenceNumbers(credit)
        val commonRefs = debitRefs.intersect(creditRefs).filter { it.length >= 10 }
        if (commonRefs.isNotEmpty()) {
            return 100
        }

        //-------------------------------------------------------------
        // Rule 2: Target Account Number Match (Confidence: 90)
        // E.g. Indian Bank narration contains receiver masked account "XXXXXXXX9012"
        // and credit transaction belongs to account with last4 "9012".
        //-------------------------------------------------------------
        val targetAccountsInDebit = extractTargetAccountLast4(debit)
        val creditLast4 = credit.accountLast4 ?: snapshots.find { it.accountId == credit.accountId }?.accountLast4

        if (!creditLast4.isNullOrBlank() && targetAccountsInDebit.contains(creditLast4)) {
            return 90
        }

        // Reverse check: credit narration mentions source account
        val targetAccountsInCredit = extractTargetAccountLast4(credit)
        val debitLast4 = debit.accountLast4 ?: snapshots.find { it.accountId == debit.accountId }?.accountLast4
        if (!debitLast4.isNullOrBlank() && targetAccountsInCredit.contains(debitLast4)) {
            return 90
        }

        //-------------------------------------------------------------
        // Rule 3: Target IFSC / Bank Match (Confidence: 80)
        // E.g. ICICI Bank narration contains destination IFSC "IDIB0001234"
        // and credit transaction belongs to Indian Bank.
        //-------------------------------------------------------------
        val ifscCodesInDebit = extractIfscCodes(debit)
        if (ifscCodesInDebit.isNotEmpty()) {
            val creditSnapshot = snapshots.find { it.accountId == credit.accountId }
            val creditIfsc = creditSnapshot?.ifscCode

            for (ifsc in ifscCodesInDebit) {
                // Exact IFSC match
                if (!creditIfsc.isNullOrBlank() && creditIfsc.equals(ifsc, ignoreCase = true)) {
                    return 85
                }

                // Bank code prefix match
                val prefix = ifsc.take(4).uppercase()
                val targetBankName = IFSC_PREFIX_MAP[prefix]
                val creditBank = credit.bankName ?: creditSnapshot?.bankName

                if (targetBankName != null && creditBank != null) {
                    if (creditBank.contains(targetBankName, ignoreCase = true) ||
                        targetBankName.contains(creditBank, ignoreCase = true) ||
                        BankInfoHelper.getBankShortName(creditBank).equals(
                            BankInfoHelper.getBankShortName(targetBankName),
                            ignoreCase = true
                        )
                    ) {
                        return 80
                    }
                }
            }
        }

        // Reverse check: Credit contains source IFSC matching debit's bank
        val ifscCodesInCredit = extractIfscCodes(credit)
        if (ifscCodesInCredit.isNotEmpty()) {
            val debitSnapshot = snapshots.find { it.accountId == debit.accountId }
            val debitIfsc = debitSnapshot?.ifscCode

            for (ifsc in ifscCodesInCredit) {
                if (!debitIfsc.isNullOrBlank() && debitIfsc.equals(ifsc, ignoreCase = true)) {
                    return 85
                }
                val prefix = ifsc.take(4).uppercase()
                val targetBankName = IFSC_PREFIX_MAP[prefix]
                val debitBank = debit.bankName ?: debitSnapshot?.bankName
                if (targetBankName != null && debitBank != null) {
                    if (debitBank.contains(targetBankName, ignoreCase = true) ||
                        targetBankName.contains(debitBank, ignoreCase = true)
                    ) {
                        return 80
                    }
                }
            }
        }

        return 0
    }

    /**
     * Extracts potential destination/source account last 4 digits from narration text.
     */
    private fun extractTargetAccountLast4(tx: TransactionEntity): Set<String> {
        val results = mutableSetOf<String>()
        val texts = listOfNotNull(tx.rawDescription, tx.description)

        for (text in texts) {
            MASKED_ACCOUNT_REGEX.findAll(text).forEach { match ->
                match.groupValues.getOrNull(1)?.let { results.add(it) }
            }
            EXPLICIT_ACCOUNT_REGEX.findAll(text).forEach { match ->
                match.groupValues.getOrNull(1)?.let { results.add(it) }
            }
        }

        return results
    }

    /**
     * Extracts all IFSC codes mentioned in transaction narration.
     */
    private fun extractIfscCodes(tx: TransactionEntity): Set<String> {
        val results = mutableSetOf<String>()
        val texts = listOfNotNull(tx.rawDescription, tx.description)

        for (text in texts) {
            IFSC_REGEX.findAll(text).forEach { match ->
                match.groupValues.getOrNull(1)?.uppercase()?.let { results.add(it) }
            }
        }

        return results
    }

    /**
     * Extracts all reference numbers (10 to 18 digits) from referenceNumber and narration.
     */
    private fun extractAllReferenceNumbers(tx: TransactionEntity): Set<String> {
        val results = mutableSetOf<String>()
        tx.referenceNumber?.trim()?.takeIf { it.isNotBlank() }?.let { results.add(it) }

        val texts = listOfNotNull(tx.rawDescription, tx.description)
        for (text in texts) {
            REF_NUM_REGEX.findAll(text).forEach { match ->
                match.groupValues.getOrNull(1)?.let { results.add(it) }
            }
        }

        return results
    }
}
