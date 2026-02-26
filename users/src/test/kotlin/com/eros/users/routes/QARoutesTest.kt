package com.eros.users.routes

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.common.plugins.configureExceptionHandling
import com.eros.users.ProfileAccessControl
import com.eros.users.models.AddUserQARequest
import com.eros.users.models.Question
import com.eros.users.models.QuestionDTO
import com.eros.users.models.UserQAItem
import com.eros.users.models.UserQAItemResponse
import com.eros.users.service.QAService
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
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
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals


class QARoutesTest {

    private val mockQAService = mockk<QAService>()
    private val mockProfileAccessControl = mockk<ProfileAccessControl>()

    @Nested
    inner class `POST QA` {
        @Test
        fun `successful POST user QA`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = createValidQARequest()
            val qa = createValidQA()

            coEvery {mockQAService.createUserQA(request)} returns qa

            val response = client.post("/qa") {
                setAuthenticatedUser(request.userId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Created, response.status)
            val returnedQA = response.body<UserQAItemResponse>()
            println(returnedQA)

            coVerify { mockQAService.createUserQA(request) }
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
                    qaRoutes(mockQAService, mockProfileAccessControl)
                }
            }
        }
    }

    private fun HttpRequestBuilder.setAuthenticatedUser(userId: String) {
        header(HttpHeaders.Authorization, "Bearer user-$userId")
    }

    private val question1 = "What do you see when you close your eyes?"
    private val answer1 = "Darkness..."
    private fun createValidQARequest(userId: String = "test-user-id") : AddUserQARequest {
        return AddUserQARequest(userId,
            QuestionDTO(1L,question1),
            answer1,
            1
        )
    }

    private fun createValidQA(userId: String = "test-user-id") : UserQAItem {
        return UserQAItem(
            userId = userId,
            question = Question(1L, question1,Instant.now(),Instant.now()),
            answer = answer1,
            displayOrder = 1,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}