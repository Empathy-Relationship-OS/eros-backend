package com.eros

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import java.util.*
import kotlin.test.*

/**
 * Integration tests for JWT authentication plugin.
 *
 * Tests verify:
 * - Protected routes return 401 without valid JWT
 * - Protected routes succeed with valid JWT
 * - JWT payload extraction works correctly
 * - Invalid/expired tokens are rejected
 * - Proper error messages are returned
 */
class AuthenticationTest : IntegrationTestBase() {

    private val testJwtSecret = "test-secret-key-for-jwt-authentication-testing"
    private val testAudience = "jwt-audience"
    private val testIssuer = "https://jwt-provider-domain/"

    /**
     * Helper function to generate a valid JWT token for testing.
     */
    private fun generateValidToken(
        userId: UUID = UUID.randomUUID(),
        email: String = "test@example.com",
        expiresInMinutes: Long = 60
    ): String {
        return JWT.create()
            .withAudience(testAudience)
            .withIssuer(testIssuer)
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + expiresInMinutes * 60 * 1000))
            .sign(Algorithm.HMAC256(testJwtSecret))
    }

    /**
     * Helper function to generate an expired JWT token for testing.
     */
    private fun generateExpiredToken(
        userId: UUID = UUID.randomUUID(),
        email: String = "test@example.com"
    ): String {
        return JWT.create()
            .withAudience(testAudience)
            .withIssuer(testIssuer)
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() - 60000)) // Expired 1 minute ago
            .sign(Algorithm.HMAC256(testJwtSecret))
    }

    @Test
    fun `test protected route returns 401 without JWT token`() = testApplication {
        setupTestEnvironment()

        val response = client.get("/users/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(body.containsKey("error"), "Response should contain error field")
        assertTrue(body.containsKey("message"), "Response should contain message field")
        assertEquals("unauthorized", body["error"]?.jsonPrimitive?.content)
    }

    @Test
    fun `test protected route returns 401 with invalid JWT token`() = testApplication {
        setupTestEnvironment()

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer invalid-token-12345")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(body.containsKey("error"), "Response should contain error field")
        assertEquals("unauthorized", body["error"]?.jsonPrimitive?.content)
    }

    @Test
    fun `test protected route returns 401 with expired JWT token`() = testApplication {
        setupTestEnvironment()

        val expiredToken = generateExpiredToken()

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer $expiredToken")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test protected route succeeds with valid JWT token`() = testApplication {
        setupTestEnvironment()

        val userId = UUID.randomUUID()
        val email = "testuser@example.com"
        val validToken = generateValidToken(userId, email)

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer $validToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(body.containsKey("userId"), "Response should contain userId")
        assertTrue(body.containsKey("email"), "Response should contain email")

        assertEquals(userId.toString(), body["userId"]?.jsonPrimitive?.content)
        assertEquals(email, body["email"]?.jsonPrimitive?.content)
    }

    @Test
    fun `test JWT payload extraction contains correct claims`() = testApplication {
        setupTestEnvironment()

        val userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val email = "specific.user@example.com"
        val validToken = generateValidToken(userId, email)

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer $validToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(userId.toString(), body["userId"]?.jsonPrimitive?.content,
            "User ID should match JWT subject claim")
        assertEquals(email, body["email"]?.jsonPrimitive?.content,
            "Email should match JWT email claim")
    }

    @Test
    fun `test JWT with wrong audience is rejected`() = testApplication {
        setupTestEnvironment()

        val wrongAudienceToken = JWT.create()
            .withAudience("wrong-audience")
            .withIssuer(testIssuer)
            .withSubject(UUID.randomUUID().toString())
            .withClaim("email", "test@example.com")
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.HMAC256(testJwtSecret))

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer $wrongAudienceToken")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test JWT with wrong issuer is rejected`() = testApplication {
        setupTestEnvironment()

        val wrongIssuerToken = JWT.create()
            .withAudience(testAudience)
            .withIssuer("https://wrong-issuer.com/")
            .withSubject(UUID.randomUUID().toString())
            .withClaim("email", "test@example.com")
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.HMAC256(testJwtSecret))

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer $wrongIssuerToken")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test JWT without subject claim is rejected`() = testApplication {
        setupTestEnvironment()

        val noSubjectToken = JWT.create()
            .withAudience(testAudience)
            .withIssuer(testIssuer)
            .withClaim("email", "test@example.com")
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.HMAC256(testJwtSecret))

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer $noSubjectToken")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test JWT without email claim is rejected`() = testApplication {
        setupTestEnvironment()

        val noEmailToken = JWT.create()
            .withAudience(testAudience)
            .withIssuer(testIssuer)
            .withSubject(UUID.randomUUID().toString())
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.HMAC256(testJwtSecret))

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer $noEmailToken")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test JWT with invalid UUID format in subject is rejected`() = testApplication {
        setupTestEnvironment()

        val invalidUuidToken = JWT.create()
            .withAudience(testAudience)
            .withIssuer(testIssuer)
            .withSubject("not-a-valid-uuid")
            .withClaim("email", "test@example.com")
            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
            .sign(Algorithm.HMAC256(testJwtSecret))

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer $invalidUuidToken")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test error response contains timestamp`() = testApplication {
        setupTestEnvironment()

        val response = client.get("/users/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(body.containsKey("timestamp"), "Response should contain timestamp")

        val timestamp = body["timestamp"]?.jsonPrimitive?.content
        assertNotNull(timestamp, "Timestamp should be present")
        val timestampLong = timestamp.toLongOrNull()
        assertNotNull(timestampLong, "Timestamp should be a valid long string")
        assertTrue(timestampLong > 0, "Timestamp should be positive")
    }

    /**
     * Sets up test environment with database and JWT configuration.
     * All configuration is injected via Ktor's config system - no need for environment variables!
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

                // JWT configuration - injected directly into config for testing
                "jwt.secret" to testJwtSecret,
                "jwt.domain" to testIssuer,
                "jwt.audience" to testAudience,
                "jwt.realm" to "ktor test app"
            )
        }
        application {
            module()
        }
    }
}
