package com.smartbudget.app.data.repository

import com.smartbudget.app.data.local.dao.BudgetDao
import com.smartbudget.app.data.local.entity.ExpenseEntity
import com.smartbudget.app.data.local.entity.IncomePayoutEntity
import com.smartbudget.app.data.local.entity.StopWordEntity
import com.smartbudget.app.data.network.VkusvillMcpClient
import com.smartbudget.app.data.network.model.CartItemRequest
import com.smartbudget.app.domain.calculator.BudgetDistributionEngine
import com.smartbudget.app.domain.calculator.BudgetSplitResult
import com.smartbudget.app.domain.calendar.PayrollCalendarEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class BudgetRepository(
    private val dao: BudgetDao,
    private val vkusvillClient: VkusvillMcpClient
) {

    val latestPayout: Flow<IncomePayoutEntity?> = dao.getLatestPayout()
    val allExpenses: Flow<List<ExpenseEntity>> = dao.getAllExpenses()
    val stopWords: Flow<List<StopWordEntity>> = dao.getAllStopWords()

    /**
     * Возвращает текущий безопасный дневной лимит с учетом последних трат.
     */
    fun getDailySafeToSpend(): Flow<Double> {
        val today = LocalDate.now()
        val sprintStart = PayrollCalendarEngine.getLastPayoutDate(today).toString()
        val daysRemaining = PayrollCalendarEngine.getDaysUntilNextPayout(today)

        return combine(
            latestPayout,
            dao.getSpentByCategorySince("FOOD", sprintStart),
            dao.getSpentByCategorySince("PERSONAL", sprintStart)
        ) { payout, foodSpent, personalSpent ->
            if (payout == null) return@combine 1500.0 // Базовый ориентир до первого ввода

            val remainingFood = (payout.foodAmount - (foodSpent ?: 0.0)).coerceAtLeast(0.0)
            val remainingPersonal = (payout.personalAmount - (personalSpent ?: 0.0)).coerceAtLeast(0.0)

            BudgetDistributionEngine.recalculateDailyAllowance(
                remainingFoodBudget = remainingFood,
                remainingPersonalBudget = remainingPersonal,
                daysRemaining = daysRemaining
            )
        }
    }

    /**
     * Записывает новую полученную выплату и автоматически раскладывает её по конвертам.
     */
    suspend fun saveIncomePayout(
        income: Double,
        housing: Double
    ): BudgetSplitResult {
        val today = LocalDate.now()
        val daysInSprint = PayrollCalendarEngine.getSprintTotalDays(today)

        val split = BudgetDistributionEngine.calculateSplit(
            income = income,
            housing = housing,
            daysInSprint = daysInSprint
        )

        dao.insertPayout(
            IncomePayoutEntity(
                dateIso = today.toString(),
                totalIncome = split.incomeAmount,
                housingAmount = split.housingAmount,
                foodAmount = split.foodAmount,
                savingsAmount = split.savingsAmount,
                personalAmount = split.personalAmount,
                bufferAmount = split.bufferAmount,
                daysInSprint = split.daysInSprint
            )
        )
        return split
    }

    /**
     * Фиксирует новый расход в определенной категории.
     */
    suspend fun addExpense(
        amount: Double,
        category: String,
        note: String = ""
    ) {
        dao.insertExpense(
            ExpenseEntity(
                dateIso = LocalDate.now().toString(),
                amount = amount,
                category = category,
                note = note
            )
        )
    }

    suspend fun addStopWord(word: String) {
        dao.insertStopWord(StopWordEntity(word.trim().lowercase()))
    }

    suspend fun removeStopWord(word: String) {
        dao.deleteStopWord(StopWordEntity(word.trim().lowercase()))
    }

    suspend fun createVkusvillCartLink(items: List<CartItemRequest>): Result<String> {
        return vkusvillClient.createCartShareLink(items)
    }
}
