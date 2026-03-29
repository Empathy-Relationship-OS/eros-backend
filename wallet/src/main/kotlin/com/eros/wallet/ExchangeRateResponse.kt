package com.eros.wallet

// 1. Fixed the import to the CLIENT version
import com.eros.common.errors.ExchangeRateException
import io.ktor.client.*
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.math.BigDecimal
import java.math.RoundingMode

// Use Ktor HttpClient for non-blocking I/O with timeout
private val client = HttpClient(Apache) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 5000
        connectTimeoutMillis = 2000
    }
}

@Serializable
data class ExchangeRateResponse(
    val rates: Map<String, Double>
)

private val rateCache = ConcurrentHashMap<String, Pair<Instant, Map<String, Double>>>()
private val cacheTtl = Duration.ofHours(1)

suspend fun getExchangeRate(fromCurrency: String, toCurrency: String): Double {
    val cached = rateCache[fromCurrency.uppercase()]
    if (cached != null && Instant.now().isBefore(cached.first.plus(cacheTtl))) {
        val rate = cached.second[toCurrency.uppercase()]
        if (rate != null) return rate
    }

    try {
        val response: HttpResponse =
            client.get("https://api.exchangerate-api.com/v4/latest/${fromCurrency.uppercase()}")

        if (!response.status.isSuccess()) {
            throw ExchangeRateException("Failed to fetch rates: ${response.status}")
        }

        val data: ExchangeRateResponse = response.body()
        rateCache[fromCurrency.uppercase()] = Pair(Instant.now(), data.rates)

        val rate = data.rates[toCurrency.uppercase()]
            ?: throw IllegalArgumentException("Currency $toCurrency not found")
        return rate

    } catch (e: Exception) {
        if (e is IllegalArgumentException) throw e
        throw ExchangeRateException("Error fetching exchange rate: ${e.message}")
    }
}

suspend fun convertToUserCurrency(amountInGBP: BigDecimal, targetCurrency: String): BigDecimal {
    if (targetCurrency.equals("gbp", ignoreCase = true)) {
        return amountInGBP
    }

    val rate = BigDecimal.valueOf(getExchangeRate("GBP", targetCurrency))
    return (amountInGBP.multiply(rate)).setScale(2, RoundingMode.HALF_UP)
}