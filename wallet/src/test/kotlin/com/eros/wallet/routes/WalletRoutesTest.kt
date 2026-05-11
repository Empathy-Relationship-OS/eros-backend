package com.eros.wallet.routes

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.common.DateActivity
import com.eros.common.errors.BadRequestException
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
import com.eros.wallet.models.transaction
import com.eros.wallet.services.PaymentService
import com.google.firebase.auth.FirebaseToken
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class WalletRoutesTest {

    private val mockPaymentService = mockk<PaymentService>()

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

    private fun HttpRequestBuilder.setAuthenticatedUser(userId: String) {
        header(HttpHeaders.Authorization, "Bearer user-$userId")
    }

}