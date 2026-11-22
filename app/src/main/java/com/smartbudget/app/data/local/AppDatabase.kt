package com.smartbudget.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smartbudget.app.data.local.dao.BudgetDao
import com.smartbudget.app.data.local.entity.ExpenseEntity
import com.smartbudget.app.data.local.entity.IncomePayoutEntity
import com.smartbudget.app.data.local.entity.StopWordEntity

@Database(
    entities = [
        IncomePayoutEntity::class,
        ExpenseEntity::class,
        StopWordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_budget.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
