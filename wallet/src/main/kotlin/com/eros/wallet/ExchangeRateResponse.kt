package com.eros.wallet

import com.google.gson.Gson
import java.math.BigDecimal
import java.net.URL

data class ExchangeRateResponse(
    val rates: Map<String, Double>
)

fun getExchangeRate(fromCurrency: String, toCurrency: String): Double {
    // Using exchangerate-api.com (free tier: 1,500 requests/month)
    val url = "https://api.exchangerate-api.com/v4/latest/$fromCurrency"
    val response = URL(url).readText()
    val data = Gson().fromJson(response, ExchangeRateResponse::class.java)

    return data.rates[toCurrency.uppercase()]
        ?: throw IllegalArgumentException("Currency $toCurrency not found")
}

fun convertToUserCurrency(amountInGBP: BigDecimal, targetCurrency: String): BigDecimal {
    if (targetCurrency.equals("gbp", ignoreCase = true)) {
        return amountInGBP
    }

    val rate = getExchangeRate("GBP", targetCurrency).toBigDecimal()
    return (amountInGBP * rate)
}