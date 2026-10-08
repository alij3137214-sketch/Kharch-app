package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BillReminderEntity
import com.example.data.model.BudgetEntity
import com.example.data.model.CommitteeEntity
import com.example.data.model.DebtEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.WishItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KharchDao {

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE timestamp >= :from AND timestamp < :to")
    suspend fun deleteTransactionsBetween(from: Long, to: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    // Budgets
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsOnce(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets")
    suspend fun clearAllBudgets()

    // Savings goals
    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getSavingsGoalById(id: Long): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals")
    fun getAllSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoals(goals: List<SavingsGoalEntity>)

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals")
    suspend fun clearAllSavingsGoals()

    // Bills
    @Query("SELECT * FROM bill_reminders WHERE id = :id LIMIT 1")
    suspend fun getBillReminderById(id: Long): BillReminderEntity?

    @Query("SELECT * FROM bill_reminders ORDER BY dueDate ASC")
    fun getAllBillReminders(): Flow<List<BillReminderEntity>>

    @Query("SELECT * FROM bill_reminders ORDER BY dueDate ASC")
    suspend fun getAllBillRemindersOnce(): List<BillReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillReminder(bill: BillReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillReminders(bills: List<BillReminderEntity>)

    @Update
    suspend fun updateBillReminder(bill: BillReminderEntity)

    @Delete
    suspend fun deleteBillReminder(bill: BillReminderEntity)

    @Query("DELETE FROM bill_reminders")
    suspend fun clearAllBillReminders()

    // Wish list
    @Query("SELECT * FROM wish_items WHERE id = :id LIMIT 1")
    suspend fun getWishItemById(id: Long): WishItemEntity?

    @Query("SELECT * FROM wish_items ORDER BY addedAt DESC")
    fun getAllWishItems(): Flow<List<WishItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishItem(item: WishItemEntity): Long

    @Update
    suspend fun updateWishItem(item: WishItemEntity)

    @Delete
    suspend fun deleteWishItem(item: WishItemEntity)

    @Query("DELETE FROM wish_items")
    suspend fun clearAllWishItems()

    // Udhaar
    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun getDebtById(id: Long): DebtEntity?

    @Query("SELECT * FROM debts ORDER BY createdAt DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("DELETE FROM debts")
    suspend fun clearAllDebts()

    // Committees
    @Query("SELECT * FROM committees WHERE id = :id LIMIT 1")
    suspend fun getCommitteeById(id: Long): CommitteeEntity?

    @Query("SELECT * FROM committees ORDER BY id DESC")
    fun getAllCommittees(): Flow<List<CommitteeEntity>>

    @Query("SELECT * FROM committees ORDER BY id DESC")
    suspend fun getAllCommitteesOnce(): List<CommitteeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommittee(committee: CommitteeEntity): Long

    @Update
    suspend fun updateCommittee(committee: CommitteeEntity)

    @Delete
    suspend fun deleteCommittee(committee: CommitteeEntity)

    @Query("DELETE FROM committees")
    suspend fun clearAllCommittees()
}
