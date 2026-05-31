package com.eros.wallet.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.common.errors.ForbiddenException
import com.eros.common.errors.NotFoundException
import com.eros.wallet.models.PurchaseRequest
import com.eros.wallet.models.RefundTokenRequest
import com.eros.wallet.models.SpendTokenRequest
import com.eros.wallet.models.TransactionType
import com.eros.wallet.models.WalletResponse
import com.eros.wallet.models.toDTO
import com.eros.wallet.services.PaymentService
import com.eros.wallet.services.WalletService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.math.BigDecimal

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
            call.respond(HttpStatusCode.Created, transaction.toDTO())

        }

    }
}

/**
 * Admin routes for wallet management.
 *
 * These routes are restricted to ADMIN and EMPLOYEE roles only.
 *
 * @param walletService Service for wallet operations
 */
fun Route.walletAdminRoutes(walletService: WalletService) {
    route("/wallet/admin") {
        requireRoles("ADMIN", "EMPLOYEE")

        /**
         * POST /wallet/admin/ensure/{userId}
         *
         * Ensures a wallet exists for the specified user.
         * If the wallet already exists, returns the existing wallet.
         * If the wallet doesn't exist, creates it with the specified currency (defaults to GBP).
         *
         * This is an idempotent operation - safe to call multiple times.
         *
         * Use cases:
         * - Repair missing wallets due to failed creation during signup
         * - Manually create wallets for users created before wallet feature was implemented
         * - Administrative wallet management
         *
         * Request Headers:
         * - Authorization: Bearer <firebase-id-token> (must have ADMIN or EMPLOYEE role)
         *
         * Query Parameters:
         * - currency: Optional currency code (default: GBP)
         *   TODO: Implement location-based currency determination
         *
         * Response: WalletResponse JSON
         *
         * Status Codes:
         * - 200 OK: Wallet already existed, returned existing wallet
         * - 201 Created: New wallet was created
         * - 400 Bad Request: Invalid userId or currency
         * - 403 Forbidden: User doesn't have ADMIN or EMPLOYEE role
         * - 404 Not Found: User profile doesn't exist
         */
        post("/ensure/{userId}") {
            val userId = call.parameters["userId"]
                ?: throw BadRequestException("userId path parameter is required")

            val currency = call.request.queryParameters["currency"] ?: "GBP"

            // Validate currency format (basic validation)
            if (currency.length != 3 || currency != currency.uppercase()) {
                throw BadRequestException("Currency must be a 3-letter uppercase code (e.g., GBP, USD, EUR)")
            }

            val (wallet, wasCreated) = try {
                // Try to create the wallet
                val newWallet = walletService.createWallet(userId, currency)
                call.application.log.info("Admin created wallet for user $userId with currency $currency by ${call.requireFirebasePrincipal().uid}")
                newWallet to true
            } catch (_: ForbiddenException) {
                // Wallet already exists, fetch it
                val existingWallet = walletService.getWallet(userId)
                    ?: throw NotFoundException("User $userId exists but wallet retrieval failed")
                call.application.log.info("Admin requested wallet for user $userId - wallet already exists")
                existingWallet to false
            }

            // Return appropriate status code
            val statusCode = if (wasCreated) HttpStatusCode.Created else HttpStatusCode.OK

            // Return wallet as WalletResponse DTO
            val response = WalletResponse(
                balance = wallet.tokenBalance,
                pendingBalance = BigDecimal.ZERO, // No pending transactions for newly created/retrieved wallet
                lifetimeSpent = wallet.lifetimeSpent,
                lifetimePurchased = wallet.lifetimePurchased,
                currency = wallet.currency
            )

            call.respond(statusCode, response)
        }
    }
}