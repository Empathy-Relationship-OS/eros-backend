package com.eros.wallet.routes

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.common.DateActivity
import com.eros.common.errors.BadRequestException
import com.eros.common.errors.ForbiddenException
import com.eros.common.plugins.configureExceptionHandling
import com.eros.wallet.models.PurchaseRequest
import com.eros.wallet.models.PurchaseResponse
import com.eros.wallet.models.SpendTokenRequest
import com.eros.wallet.models.TransactionHistory
import com.eros.wallet.models.TransactionHistoryResponse
import com.eros.wallet.models.TransactionResponse
import com.eros.wallet.models.TransactionType
import com.eros.wallet.models.WalletResponse
import com.eros.wallet.models.WalletWithPending
import com.eros.wallet.models.createTestPurchase
import com.eros.wallet.models.createTestWallet
import com.eros.wallet.models.transaction
import com.eros.wallet.services.PaymentService
import com.eros.wallet.services.WalletService
import com.google.firebase.auth.FirebaseToken
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
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

    private val mockPaymentService = mockk<PaymentService>()
    private val mockWalletService = mockk<WalletService>()

    // -------------------------------------------------------------------------
    // GET /wallet/balance
    // -------------------------------------------------------------------------

    @Nested
    inner class `GET balance` {

        @Test
        fun `returns 200 with wallet for authenticated user`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val fakeWallet = WalletWithPending(
                tokenBalance = 100.toBigDecimal(),
                pendingTokenBalance = 110.toBigDecimal(),
                lifetimeSpent = 50.toBigDecimal(),
                lifetimePurchased = 200.toBigDecimal()
            )
            coEvery { mockPaymentService.getBalance("test-user-id") } returns fakeWallet

            val response = client.get("/wallet/balance") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.body<WalletResponse>() // adjust to your actual DTO type
            assertEquals(110.toBigDecimal(), body.pendingBalance)
        }

        @Test
        fun `returns 401 when no auth token is provided`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/wallet/balance")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `returns correct balance values in response body`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val fakeWallet = WalletWithPending(
                tokenBalance = 500.toBigDecimal(),
                pendingTokenBalance = 750.toBigDecimal(),
                lifetimeSpent = 200.toBigDecimal(),
                lifetimePurchased = 1000.toBigDecimal()
            )
            coEvery { mockPaymentService.getBalance("test-user-id") } returns fakeWallet

            val response = client.get("/wallet/balance") {
                setAuthenticatedUser("test-user-id")
            }

            val body = response.body<WalletResponse>()
            assertEquals(500.toBigDecimal(), body.balance)
            assertEquals(750.toBigDecimal(), body.pendingBalance)
            assertEquals(200.toBigDecimal(), body.lifetimeSpent)
            assertEquals(1000.toBigDecimal(), body.lifetimePurchased)
        }

        @Test
        fun `returns correct balance for a different user`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userAWallet = WalletWithPending(100.toBigDecimal(), 100.toBigDecimal(), 0.toBigDecimal(), 100.toBigDecimal())
            val userBWallet = WalletWithPending(999.toBigDecimal(), 999.toBigDecimal(), 0.toBigDecimal(), 999.toBigDecimal())

            coEvery { mockPaymentService.getBalance("user-a") } returns userAWallet
            coEvery { mockPaymentService.getBalance("user-b") } returns userBWallet

            val responseA = client.get("/wallet/balance") { setAuthenticatedUser("user-a") }
            val responseB = client.get("/wallet/balance") { setAuthenticatedUser("user-b") }

            assertEquals(100.toBigDecimal(), responseA.body<WalletResponse>().balance)
            assertEquals(999.toBigDecimal(), responseB.body<WalletResponse>().balance)
        }

        @Test
        fun `returns 403 when user has wrong role`() = testApplication {
            setupTestApp(role = "GUEST")
            val client = configuredClient()

            val response = client.get("/wallet/balance") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    // -------------------------------------------------------------------------
    // GET /wallet/transactions
    // -------------------------------------------------------------------------

    @Nested
    inner class `GET transactions` {

        @Test
        fun `returns 200 with transaction list for valid params`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val fakeTransactions = TransactionHistory(listOf(transaction(),transaction(idempotencyKey = "test-2", transactionId = 2L)),2,false)
            coEvery {
                mockPaymentService.getTransactionHistory("test-user-id", 10, 0, null)
            } returns fakeTransactions

            val response = client.get("/wallet/transactions?limit=10&offset=0") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        @Test
        fun `returns 400 when limit is missing`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/wallet/transactions?offset=0") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `returns 400 when offset is negative`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/wallet/transactions?limit=10&offset=-1") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `returns 400 for an invalid transaction type`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/wallet/transactions?limit=10&offset=0&type=NONSENSE") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `returns correct transaction count in body`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val fakeTransactions = TransactionHistory(
                transactions = listOf(
                    transaction(),
                    transaction(idempotencyKey = "test-2", transactionId = 2L),
                    transaction(idempotencyKey = "test-3", transactionId = 3L)
                ),
                total = 3,
                hasMore = false
            )
            coEvery {
                mockPaymentService.getTransactionHistory("test-user-id", 10, 0, null)
            } returns fakeTransactions

            val response = client.get("/wallet/transactions?limit=10&offset=0") {
                setAuthenticatedUser("test-user-id")
            }

            val body = response.body<TransactionHistoryResponse>()
            assertEquals(3, body.transactions.size)
            assertEquals(3, body.total)
            assertEquals(false, body.hasMore)
        }

        @Test
        fun `returns 400 when limit exceeds maximum of 100`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/wallet/transactions?limit=101&offset=0") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `returns 400 when limit is zero`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/wallet/transactions?limit=0&offset=0") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `filters by valid transaction type`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val fakeTransactions = TransactionHistory(listOf(transaction()), 1, false)
            coEvery {
                mockPaymentService.getTransactionHistory("test-user-id", 10, 0, TransactionType.PURCHASE.name)
            } returns fakeTransactions

            val response = client.get("/wallet/transactions?limit=10&offset=0&type=PURCHASE") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        @Test
        fun `handles pagination with offset correctly`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val fakeTransactions = TransactionHistory(listOf(transaction()), 11, false)
            coEvery {
                mockPaymentService.getTransactionHistory("test-user-id", 10, 10, null)
            } returns fakeTransactions

            val response = client.get("/wallet/transactions?limit=10&offset=10") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        @Test
        fun `hasMore is true when further pages exist`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val fakeTransactions = TransactionHistory(listOf(transaction()), 50, hasMore = true)
            coEvery {
                mockPaymentService.getTransactionHistory("test-user-id", 10, 0, null)
            } returns fakeTransactions

            val response = client.get("/wallet/transactions?limit=10&offset=0") {
                setAuthenticatedUser("test-user-id")
            }

            val body = response.body<TransactionHistoryResponse>()
            assertEquals(true, body.hasMore)
        }

    }

    // -------------------------------------------------------------------------
    // POST /wallet/purchase
    // -------------------------------------------------------------------------

    @Nested
    inner class `POST purchase` {

        @Test
        fun `returns 201 with pending purchase for valid request`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val fakeRequest = PurchaseRequest(
                packageType = "STARTER",
                paymentMethodId = "pm_test_123",
                idempotencyKey = "key-abc",
                acceptedTerms = true
            )
            val fakePurchase = createTestPurchase()

            coEvery {
                mockPaymentService.purchaseTokens("test-user-id", fakeRequest)
            } returns fakePurchase

            val response = client.post("/wallet/purchase") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(fakeRequest)
            }

            assertEquals(HttpStatusCode.Created, response.status)
        }

        @Test
        fun `returns 401 when no auth token is provided`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.post("/wallet/purchase") {
                contentType(ContentType.Application.Json)
                setBody(PurchaseRequest("STARTER", "pm_test_123", "key-abc", true))
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `returns purchase details in response body`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val fakeRequest = PurchaseRequest(
                packageType = "STARTER",
                paymentMethodId = "pm_test_123",
                idempotencyKey = "key-abc",
                acceptedTerms = true
            )
            val fakePurchase = createTestPurchase(
                status = "pending",
                tokenAmount = 100.toBigDecimal(),
                amountPaid = 9.99.toBigDecimal()
            )
            coEvery { mockPaymentService.purchaseTokens("test-user-id", fakeRequest) } returns fakePurchase

            val response = client.post("/wallet/purchase") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(fakeRequest)
            }

            val body = response.body<PurchaseResponse>() // adjust to your actual DTO
            assertEquals("pending", body.status)
            assertEquals(100.toBigDecimal(), body.tokenAmount)
            assertEquals(9.99.toBigDecimal(), body.amount)
        }

        @Test
        fun `returns 400 when request body is malformed json`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.post("/wallet/purchase") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody("{invalid-json}")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `returns 400 when request body is missing required fields`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.post("/wallet/purchase") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody("""{ "packageType": "STARTER" }""")  // missing paymentMethodId, idempotencyKey etc.
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    // -------------------------------------------------------------------------
    // POST /wallet/spend
    // -------------------------------------------------------------------------

    @Nested
    inner class `POST spend` {

        @Test
        fun `returns 200 with transaction for valid spend request`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = SpendTokenRequest(relatedDateId = 1L, DateActivity.DINNER.name,"blah")
            val transaction = transaction() // replace with a real test fixture

            coEvery {
                mockPaymentService.spendToken("test-user-id", request)
            } returns transaction

            val response = client.post("/wallet/spend") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Created, response.status)
        }

        @Test
        fun `returns 401 when no auth token is provided`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = SpendTokenRequest(relatedDateId = 1L, DateActivity.DINNER.name,"blah")
            val response = client.post("/wallet/spend") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
        @Test
        fun `returns transaction details in response body`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val fakeRequest = SpendTokenRequest(relatedDateId = 1L, DateActivity.DINNER.name, "blah")
            val fakeTransaction = transaction(transactionId = 99L)

            coEvery { mockPaymentService.spendToken("test-user-id", fakeRequest) } returns fakeTransaction

            val response = client.post("/wallet/spend") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(fakeRequest)
            }

            val body = response.body<TransactionResponse>() // adjust to your actual DTO
            assertEquals(99L, body.transactionId)
        }

        @Test
        fun `returns 400 when service throws BadRequestException`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val fakeRequest = SpendTokenRequest(relatedDateId = 1L, DateActivity.DINNER.name, "blah")
            coEvery {
                mockPaymentService.spendToken("test-user-id", fakeRequest)
            } throws BadRequestException("Insufficient balance")

            val response = client.post("/wallet/spend") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(fakeRequest)
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    // -------------------------------------------------------------------------
    // POST /wallet/admin/ensure/{userId}
    // -------------------------------------------------------------------------

    @Nested
    inner class `POST admin ensure wallet` {

        @Test
        fun `test simple endpoint access`() = testApplication {
            setupAdminTestApp()
            val client = configuredClient()

            val newWallet = createTestWallet(userId = "test-id")
            coEvery { mockWalletService.createWallet("test-id", "GBP") } returns newWallet

            val response = client.post("/wallet/admin/ensure/test-id") {
                setAuthenticatedUser("admin-id")
            }

            val body = response.bodyAsText()
            assertEquals(
                HttpStatusCode.Created,
                response.status,
                "Expected 201 Created but got ${response.status}. Response body: $body"
            )
        }

        @Test
        fun `returns 201 when wallet is created successfully`() = testApplication {
            setupAdminTestApp()
            val client = configuredClient()

            val newWallet = createTestWallet(userId = "target-user-id")
            coEvery { mockWalletService.createWallet("target-user-id", "GBP") } returns newWallet

            val response = client.post("/wallet/admin/ensure/target-user-id") {
                setAuthenticatedUser("admin-user-id")
            }

            val responseBody = response.bodyAsText()
            assertEquals(HttpStatusCode.Created, response.status, "Expected 201 but got ${response.status}. Body: $responseBody")
            val body = response.body<WalletResponse>()
            assertEquals(newWallet.tokenBalance, body.balance)
            assertEquals(newWallet.currency, body.currency)
        }

        @Test
        fun `returns 200 when wallet already exists`() = testApplication {
            setupAdminTestApp()
            val client = configuredClient()

            val existingWallet = createTestWallet(userId = "target-user-id", tokenBalance = 100.toBigDecimal())
            coEvery { mockWalletService.createWallet("target-user-id", "GBP") } throws ForbiddenException("Wallet already exists")
            coEvery { mockWalletService.getWallet("target-user-id") } returns existingWallet

            val response = client.post("/wallet/admin/ensure/target-user-id") {
                setAuthenticatedUser("admin-user-id")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.body<WalletResponse>()
            assertEquals(100.toBigDecimal(), body.balance)
        }

        @Test
        fun `creates wallet with custom currency when specified`() = testApplication {
            setupAdminTestApp()
            val client = configuredClient()

            val newWallet = createTestWallet(userId = "target-user-id", currency = "USD")
            coEvery { mockWalletService.createWallet("target-user-id", "USD") } returns newWallet

            val response = client.post("/wallet/admin/ensure/target-user-id?currency=USD") {
                setAuthenticatedUser("admin-user-id")
            }

            assertEquals(HttpStatusCode.Created, response.status)
            val body = response.body<WalletResponse>()
            assertEquals("USD", body.currency)
        }

        @Test
        fun `returns 400 when currency format is invalid`() = testApplication {
            setupAdminTestApp()
            val client = configuredClient()

            val response = client.post("/wallet/admin/ensure/target-user-id?currency=invalid") {
                setAuthenticatedUser("admin-user-id")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `returns 400 when currency is not uppercase`() = testApplication {
            setupAdminTestApp()
            val client = configuredClient()

            val response = client.post("/wallet/admin/ensure/target-user-id?currency=usd") {
                setAuthenticatedUser("admin-user-id")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `returns 403 when user is not admin or employee`() = testApplication {
            setupAdminTestApp(role = "USER")
            val client = configuredClient()

            val response = client.post("/wallet/admin/ensure/target-user-id") {
                setAuthenticatedUser("regular-user-id")
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

        @Test
        fun `returns 401 when not authenticated`() = testApplication {
            setupAdminTestApp()
            val client = configuredClient()

            val response = client.post("/wallet/admin/ensure/target-user-id")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `allows EMPLOYEE role to create wallets`() = testApplication {
            setupAdminTestApp(role = "EMPLOYEE")
            val client = configuredClient()

            val newWallet = createTestWallet(userId = "target-user-id")
            coEvery { mockWalletService.createWallet("target-user-id", "GBP") } returns newWallet

            val response = client.post("/wallet/admin/ensure/target-user-id") {
                setAuthenticatedUser("employee-user-id")
            }

            assertEquals(HttpStatusCode.Created, response.status)
        }

        @Test
        fun `returns 404 when wallet retrieval fails after existence check`() = testApplication {
            setupAdminTestApp()
            val client = configuredClient()

            coEvery { mockWalletService.createWallet("target-user-id", "GBP") } throws ForbiddenException("Wallet already exists")
            coEvery { mockWalletService.getWallet("target-user-id") } returns null

            val response = client.post("/wallet/admin/ensure/target-user-id") {
                setAuthenticatedUser("admin-user-id")
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

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
                    paymentRoutes(mockPaymentService)
                }
            }
        }
    }

    private fun ApplicationTestBuilder.setupAdminTestApp(role: String = "ADMIN") {
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
                            role = role  // Use the role parameter from setupAdminTestApp
                        )
                    }
                }
            }

            routing {
                authenticate("firebase-auth") {
                    walletAdminRoutes(mockWalletService)
                }
            }
        }
    }

    private fun HttpRequestBuilder.setAuthenticatedUser(userId: String) {
        // For admin tests, ignore the role parameter and just use simple format
        header(HttpHeaders.Authorization, "Bearer user-$userId")
    }

}