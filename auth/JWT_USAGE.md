# JWT Configuration and Usage Guide

## Overview

The JWT module provides token generation and verification for authentication in the Eros backend. It follows Ktor's configuration patterns, loading settings from `application.yaml` with environment variable support.

## Configuration

### application.yaml

Add the following configuration to your `application.yaml`:

```yaml
jwt:
  secret: ${JWT_SECRET:}                      # REQUIRED: Set via environment variable
  issuer: ${JWT_ISSUER:eros-backend}          # Optional: Defaults to "eros-backend"
  audience: ${JWT_AUDIENCE:eros-users}        # Optional: Defaults to "eros-users"
  realm: ${JWT_REALM:eros-api}                # Optional: Defaults to "eros-api"
```

### Environment Variables

- **JWT_SECRET** (REQUIRED): A secure random string for signing tokens. Generate with:
  ```bash
  openssl rand -base64 32
  ```
- **JWT_ISSUER** (Optional): The token issuer claim
- **JWT_AUDIENCE** (Optional): The token audience claim
- **JWT_REALM** (Optional): The authentication realm name

### Local Development

Create a `.env` file or set environment variables:

```bash
export JWT_SECRET="your-super-secret-key-here"
export JWT_ISSUER="eros-backend-dev"
export JWT_AUDIENCE="eros-users-dev"
```

### Production

Set environment variables in your deployment platform:
- Docker: Use `-e JWT_SECRET=...` or docker-compose environment section
- Kubernetes: Use ConfigMap/Secret
- Cloud platforms: Use environment variable configuration

## Usage Examples

### 1. Initialize in Application Plugin

The typical pattern is to create a Ktor plugin similar to `DatabasePlugin`:

```kotlin
// In app/src/main/kotlin/Security.kt or similar

package com.eros

import com.eros.auth.JwtConfig
import com.eros.auth.models.JwtSettings
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    // Load JWT settings from application.yaml
    val jwtSettings = JwtSettings.fromApplicationConfig(environment.config)
    val jwtConfig = JwtConfig(jwtSettings)

    // Configure Ktor JWT authentication
    authentication {
        jwt("auth-jwt") {
            realm = jwtSettings.realm
            verifier(jwtConfig.verifier)

            validate { credential ->
                // Extract and validate the JWT payload
                try {
                    val payload = jwtConfig.extractPayload(credential.payload)
                    // Return a Principal with user information
                    JWTPrincipal(credential.payload)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
```

### 2. Generate Tokens (e.g., Login Endpoint)

```kotlin
// In auth/src/main/kotlin/com/eros/auth/routes/AuthRoutes.kt

import com.eros.auth.JwtConfig
import com.eros.auth.models.JwtPayload
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.authRoutes(jwtConfig: JwtConfig) {

    post("/auth/login") {
        // 1. Receive login credentials
        val credentials = call.receive<LoginRequest>()

        // 2. Verify credentials (check database, verify password, etc.)
        val user = verifyCredentials(credentials) // Your implementation

        // 3. Create JWT payload
        val payload = JwtPayload.fromUserId(
            userId = user.id,
            email = user.email,
            roles = user.roles // e.g., listOf("user", "premium")
        )

        // 4. Generate token
        val token = jwtConfig.generateToken(payload)

        // 5. Return token to client
        call.respond(mapOf(
            "token" to token,
            "expiresIn" to "7 days"
        ))
    }

    post("/auth/refresh") {
        // Similar pattern for refresh tokens
    }
}
```

### 3. Verify Tokens (Protected Routes)

