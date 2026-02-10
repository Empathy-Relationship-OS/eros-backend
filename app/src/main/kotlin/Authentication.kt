package com.eros

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import java.util.*

/**
 * JWT Payload data class for type-safe principal extraction.
 *
 * This class represents the validated JWT claims that can be accessed
 * from authenticated routes via `call.principal<JwtPayload>()`.
 *
 * Note: In modern Ktor, the Principal interface is no longer required.
 * The authentication system has been refactored to support Any type,
 * making the marker interface redundant.
 *
 * @property userId The unique identifier of the authenticated user (from "sub" claim)
 * @property email The email address of the authenticated user (from "email" claim)
 * @property audience The intended audience of the token (from "aud" claim)
 * @property issuer The issuer of the token (from "iss" claim)
 * @property expiresAt The expiration time of the token (from "exp" claim)
 */
data class JwtPayload(
    val userId: UUID,
    val email: String,
    val audience: String,
    val issuer: String,
    val expiresAt: Date
)

/**
 * Configures JWT-based authentication for the application.
 *
 * This plugin sets up JWT authentication with the following features:
 * - Reads configuration from application.yaml and environment variables
 * - Validates JWT tokens using HMAC256 algorithm
 * - Extracts and validates standard JWT claims (aud, iss, exp, sub)
 * - Returns 401 Unauthorized with descriptive error messages on authentication failure
 * - Provides type-safe access to JWT claims via JwtPayload principal
 *
 * Configuration:
 * - JWT secret must be provided via JWT_SECRET environment variable
 * - JWT domain, audience, and realm are read from application.yaml
 *
 * Usage in routes:
 * ```kotlin
 * authenticate("jwt-auth") {
 *     get("/protected") {
 *         val payload = call.principal<JwtPayload>()
 *         // Access payload.userId, payload.email, etc.
 *     }
 * }
 * ```
 *
 * Security Notes:
 * - JWT_SECRET should be a strong, random secret and never committed to version control
 * - Consider using RS256/ES256 for production environments
 * - Always use HTTPS in production
 * - Tokens should have reasonable expiration times
 *
 * @throws IllegalStateException if JWT_SECRET environment variable is not set
 */
fun Application.configureAuthentication() {
    // Read JWT configuration from application.yaml
    // Values are injected from environment variables via ${ENV_VAR} syntax
    val jwtSecret = environment.config.propertyOrNull("jwt.secret")?.getString()
        ?: throw IllegalStateException(
            "JWT secret not configured. " +
                    "Set JWT_SECRET environment variable or configure jwt.secret in application.yaml. " +
                    "Generate a strong secret: openssl rand -base64 32"
        )

    val jwtAudience = environment.config.propertyOrNull("jwt.audience")?.getString()
        ?: throw IllegalStateException("JWT audience not configured in application.yaml")

    val jwtDomain = environment.config.propertyOrNull("jwt.domain")?.getString()
        ?: throw IllegalStateException("JWT domain not configured in application.yaml")

    val jwtRealm = environment.config.propertyOrNull("jwt.realm")?.getString()
        ?: "ktor sample app"

    // Read OAuth configuration
    val oauthCallbackUrl = environment.config.propertyOrNull("oauth.callbackUrl")?.getString()
        ?: "http://localhost:8080/callback"
    val googleClientId = environment.config.propertyOrNull("oauth.google.clientId")?.getString()
    val googleClientSecret = environment.config.propertyOrNull("oauth.google.clientSecret")?.getString()

    install(Authentication) {
        // Configure JWT authentication provider
        jwt("jwt-auth") {
            realm = jwtRealm

            // Configure JWT verifier with security best practices
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )

            // Validate JWT credentials and extract claims into JwtPayload
            validate { credential ->
                try {
                    val payload = credential.payload

                    // Extract user ID from "sub" claim
                    val userIdString = payload.subject
                        ?: run {
                            application.log.warn("JWT validation failed: Missing subject claim")
                            return@validate null
                        }

                    val userId = try {
                        UUID.fromString(userIdString)
                    } catch (e: IllegalArgumentException) {
                        application.log.warn("JWT validation failed: Invalid user ID format: $userIdString", e)
                        return@validate null
                    }

                    // Extract email from "email" claim
                    val email = payload.getClaim("email").asString()
                        ?: run {
                            application.log.warn("JWT validation failed: Missing email claim")
                            return@validate null
                        }

                    // Create and return JwtPayload principal
                    JwtPayload(
                        userId = userId,
                        email = email,
                        audience = payload.audience.first(),
                        issuer = payload.issuer,
                        expiresAt = payload.expiresAt
                    )
                } catch (e: Exception) {
                    application.log.error("JWT validation error", e)
                    null
                }
            }

            // Configure challenge response for authentication failures
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "error" to "unauthorized",
                        "message" to "Invalid or expired JWT token. Please authenticate and provide a valid token.",
                        "timestamp" to System.currentTimeMillis().toString()
                    )
                )
            }
        }

        // Configure OAuth authentication provider for Google
        // Only configure if credentials are available
        if (!googleClientId.isNullOrBlank() && !googleClientSecret.isNullOrBlank()) {
            oauth("auth-oauth-google") {
                urlProvider = { oauthCallbackUrl }
                providerLookup = {
                    OAuthServerSettings.OAuth2ServerSettings(
                        name = "google",
                        authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                        accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                        requestMethod = HttpMethod.Post,
                        clientId = requireNotNull(googleClientId) { "GOOGLE_CLIENT_ID is required" },
                        clientSecret = requireNotNull(googleClientSecret) { "GOOGLE_CLIENT_SECRET is required" },
                        defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile")
                    )
                }
                client = HttpClient(Apache)
            }
        }
    }

    // Configure OAuth routes if OAuth is enabled
    if (!googleClientId.isNullOrBlank() && !googleClientSecret.isNullOrBlank()) {
        routing {
            authenticate("auth-oauth-google") {
                get("login") {
                    call.respondRedirect("/callback")
                }

                get("/callback") {
                    val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
                    call.sessions.set(UserSession(principal?.accessToken.toString()))
                    call.respondRedirect("/hello")
                }
            }
        }
        log.debug("OAuth authentication configured for Google")
    } else {
        log.warn("Google OAuth credentials not found - OAuth authentication disabled. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET to enable.")
    }

    log.debug("JWT authentication configured successfully")
    log.debug("JWT Realm: $jwtRealm")
    log.debug("JWT Audience: $jwtAudience")
    log.debug("JWT Issuer: $jwtDomain")
}

/**
 * User session data class for OAuth authentication.
 * Stores the OAuth access token received from the provider.
 */
data class UserSession(val accessToken: String)
