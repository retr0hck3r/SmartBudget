package com.smartbudget.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.*
import com.smartbudget.app.data.local.AppDatabase
import com.smartbudget.app.data.network.VkusvillMcpClient
import com.smartbudget.app.worker.PayrollReminderWorker
import com.smartbudget.app.worker.VkusvillSyncWorker
import java.util.concurrent.TimeUnit

class SmartBudgetApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var vkusvillClient: VkusvillMcpClient
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        vkusvillClient = VkusvillMcpClient()

        createNotificationChannels()
        scheduleBackgroundWorkers()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_PAYROLL,
                "Уведомления о выплатах и лимитах",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о внесении ЗП и контроле дневного лимита"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun scheduleBackgroundWorkers() {
        val workManager = WorkManager.getInstance(this)

        // Ежедневный воркер напоминаний о датах выплат (в 10:00)
        val dailyCheckRequest = PeriodicWorkRequestBuilder<PayrollReminderWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "payroll_reminder_work",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyCheckRequest
        )

        // Еженедельная синхронизация цен ВкусВилл (раз в 7 дней при наличии сети)
        val vkusvillSyncRequest = PeriodicWorkRequestBuilder<VkusvillSyncWorker>(7, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "vkusvill_sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            vkusvillSyncRequest
        )
    }

    companion object {
        const val CHANNEL_ID_PAYROLL = "smartbudget_payroll_channel"

        lateinit var instance: SmartBudgetApplication
            private set
    }
}
