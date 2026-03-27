package com.eros.wallet.routes

import com.eros.wallet.stripe.InvalidWebhookSignatureException
import com.eros.wallet.stripe.StripeWebhookHandler
import com.eros.wallet.stripe.WebhookResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("StripeWebhookRoute")


fun Route.webhookRoute(webhookHandler: StripeWebhookHandler) {

    route("/webhooks") {
        post("/stripe") {
            val payload = call.receiveText()
            val signature = call.request.headers["Stripe-Signature"]
            if (signature.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing Stripe-Signature header")
                return@post
            }

            try {
                val result = webhookHandler.handleWebhook(payload, signature)

                // Map the service result to HTTP responses
                when (result) {
                    is WebhookResult.Success -> {
                        call.respond(HttpStatusCode.OK, "Tokens credited.")
                    }

                    is WebhookResult.Ignored -> {
                        call.respond(HttpStatusCode.OK, "Event ignored.")
                    }

                    is WebhookResult.Failure -> {
                        call.respond(HttpStatusCode.OK, "Transaction failed.")
                    }

                    is WebhookResult.Cancelled -> {
                        call.respond(HttpStatusCode.OK, "Transaction cancelled.")
                    }

                    is WebhookResult.Error -> {
                        logger.error("Webhook error: ${result.message}")
                        call.respond(HttpStatusCode.InternalServerError, result.message)
                    }
                }

            } catch (e: InvalidWebhookSignatureException) {
                logger.error("Invalid signature", e)
                call.respond(HttpStatusCode.BadRequest, "Invalid signature")
            } catch (e: Exception) {
                logger.error("Unexpected error processing Stripe webhook", e)
                call.respond(HttpStatusCode.InternalServerError, "Internal server error")
            }
        }
    }

}