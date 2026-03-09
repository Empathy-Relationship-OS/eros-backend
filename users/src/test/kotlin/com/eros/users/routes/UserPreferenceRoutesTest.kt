package com.eros.users.routes

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.common.errors.NotFoundException
import com.eros.common.plugins.configureExceptionHandling
import com.eros.users.models.*
import com.eros.users.service.PreferenceService
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class UserPreferenceRoutesTest {

    private val mockUserPreferenceService = mockk<PreferenceService>()


    @Nested
    inner class GetUserPreferences {

        @Test
        fun `GET me returns user preferences when they exist`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val testUserId = "test-user-123"
            val testPreference = createTestUserPreference(userId = testUserId)

            coEvery { mockUserPreferenceService.findByUserId(testUserId) } returns testPreference

            val response = client.get("/preference/me") {
                setAuthenticatedUser(testUserId)
            }

            assertEquals(HttpStatusCode.OK, response.status)

            val responseBody = response.body<UserPreferenceDTO>()
            assertEquals(testUserId, responseBody.userId)
            assertEquals(testPreference.genderIdentities, responseBody.genderIdentities)
            assertEquals(testPreference.ageRangeMin, responseBody.ageRangeMin)
            assertEquals(testPreference.ageRangeMax, responseBody.ageRangeMax)
            coVerify(exactly = 1) { mockUserPreferenceService.findByUserId(testUserId) }
        }


        @Test
        fun `GET me returns 404 when preferences don't exist`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val testUserId = "test-user-456"

            coEvery { mockUserPreferenceService.findByUserId(testUserId) } throws NotFoundException("Not found")

            val response = client.get("/preference/me") {
                setAuthenticatedUser(testUserId)
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertTrue(response.bodyAsText().contains("Not found"))

            coVerify(exactly = 1) { mockUserPreferenceService.findByUserId(testUserId) }
        }


        @Test
        fun `GET me returns 401 when user is not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/preference/me")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }


    // Helper functions

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
                        // Token format must be: "user-{userId}"
                        val token = credential.token
                        if (!token.startsWith("user-")) return@authenticate null
                        val userId = token.removePrefix("user-")
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
                    userPreferenceRoutes(mockUserPreferenceService)
                }
            }
        }
    }

    private fun HttpRequestBuilder.setAuthenticatedUser(userId: String) {
        header(HttpHeaders.Authorization, "Bearer user-$userId")
    }

    private fun createTestUserPreference(
        userId: String = "test-user-id",
        ageRangeMin: Int = 25,
        ageRangeMax: Int = 35
    ): UserPreference {
        val now = Instant.now()
        return UserPreference(
            userId = userId,
            genderIdentities = listOf(Gender.FEMALE),
            ageRangeMin = ageRangeMin,
            ageRangeMax = ageRangeMax,
            heightRangeMin = 160,
            heightRangeMax = 180,
            ethnicity = listOf(Ethnicity.BLACK_AFRICAN_DESCENT),
            dateLanguages = listOf(Language.ENGLISH),
            dateActivities = listOf(Activity.TAKING_A_WALK),
            dateLimit = 5,
            dateCities = listOf(
                City(
                    cityId = 1L,
                    cityName = "London",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    longitude = 5.0,
                    latitude = 5.0
                )
            ),
            reachLevel = ReachLevel.OPEN_MINDED,
            createdAt = now,
            updatedAt = now
        )
    }


}