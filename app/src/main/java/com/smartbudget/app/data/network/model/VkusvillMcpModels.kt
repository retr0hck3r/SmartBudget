package com.smartbudget.app.data.network.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: String,
    val method: String,
    val params: JsonObject
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: String? = null,
    val result: JsonObject? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String
)

@Serializable
data class CartItemRequest(
    val id: String,
    val quantity: Int
)

@Serializable
data class VkusvillProduct(
    val id: String,
    val name: String,
    val price: Double,
    val unit: String = "шт",
    val category: String? = null,
    val composition: String? = null,
    val rating: Double? = null,
    val imageUrl: String? = null
)
