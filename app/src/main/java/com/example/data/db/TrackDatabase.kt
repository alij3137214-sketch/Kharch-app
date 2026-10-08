package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Category
import com.example.data.model.Expense

/**
 * Room Database schema for the 'Track' feature.
 *
 * Defines the database tables for user spending data persistence:
 * - [Expense]: Records of user expenditures with amount, date, description, category, and categoryId.
 * - [Category]: Categories for organizing spending (Food, Transportation, Utilities, etc.) with colors and limits.
 */
@Database(
    entities = [
        Expense::class,
        Category::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TrackDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: TrackDatabase? = null

        fun getDatabase(context: Context): TrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackDatabase::class.java,
                    "track_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
