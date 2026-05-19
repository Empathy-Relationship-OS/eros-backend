package com.eros

import com.eros.wallet.stripe.StripeConfig
import io.ktor.server.application.Application

fun Application.configureStripe() {

    StripeConfig.initialize(environment.config)

}