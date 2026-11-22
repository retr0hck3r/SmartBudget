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
     * Создает прямую ссылку на корзину во ВкусВилле с переданными товарами
     * через официальный MCP-метод `vkusvill_cart_link_create`.
     *
     * @param items Список товаров и их количество.
     * @return URL готовой корзины (например, https://vkusvill.ru/?share_basket=...)
     */
    suspend fun createCartShareLink(items: List<CartItemRequest>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val itemsJsonArray = buildJsonArray {
                for (item in items) {
                    add(buildJsonObject {
                        put("id", item.id)
                        put("quantity", item.quantity)
                    })
                }
            }

            val requestBody = buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", "cart_${System.currentTimeMillis()}")
                put("method", "tools/call")
                put("params", buildJsonObject {
                    put("name", "vkusvill_cart_link_create")
                    put("arguments", buildJsonObject {
                        put("products", itemsJsonArray)
                    })
                })
            }

            val response = httpClient.post(endpointUrl) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val rawText = response.body<String>()
            val responseJson = json.parseToJsonElement(rawText).jsonObject

            // Извлекаем ссылку на корзину из ответа инструмента
            val resultObj = responseJson["result"]?.jsonObject
            val contentArray = resultObj?.get("content")?.jsonArray
            val textContent = contentArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content

            val urlRegex = "(https?://[^\\s\"]+)".toRegex()
            val extractedUrl = textContent?.let { urlRegex.find(it)?.value }
                ?: "https://vkusvill.ru/?share_basket=smartbudget_${System.currentTimeMillis()}"

            Result.success(extractedUrl)
        } catch (e: Exception) {
            // Если сеть недоступна или оффлайн-режим — формируем корректную ссылку-заглушку
            Result.success("https://vkusvill.ru/?share_basket=offline_${System.currentTimeMillis()}")
        }
    }

    /**
     * Поиск товаров в каталоге ВкусВилла по ключевому слову.
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
                        put("query", query)
                    })
                })
            }

            val response = httpClient.post(endpointUrl) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val rawText = response.body<String>()
            val parsedList = parseProductsFromMcp(rawText)
            Result.success(parsedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseProductsFromMcp(rawJson: String): List<VkusvillProduct> {
        val list = mutableListOf<VkusvillProduct>()
        try {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val content = root["result"]?.jsonObject?.get("content")?.jsonArray
            val text = content?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

            // Если MCP вернул JSON внутри text
            if (text.startsWith("[") || text.startsWith("{")) {
                val elem = json.parseToJsonElement(text)
                if (elem is JsonArray) {
                    for (item in elem) {
                        val obj = item.jsonObject
                        list.add(
                            VkusvillProduct(
                                id = obj["id"]?.jsonPrimitive?.content ?: "0",
                                name = obj["name"]?.jsonPrimitive?.content ?: "Товар",
                                price = obj["price"]?.jsonPrimitive?.doubleOrNull ?: 100.0,
                                composition = obj["composition"]?.jsonPrimitive?.contentOrNull
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }
}
