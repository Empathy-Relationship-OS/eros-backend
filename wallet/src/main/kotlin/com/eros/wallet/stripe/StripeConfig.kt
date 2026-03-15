package com.eros.wallet.stripe

import com.stripe.Stripe

object StripeConfig {
    val secretKey: String = System.getenv("STRIPE_SECRET_KEY")
        ?: throw IllegalStateException("STRIPE_SECRET_KEY environment variable not set")

    val webhookSecret: String = System.getenv("STRIPE_WEBHOOK_SECRET")
        ?: throw IllegalStateException("STRIPE_WEBHOOK_SECRET environment variable not set")

    val publishableKey: String = System.getenv("STRIPE_PUBLISHABLE_KEY")
        ?: throw IllegalStateException("STRIPE_PUBLISHABLE_KEY environment variable not set")

    /**
     * Determines if running in test mode based on API key prefix
     * Test keys start with sk_test_ or pk_test_
     * Live keys start with sk_live_ or pk_live_
     */
    val isTestMode: Boolean = secretKey.startsWith("sk_test_")

    init {
        // Initialize Stripe SDK
        Stripe.apiKey = secretKey

        // Validate keys are consistent (all test or all live)
        validateKeyConsistency()

    }

    private fun validateKeyConsistency() {
        val secretIsTest = secretKey.startsWith("sk_test_")
        val publishableIsTest = publishableKey.startsWith("pk_test_")
        val webhookIsTest = webhookSecret.startsWith("whsec_")

        if (secretIsTest != publishableIsTest) {
            throw IllegalStateException(
                "Stripe key mismatch: Secret key and publishable key must both be test or live mode"
            )
        }

        // Validate key formats
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