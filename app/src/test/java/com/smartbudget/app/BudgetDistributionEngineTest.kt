package com.smartbudget.app

import com.smartbudget.app.domain.calculator.BudgetDistributionEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetDistributionEngineTest {

    @Test
    fun `test standard budget calculation with housing deduction`() {
        val income = 100_000.0
        val housing = 30_000.0
        val daysInSprint = 15L
        val dailyFoodCost = 1_000.0 // 15 000 на еду

        val result = BudgetDistributionEngine.calculateSplit(
            income = income,
            housing = housing,
            daysInSprint = daysInSprint,
            dailyFoodCost = dailyFoodCost,
            savingsPct = 0.40,
            personalPct = 0.45,
            bufferPct = 0.15
        )

        // 1. Еда = 15 * 1000 = 15 000
        assertEquals(15_000.0, result.foodAmount, 0.01)

        // 2. Свободный остаток = 100 000 - 30 000 (жилье) - 15 000 (еда) = 55 000
        assertEquals(55_000.0, result.distributableRemainder, 0.01)

        // 3. Накопления = 55 000 * 0.40 = 22 000
        assertEquals(22_000.0, result.savingsAmount, 0.01)

        // 4. Себе = 55 000 * 0.45 = 24 750
        assertEquals(24_750.0, result.personalAmount, 0.01)

        // 5. Буфер = 55 000 * 0.15 = 8 250
        assertEquals(8_250.0, result.bufferAmount, 0.01)

        // 6. Дневной безопасный лимит = (15 000 еда + 24 750 себе) / 15 дней = 39 750 / 15 = 2 650
        assertEquals(2_650.0, result.dailySafeToSpend, 0.01)
    }

    @Test
    fun `test zero housing in advance payout`() {
        val income = 50_000.0
        val housing = 0.0 // во второй половине месяца жилье уже оплачено
        val daysInSprint = 15L
        val dailyFoodCost = 900.0 // 13 500 на еду

        val result = BudgetDistributionEngine.calculateSplit(
            income = income,
            housing = housing,
            daysInSprint = daysInSprint,
            dailyFoodCost = dailyFoodCost
        )

        assertEquals(13_500.0, result.foodAmount, 0.01)
        assertEquals(36_500.0, result.distributableRemainder, 0.01)
    }
}
