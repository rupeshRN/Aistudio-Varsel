package com.varsel.expensetracker.di

import com.varsel.expensetracker.data.repository.BudgetRepositoryImpl
import com.varsel.expensetracker.data.repository.LoanRepositoryImpl
import com.varsel.expensetracker.data.repository.StatementSnapshotRepositoryImpl
import com.varsel.expensetracker.data.repository.TransactionLinkGroupRepositoryImpl
import com.varsel.expensetracker.data.repository.TransactionRepositoryImpl
import com.varsel.expensetracker.domain.repository.BudgetRepository
import com.varsel.expensetracker.domain.repository.LoanRepository
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import com.varsel.expensetracker.domain.repository.TransactionLinkGroupRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Dependency Injection module responsible for binding domain repository interfaces 
 * to their concrete data layer implementations.
 * 
 * Declared as an abstract class because it utilizes Dagger's @Binds annotation, 
 * which generates optimized code without requiring manual object instantiation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds [TransactionRepositoryImpl] to the [TransactionRepository] interface contract.
     * 
     * Whenever a ViewModel or UseCase requests a dependency on [TransactionRepository], 
     * Hilt automatically provides the singleton instance of [TransactionRepositoryImpl].
     *
     * @param impl The concrete repository implementation, constructed via its @Inject constructor.
     * @return The interface contract exposed to the domain layer.
     */
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    /** Binds [statementsnapshotrepoimplementation] to the [statementsnapshotrepo] interface contract */
    @Binds
    @Singleton
    abstract fun bindStatementSnapshotRepository(
        impl: StatementSnapshotRepositoryImpl
    ): StatementSnapshotRepository

    /** Binds [TransactionLinkGroupRepositoryImpl] to the [TransactionLinkGroupRepository] interface contract */
    @Binds
    @Singleton
    abstract fun bindTransactionLinkGroupRepository(
        impl: TransactionLinkGroupRepositoryImpl
    ): TransactionLinkGroupRepository

    /** Binds [LoanRepositoryImpl] to the [LoanRepository] interface contract */
    @Binds
    @Singleton
    abstract fun bindLoanRepository(
        impl: LoanRepositoryImpl
    ): LoanRepository

    /** Binds [BudgetRepositoryImpl] to the [BudgetRepository] interface contract */
    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        impl: BudgetRepositoryImpl
    ): BudgetRepository
}
