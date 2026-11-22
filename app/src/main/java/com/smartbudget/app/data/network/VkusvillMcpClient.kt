package com.smartbudget.app.data.network

import com.smartbudget.app.data.network.model.CartItemRequest
import com.smartbudget.app.data.network.model.VkusvillProduct
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class VkusvillAuthRequiredException(message: String) : Exception(message)

class VkusvillMcpClient(
    private val endpointUrl: String = "https://mcp.vkusvill.ru/mcp"
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    /**
     * Создает прямую мобильную ссылку на готовую корзину во ВкусВилле через официальный MCP-метод `vkusvill_cart_link_create`.
     * Входной формат: products: [ { id: Int, q: Double } ]
     * Возвращает URL: https://vkusvill.ru/mobile?basket&share=XXXXXX
     * Если передан jwtToken, авторизуется через заголовок Authorization: Bearer <token>.
     */
    suspend fun createCartShareLink(items: List<CartItemRequest>, jwtToken: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Формируем валидный массив товаров с числовыми id и количеством q
            val validProducts = items.mapNotNull { item ->
                val intId = item.id.toIntOrNull() ?: 185
                if (intId > 0) {
                    buildJsonObject {
                        put("id", intId)
                        put("q", item.quantity.coerceIn(1, 40))
                    }
                } else null
            }.distinctBy { it["id"]?.jsonPrimitive?.content }

            if (validProducts.isEmpty()) {
                return@withContext Result.failure(Exception("Нет доступных товаров для создания корзины ВкусВилл"))
            }

            val requestBody = buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", "cart_${System.currentTimeMillis()}")
                put("method", "tools/call")
                put("params", buildJsonObject {
                    put("name", "vkusvill_cart_link_create")
                    put("arguments", buildJsonObject {
                        put("products", JsonArray(validProducts))
                    })
                })
            }

            val response = httpClient.post(endpointUrl) {
                headers.append("Accept", "application/json, text/event-stream")
                if (!jwtToken.isNullOrBlank()) {
                    headers.append("Authorization", "Bearer $jwtToken")
                }
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                setBody(requestBody.toString())
            }

            if (response.status == HttpStatusCode.Unauthorized || response.status.value == 401) {
                return@withContext Result.failure(
                    VkusvillAuthRequiredException(
                        "Сервер ВкусВилл требует авторизацию (OAuth 2.0 / JWT). " +
                        "В октябре 2026 ВкусВилл закрыл анонимный доступ к MCP. " +
                        "Вы можете ввести токен в Настройках или скопировать корзину прямо в приложение."
                    )
                )
            }

            val rawText = response.body<String>()
            val responseJson = json.parseToJsonElement(rawText).jsonObject

            // Проверяем ошибку внутри JSON-RPC
            val errorObj = responseJson["error"]?.jsonObject
            if (errorObj != null) {
                val errorMsg = errorObj["message"]?.jsonPrimitive?.content ?: "Ошибка MCP"
                return@withContext Result.failure(Exception("ВкусВилл MCP: $errorMsg"))
            }

            val resultObj = responseJson["result"]?.jsonObject
            val contentArray = resultObj?.get("content")?.jsonArray
            val innerText = contentArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content

            if (!innerText.isNullOrBlank()) {
                val dataObj = json.parseToJsonElement(innerText).jsonObject["data"]?.jsonObject
                val directLink = dataObj?.get("link")?.jsonPrimitive?.content
                if (!directLink.isNullOrBlank()) {
                    return@withContext Result.success(directLink)
                }
            }

            // Запасной поиск ссылки в тексте
            val urlRegex = "(https?://[^\\s\"]+)".toRegex()
            val extractedUrl = innerText?.let { urlRegex.find(it)?.value }
            if (extractedUrl != null) {
                Result.success(extractedUrl)
            } else {
                Result.failure(Exception("Не удалось получить ссылку на корзину от ВкусВилл"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Поиск товаров в каталоге ВкусВилла по ключевому слову через MCP `vkusvill_products_search`.
     */
    suspend fun searchProducts(query: String): Result<List<VkusvillProduct>> = withContext(Dispatchers.IO) {
        try {
            val requestBody = buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", "search_${System.currentTimeMillis()}")
                put("method", "tools/call")
                put("params", buildJsonObject {
                    put("name", "vkusvill_products_search")
                    put("arguments", buildJsonObject {
                        put("q", query)
                        put("mode", "short")
                    })
                })
            }

            val response = httpClient.post(endpointUrl) {
                headers.append("Accept", "application/json, text/event-stream")
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                setBody(requestBody.toString())
            }

            val rawText = response.body<String>()
            val responseJson = json.parseToJsonElement(rawText).jsonObject
            val contentArray = responseJson["result"]?.jsonObject?.get("content")?.jsonArray
            val innerText = contentArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content

            if (!innerText.isNullOrBlank()) {
                val dataObj = json.parseToJsonElement(innerText).jsonObject["data"]?.jsonObject
                val itemsArray = dataObj?.get("items")?.jsonArray ?: JsonArray(emptyList())

                val products = itemsArray.mapNotNull { itemElem ->
                    val obj = itemElem.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val name = obj["name"]?.jsonPrimitive?.content ?: ""
                    val priceObj = obj["price"]?.jsonObject
                    val price = priceObj?.get("current")?.jsonPrimitive?.doubleOrNull ?: 0.0

                    VkusvillProduct(
                        id = id,
                        name = name,
                        price = price,
                        category = "",
                        unit = obj["unit"]?.jsonPrimitive?.content ?: "шт",
                        imageUrl = null
                    )
                }
                Result.success(products)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
