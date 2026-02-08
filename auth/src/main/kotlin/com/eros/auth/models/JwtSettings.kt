package com.eros.auth.models

import io.ktor.server.config.*

/**
 * JWT configuration settings loaded from application.yaml.
 *
 * **What**: Holds all necessary parameters for JWT token generation and verification.
 *
 * **How**: Loads configuration from Ktor's ApplicationConfig (application.yaml) with support
 * for environment variable substitution. Validates that the secret is not blank.
 *
 * **Why**: Centralizes JWT configuration in a type-safe, immutable data class. This approach
 * enables environment-specific configuration through environment variables while providing
 * sensible defaults for non-sensitive settings.
 *
 * Configuration example in application.yaml:
 * ```yaml
 * jwt:
 *   secret: ${JWT_SECRET:}
 *   issuer: ${JWT_ISSUER:eros-backend}
 *   audience: ${JWT_AUDIENCE:eros-users}
 *   realm: ${JWT_REALM:eros-api}
 * ```
 *
 * @property secret The secret key used for signing JWT tokens (REQUIRED, no default)
 * @property issuer The issuer claim for JWT tokens (default: "eros-backend")
 * @property audience The audience claim for JWT tokens (default: "eros-users")
 * @property realm The authentication realm name (default: "eros-api")
 */
data class JwtSettings(
    val secret: String,
    val issuer: String = "eros-backend",
    val audience: String = "eros-users",
    val realm: String = "eros-api"
) {
    init {
        require(secret.isNotBlank()) {
            "JWT secret must not be blank. Please set the JWT_SECRET environment variable or configure jwt.secret in application.yaml"
        }
    }

    override fun toString(): String =
        "JwtSettings(secret=***, issuer=$issuer, audience=$audience, realm=$realm)"

    companion object {
        /**
         * Factory method to create JwtSettings from Ktor's ApplicationConfig.
         *
         * **What**: Extracts JWT configuration from application.yaml and constructs a JwtSettings instance.
         *
         * **How**: Reads properties from the "jwt.*" namespace in ApplicationConfig.
         * Supports environment variable substitution via Ktor's `${VAR:default}` syntax.
         *
         * **Why**: Provides a single source of truth for configuration loading. By using Ktor's
         * ApplicationConfig, we automatically get environment variable support and validation.
         *
         * Usage example:
         * ```kotlin
         * fun Application.configureSecurity() {
         *     val jwtSettings = JwtSettings.fromApplicationConfig(environment.config)
         *     val jwtConfig = JwtConfig(jwtSettings)
         *     // ... use jwtConfig for authentication
         * }
         * ```
         *
         * @param config Ktor ApplicationConfig instance (typically from Application.environment.config)
         * @return JwtSettings instance populated from configuration
         * @throws ApplicationConfigurationException if required properties are missing
         * @throws IllegalArgumentException if the secret is blank
         */
        fun fromApplicationConfig(config: ApplicationConfig): JwtSettings {
            return JwtSettings(
                secret = config.property("jwt.secret").getString(),
                issuer = config.property("jwt.issuer").getString(),
                audience = config.property("jwt.audience").getString(),
                realm = config.property("jwt.realm").getString()
            )
        }
    }
}
