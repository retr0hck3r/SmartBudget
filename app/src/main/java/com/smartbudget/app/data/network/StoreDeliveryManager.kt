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
        packageName = "com.icemobile.lenta.prod",
        fallbackPackage = "com.lenta.app",
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
     * Надежно открывает приложение магазина на телефоне с копированием поискового запроса в буфер обмена.
     * Если приложение установлено — открывается ОНО, а не браузер!
     */
    fun openProductSearch(context: Context, store: DeliveryStore, rawProductName: String) {
        val query = cleanSearchQuery(rawProductName)
        val encodedQuery = Uri.encode(query)
        val webSearchUrl = store.searchUrlPrefix + encodedQuery

        // 1. Всегда копируем очищенное название товара в буфер обмена для быстрой вставки
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Product Search", query)
        clipboard.setPrimaryClip(clip)

        val pm = context.packageManager

        // 2. Определяем установленный пакет
        val targetPkg = if (pm.getLaunchIntentForPackage(store.packageName) != null) {
            store.packageName
        } else if (store.fallbackPackage != null && pm.getLaunchIntentForPackage(store.fallbackPackage) != null) {
            store.fallbackPackage
        } else {
            null
        }

        if (targetPkg != null) {
            // Пробуем передать веб-ссылку поиска напрямую внутрь установленного приложения
            try {
                val inAppSearchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webSearchUrl)).apply {
                    setPackage(targetPkg)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(inAppSearchIntent)
                Toast.makeText(context, "«$query» скопировано. Открываем ${store.title}", Toast.LENGTH_SHORT).show()
                return
            } catch (_: Exception) {
                // Если приложение не поддерживает переход по веб-урлу поиска напрямую,
                // запускаем само приложение — название товара уже в буфере!
                val launchIntent = pm.getLaunchIntentForPackage(targetPkg)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    Toast.makeText(context, "«$query» скопировано. Вставьте в поиск ${store.title}", Toast.LENGTH_LONG).show()
                    return
                }
            }
        }

        // 3. Только если приложение вообще не установлено на телефоне — открываем веб-поиск
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webSearchUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
            Toast.makeText(context, "Приложение ${store.title} не найдено, открываем сайт", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Не удалось открыть поиск магазина", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Копирует полный структурированный список покупок в буфер обмена телефона
     * и сразу же открывает выбранное приложение магазина.
     */
    fun openStoreWithFullList(context: Context, store: DeliveryStore, items: List<ShoppingItem>, totalCost: Double) {
        copyShoppingListToClipboard(context, items, totalCost)

        val pm = context.packageManager
        val targetPkg = if (pm.getLaunchIntentForPackage(store.packageName) != null) {
            store.packageName
        } else if (store.fallbackPackage != null && pm.getLaunchIntentForPackage(store.fallbackPackage) != null) {
            store.fallbackPackage
        } else {
            null
        }

        if (targetPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPkg)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                return
            }
        }

        // Фолбэк на сайт
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(store.searchUrlPrefix.substringBefore("/search"))).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        } catch (_: Exception) {}
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
