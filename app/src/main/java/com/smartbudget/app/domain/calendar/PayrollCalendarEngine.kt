package com.smartbudget.app.domain.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Движок производственного календаря РФ для расчета дат выплат зарплаты
 * в строгом соответствии со ст. 136 Трудового кодекса РФ:
 * "При совпадении дня выплаты с выходным или нерабочим праздничным днем выплата
 * заработной платы производится накануне этого дня".
 * 
 * Поддерживает как стандартные даты (10 и 25 числа), так и любые пользовательские
 * дни выплат (1..31), заданные в разделе Настроек.
 */
object PayrollCalendarEngine {

    val DEFAULT_PAYOUT_DAYS: Set<Int> = setOf(10, 25)

    @Volatile
    var configuredPayoutDays: Set<Int> = DEFAULT_PAYOUT_DAYS

    /**
     * Официальные нерабочие праздничные дни в РФ (ст. 112 ТК РФ).
     * Формат: (Месяц, День)
     */
    private val staticHolidays = setOf(
        Pair(1, 1), Pair(1, 2), Pair(1, 3), Pair(1, 4),
        Pair(1, 5), Pair(1, 6), Pair(1, 7), Pair(1, 8), // Новогодние каникулы и Рождество
        Pair(2, 23), // День защитника Отечества
        Pair(3, 8),  // Международный женский день
        Pair(5, 1),  // Праздник Весны и Труда
        Pair(5, 9),  // День Победы
        Pair(6, 12), // День России
        Pair(11, 4)  // День народного единства
    )

    /**
     * Проверяет, является ли день нерабочим (выходным: сб/вс или праздничным).
     */
    fun isNonWorkingDay(date: LocalDate): Boolean {
        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            return true
        }
        return staticHolidays.contains(Pair(date.monthValue, date.dayOfMonth))
    }

    /**
     * Вычисляет фактическую дату выплаты для базового дня заданного месяца и года.
     * Если день нерабочий — сдвигает назад до ближайшего рабочего дня.
     */
    fun getActualPayoutDate(year: Int, month: Int, baseDay: Int): LocalDate {
        require(baseDay in 1..31) { "Базовый день выплаты должен быть от 1 до 31" }
        val maxDays = LocalDate.of(year, month, 1).lengthOfMonth()
        val clampedDay = baseDay.coerceAtMost(maxDays)
        var date = LocalDate.of(year, month, clampedDay)
        
        // Сдвигаем накануне, пока не попадем на рабочий день
        while (isNonWorkingDay(date)) {
            date = date.minusDays(1)
        }
        return date
    }

    /**
     * Возвращает список всех фактических дат выплат на указанный год.
     * За январь первых чисел выплата обычно переносится на конец декабря предшествующего года.
     */
    fun getYearPayouts(year: Int, days: Set<Int> = configuredPayoutDays): List<LocalDate> {
        val effectiveDays = if (days.isEmpty()) DEFAULT_PAYOUT_DAYS else days
        val list = mutableListOf<LocalDate>()
        for (m in 1..12) {
            for (day in effectiveDays) {
                list.add(getActualPayoutDate(year, m, day))
            }
        }
        // Добавляем переносы начала следующего года, если они сдвинулись в текущий декабрь
        for (day in effectiveDays.filter { it <= 10 }) {
            val janNextPayout = getActualPayoutDate(year + 1, 1, day)
            if (janNextPayout.year == year) {
                list.add(janNextPayout)
            }
        }
        return list.sorted().distinct()
    }

    /**
     * Находит ближайшую следующую дату выплаты относительно текущего дня.
     */
    fun getNextPayoutDate(today: LocalDate = LocalDate.now(), days: Set<Int> = configuredPayoutDays): LocalDate {
        val currentYearPayouts = getYearPayouts(today.year, days) + getYearPayouts(today.year + 1, days)
        return currentYearPayouts.firstOrNull { it > today }
            ?: today.plusDays(15)
    }

    /**
     * Возвращает дату последней (прошедшей) выплаты, открывшей текущий финансовый спринт.
     */
    fun getLastPayoutDate(today: LocalDate = LocalDate.now(), days: Set<Int> = configuredPayoutDays): LocalDate {
        val allPayouts = getYearPayouts(today.year - 1, days) + getYearPayouts(today.year, days)
        return allPayouts.filter { it <= today }.maxOrNull()
            ?: today.minusDays(15)
    }

    /**
     * Возвращает границы текущего финансового спринта: (Дата_Старта, Дата_Следующей_Выплаты).
     */
    fun getCurrentSprint(today: LocalDate = LocalDate.now(), days: Set<Int> = configuredPayoutDays): Pair<LocalDate, LocalDate> {
        val start = getLastPayoutDate(today, days)
        val end = getNextPayoutDate(today, days)
        return Pair(start, end)
    }

    /**
     * Количество дней, оставшихся до следующей выплаты (включая сегодняшний день).
     */
    fun getDaysUntilNextPayout(today: LocalDate = LocalDate.now(), days: Set<Int> = configuredPayoutDays): Long {
        val next = getNextPayoutDate(today, days)
        val diff = ChronoUnit.DAYS.between(today, next)
        return if (diff < 1) 1 else diff
    }

    /**
     * Полная длительность текущего спринта в днях.
     */
    fun getSprintTotalDays(today: LocalDate = LocalDate.now(), days: Set<Int> = configuredPayoutDays): Long {
        val (start, end) = getCurrentSprint(today, days)
        val diff = ChronoUnit.DAYS.between(start, end)
        return if (diff < 1) 1 else diff
    }
}
