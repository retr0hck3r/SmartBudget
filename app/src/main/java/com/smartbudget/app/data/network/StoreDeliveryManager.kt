package com.smartbudget.app.data.network

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.smartbudget.app.domain.menu.ShoppingItem

enum class DeliveryStore(
    val title: String,
    val packageName: String,
    val fallbackPackage: String? = null,
    val searchUrlPrefix: String,
    val appSearchScheme: String
) {
    VKUSVILL(
        title = "ВкусВилл",
        packageName = "ru.vkusvill",
        fallbackPackage = null,
        searchUrlPrefix = "https://vkusvill.ru/goods/search/?q=",
        appSearchScheme = "vkusvill://search?query="
    ),
    LENTA(
        title = "Лента Онлайн",
        packageName = "com.lenta.app",
        fallbackPackage = null,
        searchUrlPrefix = "https://lenta.com/search/?searchText=",
        appSearchScheme = "lenta://search?searchText="
    ),
    YANDEX_LAVKA(
        title = "Яндекс.Лавка",
        packageName = "com.yandex.lavka",
        fallbackPackage = "ru.yandex.eda",
        searchUrlPrefix = "https://lavka.yandex.ru/search?text=",
        appSearchScheme = "yandexlavka://search?text="
    )
}

object StoreDeliveryManager {

    /**
     * Очищает название ингредиента от служебных приставок для точного поиска в каталоге.
     * Например: "Творог 5% ВкусВилл" -> "Творог 5%", "Капуста брокколи зам." -> "Брокколи"
     */
    fun cleanSearchQuery(rawName: String): String {
        return rawName
            .replace("ВкусВилл", "", ignoreCase = true)
            .replace("зам.", "", ignoreCase = true)
            .replace("свежий", "", ignoreCase = true)
            .replace("свежая", "", ignoreCase = true)
            .replace("свежие", "", ignoreCase = true)
            .replace("молодой", "", ignoreCase = true)
            .replace("атлантической", "", ignoreCase = true)
            .replace("духовая", "", ignoreCase = true)
            .trim()
    }

    /**
     * Открывает карточку поиска конкретного товара в приложении выбранного магазина
     * или в веб-каталоге, если приложение не установлено.
     */
    fun openProductSearch(context: Context, store: DeliveryStore, rawProductName: String) {
        val query = cleanSearchQuery(rawProductName)
        val encodedQuery = Uri.encode(query)
        val webSearchUrl = store.searchUrlPrefix + encodedQuery
        val appSchemeUrl = store.appSearchScheme + encodedQuery

        val pm = context.packageManager

        // 1. Проверяем основное приложение (или фоллбэк пакет для Яндекс.Еды/Лавки)
        val targetPkg = if (pm.getLaunchIntentForPackage(store.packageName) != null) {
            store.packageName
        } else if (store.fallbackPackage != null && pm.getLaunchIntentForPackage(store.fallbackPackage) != null) {
            store.fallbackPackage
        } else {
            null
        }

        if (targetPkg != null) {
            // Пробуем кастомную deep link схему
            try {
                val schemeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(appSchemeUrl)).apply {
                    setPackage(targetPkg)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(schemeIntent)
                return
            } catch (_: Exception) {
                // Если кастомная схема не подошла, пробуем открыть https URL в приложении
                try {
                    val webInAppIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webSearchUrl)).apply {
                        setPackage(targetPkg)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webInAppIntent)
                    return
                } catch (_: Exception) {
                    // Фолбэк на запуск приложения
                    val launchIntent = pm.getLaunchIntentForPackage(targetPkg)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                        return
                    }
                }
            }
        }

        // 2. Фолбэк: открытие поиска в веб-браузере
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webSearchUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось открыть страницу магазина", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Копирует полный структурированный список покупок в буфер обмена телефона.
     */
    fun copyShoppingListToClipboard(context: Context, items: List<ShoppingItem>, totalCost: Double) {
        val sb = StringBuilder()
        sb.appendLine("🛒 Список продуктов на спринт (SmartBudget):")
        sb.appendLine("----------------------------------------")
        items.forEachIndexed { index, item ->
            val qty = if (item.totalQuantity % 1.0 == 0.0) item.totalQuantity.toInt().toString() else "%.1f".format(item.totalQuantity)
            sb.appendLine("${index + 1}. ${item.name} — $qty ${item.unit} (~${item.estimatedTotalCost.toInt()} ₽)")
        }
        sb.appendLine("----------------------------------------")
        sb.appendLine("Итого расчетная сумма: ~${totalCost.toInt()} ₽")

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SmartBudget Shopping List", sb.toString())
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "Список из ${items.size} товаров скопирован в буфер обмена!", Toast.LENGTH_SHORT).show()
    }
}
