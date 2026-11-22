package com.smartbudget.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_payouts")
data class IncomePayoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateIso: String,
    val totalIncome: Double,
    val housingAmount: Double,
    val foodAmount: Double,
    val savingsAmount: Double,
    val personalAmount: Double,
    val bufferAmount: Double,
    val daysInSprint: Long
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateIso: String,
    val amount: Double,
    val category: String, // "HOUSING", "FOOD", "PERSONAL", "BUFFER"
    val note: String = ""
)

@Entity(tableName = "food_stop_words")
data class StopWordEntity(
    @PrimaryKey
    val word: String,
    val isCategoryPreset: Boolean = false
)
