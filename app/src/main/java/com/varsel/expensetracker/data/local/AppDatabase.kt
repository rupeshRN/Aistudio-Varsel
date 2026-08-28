package com.varsel.expensetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.varsel.expensetracker.data.local.dao.CategoryDao
import com.varsel.expensetracker.data.local.dao.CustomRuleDao
import com.varsel.expensetracker.data.local.dao.StatementSnapshotDao
import com.varsel.expensetracker.data.local.dao.TransactionDao
import com.varsel.expensetracker.data.local.entity.CategoryEntity
import com.varsel.expensetracker.data.local.entity.CustomRuleEntity
import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import com.varsel.expensetracker.data.local.entity.TransactionEntity
import com.varsel.expensetracker.data.local.entity.TransactionLinkGroupEntity
import com.varsel.expensetracker.data.local.dao.TransactionLinkGroupDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import com.varsel.expensetracker.data.local.entity.FinancialEventAllocationEntity
import com.varsel.expensetracker.data.local.dao.FinancialEventAllocationDao

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CustomRuleEntity::class,
        StatementSnapshotEntity::class,
        TransactionLinkGroupEntity::class,
        FinancialEventAllocationEntity::class
    ],
    version = 10,
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

    companion object {

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

                CategoryEntity(
                    name = "Salary",
                    iconName = "ic_salary",
                    colorHex = "#4CAF50",
                    keywords =
                        "SALARY,PAYROLL,ACH CREDIT,NEFT CREDIT,STIPEND"
                ),

                CategoryEntity(
                    name = "Groceries",
                    iconName = "ic_cart",
                    colorHex = "#FF9800",
                    keywords =
                        "WALMART,DMART,SUPERMARKET,GROCERY,BIGBASKET,PRODUCE,WHOLEFOODS"
                ),

                CategoryEntity(
                    name = "Dining & Food",
                    iconName = "ic_restaurant",
                    colorHex = "#E91E63",
                    keywords =
                        "STARBUCKS,MCDONALD,SWIGGY,ZOMATO,RESTAURANT,CAFE,BAKERY,PIZZA"
                ),

                CategoryEntity(
                    name = "Fuel & Transport",
                    iconName = "ic_car",
                    colorHex = "#9C27B0",
                    keywords =
                        "SHELL,PETROL,UBER,OLA,PARKING,TOLL,METRO,CHEVRON,GASOLINE"
                ),

                CategoryEntity(
                    name = "Utilities",
                    iconName = "ic_lightning",
                    colorHex = "#2196F3",
                    keywords =
                        "ELECTRIC,WATER,AIRTEL,JIO,BROADBAND,VERIZON,ATT,GAS BILL"
                ),

                CategoryEntity(
                    name = "Healthcare",
                    iconName = "ic_hospital",
                    colorHex = "#F44336",
                    keywords =
                        "PHARMACY,HOSPITAL,CLINIC,CVS,WALGREENS,MEDICARE,APOLLO"
                ),

                CategoryEntity(
                    name = "Shopping",
                    iconName = "ic_bag",
                    colorHex = "#00BCD4",
                    keywords =
                        "AMAZON,FLIPKART,TARGET,ZARA,CLOTHING,FOOTWEAR,MALL"
                ),

                CategoryEntity(
                    name = "Uncategorized",
                    iconName = "ic_help",
                    colorHex = "#9E9E9E",
                    keywords = ""
                )
            )

            categoryDao.insertCategories(
                defaultCategories
            )
        }
    }
}
