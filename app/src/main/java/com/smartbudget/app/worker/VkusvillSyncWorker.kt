package com.smartbudget.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartbudget.app.SmartBudgetApplication

class VkusvillSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val client = SmartBudgetApplication.instance.vkusvillClient
        return try {
            // Опрашиваем ВкусВилл по базовым категориям рациона
            val queries = listOf("филе индейки", "яйца куриные", "гречневая крупа", "творог 5%")
            for (q in queries) {
                client.searchProducts(q)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
