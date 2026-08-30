package com.varsel.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.varsel.expensetracker.data.local.entity.LoanPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanPaymentDao {

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY paymentDateTimestamp DESC, id DESC")
    fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY paymentDateTimestamp ASC, id ASC")
    suspend fun getPaymentsForLoanAscSync(loanId: Long): List<LoanPaymentEntity>

    @Query("SELECT * FROM loan_payments ORDER BY paymentDateTimestamp DESC")
    fun getAllPayments(): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments WHERE linkedTransactionId = :transactionId LIMIT 1")
    suspend fun getPaymentByTransactionId(transactionId: Long): LoanPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: LoanPaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: LoanPaymentEntity)

    @Delete
    suspend fun deletePayment(payment: LoanPaymentEntity)

    @Query("DELETE FROM loan_payments WHERE loanId = :loanId")
    suspend fun deletePaymentsForLoan(loanId: Long)

    @Query("DELETE FROM loan_payments WHERE linkedTransactionId = :transactionId")
    suspend fun deletePaymentByTransactionId(transactionId: Long)

    @Query("DELETE FROM loan_payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)
}
