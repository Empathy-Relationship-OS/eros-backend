package com.eros.users.routes

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.users.models.*
import com.eros.users.service.UserService
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserRoutesTest {

    private val mockUserService = mockk<UserService>()

    @Nested
    inner class `POST users` {

        @Test
        fun `should create user and return 201 when valid request`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = createValidUserRequest()
            val createdUser = createTestUser()

            coEvery { mockUserService.userExists(request.userId) } returns false
            coEvery { mockUserService.createUser(request) } returns createdUser

            val response = client.post("/users") {
                setAuthenticatedUser(request.userId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Created, response.status)
            val returnedUser = response.body<User>()
            assertEquals(createdUser, returnedUser)
            coVerify { mockUserService.userExists(request.userId) }
            coVerify { mockUserService.createUser(request) }
        }

        @Test
        fun `should return 401 when not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = createValidUserRequest()

            val response = client.post("/users") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 403 when userId does not match principal`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = createValidUserRequest(userId = "different-user-id")

            val response = client.post("/users") {
                setAuthenticatedUser("authenticated-user-id")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertTrue(response.bodyAsText().contains("Cannot create profile for another user"))
        }

        @Test
        fun `should return 409 when user already exists`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = createValidUserRequest()

            coEvery { mockUserService.userExists(request.userId) } returns true

            val response = client.post("/users") {
                setAuthenticatedUser(request.userId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Conflict, response.status)
            assertTrue(response.bodyAsText().contains("User profile already exists"))
        }

        @Test
        fun `should return 400 when invalid input (IllegalArgumentException)`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = createValidUserRequest()

            coEvery { mockUserService.userExists(request.userId) } returns false
            coEvery { mockUserService.createUser(request) } throws IllegalArgumentException("Invalid data")

            val response = client.post("/users") {
                setAuthenticatedUser(request.userId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Invalid data"))
        }

        @Test
        fun `should return 500 when database error`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = createValidUserRequest()

            coEvery { mockUserService.userExists(request.userId) } returns false
            coEvery { mockUserService.createUser(request) } throws RuntimeException("Database error")

            val response = client.post("/users") {
                setAuthenticatedUser(request.userId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }

        @Test
        fun `should return 500 when unexpected error`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = createValidUserRequest()

            coEvery { mockUserService.userExists(request.userId) } returns false
            coEvery { mockUserService.createUser(request) } throws RuntimeException("Unexpected error")

            val response = client.post("/users") {
                setAuthenticatedUser(request.userId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to create user profile"))
        }
    }

    @Nested
    inner class `GET users me` {

        @Test
        fun `should return user profile when found`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"
            val user = createTestUser(userId = userId)

            coEvery { mockUserService.findByUserId(userId) } returns user

            val response = client.get("/users/me") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val returnedUser = response.body<User>()
            assertEquals(user, returnedUser)
            coVerify { mockUserService.findByUserId(userId) }
        }

        @Test
        fun `should return 404 when profile not found`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"

            coEvery { mockUserService.findByUserId(userId) } returns null

            val response = client.get("/users/me") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertTrue(response.bodyAsText().contains("User profile not found"))
        }

        @Test
        fun `should return 401 when not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/users/me")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 500 when database error`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"

            coEvery { mockUserService.findByUserId(userId) } throws RuntimeException("DB error")

            val response = client.get("/users/me") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }
    }

    @Nested
    inner class `GET users exists` {

        @Test
        fun `should return exists true when user exists`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"

            coEvery { mockUserService.userExists(userId) } returns true

            val response = client.get("/users/exists") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val exists = response.body<UserExistsResponse>()
            assertTrue(exists.exists)
            assertEquals(userId, exists.userId)
        }

        @Test
        fun `should return exists false when user does not exist`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"

            coEvery { mockUserService.userExists(userId) } returns false

            val response = client.get("/users/exists") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val exists = response.body<UserExistsResponse>()
            assertFalse(exists.exists)
            assertEquals(userId, exists.userId)
        }

        @Test
        fun `should return 401 when not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/users/exists")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 500 when unexpected error`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"

            coEvery { mockUserService.userExists(userId) } throws RuntimeException("Error")

            val response = client.get("/users/exists") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }
    }

    @Nested
    inner class `GET users id` {

        @Test
        fun `should return user profile when found`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"
            val user = createTestUser(userId = userId)

            coEvery { mockUserService.findByUserId(userId) } returns user

            val response = client.get("/users/$userId") {
                setAuthenticatedUser("some-authenticated-user")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val returnedUser = response.body<User>()
            assertEquals(user, returnedUser)
            coVerify { mockUserService.findByUserId(userId) }
        }

        @Test
        fun `should return 404 when not found`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"

            coEvery { mockUserService.findByUserId(userId) } returns null

            val response = client.get("/users/$userId") {
                setAuthenticatedUser("some-authenticated-user")
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertTrue(response.bodyAsText().contains("User profile not found"))
        }

        @Test
        fun `should return 401 when not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/users/some-id")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 500 when database error`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"

            coEvery { mockUserService.findByUserId(userId) } throws RuntimeException("DB error")

            val response = client.get("/users/$userId") {
                setAuthenticatedUser("some-authenticated-user")
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }
    }

    @Nested
    inner class `PUT users me` {

        @Test
        fun `should update user and return updated profile`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"
            val updateRequest = UpdateUserRequest(firstName = "UpdatedName")
            val updatedUser = createTestUser(userId = userId, firstName = "UpdatedName")

            coEvery { mockUserService.updateUser(userId, updateRequest) } returns updatedUser

            val response = client.put("/users/me") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody(updateRequest)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val returnedUser = response.body<User>()
            assertEquals("UpdatedName", returnedUser.firstName)
            assertEquals(updatedUser, returnedUser)
            coVerify { mockUserService.updateUser(userId, updateRequest) }
        }

        @Test
        fun `should return 404 when user not found`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"
            val updateRequest = UpdateUserRequest(firstName = "UpdatedName")

            coEvery { mockUserService.updateUser(userId, updateRequest) } returns null

            val response = client.put("/users/me") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody(updateRequest)
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertTrue(response.bodyAsText().contains("User profile not found"))
        }

        @Test
        fun `should return 400 when invalid input`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"
            val updateRequest = UpdateUserRequest(bio = "Updated bio")

            coEvery { mockUserService.updateUser(userId, updateRequest) } throws IllegalArgumentException("Invalid input")

            val response = client.put("/users/me") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody(updateRequest)
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Invalid input"))
        }

        @Test
        fun `should return 401 when not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val updateRequest = UpdateUserRequest(firstName = "UpdatedName")

            val response = client.put("/users/me") {
                contentType(ContentType.Application.Json)
                setBody(updateRequest)
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 500 when database error`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"
            val updateRequest = UpdateUserRequest(firstName = "UpdatedName")

            coEvery { mockUserService.updateUser(userId, updateRequest) } throws RuntimeException("DB error")

            val response = client.put("/users/me") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody(updateRequest)
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
        }
    }

    @Nested
    inner class `DELETE users me` {

        @Test
        fun `should delete user and return 204`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"

            coEvery { mockUserService.deleteUser(userId) } returns 1

            val response = client.delete("/users/me") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.NoContent, response.status)
            coVerify { mockUserService.deleteUser(userId) }
        }

        @Test
        fun `should return 404 when user not found`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"

            coEvery { mockUserService.deleteUser(userId) } returns 0

            val response = client.delete("/users/me") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertTrue(response.bodyAsText().contains("User profile not found"))
        }

        @Test
        fun `should return 401 when not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.delete("/users/me")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 500 when database error`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-id"

            coEvery { mockUserService.deleteUser(userId) } throws RuntimeException("DB error")

            val response = client.delete("/users/me") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
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
                            token = mockToken
                        )
                    }
                }
            }

            routing {
                userRoutes(mockUserService)
            }
        }
    }

    private fun HttpRequestBuilder.setAuthenticatedUser(userId: String) {
        header(HttpHeaders.Authorization, "Bearer user-$userId")
    }

    private fun createValidUserRequest(userId: String = "test-user-id"): CreateUserRequest {
        return CreateUserRequest(
            userId = userId,
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            heightCm = 180,
            dateOfBirth = LocalDate.of(1990, 1, 1),
            city = "London",
            educationLevel = EducationLevel.UNIVERSITY,
            gender = Gender.MALE,
            preferredLanguage = Language.ENGLISH,
            occupation = "Engineer",
            bio = "Test bio",
            interests = List(5) { "Interest$it" },
            traits = List(3) { Trait.entries[it] },
            ethnicity = listOf(Ethnicity.BLACK_AFRICAN_DESCENT)
        )
    }

    private fun createTestUser(
        userId: String = "test-user-id",
        firstName: String = "John"
    ): User {
        return User(
            userId = userId,
            firstName = firstName,
            lastName = "Doe",
            email = "john.doe@example.com",
            heightCm = 180,
            dateOfBirth = LocalDate.of(1990, 1, 1),
            city = "London",
            educationLevel = EducationLevel.UNIVERSITY,
            gender = Gender.MALE,
            occupation = "Engineer",
            bio = "Test bio",
            interests = List(5) { "Interest$it" },
            traits = List(3) { Trait.entries[it] },
            preferredLanguage = Language.ENGLISH,
            spokenLanguages = DisplayableField(listOf(Language.ENGLISH), false),
            religion = DisplayableField(null, false),
            politicalView = DisplayableField(null, false),
            alcoholConsumption = DisplayableField(null, false),
            smokingStatus = DisplayableField(null, false),
            diet = DisplayableField(null, false),
            dateIntentions = DisplayableField(DateIntentions.SERIOUS_DATING, false),
            relationshipType = DisplayableField(RelationshipType.MONOGAMOUS, false),
            kidsPreference = DisplayableField(KidsPreference.OPEN_TO_KIDS, false),
            sexualOrientation = DisplayableField(SexualOrientation.STRAIGHT, false),
            pronouns = DisplayableField(null, false),
            starSign = DisplayableField(null, false),
            ethnicity = DisplayableField(listOf(Ethnicity.BLACK_AFRICAN_DESCENT), false),
            brainAttributes = DisplayableField(null, false),
            brainDescription = DisplayableField(null, false),
            bodyAttributes = DisplayableField(null, false),
            bodyDescription = DisplayableField(null, false),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            deletedAt = null
        )
    }
}
