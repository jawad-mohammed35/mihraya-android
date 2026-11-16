package com.mihraya.app.feature.more

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request

data class MetalQuote(val pricePerGram: String, val updated: Long)
object MetalPrices {
    private val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
    const val SOURCE = "Gold API · api.gold-api.com"
    fun isFresh(updated: Long, now: Long = System.currentTimeMillis()) = updated > 0 && now - updated in 0..86_400_000L
    fun parse(body: String, symbol: String, now: Long = System.currentTimeMillis()): MetalQuote {
        val obj = Json.parseToJsonElement(body).jsonObject
        require(obj.getValue("symbol").jsonPrimitive.content == symbol)
        require(obj.getValue("currency").jsonPrimitive.content == "USD")
        val price = obj.getValue("price").jsonPrimitive.content.toBigDecimal()
        val updated = Instant.parse(obj.getValue("updatedAt").jsonPrimitive.content).toEpochMilli()
        require(price > BigDecimal.ZERO && updated > 0 && updated <= now + 300_000) { "Invalid market quote" }
        return MetalQuote(price.divide(BigDecimal("31.1034768"), 6, RoundingMode.HALF_UP).toPlainString(), updated)
    }
    suspend fun fetch(symbol: String): MetalQuote = withContext(Dispatchers.IO) {
        require(symbol in listOf("XAU", "XAG"))
        client.newCall(Request.Builder().url("https://api.gold-api.com/price/$symbol").build()).execute().use {
            check(it.isSuccessful) { "Pricing unavailable" }
            parse(it.body?.string() ?: error("Empty quote"), symbol)
        }
    }
}
