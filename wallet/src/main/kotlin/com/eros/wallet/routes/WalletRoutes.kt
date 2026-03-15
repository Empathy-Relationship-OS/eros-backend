package com.eros.wallet.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import com.eros.wallet.services.WalletService
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import com.eros.auth.extensions.requireRoles
import com.eros.common.DateActivity
import com.eros.common.errors.BadRequestException
import com.eros.wallet.models.PurchaseRequest
import com.eros.wallet.models.SpendTokenRequest
import com.eros.wallet.models.toDTO
import com.stripe.net.Webhook
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post


fun Route.walletRoutes(walletService: WalletService) {

    route("/wallet") {
        requireRoles("USER", "ADMIN", "EMPLOYEE")

        get("/balance") {
            // Get user.
            val principal = call.requireFirebasePrincipal()

            // Get users wallet.
            val walletWithPending = walletService.getBalance(principal.uid)

            // Return wallet with pending dto.
            call.respond(HttpStatusCode.OK, walletWithPending.toDTO())

        }

        get("/transactions"){
            // Get the params from the query.
            val limit = call.request.queryParameters["limit"]?.toInt() ?: throw BadRequestException("Limit requires an integer")
            val offset = call.request.queryParameters["offset"]?.toInt() ?: throw BadRequestException("Offset requires an integer")
            val type = call.request.queryParameters["type"]

            // Get the user id.
            val principal = call.requireFirebasePrincipal()

            // Find the transactions.
            val transactions = walletService.getTransactionHistory(principal.uid, limit, offset, type)

            // Return to the user.
            call.respond(HttpStatusCode.OK, transactions.toDTO())

        }

        post("/purchase"){

            // Get user and request.
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<PurchaseRequest>()

            // Create purchase transaction and return the purchase.
            val purchase = walletService.createPurchase(principal.uid, request)

            call.respond(HttpStatusCode.Created, purchase.toDTO())

        }

        post("/refund"){

        }

        /**
         * Route for a user to spend their tokens.
         */
        post("/spend"){

            // Get user and request.
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<SpendTokenRequest>()

            // Get the amount of tokens for the date.
            val amount = DateActivity.valueOf(request.activity).tokenCost.toBigDecimal()

            // Spend the tokens and create a transaction.
            val transaction = walletService.spendTokens(
                userId = principal.uid,
                amount = amount,
                dateId = request.relatedDateId,
                description = "Activity: ${request.activity}",
                idempotencyKey = request.idempotencyKey,
                metadata = mapOf(
                    "activity" to request.activity,
                    "relatedDateId" to request.relatedDateId.toString()
                )
            )

            // Return the transaction to the user.
            call.respond(HttpStatusCode.OK,transaction.toDTO())

        }

    }

    route("/webhooks"){
        post("/stripe") {
            val payload = call.receiveText()
            val sigHeader = call.request.headers["Stripe-Signature"]

            val endpointSecret = System.getenv("STRIPE_WEBHOOK_SECRET")

            val event = Webhook.constructEvent(
                payload,
                sigHeader,
                endpointSecret
            )

            when (event.type) {

                "payment_intent.succeeded" -> {
                    val paymentIntent = event.dataObjectDeserializer
                        .`object`
                        .get()

                    println("Payment succeeded")
                }
            }

            call.respond(HttpStatusCode.OK)
        }

    }


}