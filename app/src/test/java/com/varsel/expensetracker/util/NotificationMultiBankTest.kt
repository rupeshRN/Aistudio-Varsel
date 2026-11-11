package com.varsel.expensetracker.util

import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import com.varsel.expensetracker.util.notification.NotificationReminderManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationMultiBankTest {

    @Test
    fun `test multi-bank statement status when no snapshots exist`() {
        val startOfMonthTimestamp = 1700000000000L
        val status = NotificationReminderManager.evaluateMultiBankStatus(
            snapshots = emptyList(),
            startOfMonthTimestamp = startOfMonthTimestamp
        )

        assertEquals(0, status.totalTrackedAccounts)
        assertFalse(status.hasAnyUploadedThisMonth)
        assertTrue(status.pendingBankNames.isEmpty())
    }

    @Test
    fun `test multi-bank status when some banks uploaded this month and one is pending`() {
        val startOfMonthTimestamp = 1700000000000L
        val uploadedThisMonth = startOfMonthTimestamp + 5000000L
        val uploadedPreviousMonth = startOfMonthTimestamp - 10000000L

        val hdfcSnapshot = StatementSnapshotEntity(
            id = 1L,
            accountId = "acc_hdfc",
            accountLast4 = "4321",
            bankName = "HDFC Bank",
            importedAt = uploadedThisMonth // Uploaded this month
        )

        val sbiSnapshot = StatementSnapshotEntity(
            id = 2L,
            accountId = "acc_sbi",
            accountLast4 = "7890",
            bankName = "State Bank of India",
            importedAt = uploadedPreviousMonth // Only previous month -> pending this month
        )

        val status = NotificationReminderManager.evaluateMultiBankStatus(
            snapshots = listOf(hdfcSnapshot, sbiSnapshot),
            startOfMonthTimestamp = startOfMonthTimestamp
        )

        assertEquals(2, status.totalTrackedAccounts)
        assertTrue(status.hasAnyUploadedThisMonth)
        assertEquals(1, status.pendingBankNames.size)
        assertEquals("State Bank of India (••7890)", status.pendingBankNames.first())
    }

    @Test
    fun `test multi-bank status when none uploaded this month`() {
        val startOfMonthTimestamp = 1700000000000L
        val uploadedPreviousMonth = startOfMonthTimestamp - 10000000L

        val hdfcSnapshot = StatementSnapshotEntity(
            id = 1L,
            accountId = "acc_hdfc",
            accountLast4 = "4321",
            bankName = "HDFC Bank",
            importedAt = uploadedPreviousMonth
        )

        val sbiSnapshot = StatementSnapshotEntity(
            id = 2L,
            accountId = "acc_sbi",
            accountLast4 = "7890",
            bankName = "State Bank of India",
            importedAt = uploadedPreviousMonth
        )

        val status = NotificationReminderManager.evaluateMultiBankStatus(
            snapshots = listOf(hdfcSnapshot, sbiSnapshot),
            startOfMonthTimestamp = startOfMonthTimestamp
        )

        assertEquals(2, status.totalTrackedAccounts)
        assertFalse(status.hasAnyUploadedThisMonth)
        assertEquals(2, status.pendingBankNames.size)
    }

    @Test
    fun `test multi-bank status when all accounts uploaded this month`() {
        val startOfMonthTimestamp = 1700000000000L
        val uploadedThisMonth = startOfMonthTimestamp + 5000000L

        val hdfcSnapshot = StatementSnapshotEntity(
            id = 1L,
            accountId = "acc_hdfc",
            accountLast4 = "4321",
            bankName = "HDFC Bank",
            importedAt = uploadedThisMonth
        )

        val sbiSnapshot = StatementSnapshotEntity(
            id = 2L,
            accountId = "acc_sbi",
            accountLast4 = "7890",
            bankName = "State Bank of India",
            importedAt = uploadedThisMonth
        )

        val status = NotificationReminderManager.evaluateMultiBankStatus(
            snapshots = listOf(hdfcSnapshot, sbiSnapshot),
            startOfMonthTimestamp = startOfMonthTimestamp
        )

        assertEquals(2, status.totalTrackedAccounts)
        assertTrue(status.hasAnyUploadedThisMonth)
        assertTrue(status.pendingBankNames.isEmpty())
    }
}