```kotlin
// Protect routes with JWT authentication

import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.protectedRoutes() {

    // All routes in this block require valid JWT
    authenticate("auth-jwt") {

        get("/users/me") {
            // Get the JWT principal from the call
            val principal = call.principal<JWTPrincipal>()!!
            val userId = principal.payload.subject
            val email = principal.payload.getClaim("email").asString()
            val roles = principal.payload.getClaim("roles").asList(String::class.java)

            // Use the user information
            call.respond(mapOf(
                "userId" to userId,
                "email" to email,
                "roles" to roles
            ))
        }

        get("/admin/dashboard") {
            val principal = call.principal<JWTPrincipal>()!!
            val roles = principal.payload.getClaim("roles").asList(String::class.java)

            // Check roles
            if ("admin" !in roles) {
                call.respond(HttpStatusCode.Forbidden, "Admin access required")
                return@get
            }

            // Admin-only logic here
            call.respond(mapOf("message" to "Welcome, admin!"))
        }
    }
}
```

### 4. Manual Token Verification (Advanced)

```kotlin
// If you need to manually verify tokens outside of Ktor's auth system

import com.auth0.jwt.exceptions.TokenExpiredException

suspend fun verifyAndProcessToken(tokenString: String, jwtConfig: JwtConfig) {
    try {
        // Verify and extract payload in one call
        val payload = jwtConfig.verifyAndExtract(tokenString)

        println("User ID: ${payload.sub}")
        println("Email: ${payload.email}")
        println("Roles: ${payload.roles}")

        // Process the authenticated user
        // ...

    } catch (e: TokenExpiredException) {
        // Token has expired
        println("Token expired, please login again")
    } catch (e: Exception) {
        // Invalid token (bad signature, wrong issuer, etc.)
        println("Invalid token: ${e.message}")
    }
}
```

## Token Structure

Generated tokens contain the following claims:

```json
{
  "sub": "123e4567-e89b-12d3-a456-426614174000",  // User ID
  "email": "user@example.com",                    // User email
  "roles": ["user", "premium"],                   // User roles
  "iat": 1707408000,                              // Issued at (Unix timestamp)
  "exp": 1708012800,                              // Expires at (iat + 7 days)
  "iss": "eros-backend",                          // Issuer
  "aud": "eros-users"                             // Audience
}
```

## Security Best Practices

1. **Secret Management**:
   - Never commit JWT_SECRET to version control
   - Use strong, randomly generated secrets (minimum 32 bytes)
   - Rotate secrets periodically in production

2. **Token Transmission**:
   - Always use HTTPS in production
   - Send tokens in the `Authorization` header: `Bearer <token>`
   - Never send tokens in URL parameters

3. **Token Storage** (Client-side):
   - Use httpOnly cookies for web applications
   - Use secure storage APIs for mobile applications
   - Avoid localStorage for highly sensitive applications

4. **Token Validation**:
   - Always verify issuer and audience claims
   - Check token expiration
   - Implement token revocation for critical operations

5. **Error Handling**:
   - Don't leak information in error messages
   - Log authentication failures for security monitoring
   - Implement rate limiting on auth endpoints

## Testing

The module includes comprehensive tests. Run with:

```bash
./gradlew :auth:test --tests JwtConfigTest
```

For integration testing, create test fixtures:

```kotlin
// In your test files
val testSettings = JwtSettings(
    secret = "test-secret-key-for-testing",
    issuer = "test-issuer",
    audience = "test-audience",
    realm = "test-realm"
)
val testJwtConfig = JwtConfig(testSettings)

// Generate test token
val testPayload = JwtPayload.fromUserId(
    userId = UUID.randomUUID(),
    email = "test@example.com",
    roles = listOf("user")
)
val testToken = testJwtConfig.generateToken(testPayload)
```

## Troubleshooting

### "JWT secret must not be blank"
- Ensure JWT_SECRET environment variable is set
- Check that application.yaml has `jwt.secret: ${JWT_SECRET:}`

### "The Token's Signature resulted invalid"
- Token was signed with a different secret
- Token was tampered with
- Using wrong JwtConfig instance for verification

### "Token has expired"
- Token is older than 7 days
- Consider implementing refresh token flow

### Tokens not working across environments
- Ensure JWT_SECRET is the same across all instances
- Check that issuer and audience match configuration
