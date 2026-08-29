package com.varsel.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.varsel.expensetracker.data.local.entity.LoanAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanAccountDao {

    @Query("SELECT * FROM loan_accounts ORDER BY createdAt DESC")
    fun getAllLoanAccounts(): Flow<List<LoanAccountEntity>>

    @Query("SELECT * FROM loan_accounts WHERE id = :id")
    fun getLoanAccountById(id: Long): Flow<LoanAccountEntity?>

    @Query("SELECT * FROM loan_accounts WHERE id = :id")
    suspend fun getLoanAccountByIdSync(id: Long): LoanAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanAccount(loan: LoanAccountEntity): Long

    @Update
    suspend fun updateLoanAccount(loan: LoanAccountEntity)

    @Delete
    suspend fun deleteLoanAccount(loan: LoanAccountEntity)

    @Query("DELETE FROM loan_accounts WHERE id = :id")
    suspend fun deleteLoanAccountById(id: Long)
}
