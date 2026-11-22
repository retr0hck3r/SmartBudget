package com.smartbudget.app.data.local.dao

import androidx.room.*
import com.smartbudget.app.data.local.entity.ExpenseEntity
import com.smartbudget.app.data.local.entity.IncomePayoutEntity
import com.smartbudget.app.data.local.entity.StopWordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    // ВЫПЛАТЫ (INCOME)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayout(payout: IncomePayoutEntity): Long

    @Query("SELECT * FROM income_payouts ORDER BY id DESC LIMIT 1")
    fun getLatestPayout(): Flow<IncomePayoutEntity?>

    @Query("SELECT * FROM income_payouts ORDER BY id DESC")
    fun getAllPayouts(): Flow<List<IncomePayoutEntity>>

    // РАСХОДЫ (EXPENSES)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE dateIso >= :startDateIso ORDER BY id DESC")
    fun getExpensesSince(startDateIso: String): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses WHERE category = :category AND dateIso >= :startDateIso")
    fun getSpentByCategorySince(category: String, startDateIso: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE dateIso = :dateIso")
    fun getTodayTotalSpent(dateIso: String): Flow<Double?>

    // СТОП-ЛИСТ
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStopWord(entity: StopWordEntity)

    @Delete
    suspend fun deleteStopWord(entity: StopWordEntity)

    @Query("SELECT * FROM food_stop_words ORDER BY word ASC")
    fun getAllStopWords(): Flow<List<StopWordEntity>>
}
