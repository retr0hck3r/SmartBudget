package com.smartbudget.app.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import com.smartbudget.app.data.local.AppDatabase
import com.smartbudget.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class MirPayNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkgName = sbn.packageName ?: ""
        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

        val combined = "$title $text $bigText"

        // Проверяем, что уведомление пришло от Mir Pay или содержит признаки оплаты картой Mir
        val isMirPay = pkgName.contains("mirpay", ignoreCase = true) ||
                pkgName == "ru.nspk.mirpay" ||
                title.contains("Mir Pay", ignoreCase = true) ||
                combined.contains("Карта Mir", ignoreCase = true) ||
                combined.contains("Оплата покупки", ignoreCase = true)

        if (!isMirPay) return

        // Парсим строго сумму платежа (например: "Сумма 60 ₽", "Сумма 1 250.50 ₽")
        val amount = parsePaymentAmount(combined) ?: return

        // Записываем сумму в базу данных как расход
        scope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            db.budgetDao().insertExpense(
                ExpenseEntity(
                    dateIso = LocalDate.now().toString(),
                    amount = amount,
                    category = "FOOD", // Базово в категорию повседневных расходов
                    note = "Mir Pay (Авто)"
                )
            )
        }
    }

    companion object {
        private val amountRegex = Regex(
            """(?:Сумма\s+)?([0-9]+(?:[\s\u00A0][0-9]{3})*(?:[.,][0-9]{1,2})?)\s*₽""",
            RegexOption.IGNORE_CASE
        )

        /**
         * Извлекает числовое значение суммы из строки уведомления.
         * Защита персональных данных: считывает ТОЛЬКО число, номер карты и локация отбрасываются.
         */
        fun parsePaymentAmount(text: String): Double? {
            val match = amountRegex.find(text) ?: return null
            val rawNum = match.groupValues[1]
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace(",", ".")
            return rawNum.toDoubleOrNull()
        }

        /**
         * Проверяет, включено ли разрешение на чтение уведомлений пользователем в Android Settings.
         */
        fun isNotificationServiceEnabled(context: Context): Boolean {
            val pkgName = context.packageName
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            if (!TextUtils.isEmpty(flat)) {
                val names = flat.split(":")
                for (name in names) {
                    val cn = ComponentName.unflattenFromString(name)
                    if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                        return true
                    }
                }
            }
            return false
        }
    }
}
