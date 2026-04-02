package com.eros.wallet.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.wallet.models.PurchaseRequest
import com.eros.wallet.models.RefundTokenRequest
import com.eros.wallet.models.SpendTokenRequest
import com.eros.wallet.models.TransactionType
import com.eros.wallet.models.toDTO
import com.eros.wallet.services.PaymentService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post

fun Route.paymentRoutes(paymentService: PaymentService) {

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
        get("/transactions") {
            // Get the params from the query.
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()
                ?.takeIf { it in 1..100 }
                ?: throw BadRequestException("Limit requires a valid integer between 1 and 100")
            val offset = call.request.queryParameters["offset"]?.toLongOrNull()
                ?.takeIf { it >= 0 }
                ?: throw BadRequestException("Offset requires a non-negative integer")
            val type = call.request.queryParameters["type"]
                ?.let { t ->
                    TransactionType.entries.find { it.name == t }
                        ?: throw BadRequestException("Invalid transaction type")
                }

            // Get the user id.
            val principal = call.requireFirebasePrincipal()

            // Find the transactions.
            val transactions = paymentService.getTransactionHistory(principal.uid, limit, offset, type?.name)

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
        post("/purchase") {

            // Get user and request.
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<PurchaseRequest>()
            val userId = principal.uid

            // Create purchase intent, create pending
            val purchase = paymentService.purchaseTokens(userId, request)

            // Send the pending purchase back to the client.
            call.respond(HttpStatusCode.Created, purchase.toDTO())

        }


        /**
         * Route for refunding token purchases.
         */
        post("/refund") {
            // Get user and request.
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<RefundTokenRequest>()

            val refund = paymentService.refundTokens(principal.uid, request)

            call.respond(HttpStatusCode.Created, refund.toDTO())

        }

        /**
         * Route for a user to spend their tokens.
         */
        post("/spend") {

            // Get user and request.
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<SpendTokenRequest>()

            // Spend the tokens and create a transaction.
            val transaction = paymentService.spendToken(principal.uid, request)

            // Return the transaction to the user.
            call.respond(HttpStatusCode.OK, transaction.toDTO())

        }

    }
}