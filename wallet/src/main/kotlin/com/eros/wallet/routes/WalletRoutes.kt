package com.eros.wallet.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import com.eros.wallet.services.WalletService
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.common.errors.NotFoundException
import com.eros.wallet.models.PurchaseRequest
import com.eros.wallet.models.toDTO
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
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

            val limit = call.request.queryParameters["limit"]?.toInt() ?: throw BadRequestException("Limit requires an integer")
            val offset = call.request.queryParameters["offset"]?.toInt() ?: throw BadRequestException("Offset requires an integer")
            val type = call.request.queryParameters["type"]

            val principal = call.requireFirebasePrincipal()

            val transactions = walletService.getTransactionHistory(principal.uid, limit, offset, type)

            call.respond(HttpStatusCode.OK, transactions.toDTO())

        }

        post("/purchase"){

            // Get user and request.
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<PurchaseRequest>()

            val transaction = walletService.createPurchase(principal.uid, request)

            call.respond(HttpStatusCode.Created, transaction.toDTO())

        }

    }

}