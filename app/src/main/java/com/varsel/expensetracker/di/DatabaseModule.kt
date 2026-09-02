package com.varsel.expensetracker.di

import android.content.Context
import androidx.room.Room
import com.varsel.expensetracker.data.local.AppDatabase
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.dao.CustomRuleDao
import com.varsel.expensetracker.data.local.dao.FinancialEventAllocationDao
import com.varsel.expensetracker.data.local.dao.LoanAccountDao
import com.varsel.expensetracker.data.local.dao.LoanPaymentDao
import com.varsel.expensetracker.data.local.dao.StatementSnapshotDao
import com.varsel.expensetracker.data.local.dao.TransactionDao
import com.varsel.expensetracker.data.local.dao.TransactionLinkGroupDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val PREFS_NAME =
        "encrypted_db_secure_prefs"

    private const val PASSPHRASE_KEY =
        "db_passphrase_key"

    @Provides
    @Singleton
    fun provideDatabasePassphrase(
        @ApplicationContext context: Context
    ): ByteArray {

        val prefs =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        var keyString =
            prefs.getString(
                PASSPHRASE_KEY,
                null
            )

        if (keyString == null) {

            val randomBytes =
                ByteArray(32)

            SecureRandom()
                .nextBytes(randomBytes)

            keyString =
                randomBytes.joinToString("") {
                    "%02x".format(it)
                }

            prefs.edit()
                .putString(
                    PASSPHRASE_KEY,
                    keyString
                )
                .apply()
        }

        return keyString
            .toByteArray(
                StandardCharsets.UTF_8
            )
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        passphrase: ByteArray,
        categoryDaoProvider: Provider<CategoryDao>
    ): AppDatabase {

        val factory =
            SupportOpenHelperFactory(
                passphrase
            )

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "encrypted_expense_tracker.db"
        )
            .openHelperFactory(factory)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14
            )
            .addCallback(
                AppDatabase.SeedCallback(
                    categoryDaoProvider
                )
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideTransactionDao(
        db: AppDatabase
    ): TransactionDao =
        db.transactionDao()

    @Provides
    fun provideCategoryDao(
        db: AppDatabase
    ): CategoryDao =
        db.categoryDao()

    @Provides
    fun provideCustomRuleDao(
        db: AppDatabase
    ): CustomRuleDao =
        db.customRuleDao()

    @Provides
    fun provideStatementSnapshotDao(
        db: AppDatabase
    ): StatementSnapshotDao =
        db.statementSnapshotDao()

    @Provides
    fun provideTransactionLinkGroupDao(
        db: AppDatabase
    ): TransactionLinkGroupDao =
        db.transactionLinkGroupDao()

    @Provides
    @Singleton
    fun provideFinancialEventAllocationDao(
        database: AppDatabase
    ): FinancialEventAllocationDao =
        database.financialEventAllocationDao()

    @Provides
    fun provideLoanAccountDao(
        db: AppDatabase
    ): LoanAccountDao =
        db.loanAccountDao()

    @Provides
    fun provideLoanPaymentDao(
        db: AppDatabase
    ): LoanPaymentDao =
        db.loanPaymentDao()
}
