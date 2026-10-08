package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        BillReminderEntity::class,
        Expense::class,
        Category::class
    ],
    version = 4,
    exportSchema = false
)
abstract class KharchDatabase : RoomDatabase() {

    abstract fun kharchDao(): KharchDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: KharchDatabase? = null

        fun getDatabase(context: Context): KharchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KharchDatabase::class.java,
                    "kharch_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
