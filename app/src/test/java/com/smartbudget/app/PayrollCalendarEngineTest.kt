package com.smartbudget.app

import com.smartbudget.app.domain.calendar.PayrollCalendarEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PayrollCalendarEngineTest {

    @Test
    fun `test payout shifts to Friday when 10th falls on Saturday`() {
        // Октябрь 2026: 10 октября — это суббота
        val actual = PayrollCalendarEngine.getActualPayoutDate(2026, 10, 10)
        assertEquals(LocalDate.of(2026, 10, 9), actual)
        assertEquals(DayOfWeek.FRIDAY, actual.dayOfWeek)
    }

    @Test
    fun `test payout shifts to Friday when 25th falls on Sunday`() {
        // Октябрь 2026: 25 октября — это воскресенье
        val actual = PayrollCalendarEngine.getActualPayoutDate(2026, 10, 25)
        assertEquals(LocalDate.of(2026, 10, 23), actual)
        assertEquals(DayOfWeek.FRIDAY, actual.dayOfWeek)
    }

    @Test
    fun `test payout shifts when base day is on official holiday`() {
        // 10 января — новогодние праздники в РФ. Должно сместиться на последний рабочий день декабря
        val actual = PayrollCalendarEngine.getActualPayoutDate(2027, 1, 10)
        assertTrue("Дата должна быть в декабре или до 1 января", actual.isBefore(LocalDate.of(2027, 1, 1)))
        assertTrue("Дата должна быть рабочим днем", !PayrollCalendarEngine.isNonWorkingDay(actual))
    }

    @Test
    fun `test next payout date is always strictly in future`() {
        val today = LocalDate.of(2026, 11, 18)
        val next = PayrollCalendarEngine.getNextPayoutDate(today)
        assertTrue(next.isAfter(today))
    }
}
