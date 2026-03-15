package com.eros.wallet.routes

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.common.plugins.configureExceptionHandling
import com.eros.users.models.createTestUser
import com.eros.wallet.models.WalletResponse
import com.eros.wallet.models.WalletWithPending
import com.eros.wallet.models.createTestWallet
import com.eros.wallet.services.TransactionService
import com.eros.wallet.services.WalletService
import com.google.firebase.auth.FirebaseToken
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation


class WalletRoutesTest {

    private val mockWalletService = mockk<WalletService>()
    private val mockTransactionService = mockk<TransactionService>()

    @Nested
    inner class `GET Wallet` {

        @Test
        fun `successfully retrieve a users balance`() = testApplication {
            setupTestApp("USER")
            val client = configuredClient()

            val user = createTestUser(userId = "test-user-id")
            val wallet = createTestWallet()

            val walletWithPending = WalletWithPending(
                wallet.tokenBalance,
                wallet.tokenBalance + 10.toBigDecimal(),
                lifetimeSpent = wallet.lifetimeSpent,
                wallet.lifetimePurchased
            )

            coEvery { mockWalletService.getBalance(user.userId) } returns walletWithPending

            val response = client.get("/wallet/balance"){
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.OK, response.status)

            val returnedWallet = response.body<WalletResponse>()

            assertEquals(wallet.tokenBalance + 10.toBigDecimal(), returnedWallet.pendingBalance)

        }
    }



    // Helper Functions

    /**
     * Creates a configured HTTP client for tests with JSON content negotiation.
     * Each test should create its own client instance to ensure proper serialization.
     */
    private fun ApplicationTestBuilder.configuredClient() = createClient {
        install(ClientContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    private fun ApplicationTestBuilder.setupTestApp(role : String = "USER") {
        application {
            configureExceptionHandling()
            // Install server-side content negotiation
            install(ServerContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }

            // Install authentication
            install(Authentication) {
                bearer("firebase-auth") {
                    realm = "test-realm"
                    authenticate { credential ->
                        // Return mock principal based on credential token
                        // Token format: "user-{userId}"
                        val userId = credential.token.removePrefix("user-")
                        val mockToken = mockk<FirebaseToken>(relaxed = true) {
                            coEvery { uid } returns userId
                        }
                        FirebaseUserPrincipal(
                            uid = userId,
                            email = "$userId@example.com",
                            phoneNumber = null,
                            emailVerified = true,
                            token = mockToken,
                            role = role
                        )
                    }
                }
            }

            routing {
                authenticate("firebase-auth") {
                    walletRoutes(mockWalletService)
                }
            }
        }
    }

    private fun HttpRequestBuilder.setAuthenticatedUser(userId: String) {
        header(HttpHeaders.Authorization, "Bearer user-$userId")
    }

}