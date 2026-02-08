package com.eros.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.eros.auth.models.JwtPayload
import com.eros.auth.models.JwtSettings
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for JwtConfig.
 *
 * These tests verify:
 * - Token generation with correct claims
 * - Token verification and signature validation
 * - Expired token detection
 * - Payload extraction
 * - Configuration management
 */
class JwtConfigTest {

    private lateinit var jwtConfig: JwtConfig
    private lateinit var testSettings: JwtSettings

    @Before
    fun setUp() {
        // Create test settings
        testSettings = JwtSettings(
            secret = "test-secret-key-for-jwt-testing",
            issuer = "test-issuer",
            audience = "test-audience",
            realm = "test-realm"
        )
        jwtConfig = JwtConfig(testSettings)
    }

    @Test
    fun `generateToken creates valid token with correct claims`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val roles = listOf("user", "admin")
        val payload = JwtPayload.fromUserId(userId, email, roles)

        // When
        val token = jwtConfig.generateToken(payload)

        // Then
        assertNotNull(token, "Token should not be null")
        assertTrue(token.isNotEmpty(), "Token should not be empty")

        // Verify token can be decoded and contains correct claims
        val decoded = jwtConfig.verifyToken(token)
        assertEquals(userId.toString(), decoded.subject, "Subject should match user ID")
        assertEquals(email, decoded.getClaim("email").asString(), "Email claim should match")
        assertEquals(roles, decoded.getClaim("roles").asList(String::class.java), "Roles claim should match")
        assertEquals(testSettings.issuer, decoded.issuer, "Issuer should match")
        assertTrue(decoded.audience.contains(testSettings.audience), "Audience should match")
    }

    @Test
    fun `verifyToken successfully verifies valid token`() {
        // Given
        val payload = JwtPayload(
            sub = UUID.randomUUID().toString(),
            email = "verify@example.com",
            roles = listOf("user")
        )
        val token = jwtConfig.generateToken(payload)

        // When
        val decoded = jwtConfig.verifyToken(token)

        // Then
        assertNotNull(decoded, "Decoded token should not be null")
        assertEquals(payload.sub, decoded.subject, "Subject should match payload")
    }

    @Test
    fun `verifyToken throws exception for invalid signature`() {
        // Given
        val payload = JwtPayload(
            sub = UUID.randomUUID().toString(),
            email = "invalid@example.com",
            roles = listOf("user")
        )
        val token = jwtConfig.generateToken(payload)

        // Tamper with the token by modifying the last character
        val tamperedToken = token.dropLast(1) + "X"

        // When & Then
        assertFailsWith<JWTVerificationException>(
            message = "Should throw JWTVerificationException for tampered token"
        ) {
            jwtConfig.verifyToken(tamperedToken)
        }
    }

    @Test
    fun `verifyToken throws TokenExpiredException for expired token`() {
        // Given - Create a token that's already expired
        // We'll need to create an expired token manually for this test
        val expiredToken = createExpiredToken()

        // When & Then
        assertFailsWith<TokenExpiredException>(
            message = "Should throw TokenExpiredException for expired token"
        ) {
            jwtConfig.verifyToken(expiredToken)
        }
    }

    @Test
    fun `extractPayload correctly extracts all claims`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "extract@example.com"
        val roles = listOf("user", "moderator")
        val originalPayload = JwtPayload.fromUserId(userId, email, roles)
        val token = jwtConfig.generateToken(originalPayload)
        val decoded = jwtConfig.verifyToken(token)

        // When
        val extractedPayload = jwtConfig.extractPayload(decoded)

        // Then
        assertEquals(userId.toString(), extractedPayload.sub, "Subject should match")
        assertEquals(email, extractedPayload.email, "Email should match")
        assertEquals(roles, extractedPayload.roles, "Roles should match")
    }

    @Test
    fun `verifyAndExtract combines verification and extraction`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "combined@example.com"
        val roles = listOf("user")
        val originalPayload = JwtPayload.fromUserId(userId, email, roles)
        val token = jwtConfig.generateToken(originalPayload)

        // When
        val extractedPayload = jwtConfig.verifyAndExtract(token)

        // Then
        assertEquals(userId.toString(), extractedPayload.sub, "Subject should match")
        assertEquals(email, extractedPayload.email, "Email should match")
        assertEquals(roles, extractedPayload.roles, "Roles should match")
    }

    @Test
    fun `token contains issued at and expires at claims`() {
        // Given
        val payload = JwtPayload(
            sub = UUID.randomUUID().toString(),
            email = "timing@example.com",
            roles = listOf("user")
        )

        // When
        val token = jwtConfig.generateToken(payload)
        val decoded = jwtConfig.verifyToken(token)

        // Then
        assertNotNull(decoded.issuedAt, "Token should have issued at claim")
        assertNotNull(decoded.expiresAt, "Token should have expires at claim")

        // Verify expiry is approximately 7 days from issuance
        val sevenDaysInMs = 7L * 24 * 60 * 60 * 1000
        val actualDuration = decoded.expiresAt.time - decoded.issuedAt.time
        assertTrue(
            actualDuration >= sevenDaysInMs - 1000 && actualDuration <= sevenDaysInMs + 1000,
            "Token should expire in approximately 7 days (actual: $actualDuration ms)"
        )
    }

    @Test
    fun `JwtPayload fromUserId creates correct payload`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "factory@example.com"
        val roles = listOf("admin", "user")

        // When
        val payload = JwtPayload.fromUserId(userId, email, roles)

        // Then
        assertEquals(userId.toString(), payload.sub, "Subject should be user ID as string")
        assertEquals(email, payload.email, "Email should match")
        assertEquals(roles, payload.roles, "Roles should match")
    }

    @Test
    fun `JwtPayload fromUserId uses default user role when not specified`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "default@example.com"

        // When
        val payload = JwtPayload.fromUserId(userId, email)

        // Then
        assertEquals(listOf("user"), payload.roles, "Should default to 'user' role")
    }

    @Test
    fun `JwtSettings validates secret is not blank`() {
        // When & Then
        assertFailsWith<IllegalArgumentException>(
            message = "Should throw IllegalArgumentException for blank secret"
        ) {
            JwtSettings(
                secret = "",
                issuer = "test-issuer",
                audience = "test-audience"
            )
        }
    }

    @Test
    fun `JwtSettings toString masks secret`() {
        // Given
        val settings = JwtSettings(
            secret = "super-secret-key",
            issuer = "test-issuer",
            audience = "test-audience",
            realm = "test-realm"
        )

        // When
        val stringRepresentation = settings.toString()

        // Then
        assertTrue(stringRepresentation.contains("secret=***"), "Secret should be masked in toString")
        assertTrue(stringRepresentation.contains("issuer=test-issuer"), "Issuer should be visible")
        assertTrue(stringRepresentation.contains("audience=test-audience"), "Audience should be visible")
        assertTrue(stringRepresentation.contains("realm=test-realm"), "Realm should be visible")
    }

    // Helper functions

    private fun createExpiredToken(): String {
        // Create a token with negative expiry time to make it instantly expired
        // Use the same secret and settings as the test JwtConfig
        val algorithm = Algorithm.HMAC256(testSettings.secret)
        val pastTime = Date(System.currentTimeMillis() - 10000) // 10 seconds ago

        return JWT.create()
            .withSubject(UUID.randomUUID().toString())
            .withClaim("email", "expired@example.com")
            .withArrayClaim("roles", arrayOf("user"))
            .withIssuedAt(Date(System.currentTimeMillis() - 20000)) // 20 seconds ago
            .withExpiresAt(pastTime) // Expired 10 seconds ago
            .withIssuer(testSettings.issuer)
            .withAudience(testSettings.audience)
            .sign(algorithm)
    }
}
