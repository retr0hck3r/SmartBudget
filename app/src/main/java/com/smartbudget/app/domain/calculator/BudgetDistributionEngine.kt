package com.smartbudget.app.domain.calculator

import kotlin.math.max
import kotlin.math.roundToInt

data class BudgetSplitResult(
    val incomeAmount: Double,
    val housingAmount: Double,
    val foodAmount: Double,
    val savingsAmount: Double,
    val personalAmount: Double,
    val bufferAmount: Double,
    val daysInSprint: Long,
    val dailyFoodCost: Double,
    val dailySafeToSpend: Double,
    val distributableRemainder: Double
)

object BudgetDistributionEngine {

    /**
     * По умолчанию средняя стоимость суточного полноценного рациона по каталогу ВкусВилла.
     * Калибруется через MCP-сервис.
     */
    const val DEFAULT_DAILY_FOOD_COST = 950.0

    /**
     * Вычисляет автоматическое распределение бюджета.
     *
     * @param income Сумма поступившего дохода (зарплата / аванс).
     * @param housing Сумма, зарезервированная на квартиру (аренда / ипотека + ЖКХ).
     * @param daysInSprint Количество дней в текущем спринте (до следующей ЗП).
     * @param dailyFoodCost Суточная норма корзины питания по ВкусВиллу.
     * @param savingsPct Доля чистого остатка в накопления/инвестиции (по умолч. 40%).
     * @param personalPct Доля чистого остатка на личные траты / "себе" (по умолч. 45%).
     * @param bufferPct Доля чистого остатка в резервный буфер форс-мажора (по умолч. 15%).
     */
    fun calculateSplit(
        income: Double,
        housing: Double,
        daysInSprint: Long,
        dailyFoodCost: Double = DEFAULT_DAILY_FOOD_COST,
        savingsPct: Double = 0.40,
        personalPct: Double = 0.45,
        bufferPct: Double = 0.15
    ): BudgetSplitResult {
        val safeDays = max(1L, daysInSprint)
        
        // 1. Обязательный продуктовый бюджет под реальные дни спринта
        val foodAmount = (safeDays * dailyFoodCost).roundTo2Decimals()

        // 2. Свободный остаток после вычета жилья и продуктов
        val remainder = max(0.0, income - housing - foodAmount)

        // 3. Распределение остатка по фондам
        val totalPct = savingsPct + personalPct + bufferPct
        val normSavings = if (totalPct > 0) savingsPct / totalPct else 0.40
        val normPersonal = if (totalPct > 0) personalPct / totalPct else 0.45
        val normBuffer = if (totalPct > 0) bufferPct / totalPct else 0.15

        val savingsAmount = (remainder * normSavings).roundTo2Decimals()
        val personalAmount = (remainder * normPersonal).roundTo2Decimals()
        val bufferAmount = (remainder * normBuffer).roundTo2Decimals()

        // 4. Безопасный дневной лимит трат (еда + личные карманные деньги / количество дней)
        val dailySafeToSpend = ((foodAmount + personalAmount) / safeDays).roundTo2Decimals()

        return BudgetSplitResult(
            incomeAmount = income.roundTo2Decimals(),
            housingAmount = housing.roundTo2Decimals(),
            foodAmount = foodAmount,
            savingsAmount = savingsAmount,
            personalAmount = personalAmount,
            bufferAmount = bufferAmount,
            daysInSprint = safeDays,
            dailyFoodCost = dailyFoodCost,
            dailySafeToSpend = dailySafeToSpend,
            distributableRemainder = remainder.roundTo2Decimals()
        )
    }

    /**
     * Пересчитывает дневной лимит в реальном времени с учетом уже совершенных расходов.
     */
    fun recalculateDailyAllowance(
        remainingFoodBudget: Double,
        remainingPersonalBudget: Double,
        daysRemaining: Long
    ): Double {
        val safeDays = max(1L, daysRemaining)
        val totalAvailable = max(0.0, remainingFoodBudget + remainingPersonalBudget)
        return (totalAvailable / safeDays).roundTo2Decimals()
    }

    private fun Double.roundTo2Decimals(): Double {
        return (this * 100.0).roundToInt() / 100.0
    }
}
