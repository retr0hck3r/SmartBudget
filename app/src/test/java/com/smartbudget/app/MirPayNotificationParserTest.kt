package com.smartbudget.app

import com.smartbudget.app.service.MirPayNotificationListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MirPayNotificationParserTest {

    @Test
    fun `test parses amount from user screenshot notification`() {
        val sampleText = "Mir Pay Оплата покупки Сумма 60 ₽. 22.11.2026, 13:57. Карта Mir • • • • 2500"
        val parsed = MirPayNotificationListener.parsePaymentAmount(sampleText)
        assertNotNull(parsed)
        assertEquals(60.0, parsed!!, 0.01)
    }

    @Test
    fun `test parses large amount with spaces and kopecks`() {
        val sampleText = "Оплата покупки Сумма 1 450,50 ₽. Карта Mir 1234"
        val parsed = MirPayNotificationListener.parsePaymentAmount(sampleText)
        assertNotNull(parsed)
        assertEquals(1450.50, parsed!!, 0.01)
    }
}
