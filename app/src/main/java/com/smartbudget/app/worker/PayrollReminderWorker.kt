package com.smartbudget.app.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartbudget.app.R
import com.smartbudget.app.SmartBudgetApplication
import com.smartbudget.app.domain.calendar.PayrollCalendarEngine
import com.smartbudget.app.ui.MainActivity
import java.time.LocalDate

class PayrollReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val nextPayout = PayrollCalendarEngine.getNextPayoutDate(today)

        // 1. Проверяем, является ли сегодня днем выплаты
        val actualTodayPayout = PayrollCalendarEngine.getActualPayoutDate(today.year, today.monthValue, if (today.dayOfMonth <= 15) 10 else 25)
        if (today == actualTodayPayout) {
            sendNotification(
                title = "💰 Сегодня день выплаты!",
                message = "Внесите полученную сумму в SmartBudget для автоматического распределения бюджета."
            )
            return Result.success()
        }

        // 2. Напоминание за 1 день до выплаты
        if (today.plusDays(1) == nextPayout) {
            sendNotification(
                title = "⏳ Завтра день выплаты",
                message = "Завтра поступит зарплата/аванс. Приложение автоматически подготовит расчет."
            )
        }

        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_INCOME_INPUT", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SmartBudgetApplication.CHANNEL_ID_PAYROLL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (_: SecurityException) {}
    }
}
