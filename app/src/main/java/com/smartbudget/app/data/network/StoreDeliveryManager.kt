package com.smartbudget.app.data.network

import android.content.Context
import android.content.Intent
import android.net.Uri

enum class DeliveryStore(
    val title: String,
    val packageName: String,
    val searchUrlPrefix: String,
    val baseAppDeepLink: String
) {
    VKUSVILL(
        title = "ВкусВилл",
        packageName = "ru.vkusvill",
        searchUrlPrefix = "https://vkusvill.ru/goods/search/?q=",
        baseAppDeepLink = "vkusvill://cart"
    ),
    LENTA(
        title = "Лента Онлайн",
        packageName = "com.lenta.app",
        searchUrlPrefix = "https://lenta.com/search/?searchText=",
        baseAppDeepLink = "lenta://search"
    ),
    YANDEX_LAVKA(
        title = "Яндекс.Лавка",
        packageName = "com.yandex.lavka",
        searchUrlPrefix = "https://lavka.yandex.ru/search?text=",
        baseAppDeepLink = "yandexlavka://main"
    )
}

object StoreDeliveryManager {

    /**
     * Открывает приложение магазина для поиска или заказа.
     * Если приложение установлено — открывает напрямую нативное приложение.
     * Если приложения нет — открывает веб-версию каталога в браузере.
     */
    fun openStore(context: Context, store: DeliveryStore, searchQuery: String? = null) {
        val targetUrl = if (!searchQuery.isNullOrBlank()) {
            store.searchUrlPrefix + Uri.encode(searchQuery)
        } else {
            when (store) {
                DeliveryStore.VKUSVILL -> "https://vkusvill.ru/cart/"
                DeliveryStore.LENTA -> "https://lenta.com/cart/"
                DeliveryStore.YANDEX_LAVKA -> "https://lavka.yandex.ru/"
            }
        }

        // Проверяем наличие установленного приложения
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(store.packageName)

        if (launchIntent != null) {
            // Пытаемся передать DeepLink в приложение
            val deepIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                setPackage(store.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(deepIntent)
                return
            } catch (_: Exception) {
                // Если специфический URL не принят, просто открываем само приложение
                context.startActivity(launchIntent)
                return
            }
        }

        // Фолбэк: открытие в браузере
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(browserIntent)
    }

    /**
     * Создает прямую ссылку на корзину для ВкусВилла или открывает приложение.
     */
    fun openCart(context: Context, store: DeliveryStore, shareUrl: String) {
        val pm = context.packageManager
        val hasApp = pm.getLaunchIntentForPackage(store.packageName) != null

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl)).apply {
            if (hasApp) {
                setPackage(store.packageName)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Если с setPackage не получилось, открываем без привязки к пакету
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }
}
