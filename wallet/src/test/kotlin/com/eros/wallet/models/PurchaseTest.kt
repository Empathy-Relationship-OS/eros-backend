package com.eros.wallet.models

import java.math.BigDecimal

fun createTestPurchase(
    clientSecret: String = "test_secret_123",
    paymentIntentId: String = "pi_test_123",
    amountPaid: BigDecimal = 7.00.toBigDecimal(),
    currency: String = "GBP",
    tokenAmount: BigDecimal = 1.toBigDecimal(),
    status: String = "PENDING",
    newBalance: BigDecimal? = null,
    transactionId: Long? = null,
    acceptedTerms: Boolean = true
): Purchase = Purchase(
    clientSecret = clientSecret,
    paymentIntentId = paymentIntentId,
    amountPaid = amountPaid,
    currency = currency,
    tokenAmount = tokenAmount,
    status = status,
    newBalance = newBalance,
    transactionId = transactionId,
    acceptedTerms = acceptedTerms
)