package com.eros.wallet.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.wallet.models.PurchaseRequest
import com.eros.wallet.models.SpendTokenRequest
import com.eros.wallet.models.toDTO
import com.eros.wallet.services.PaymentService
import com.eros.wallet.stripe.InvalidWebhookSignatureException
import com.eros.wallet.stripe.StripeWebhookHandler
import com.eros.wallet.stripe.WebhookResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post

//todo - Rename to payment routes.
fun Route.walletRoutes(paymentService: PaymentService, webhookHandler: StripeWebhookHandler) {

    route("/wallet") {
        requireRoles("USER", "ADMIN", "EMPLOYEE")

        /**
         * Simple retrieval of a users balance with pending balance.
         */
        get("/balance") {
            // Get user.
            val principal = call.requireFirebasePrincipal()

            // Get users wallet.
            val walletWithPending = paymentService.getBalance(principal.uid)

            // Return wallet with pending dto.
            call.respond(HttpStatusCode.OK, walletWithPending.toDTO())

        }

        /**
         * Simple retrieval of a users transaction history with pagination.
         */
        get("/transactions"){
            // Get the params from the query.
            val limit = call.request.queryParameters["limit"]?.toInt() ?: throw BadRequestException("Limit requires an integer")
            val offset = call.request.queryParameters["offset"]?.toInt() ?: throw BadRequestException("Offset requires an integer")
            val type = call.request.queryParameters["type"]

            // Get the user id.
            val principal = call.requireFirebasePrincipal()

            // Find the transactions.
            val transactions = paymentService.getTransactionHistory(principal.uid, limit, offset, type)

            // Return to the user.
            call.respond(HttpStatusCode.OK, transactions.toDTO())

        }


        /**
         * Route for allow a user to purchase a bundle of tokens.
         *
         * Payload: [PurchaseRequest]
         * packageType is the name of the package to be purchased by the user.
         * paymentMethodId is the
         * idempotencyKey is the client generated key relating to this unique action.
         */
        post("/purchase"){

            // Get user and request.
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<PurchaseRequest>()
            val userId = principal.uid

            // Create purchase intent, create pending
            val purchase = paymentService.purchaseTokens(userId, request)

            // Send the pending purchase back to the client.
            call.respond(HttpStatusCode.Created, purchase.toDTO())

        }

        //todo:
        post("/refund"){

        }

        /**
         * Route for a user to spend their tokens.
         */
        post("/spend"){

            // Get user and request.
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<SpendTokenRequest>()

            // Spend the tokens and create a transaction.
            val transaction = paymentService.spendToken(principal.uid, request)

            // Return the transaction to the user.
            call.respond(HttpStatusCode.OK,transaction.toDTO())

        }

    }

    route("/webhooks") {
        post("/stripe") {
            val payload = call.receiveText()
            val signature = call.request.headers["Stripe-Signature"] ?: ""

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
                        call.respond(HttpStatusCode.InternalServerError, result.message)
                    }
                }

            } catch (_: InvalidWebhookSignatureException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid signature")
            } catch (_: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Server error")
            }
        }
    }


}