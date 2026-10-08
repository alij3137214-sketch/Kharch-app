package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CommitteeEntity
import com.example.data.model.DebtEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.WishItemEntity

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        BillReminderEntity::class,
        WishItemEntity::class,
        DebtEntity::class,
        CommitteeEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class KharchDatabase : RoomDatabase() {

    abstract fun kharchDao(): KharchDao

    companion object {
        /**
         * v4 -> v5: removes the unused legacy tables, lets bills repeat every month,
         * and adds the wish list, udhaar and committee tables. Existing records are kept.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `expenses`")
                db.execSQL("DROP TABLE IF EXISTS `categories`")
                db.execSQL("ALTER TABLE `bill_reminders` ADD COLUMN `repeatMonthly` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `wish_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, `price` REAL NOT NULL, `addedAt` INTEGER NOT NULL, " +
                        "`waitDays` INTEGER NOT NULL, `status` TEXT NOT NULL, `note` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `debts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`person` TEXT NOT NULL, `amount` REAL NOT NULL, `direction` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `dueDate` INTEGER, `note` TEXT NOT NULL, `paidAmount` REAL NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `committees` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `monthlyAmount` REAL NOT NULL, `totalMembers` INTEGER NOT NULL, " +
                        "`myTurn` INTEGER NOT NULL, `startMonth` TEXT NOT NULL, `paidMonths` INTEGER NOT NULL, " +
                        "`payoutReceived` INTEGER NOT NULL)"
                )
            }
        }

        @Volatile
        private var INSTANCE: KharchDatabase? = null

        fun getDatabase(context: Context): KharchDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KharchDatabase::class.java,
                    "kharch_database"
                )
                    .addMigrations(MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
