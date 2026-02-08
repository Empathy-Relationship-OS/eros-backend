package com.eros.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.auth0.jwt.interfaces.DecodedJWT
import com.eros.auth.models.JwtPayload
import com.eros.auth.models.JwtSettings
import java.util.Date

/**
 * JWT configuration object for token generation and verification.
 *
 * This object manages JWT tokens using the Auth0 java-jwt library with HMAC256 algorithm.
 * Tokens have a 7-day expiry period and contain user claims (sub, email, roles).
 *
 * Configuration is loaded from application.yaml:
 * ```yaml
 * jwt:
 *   secret: ${JWT_SECRET:}
 *   issuer: ${JWT_ISSUER:eros-backend}
 *   audience: ${JWT_AUDIENCE:eros-users}
 *   realm: ${JWT_REALM:eros-api}
 * ```
 */
class JwtConfig(private val settings: JwtSettings) {

    private val algorithm: Algorithm = Algorithm.HMAC256(settings.secret)

    /**
     * JWT verifier for validating token signatures, issuer, and audience.
     *
     * @throws TokenExpiredException if the token has expired
     * @throws JWTVerificationException if token verification fails
     */
    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(settings.issuer)
        .withAudience(settings.audience)
        .build()

    /**
     * Generates a JWT token with the provided payload.
     *
     * The token includes:
     * - sub (subject): User ID
     * - email: User's email address
     * - roles: Array of user roles
     * - iat (issued at): Current timestamp
     * - exp (expires at): Current timestamp + 7 days
     * - iss (issuer): Configured issuer
     * - aud (audience): Configured audience
     *
     * @param payload The JWT payload containing user claims
     * @return A signed JWT token string
     */
    fun generateToken(payload: JwtPayload): String {
        val now = Date()
        val expiresAt = Date(now.time + SEVEN_DAYS_IN_MS)

        return JWT.create()
            .withSubject(payload.sub)
            .withClaim("email", payload.email)
            .withArrayClaim("roles", payload.roles.toTypedArray())
            .withIssuedAt(now)
            .withExpiresAt(expiresAt)
            .withIssuer(settings.issuer)
            .withAudience(settings.audience)
            .sign(algorithm)
    }

    /**
     * Verifies and decodes a JWT token.
     *
     * @param token The JWT token string to verify
     * @return The decoded JWT if verification succeeds
     *
     * @throws TokenExpiredException if the token has expired
     * @throws JWTVerificationException if token verification fails (invalid signature, issuer, or audience)
     */
    fun verifyToken(token: String): DecodedJWT {
        return verifier.verify(token)
    }

    /**
     * Extracts the payload from a decoded JWT token.
     *
     * @param decodedJWT The decoded JWT token
     * @return JwtPayload containing the user claims
     */
    fun extractPayload(decodedJWT: DecodedJWT): JwtPayload {
        return JwtPayload(
            sub = decodedJWT.subject,
            email = decodedJWT.getClaim("email").asString(),
            roles = decodedJWT.getClaim("roles").asList(String::class.java)
        )
    }

    /**
     * Convenience method to verify a token and extract its payload in one call.
     *
     * @param token The JWT token string to verify
     * @return JwtPayload containing the user claims
     *
     * @throws TokenExpiredException if the token has expired
     * @throws JWTVerificationException if token verification fails
     */
    fun verifyAndExtract(token: String): JwtPayload {
        val decoded = verifyToken(token)
        return extractPayload(decoded)
    }

    companion object {
        private const val SEVEN_DAYS_IN_MS = 7L * 24 * 60 * 60 * 1000 // 7 days in milliseconds
    }
}
