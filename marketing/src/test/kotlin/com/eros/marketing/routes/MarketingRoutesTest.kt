package com.eros.marketing.routes

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.common.plugins.configureExceptionHandling
import com.eros.marketing.models.MarketingPreferenceResponse
import com.eros.marketing.models.PaginatedConsentedUsersResponse
import com.eros.marketing.models.UserMarketingConsent
import com.eros.marketing.service.MarketingPreferenceService
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class MarketingRoutesTest {

    private val mockMarketingService = mockk<MarketingPreferenceService>()

    // -------------------------------------------------------------------------
    // GET /marketing/preference - Get current user's marketing preference
    // -------------------------------------------------------------------------

    @Nested
    inner class `GET marketing preference` {

        @Test
        fun `should return user's marketing preference when record exists`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val consent = UserMarketingConsent(
                userId = userId,
                marketingConsent = true,
                createdAt = Instant.parse("2024-01-15T10:00:00Z"),
                updatedAt = Instant.parse("2024-01-15T10:00:00Z")
            )

            coEvery { mockMarketingService.getMarketingPreference(userId) } returns consent

            val response = client.get("/marketing/preference") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val result = response.body<MarketingPreferenceResponse>()
            assertEquals(userId, result.userId)
            assertTrue(result.marketingConsent)
            coVerify { mockMarketingService.getMarketingPreference(userId) }
        }

        @Test
        fun `should return default preference when no record exists`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val consent = UserMarketingConsent(
                userId = userId,
                marketingConsent = false,
                createdAt = Instant.parse("2024-01-15T10:00:00Z"),
                updatedAt = Instant.parse("2024-01-15T10:00:00Z")
            )

            coEvery { mockMarketingService.getMarketingPreference(userId) } returns consent

            val response = client.get("/marketing/preference") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val result = response.body<MarketingPreferenceResponse>()
            assertEquals(userId, result.userId)
            assertFalse(result.marketingConsent)
        }

        @Test
        fun `should return 401 when not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/marketing/preference")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    // -------------------------------------------------------------------------
    // POST /marketing/preference - Create marketing preference
    // -------------------------------------------------------------------------

    @Nested
    inner class `POST marketing preference` {

        @Test
        fun `should create marketing preference with true value`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val consent = UserMarketingConsent(
                userId = userId,
                marketingConsent = true,
                createdAt = Instant.parse("2024-01-15T10:00:00Z"),
                updatedAt = Instant.parse("2024-01-15T10:00:00Z")
            )

            coEvery {
                mockMarketingService.createMarketingPreference(userId, userId, true)
            } returns consent

            val response = client.post("/marketing/preference") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody("""{"marketingConsent": true}""")
            }

            assertEquals(HttpStatusCode.Created, response.status)
            val result = response.body<MarketingPreferenceResponse>()
            assertEquals(userId, result.userId)
            assertTrue(result.marketingConsent)
            coVerify { mockMarketingService.createMarketingPreference(userId, userId, true) }
        }

        @Test
        fun `should create marketing preference with false value`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val consent = UserMarketingConsent(
                userId = userId,
                marketingConsent = false,
                createdAt = Instant.parse("2024-01-15T10:00:00Z"),
                updatedAt = Instant.parse("2024-01-15T10:00:00Z")
            )

            coEvery {
                mockMarketingService.createMarketingPreference(userId, userId, false)
            } returns consent

            val response = client.post("/marketing/preference") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody("""{"marketingConsent": false}""")
            }

            assertEquals(HttpStatusCode.Created, response.status)
            val result = response.body<MarketingPreferenceResponse>()
            assertEquals(userId, result.userId)
            assertFalse(result.marketingConsent)
        }

        @Test
        fun `should return 401 when not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.post("/marketing/preference") {
                contentType(ContentType.Application.Json)
                setBody("""{"marketingConsent": true}""")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should map ForbiddenException to 403`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"

            coEvery {
                mockMarketingService.createMarketingPreference(userId, userId, true)
            } throws com.eros.common.errors.ForbiddenException("You can only create your own marketing preferences")

            val response = client.post("/marketing/preference") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody("""{"marketingConsent": true}""")
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    // -------------------------------------------------------------------------
    // PUT /marketing/preference - Update marketing preference
    // -------------------------------------------------------------------------

    @Nested
    inner class `PUT marketing preference` {

        @Test
        fun `should update marketing preference to true`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val consent = UserMarketingConsent(
                userId = userId,
                marketingConsent = true,
                createdAt = Instant.parse("2024-01-15T10:00:00Z"),
                updatedAt = Instant.parse("2024-01-15T11:00:00Z")
            )

            coEvery {
                mockMarketingService.updateMarketingPreference(userId, userId, true)
            } returns consent

            val response = client.put("/marketing/preference") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody("""{"marketingConsent": true}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val result = response.body<MarketingPreferenceResponse>()
            assertEquals(userId, result.userId)
            assertTrue(result.marketingConsent)
            coVerify { mockMarketingService.updateMarketingPreference(userId, userId, true) }
        }

        @Test
        fun `should update marketing preference to false`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val consent = UserMarketingConsent(
                userId = userId,
                marketingConsent = false,
                createdAt = Instant.parse("2024-01-15T10:00:00Z"),
                updatedAt = Instant.parse("2024-01-15T11:00:00Z")
            )

            coEvery {
                mockMarketingService.updateMarketingPreference(userId, userId, false)
            } returns consent

            val response = client.put("/marketing/preference") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody("""{"marketingConsent": false}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val result = response.body<MarketingPreferenceResponse>()
            assertEquals(userId, result.userId)
            assertFalse(result.marketingConsent)
        }

        @Test
        fun `should return 401 when not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.put("/marketing/preference") {
                contentType(ContentType.Application.Json)
                setBody("""{"marketingConsent": true}""")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should map ForbiddenException to 403`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"

            coEvery {
                mockMarketingService.updateMarketingPreference(userId, userId, true)
            } throws com.eros.common.errors.ForbiddenException("You can only update your own marketing preferences")

            val response = client.put("/marketing/preference") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody("""{"marketingConsent": true}""")
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    // -------------------------------------------------------------------------
    // GET /marketing/admin/preference/{userId} - Get user's preference (Admin only)
    // -------------------------------------------------------------------------

    @Nested
    inner class `GET marketing admin preference userId` {

        @Test
        fun `should return user's preference when admin requests it`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val adminId = "admin1"
            val targetUserId = "user123"
            val consent = UserMarketingConsent(
                userId = targetUserId,
                marketingConsent = true,
                createdAt = Instant.parse("2024-01-15T10:00:00Z"),
                updatedAt = Instant.parse("2024-01-15T10:00:00Z")
            )

            coEvery { mockMarketingService.getMarketingPreference(targetUserId) } returns consent

            val response = client.get("/marketing/admin/preference/$targetUserId") {
                setAuthenticatedUser(adminId, role = "ADMIN")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val result = response.body<MarketingPreferenceResponse>()
            assertEquals(targetUserId, result.userId)
            assertTrue(result.marketingConsent)
            coVerify { mockMarketingService.getMarketingPreference(targetUserId) }
        }

        @Test
        fun `should return default when user has no marketing preference record`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val adminId = "admin1"
            val targetUserId = "nonexistent"
            val defaultConsent = UserMarketingConsent(
                userId = targetUserId,
                marketingConsent = false,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            coEvery { mockMarketingService.getMarketingPreference(targetUserId) } returns defaultConsent

            val response = client.get("/marketing/admin/preference/$targetUserId") {
                setAuthenticatedUser(adminId, role = "ADMIN")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val result = response.body<MarketingPreferenceResponse>()
            assertEquals(targetUserId, result.userId)
            assertFalse(result.marketingConsent)
            coVerify { mockMarketingService.getMarketingPreference(targetUserId) }
        }

        @Test
        fun `should return 403 when regular user tries to access admin endpoint`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"

            val response = client.get("/marketing/admin/preference/user456") {
                setAuthenticatedUser(userId, role = "USER")
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /marketing/admin/preference/{userId} - Delete preference (Admin only)
    // -------------------------------------------------------------------------

    @Nested
    inner class `DELETE marketing admin preference userId` {

        @Test
        fun `should delete user's preference when admin requests it`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val adminId = "admin1"
            val targetUserId = "user123"

            coEvery { mockMarketingService.deleteMarketingPreference(targetUserId) } returns true

            val response = client.delete("/marketing/admin/preference/$targetUserId") {
                setAuthenticatedUser(adminId, role = "ADMIN")
            }

            assertEquals(HttpStatusCode.NoContent, response.status)
            coVerify { mockMarketingService.deleteMarketingPreference(targetUserId) }
        }

        @Test
        fun `should return 204 when deleting non-existent record`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val adminId = "admin1"
            val targetUserId = "nonexistent"

            coEvery { mockMarketingService.deleteMarketingPreference(targetUserId) } returns false

            val response = client.delete("/marketing/admin/preference/$targetUserId") {
                setAuthenticatedUser(adminId, role = "ADMIN")
            }

            assertEquals(HttpStatusCode.NoContent, response.status)
        }

        @Test
        fun `should return 403 when regular user tries to delete`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"

            val response = client.delete("/marketing/admin/preference/user456") {
                setAuthenticatedUser(userId, role = "USER")
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    // -------------------------------------------------------------------------
    // GET /marketing/admin/consented - Get all consented users (Admin only)
    // -------------------------------------------------------------------------

    @Nested
    inner class `GET marketing admin consented` {

        @Test
        fun `should return list of consented users when admin requests`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val adminId = "admin1"
            val consentedUsers = listOf(
                UserMarketingConsent(
                    userId = "user1",
                    marketingConsent = true,
                    createdAt = Instant.parse("2024-01-15T10:00:00Z"),
                    updatedAt = Instant.parse("2024-01-15T10:00:00Z")
                ),
                UserMarketingConsent(
                    userId = "user2",
                    marketingConsent = true,
                    createdAt = Instant.parse("2024-01-15T10:00:00Z"),
                    updatedAt = Instant.parse("2024-01-15T10:00:00Z")
                )
            )

            coEvery { mockMarketingService.getAllConsentedUsers(any(), any()) } returns consentedUsers
            coEvery { mockMarketingService.getConsentedUsersCount() } returns 2

            val response = client.get("/marketing/admin/consented") {
                setAuthenticatedUser(adminId, role = "ADMIN")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val result = response.body<PaginatedConsentedUsersResponse>()
            assertEquals(2, result.data.size)
            assertEquals(2, result.total)
            assertTrue(result.data.all { it.marketingConsent })
        }

        @Test
        fun `should return empty list when no users consented`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val adminId = "admin1"

            coEvery { mockMarketingService.getAllConsentedUsers(any(), any()) } returns emptyList()
            coEvery { mockMarketingService.getConsentedUsersCount() } returns 0

            val response = client.get("/marketing/admin/consented") {
                setAuthenticatedUser(adminId, role = "ADMIN")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val result = response.body<PaginatedConsentedUsersResponse>()
            assertTrue(result.data.isEmpty())
            assertEquals(0, result.total)
        }

        @Test
        fun `should return 403 when regular user tries to access`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"

            val response = client.get("/marketing/admin/consented") {
                setAuthenticatedUser(userId, role = "USER")
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }
    }

    // -------------------------------------------------------------------------
    // Helper functions
    // -------------------------------------------------------------------------

    /**
     * Creates a configured HTTP client with JSON serialization support.
     */
    private fun ApplicationTestBuilder.configuredClient() = createClient {
        install(ClientContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    /**
     * Sets up the test application with authentication and routing.
     */
    private fun ApplicationTestBuilder.setupTestApp() {
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
                        // Token format: "user-{userId}-role-{role}"
                        // Use regex to safely parse tokens even if userId contains "-role-" or starts with "user-"
                        val tokenPattern = Regex("^user-(.+?)-role-(.+)$")
                        val matchResult = tokenPattern.matchEntire(credential.token)

                        val userId = matchResult?.groupValues?.get(1)
                            ?: credential.token.removePrefix("user-") // Fallback for tokens without role
                        val role = matchResult?.groupValues?.get(2) ?: "USER"

                        val mockToken = mockk<com.google.firebase.auth.FirebaseToken>(relaxed = true) {
                            every { uid } returns userId
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
                    marketingRoutes(mockMarketingService)
                }
            }
        }
    }

    /**
     * Sets the Authorization header with a mock user token.
     */
    private fun HttpRequestBuilder.setAuthenticatedUser(userId: String, role: String = "USER") {
        header(HttpHeaders.Authorization, "Bearer user-$userId-role-$role")
    }
}
