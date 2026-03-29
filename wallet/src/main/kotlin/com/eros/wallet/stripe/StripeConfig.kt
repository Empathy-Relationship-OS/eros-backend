package com.eros.wallet.stripe

import com.stripe.Stripe

import io.ktor.server.config.*
import org.slf4j.LoggerFactory

object StripeConfig {
    private val logger = LoggerFactory.getLogger(StripeConfig::class.java)

    @Volatile
    private var initialized = false
    private val initLock = Any()

    lateinit var secretKey: String
        private set
    lateinit var webhookSecret: String
        private set
    lateinit var publishableKey: String
        private set

    val isTestMode: Boolean
        get() = secretKey.startsWith("sk_test_")

    fun initialize(config: ApplicationConfig) {
        if (initialized) return
        synchronized(initLock) {
            if (initialized) return
            secretKey = config.property("stripe.secretKey").getString()
            webhookSecret = config.property("stripe.webhookSecret").getString()
            publishableKey = config.property("stripe.publishableKey").getString()

            logger.info("Initializing Stripe SDK...")
            Stripe.apiKey = secretKey

            validateKeyConsistency()
            initialized = true
        }

        logger.info("Stripe initialized in ${if (isTestMode) "TEST" else "LIVE"} mode")
    }

    private fun validateKeyConsistency() {
        val secretIsTest = secretKey.startsWith("sk_test_")
        val publishableIsTest = publishableKey.startsWith("pk_test_")

        if (secretIsTest != publishableIsTest) {
            throw IllegalStateException(
                "Stripe key mismatch: Secret key and publishable key must both be test or live mode"
            )
        }

        if (!secretKey.startsWith("sk_test_") && !secretKey.startsWith("sk_live_")) {
            throw IllegalStateException("Invalid STRIPE_SECRET_KEY format")
        }

        if (!publishableKey.startsWith("pk_test_") && !publishableKey.startsWith("pk_live_")) {
            throw IllegalStateException("Invalid STRIPE_PUBLISHABLE_KEY format")
        }

        if (!webhookSecret.startsWith("whsec_")) {
            throw IllegalStateException("Invalid STRIPE_WEBHOOK_SECRET format")
        }
    }
}