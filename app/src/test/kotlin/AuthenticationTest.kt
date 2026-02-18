package com.eros

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for Firebase authentication with user routes.
 *
 * Note: These tests verify the authentication flow but require actual Firebase tokens.
 * For unit tests, Firebase authentication should be mocked. For integration tests,
 * you would need to use Firebase Auth Emulator or generate real tokens.
 *
 * Tests verify:
 * - Protected user routes return 401 without Bearer token
 * - Protected user routes return 401 with invalid token
 * - Token format validation (Bearer prefix)
 * - Error response structure
 *
 * User routes tested:
 * - GET /users/me - Get current user profile
 * - GET /users/exists - Check if user exists
 * - POST /users - Create user profile
 * - DELETE /users/me - Delete user profile
 *
 * TODO: Add Firebase Emulator integration for full end-to-end testing
 */
class AuthenticationTest : IntegrationTestBase() {

    @Test
    fun `test protected route returns 401 without Bearer token`() = testApplication {
        setupTestEnvironment()

        val response = client.get("/users/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test protected route returns 401 with missing Authorization header`() = testApplication {
        setupTestEnvironment()

        val response = client.get("/users/exists")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test protected route returns 401 with invalid Bearer token`() = testApplication {
        setupTestEnvironment()

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer invalid-firebase-token-12345")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test protected route returns 401 with malformed token`() = testApplication {
        setupTestEnvironment()

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer not.a.valid.jwt")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test protected route returns 401 with empty token`() = testApplication {
        setupTestEnvironment()

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer ")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test unprotected routes work without authentication`() = testApplication {
        setupTestEnvironment()

        val response = client.get("/")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello World!", response.bodyAsText())
    }

    @Test
    fun `test create user endpoint requires authentication`() = testApplication {
        setupTestEnvironment()

        val response = client.post("/users")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test delete user endpoint requires authentication`() = testApplication {
        setupTestEnvironment()

        val response = client.delete("/users/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // Note: Testing with valid Firebase tokens requires Firebase Emulator or actual Firebase project
    // For production tests, use Firebase Auth Emulator: https://firebase.google.com/docs/emulator-suite

    @Ignore("Requires Firebase Emulator or real Firebase tokens")
    @Test
    fun `test protected route succeeds with valid Firebase token`() = testApplication {
        setupTestEnvironment()

        // TODO: Generate valid Firebase token using Firebase Emulator
        val validFirebaseToken = "VALID_FIREBASE_TOKEN_FROM_EMULATOR"

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer $validFirebaseToken")
        }

        // Will return 404 since user doesn't exist, but authentication passed
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    /**
     * Sets up test environment with database and Firebase configuration.
     *
     * Note: These tests skip Firebase authentication since it requires:
     * 1. Firebase Auth Emulator (recommended for CI/CD)
     * 2. A Firebase project with service account
     * 3. Mocking FirebaseAuth.getInstance() in unit tests
     *
     * For now, these tests verify that user routes are protected by checking
     * that they return 401 Unauthorized without valid authentication.
     */
    private fun ApplicationTestBuilder.setupTestEnvironment() {
        environment {
            config = MapApplicationConfig(
                // Database configuration
                "database.host" to postgres.host,
                "database.port" to postgres.firstMappedPort.toString(),
                "database.name" to postgres.databaseName,
                "database.user" to postgres.username,
                "database.password" to postgres.password,
                "database.poolSize" to "5",
                "database.maxLifetime" to "600000",
                "database.connectionTimeout" to "30000",

                // Firebase configuration - use dummy values for testing
                // Tests will verify 401 responses which don't require actual Firebase
                "firebase.serviceAccountPath" to "./test-firebase-service-account.json",
                "firebase.projectId" to "test-project-id"
            )
        }

        application {
            // Configure only the plugins needed for testing without Firebase
            // This avoids Firebase initialization failures in test.json environment
            configureDatabase()
            configureSerialization()
            configureAdministration()
            configureHTTP()
            configureMonitoring()

            // Install minimal authentication that always rejects (for testing 401 responses)
            install(Authentication) {
                bearer("firebase-auth") {
                    realm = "test-realm"
                    authenticate { credential ->
                        // Always return null to simulate missing/invalid Firebase token
                        // This allows us to test that routes are protected without Firebase setup
                        null
                    }
                }
            }

            configureRouting()
        }
    }
}
