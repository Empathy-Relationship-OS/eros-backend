package com.eros.matching.routes

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.common.plugins.configureExceptionHandling
import com.eros.matching.models.DailyBatchLimitError
import com.eros.matching.models.DailyBatchResponse
import com.eros.matching.models.UserMatchProfile
import com.eros.matching.service.DailyBatchLimitExceededException
import com.eros.matching.service.MatchService
import com.eros.matching.service.NoMatchesAvailableException
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class MatchRoutesTest {

    private val mockMatchService = mockk<MatchService>()

    @Nested
    inner class `PATCH match action matchId` {

        @Test
        fun `should return 200 with MutualMatchInfo when both users like each other`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val matchId = 1L
            val mutualMatchInfo = com.eros.matching.models.MutualMatchInfo(
                matchId = matchId,
                user1Id = userId,
                user2Id = "user456",
                matchedAt = Instant.parse("2024-01-15T10:00:00Z")
            )

            coEvery { mockMatchService.matchUser(matchId, userId, true) } returns mutualMatchInfo

            val response = client.patch("/match/action/$matchId") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody("""{"liked": true}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val result = response.body<com.eros.matching.models.MutualMatchInfo>()
            assertEquals(matchId, result.matchId)
            assertEquals(userId, result.user1Id)
            assertEquals("user456", result.user2Id)
            coVerify { mockMatchService.matchUser(matchId, userId, true) }
        }

        @Test
        fun `should return 204 when user likes but no mutual match`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val matchId = 1L

            coEvery { mockMatchService.matchUser(matchId, userId, true) } returns null

            val response = client.patch("/match/action/$matchId") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody("""{"liked": true}""")
            }

            assertEquals(HttpStatusCode.NoContent, response.status)
            coVerify { mockMatchService.matchUser(matchId, userId, true) }
        }

        @Test
        fun `should return 204 when user passes on match`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val matchId = 1L

            coEvery { mockMatchService.matchUser(matchId, userId, false) } returns null

            val response = client.patch("/match/action/$matchId") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody("""{"liked": false}""")
            }

            assertEquals(HttpStatusCode.NoContent, response.status)
            coVerify { mockMatchService.matchUser(matchId, userId, false) }
        }

        @Test
        fun `should return 400 when matchId is invalid`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"

            val response = client.patch("/match/action/invalid") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody("""{"liked": true}""")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

        @Test
        fun `should return 401 when not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.patch("/match/action/1") {
                contentType(ContentType.Application.Json)
                setBody("""{"liked": true}""")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 403 when user doesn't own the match`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val matchId = 1L

            coEvery {
                mockMatchService.matchUser(matchId, userId, true)
            } throws com.eros.common.errors.ForbiddenException("You do not have permission to act on this match")

            val response = client.patch("/match/action/$matchId") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody("""{"liked": true}""")
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
            coVerify { mockMatchService.matchUser(matchId, userId, true) }
        }

        @Test
        fun `should return 404 when match doesn't exist`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val matchId = 999L

            coEvery {
                mockMatchService.matchUser(matchId, userId, true)
            } throws com.eros.common.errors.NotFoundException("Match with ID $matchId not found")

            val response = client.patch("/match/action/$matchId") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody("""{"liked": true}""")
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
            coVerify { mockMatchService.matchUser(matchId, userId, true) }
        }

        @Test
        fun `should return 409 when user already took action on match`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val matchId = 1L

            coEvery {
                mockMatchService.matchUser(matchId, userId, true)
            } throws com.eros.common.errors.ConflictException("You have already taken action on this match")

            val response = client.patch("/match/action/$matchId") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody("""{"liked": true}""")
            }

            assertEquals(HttpStatusCode.Conflict, response.status)
            coVerify { mockMatchService.matchUser(matchId, userId, true) }
        }
    }

    @Nested
    inner class `GET match` {

        @Test
        fun `should return batch of matches when available`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val profiles = listOf(
                UserMatchProfile(
                    matchId = 1L,
                    userId = "user456",
                    name = "Jane",
                    age = 28,
                    thumbnailUrl = "https://example.com/photo1.jpg",
                    badges = setOf("VERIFIED", "TRUSTED"),
                    servedAt = Instant.parse("2024-01-15T10:00:00Z")
                ),
                UserMatchProfile(
                    matchId = 2L,
                    userId = "user789",
                    name = "Bob",
                    age = 32,
                    thumbnailUrl = "https://example.com/photo2.jpg",
                    badges = setOf("VERIFIED"),
                    servedAt = Instant.parse("2024-01-15T10:00:00Z")
                )
            )
            val batchResponse = DailyBatchResponse(
                profiles = profiles,
                batchNumber = 1,
                remainingBatches = 2
            )

            coEvery { mockMatchService.fetchDailyBatch(userId) } returns batchResponse

            val response = client.get("/match/") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val result = response.body<DailyBatchResponse>()
            assertEquals(1, result.batchNumber)
            assertEquals(2, result.remainingBatches)
            assertEquals(2, result.profiles.size)
            assertEquals("user456", result.profiles[0].userId)
            assertEquals("Jane", result.profiles[0].name)
            assertEquals(28, result.profiles[0].age)
            coVerify { mockMatchService.fetchDailyBatch(userId) }
        }

        @Test
        fun `should return 204 when no matches available`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"

            coEvery { mockMatchService.fetchDailyBatch(userId) } throws NoMatchesAvailableException("No matches")

            val response = client.get("/match/") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.NoContent, response.status)
            coVerify { mockMatchService.fetchDailyBatch(userId) }
        }

        @Test
        fun `should return 429 when daily batch limit exceeded`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val today = LocalDate.now(ZoneId.of("UTC"))
            val resetAt = today.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant()

            coEvery { mockMatchService.fetchDailyBatch(userId) } throws DailyBatchLimitExceededException(
                userId = userId,
                batchesUsed = 3,
                maxBatches = 3,
                resetAt = resetAt
            )

            val response = client.get("/match/") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.TooManyRequests, response.status)

            // Check Retry-After header is present
            val retryAfter = response.headers[HttpHeaders.RetryAfter]
            kotlin.test.assertNotNull(retryAfter, "Retry-After header should be present")

            // Check response body structure
            val errorBody = response.body<DailyBatchLimitError>()
            assertEquals("Daily batch limit exceeded", errorBody.error)
            assertEquals(3, errorBody.batchesUsed)
            assertEquals(3, errorBody.maxBatches)
            assertEquals(resetAt, errorBody.resetAt)

            coVerify { mockMatchService.fetchDailyBatch(userId) }
        }

        @Test
        fun `should return 401 when not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/match/")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return batch with 7 matches when many available`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val profiles = (1..7).map { i ->
                UserMatchProfile(
                    matchId = i.toLong(),
                    userId = "user$i",
                    name = "User$i",
                    age = 25 + i,
                    thumbnailUrl = "https://example.com/photo$i.jpg",
                    badges = setOf("VERIFIED"),
                    servedAt = Instant.parse("2024-01-15T10:00:00Z")
                )
            }
            val batchResponse = DailyBatchResponse(
                profiles = profiles,
                batchNumber = 2,
                remainingBatches = 1
            )

            coEvery { mockMatchService.fetchDailyBatch(userId) } returns batchResponse

            val response = client.get("/match/") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val result = response.body<DailyBatchResponse>()
            assertEquals(2, result.batchNumber)
            assertEquals(1, result.remainingBatches)
            assertEquals(7, result.profiles.size)
            coVerify { mockMatchService.fetchDailyBatch(userId) }
        }

        @Test
        fun `should handle matches with null thumbnails`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"
            val profiles = listOf(
                UserMatchProfile(
                    matchId = 1L,
                    userId = "user456",
                    name = "Jane",
                    age = 28,
                    thumbnailUrl = null,
                    badges = null,
                    servedAt = Instant.parse("2024-01-15T10:00:00Z")
                )
            )
            val batchResponse = DailyBatchResponse(
                profiles = profiles,
                batchNumber = 1,
                remainingBatches = 2
            )

            coEvery { mockMatchService.fetchDailyBatch(userId) } returns batchResponse

            val response = client.get("/match/") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val result = response.body<DailyBatchResponse>()
            assertEquals(1, result.profiles.size)
            assertEquals(null, result.profiles[0].thumbnailUrl)
            assertEquals(null, result.profiles[0].badges)
        }

        @Test
        fun `should return 500 when unexpected error occurs`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user123"

            coEvery { mockMatchService.fetchDailyBatch(userId) } throws RuntimeException("Unexpected error")

            val response = client.get("/match/") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            coVerify { mockMatchService.fetchDailyBatch(userId) }
        }
    }

    // -------------------------------------------------------------------------
    // Helper functions
    // -------------------------------------------------------------------------

    /**
     * Creates a configured HTTP client with JSON serialization support.
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
                        // Return mock principal based on credential token
                        // Token format: "user-{userId}"
                        val userId = credential.token.removePrefix("user-")
                        val mockToken = mockk<com.google.firebase.auth.FirebaseToken>(relaxed = true) {
                            coEvery { uid } returns userId
                        }
                        FirebaseUserPrincipal(
                            uid = userId,
                            email = "$userId@example.com",
                            phoneNumber = null,
                            emailVerified = true,
                            token = mockToken,
                            role = "USER"
                        )
                    }
                }
            }

            routing {
                authenticate("firebase-auth") {
                    matchRoutes(mockMatchService)
                }
            }
        }
    }

    /**
     * Sets the Authorization header with a mock user token.
     */
    private fun HttpRequestBuilder.setAuthenticatedUser(userId: String) {
        header(HttpHeaders.Authorization, "Bearer user-$userId")
    }
}
