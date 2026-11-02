package com.varsel.expensetracker.parser

import com.varsel.expensetracker.data.local.dao.StatementSnapshotDao
import javax.inject.Inject
import javax.inject.Singleton

sealed class AccountHistoryMatchResult {
    data class Matched(
        val bankName: String,
        val accountNumber: String,
        val accountLast4: String? = null
    ) : AccountHistoryMatchResult()

    data class Unrecognized(
        val extractedAccountNumber: String?
    ) : AccountHistoryMatchResult()

    data class NoHistory(
        val extractedAccountNumber: String?
    ) : AccountHistoryMatchResult()
}

/**
 * Disambiguates unbranded or OCR-truncated statements by cross-referencing
 * the statement's account number against previously imported statement snapshots in Room.
 */
@Singleton
class AccountHistoryMatcher @Inject constructor(
    private val statementSnapshotDao: StatementSnapshotDao,
    private val accountDetailsExtractor: AccountDetailsExtractor
) {

    suspend fun matchBankByAccountHistory(rawText: String): AccountHistoryMatchResult {
        val extractedAccount = accountDetailsExtractor.extractAccountNumber(rawText)?.trim()
        val extractedLast4 = if (!extractedAccount.isNullOrBlank() && extractedAccount.length >= 4) {
            extractedAccount.takeLast(4)
        } else {
            null
        }

        val snapshots = try {
            statementSnapshotDao.getAllSnapshots()
        } catch (e: Exception) {
            emptyList()
        }

        if (snapshots.isEmpty()) {
            return AccountHistoryMatchResult.NoHistory(extractedAccount)
        }

        // 1. Check exact or containment match on accountId
        if (!extractedAccount.isNullOrBlank()) {
            val exactMatch = snapshots.firstOrNull { snapshot ->
                val savedId = snapshot.accountId?.trim()
                if (savedId.isNullOrBlank()) {
                    false
                } else {
                    savedId.equals(extractedAccount, ignoreCase = true) ||
                            extractedAccount.contains(savedId, ignoreCase = true) ||
                            savedId.contains(extractedAccount, ignoreCase = true)
                }
            }

            if (exactMatch?.bankName != null) {
                return AccountHistoryMatchResult.Matched(
                    bankName = exactMatch.bankName,
                    accountNumber = extractedAccount,
                    accountLast4 = exactMatch.accountLast4 ?: extractedLast4
                )
            }
        }

        // 2. Check match by last 4 digits if available
        if (!extractedLast4.isNullOrBlank()) {
            val last4Match = snapshots.firstOrNull { snapshot ->
                snapshot.accountLast4 == extractedLast4
            }

            if (last4Match?.bankName != null) {
                return AccountHistoryMatchResult.Matched(
                    bankName = last4Match.bankName,
                    accountNumber = extractedAccount ?: "****$extractedLast4",
                    accountLast4 = extractedLast4
                )
            }
        }

        return AccountHistoryMatchResult.Unrecognized(extractedAccount)
    }
}
