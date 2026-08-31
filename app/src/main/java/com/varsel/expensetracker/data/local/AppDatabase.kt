package com.varsel.expensetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.dao.CustomRuleDao
import com.varsel.expensetracker.data.local.dao.FinancialEventAllocationDao
import com.varsel.expensetracker.data.local.dao.LoanAccountDao
import com.varsel.expensetracker.data.local.dao.LoanPaymentDao
import com.varsel.expensetracker.data.local.dao.StatementSnapshotDao
import com.varsel.expensetracker.data.local.dao.TransactionDao
import com.varsel.expensetracker.data.local.dao.TransactionLinkGroupDao
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.data.local.entity.CustomRuleEntity
import com.varsel.expensetracker.data.local.entity.FinancialEventAllocationEntity
import com.varsel.expensetracker.data.local.entity.LoanAccountEntity
import com.varsel.expensetracker.data.local.entity.LoanPaymentEntity
import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import com.varsel.expensetracker.data.local.entity.TransactionEntity
import com.varsel.expensetracker.data.local.entity.TransactionLinkGroupEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CustomRuleEntity::class,
        StatementSnapshotEntity::class,
        TransactionLinkGroupEntity::class,
        FinancialEventAllocationEntity::class,
        LoanAccountEntity::class,
        LoanPaymentEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun financialEventAllocationDao():
    FinancialEventAllocationDao

    abstract fun categoryDao(): CategoryDao

    abstract fun customRuleDao(): CustomRuleDao

    abstract fun statementSnapshotDao(): StatementSnapshotDao

    abstract fun transactionLinkGroupDao(): TransactionLinkGroupDao

    abstract fun loanAccountDao(): LoanAccountDao

    abstract fun loanPaymentDao(): LoanPaymentDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS statement_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        openingBalance REAL,
                        endingBalance REAL,
                        totalDebits REAL,
                        totalCredits REAL,
                        importedAt INTEGER NOT NULL,
                        statementStartDate INTEGER,
                        statementEndDate INTEGER
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_rules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        keyword TEXT NOT NULL,
                        categoryName TEXT NOT NULL,
                        matchType TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {

        override fun migrate(
            database: SupportSQLiteDatabase
        ) {
            database.execSQL(
                """
                ALTER TABLE transactions
                ADD COLUMN accountId TEXT
                """.trimIndent()
            )

            database.execSQL(
                """
                ALTER TABLE transactions
                ADD COLUMN accountLast4 TEXT
                """.trimIndent()
            )

            database.execSQL(
                """
                ALTER TABLE statement_snapshots
                ADD COLUMN accountId TEXT
                """.trimIndent()
            )

            database.execSQL(
                """
                ALTER TABLE statement_snapshots
                ADD COLUMN accountLast4 TEXT
                """.trimIndent()
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {

        override fun migrate(
            database: SupportSQLiteDatabase
        ) {
            // No schema changes were introduced in this development version.
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {

        override fun migrate(
            database: SupportSQLiteDatabase
        ) {
            database.execSQL(
                """
                ALTER TABLE transactions
                ADD COLUMN role TEXT NOT NULL DEFAULT 'NORMAL'
                """.trimIndent()
            )
        }
    }

        /**
     * Adds support for manually linking related transactions.
     *
     * The value is nullable because existing transactions are
     * not linked automatically.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {

        override fun migrate(
            database: SupportSQLiteDatabase
        ) {
            database.execSQL(
                """
                ALTER TABLE transactions
                ADD COLUMN transactionLinkId TEXT
                """.trimIndent()
            )
        }
    }

    /**
     * Adds support for manually group creation.
     */
    val MIGRATION_7_8 = object : Migration(7, 8) {

    override fun migrate(
        database: SupportSQLiteDatabase
    ) {

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transaction_link_groups (
                transactionLinkId TEXT NOT NULL,
                groupName TEXT NOT NULL,
                category TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(transactionLinkId)
            )
            """.trimIndent()
        )
    }
}

    /**
 * Adds a dedicated relationship identifier for
 * account transfers.
 *
 * This is intentionally separate from transactionLinkId,
 * which belongs to Financial Events.
 */
val MIGRATION_8_9 =
    object : Migration(8, 9) {

        override fun migrate(
            database:
                SupportSQLiteDatabase
        ) {

            database.execSQL(
                """
                ALTER TABLE transactions
                ADD COLUMN transferLinkId TEXT
                """.trimIndent()
            )
        }
    }

    val MIGRATION_9_10 =
    object : Migration(9, 10) {

        override fun migrate(
            database:
                SupportSQLiteDatabase
        ) {

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS financial_event_allocations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    transactionId INTEGER NOT NULL,
                    transactionLinkId TEXT NOT NULL,
                    allocatedAmount REAL NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                index_financial_event_allocations_transactionId
                ON financial_event_allocations(transactionId)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                index_financial_event_allocations_transactionLinkId
                ON financial_event_allocations(transactionLinkId)
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                index_financial_event_allocations_transactionId_transactionLinkId
                ON financial_event_allocations(
                    transactionId,
                    transactionLinkId
                )
                """.trimIndent()
            )

            /*
             * --------------------------------------------------
             * BACKWARD COMPATIBILITY
             * --------------------------------------------------
             *
             * Every existing one-to-one Financial Event
             * relationship becomes a 100% allocation.
             *
             * Existing transaction:
             *
             * transactionLinkId = ABC
             * amount = ₹1,000
             *
             * becomes:
             *
             * transactionId = same ID
             * transactionLinkId = ABC
             * allocatedAmount = ₹1,000
             */
            database.execSQL(
                """
                INSERT INTO financial_event_allocations (
                    transactionId,
                    transactionLinkId,
                    allocatedAmount,
                    createdAt
                )
                SELECT
                    id,
                    transactionLinkId,
                    amount,
                    dateTimestamp
                FROM transactions
                WHERE transactionLinkId IS NOT NULL
                """.trimIndent()
            )
        }
    }

    val MIGRATION_10_11 =
        object : Migration(10, 11) {

            override fun migrate(
                database: SupportSQLiteDatabase
            ) {
                database.execSQL(
                    """
                    ALTER TABLE categories
                    ADD COLUMN type TEXT NOT NULL DEFAULT 'EXPENSE'
                    """.trimIndent()
                )

                // Update known income categories to INCOME
                database.execSQL(
                    """
                    UPDATE categories
                    SET type = 'INCOME'
                    WHERE UPPER(name) IN ('SALARY', 'INCOME', 'INVESTMENT', 'INVESTMENTS', 'FREELANCE', 'RENTAL', 'REFUND', 'CASHBACK', 'DIVIDENDS', 'GIFTS')
                    """.trimIndent()
                )

                // Update universal categories to BOTH
                database.execSQL(
                    """
                    UPDATE categories
                    SET type = 'BOTH'
                    WHERE UPPER(name) IN ('UNCATEGORIZED', 'TRANSFER', 'OTHER')
                    """.trimIndent()
                )

                // Seed default Income categories if not existing
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO categories (name, colorHex, iconName, budgetLimit, keywords, type)
                    VALUES ('Salary', '#4CAF50', 'ic_salary', 0.0, 'SALARY,PAYROLL,ACH CREDIT,NEFT CREDIT,STIPEND', 'INCOME')
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO categories (name, colorHex, iconName, budgetLimit, keywords, type)
                    VALUES ('Investments', '#1565C0', 'ic_trending_up', 0.0, 'DIVIDEND,INTEREST,GROWW,ZERODHA,MUTUAL FUND,STOCKS', 'INCOME')
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO categories (name, colorHex, iconName, budgetLimit, keywords, type)
                    VALUES ('Freelance & Side Hustle', '#00897B', 'ic_work', 0.0, 'UPWORK,FIVERR,FREELANCE,CLIENT PAYMENT,CONSULTING', 'INCOME')
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO categories (name, colorHex, iconName, budgetLimit, keywords, type)
                    VALUES ('Refunds & Cashback', '#00BCD4', 'ic_swap', 0.0, 'REFUND,CASHBACK,REVERSAL,CREDIT ADJUSTMENT', 'INCOME')
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO categories (name, colorHex, iconName, budgetLimit, keywords, type)
                    VALUES ('Rental & Property', '#795548', 'ic_home', 0.0, 'RENT,TENANT,LEASE', 'INCOME')
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO categories (name, colorHex, iconName, budgetLimit, keywords, type)
                    VALUES ('Gifts & Grants', '#E91E63', 'ic_gift', 0.0, 'GIFT,BONUS,REWARD,GRANT', 'INCOME')
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO categories (name, colorHex, iconName, budgetLimit, keywords, type)
                    VALUES ('Other Income', '#8BC34A', 'ic_paid', 0.0, 'INCOME,CREDIT', 'INCOME')
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_11_12 =
            object : Migration(11, 12) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {
                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS loan_accounts (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            loanType TEXT NOT NULL,
                            principal REAL NOT NULL,
                            annualInterestRate REAL NOT NULL,
                            emiAmount REAL NOT NULL,
                            totalTenureMonths INTEGER NOT NULL,
                            startDateTimestamp INTEGER NOT NULL,
                            collateralOrNotes TEXT,
                            status TEXT NOT NULL DEFAULT 'ACTIVE',
                            linkedBankAccountId TEXT,
                            bankAccountLast4 TEXT,
                            lenderName TEXT,
                            loanAccountNumber TEXT,
                            createdAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS loan_payments (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            loanId INTEGER NOT NULL,
                            paymentDateTimestamp INTEGER NOT NULL,
                            amount REAL NOT NULL,
                            principalComponent REAL NOT NULL,
                            interestComponent REAL NOT NULL,
                            paymentType TEXT NOT NULL DEFAULT 'REGULAR_EMI',
                            linkedTransactionId INTEGER,
                            notes TEXT,
                            createdAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS index_loan_payments_loanId
                        ON loan_payments(loanId)
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS index_loan_payments_linkedTransactionId
                        ON loan_payments(linkedTransactionId)
                        """.trimIndent()
                    )
                }
            }
    }
    class SeedCallback(
        private val categoryDaoProvider: Provider<CategoryDao>
    ) : RoomDatabase.Callback() {

        override fun onCreate(
            db: SupportSQLiteDatabase
        ) {
            super.onCreate(db)

            CoroutineScope(Dispatchers.IO).launch {
                seedDefaultCategories(
                    categoryDaoProvider.get()
                )
            }
        }

        private suspend fun seedDefaultCategories(
            categoryDao: CategoryDao
        ) {
            val defaultCategories = listOf(
                // Income categories
                CategoryEntity(
                    name = "Salary",
                    iconName = "ic_salary",
                    colorHex = "#4CAF50",
                    keywords = "SALARY,PAYROLL,ACH CREDIT,NEFT CREDIT,STIPEND",
                    type = "INCOME"
                ),
                CategoryEntity(
                    name = "Investments",
                    iconName = "ic_trending_up",
                    colorHex = "#1565C0",
                    keywords = "DIVIDEND,INTEREST,GROWW,ZERODHA,MUTUAL FUND,STOCKS",
                    type = "INCOME"
                ),
                CategoryEntity(
                    name = "Freelance & Side Hustle",
                    iconName = "ic_work",
                    colorHex = "#00897B",
                    keywords = "UPWORK,FIVERR,FREELANCE,CLIENT PAYMENT,CONSULTING",
                    type = "INCOME"
                ),
                CategoryEntity(
                    name = "Refunds & Cashback",
                    iconName = "ic_swap",
                    colorHex = "#00BCD4",
                    keywords = "REFUND,CASHBACK,REVERSAL,CREDIT ADJUSTMENT",
                    type = "INCOME"
                ),
                CategoryEntity(
                    name = "Rental & Property",
                    iconName = "ic_home",
                    colorHex = "#795548",
                    keywords = "RENT,TENANT,LEASE",
                    type = "INCOME"
                ),
                CategoryEntity(
                    name = "Gifts & Grants",
                    iconName = "ic_gift",
                    colorHex = "#E91E63",
                    keywords = "GIFT,BONUS,REWARD,GRANT",
                    type = "INCOME"
                ),
                CategoryEntity(
                    name = "Other Income",
                    iconName = "ic_paid",
                    colorHex = "#8BC34A",
                    keywords = "INCOME,CREDIT",
                    type = "INCOME"
                ),

                // Expense categories
                CategoryEntity(
                    name = "Dining & Food",
                    iconName = "ic_restaurant",
                    colorHex = "#FF9800",
                    keywords = "STARBUCKS,MCDONALD,SWIGGY,ZOMATO,RESTAURANT,CAFE,BAKERY,PIZZA",
                    type = "EXPENSE"
                ),
                CategoryEntity(
                    name = "Groceries",
                    iconName = "ic_cart",
                    colorHex = "#4CAF50",
                    keywords = "WALMART,DMART,SUPERMARKET,GROCERY,BIGBASKET,PRODUCE,WHOLEFOODS",
                    type = "EXPENSE"
                ),
                CategoryEntity(
                    name = "Fuel & Transport",
                    iconName = "ic_car",
                    colorHex = "#9C27B0",
                    keywords = "SHELL,PETROL,UBER,OLA,PARKING,TOLL,METRO,CHEVRON,GASOLINE",
                    type = "EXPENSE"
                ),
                CategoryEntity(
                    name = "Utilities",
                    iconName = "ic_lightning",
                    colorHex = "#2196F3",
                    keywords = "ELECTRIC,WATER,AIRTEL,JIO,BROADBAND,VERIZON,ATT,GAS BILL",
                    type = "EXPENSE"
                ),
                CategoryEntity(
                    name = "Healthcare",
                    iconName = "ic_hospital",
                    colorHex = "#F44336",
                    keywords = "PHARMACY,HOSPITAL,CLINIC,CVS,WALGREENS,MEDICARE,APOLLO",
                    type = "EXPENSE"
                ),
                CategoryEntity(
                    name = "Shopping",
                    iconName = "ic_bag",
                    colorHex = "#E91E63",
                    keywords = "AMAZON,FLIPKART,TARGET,ZARA,CLOTHING,FOOTWEAR,MALL",
                    type = "EXPENSE"
                ),
                CategoryEntity(
                    name = "Entertainment",
                    iconName = "ic_movies",
                    colorHex = "#673AB7",
                    keywords = "NETFLIX,SPOTIFY,CINEMA,MOVIE,THEATRE,STEAM,GAME",
                    type = "EXPENSE"
                ),
                CategoryEntity(
                    name = "Uncategorized",
                    iconName = "ic_help",
                    colorHex = "#9E9E9E",
                    keywords = "",
                    type = "BOTH"
                )
            )

            categoryDao.insertCategories(
                defaultCategories
            )
        }
    }
}
