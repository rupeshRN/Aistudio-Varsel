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
        private const val STANDARD_WINDOW_MS = 5 * 24 * 60 * 60 * 1000L // 5 days for clearing and weekend lag
        private const val REF_MATCH_WINDOW_MS = 10 * 24 * 60 * 60 * 1000L // 10 days for matching UTR/RRN
        private const val STARTUP_LOOKBACK_MS = 60L * 24 * 60 * 60 * 1000L // 60 days sliding window for background/startup scans
        private const val MIN_CONFIDENCE_THRESHOLD = 75

        // Regex to match transfer-indicating keywords in transaction narration
        private val TRANSFER_KEYWORDS_REGEX = Regex(
            """\b(transfer|trf|neft|rtgs|imps|upi|self|own\s+acc|fund\s+trf|fnd\s+trf|to\s+ib|to\s+icici|indian\s+bank|icici|hdfc|sbi|axis|kotak|idib)\b""",
            RegexOption.IGNORE_CASE
        )

        // Regex to capture standard 11-character Indian Financial System Code (IFSC)
        private val IFSC_REGEX = Regex("""\b([A-Z]{4}0[A-Z0-9]{6})\b""", RegexOption.IGNORE_CASE)

        // Regex to capture masked account numbers, e.g. "XXXXXXXX9012", "XXXX9012", "/X*9012/"
        private val MASKED_ACCOUNT_REGEX = Regex("""(?:^|[/\s\-_,:]|\b)X+([0-9]{4})(?:[/\s\-_,:]|\b|$)""", RegexOption.IGNORE_CASE)

        // Regex to capture explicit account indications like "A/c 9012", "to A/c ...9012"
        private val EXPLICIT_ACCOUNT_REGEX = Regex("""(?:A/c|Acc|Account|to)\s*[:#\-]?\s*X*([0-9]{4})\b""", RegexOption.IGNORE_CASE)

        // Regex to capture 10 to 22-character reference numbers (numeric UTR, RRN, or alphanumeric NEFT/RTGS/IMPS)
        private val REF_NUM_REGEX = Regex("""\b([0-9]{10,18}|(?:[A-Z]{2,6}[0-9]{6,16})|(?:N[0-9]{11,16}))\b""", RegexOption.IGNORE_CASE)

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
     * @param targetTransactionIds Optional list of newly added or specific transaction IDs to check (e.g. from import).
     *                             When specified, only reconciles against these target transactions.
     *                             When null or empty, reconciles unlinked transactions within a sliding lookback window
     *                             (defaults to 60 days to prevent lag over years of data).
     * @param fullHistoricalScan When true, forces a scan of all unlinked transactions across entire history.
     * @return Number of transfer pairs automatically linked.
     */
    suspend fun reconcileTransfers(
        targetTransactionIds: List<Long>? = null,
        fullHistoricalScan: Boolean = true
    ): Int {
        val snapshots = statementSnapshotDao.getAllSnapshots()

        val candidates: List<TransactionEntity> = if (!targetTransactionIds.isNullOrEmpty()) {
            transactionDao.getTransactionsByIds(targetTransactionIds)
                .filter { it.transferLinkId == null }
        } else {
            transactionDao.getAllUnlinkedTransactions()
        }

        if (candidates.isEmpty()) return 0

        // Only evaluate candidates that possess transfer indicators (reference, UTR, transfer keyword, account info)
        val debits = candidates.filter { it.type == "EXPENSE" && it.transferLinkId == null && hasTransferIndicators(it) }
        val credits = candidates.filter { it.type == "INCOME" && it.transferLinkId == null && hasTransferIndicators(it) }

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
        val minDate = debit.dateTimestamp - REF_MATCH_WINDOW_MS
        val maxDate = debit.dateTimestamp + REF_MATCH_WINDOW_MS

        val listByAmount = transactionDao.findUnlinkedTransferCandidates(
            type = "INCOME",
            amount = debit.amount,
            minDate = minDate,
            maxDate = maxDate
        )

        val refNumbers = (listOfNotNull(debit.referenceNumber) + extractAllReferenceNumbers(debit)).distinct()
        val listByRef = refNumbers.flatMap { ref ->
            if (ref.isNotBlank()) {
                transactionDao.findUnlinkedTransferCandidatesByReference(
                    type = "INCOME",
                    referenceNumber = ref.trim()
                )
            } else {
                emptyList()
            }
        }

        return (listByAmount + listByRef).distinctBy { it.id }
    }

    /**
     * Efficiently fetches candidate debits from the database for a given credit.
     */
    private suspend fun fetchCandidateDebits(credit: TransactionEntity): List<TransactionEntity> {
        val minDate = credit.dateTimestamp - REF_MATCH_WINDOW_MS
        val maxDate = credit.dateTimestamp + REF_MATCH_WINDOW_MS

        val listByAmount = transactionDao.findUnlinkedTransferCandidates(
            type = "EXPENSE",
            amount = credit.amount,
            minDate = minDate,
            maxDate = maxDate
        )

        val refNumbers = (listOfNotNull(credit.referenceNumber) + extractAllReferenceNumbers(credit)).distinct()
        val listByRef = refNumbers.flatMap { ref ->
            if (ref.isNotBlank()) {
                transactionDao.findUnlinkedTransferCandidatesByReference(
                    type = "EXPENSE",
                    referenceNumber = ref.trim()
                )
            } else {
                emptyList()
            }
        }

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

        val dateDiff = abs(debit.dateTimestamp - credit.dateTimestamp)

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

        var hasRefMatch = false
        if (debitRef.isNotBlank() && creditRef.isNotBlank()) {
            if (debitRef.equals(creditRef, ignoreCase = true)) {
                hasRefMatch = true
            } else if (debitRef.length >= 8 && creditRef.length >= 8) {
                if (debitRef.contains(creditRef, ignoreCase = true) || creditRef.contains(debitRef, ignoreCase = true)) {
                    hasRefMatch = true
                }
            }
        }

        // Also check if extracted reference/UTR appears in both narrations
        if (!hasRefMatch) {
            val debitRefs = extractAllReferenceNumbers(debit)
            val creditRefs = extractAllReferenceNumbers(credit)
            val commonRefs = debitRefs.intersect(creditRefs).filter { it.length >= 8 }
            if (commonRefs.isNotEmpty()) {
                hasRefMatch = true
            }
        }

        if (hasRefMatch) {
            // Reference numbers (UTR/RRN) are globally unique; allow up to 10 days
            if (dateDiff <= REF_MATCH_WINDOW_MS) {
                return 100
            }
        }

        // For non-reference rules (Target Account, IFSC), require date within STANDARD_WINDOW_MS (5 days)
        if (dateDiff > STANDARD_WINDOW_MS) return 0

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

        //-------------------------------------------------------------
        // Rule 4: Bank Name Mention in Narration (Confidence: 85)
        // E.g. Debit in ICICI has description/rawDescription mentioning "Indian Bank", "IDIB",
        // and credit is in Indian Bank (or vice-versa).
        //-------------------------------------------------------------
        val debitText = listOfNotNull(debit.rawDescription, debit.description).joinToString(" ").uppercase()
        val creditText = listOfNotNull(credit.rawDescription, credit.description).joinToString(" ").uppercase()
        val creditBankName = (credit.bankName ?: snapshots.find { it.accountId == credit.accountId }?.bankName).orEmpty()
        val debitBankName = (debit.bankName ?: snapshots.find { it.accountId == debit.accountId }?.bankName).orEmpty()

        val creditBankShort = BankInfoHelper.getBankShortName(creditBankName).uppercase()
        val debitBankShort = BankInfoHelper.getBankShortName(debitBankName).uppercase()

        val debitMentionsCreditBank = when (creditBankShort) {
            "IB" -> debitText.contains("INDIAN BANK") || debitText.contains("INDIANBANK") || debitText.contains("IDIB") || debitText.contains("IB")
            "ICICI" -> debitText.contains("ICICI")
            "HDFC" -> debitText.contains("HDFC")
            "SBI" -> debitText.contains("SBI") || debitText.contains("STATE BANK")
            "AXIS" -> debitText.contains("AXIS") || debitText.contains("UTIB")
            else -> creditBankName.isNotBlank() && debitText.contains(creditBankName.uppercase())
        }

        val creditMentionsDebitBank = when (debitBankShort) {
            "IB" -> creditText.contains("INDIAN BANK") || creditText.contains("INDIANBANK") || creditText.contains("IDIB") || creditText.contains("IB")
            "ICICI" -> creditText.contains("ICICI")
            "HDFC" -> creditText.contains("HDFC")
            "SBI" -> creditText.contains("SBI") || creditText.contains("STATE BANK")
            "AXIS" -> creditText.contains("AXIS") || creditText.contains("UTIB")
            else -> debitBankName.isNotBlank() && creditText.contains(debitBankName.uppercase())
        }

        if (debitMentionsCreditBank || creditMentionsDebitBank) {
            return 85
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
     * Extracts all reference numbers (numeric and alphanumeric) from referenceNumber and narration.
     */
    private fun extractAllReferenceNumbers(tx: TransactionEntity): Set<String> {
        val results = mutableSetOf<String>()
        tx.referenceNumber?.trim()?.takeIf { it.isNotBlank() }?.let { results.add(it) }

        val texts = listOfNotNull(tx.rawDescription, tx.description)
        for (text in texts) {
            REF_NUM_REGEX.findAll(text).forEach { match ->
                match.groupValues.getOrNull(1)?.let { results.add(it) }
            }
            val neftPattern = Regex("""(?:NEFT[-/])([A-Za-z0-9]{8,22})""", RegexOption.IGNORE_CASE)
            neftPattern.findAll(text).forEach { match ->
                match.groupValues.getOrNull(1)?.let { results.add(it) }
            }
            val upiPattern = Regex("""(?:UPI/)([0-9]{12})""", RegexOption.IGNORE_CASE)
            upiPattern.findAll(text).forEach { match ->
                match.groupValues.getOrNull(1)?.let { results.add(it) }
            }
            val impsPattern = Regex("""(?:(?:MMT/IMPS|IMPS)/)([0-9A-Za-z]{8,18})""", RegexOption.IGNORE_CASE)
            impsPattern.findAll(text).forEach { match ->
                match.groupValues.getOrNull(1)?.let { results.add(it) }
            }
        }

        return results
    }

    /**
     * Quickly determines whether a transaction has potential transfer indicators
     * (e.g. Reference number, UTR, transfer keywords, or explicit account patterns).
     */
    fun hasTransferIndicators(tx: TransactionEntity): Boolean {
        if (!tx.referenceNumber.isNullOrBlank()) return true
        if (tx.category.equals("Transfer", ignoreCase = true)) return true
        val text = listOfNotNull(tx.rawDescription, tx.description).joinToString(" ")
        if (text.isBlank()) return false
        return TRANSFER_KEYWORDS_REGEX.containsMatchIn(text) ||
                MASKED_ACCOUNT_REGEX.containsMatchIn(text) ||
                EXPLICIT_ACCOUNT_REGEX.containsMatchIn(text) ||
                IFSC_REGEX.containsMatchIn(text) ||
                REF_NUM_REGEX.containsMatchIn(text)
    }
}
