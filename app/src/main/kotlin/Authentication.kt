package com.eros

import com.eros.auth.firebase.FirebaseAuthService
import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.auth.firebase.configureFirebase
import com.google.firebase.auth.FirebaseAuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*

/**
 * Configures Firebase-based authentication for the application.
 *
 * This plugin sets up Firebase authentication with the following features:
 * - Initializes Firebase Admin SDK on application startup
 * - Validates Firebase ID tokens (JWTs) from client applications
 * - Extracts user information (UID, email, phone) from verified tokens
 * - Returns 401 Unauthorized for invalid or expired tokens
 * - Provides type-safe access to user data via FirebaseUserPrincipal
 *
 * Configuration (application.yaml):
 * - firebase.serviceAccountPath: Path to Firebase service account JSON
 * - firebase.projectId: Firebase project ID
 *
 * Usage in routes:
 * ```kotlin
 * authenticate("firebase-auth") {
 *     get("/protected") {
 *         val principal = call.principal<FirebaseUserPrincipal>()
 *         // Access principal.uid, principal.email, etc.
 *     }
 * }
 * ```
 *
 * Client Authentication Flow:
 * 1. Client authenticates with Firebase (email/password, phone, social)
 * 2. Client receives Firebase ID token (JWT)
 * 3. Client sends token in Authorization header: "Bearer <id-token>"
 * 4. Backend verifies JWT with Firebase and extracts user info
 *
 * Security Notes:
 * - Firebase handles: password hashing, OTP verification, JWT generation
 * - Backend handles: JWT verification, user profile management
 * - Always use HTTPS in production
 * - Firebase ID tokens expire after 1 hour by default
 *
 * @throws IllegalStateException if Firebase configuration is missing
 */
fun Application.configureAuthentication() {
    // Initialize Firebase Admin SDK first
    configureFirebase()

    install(Authentication) {
        // Configure Firebase JWT token authentication
        bearer("firebase-auth") {
            realm = "eros-api"

            // Authenticate by verifying Firebase ID token (JWT)
            authenticate { credential ->
                try {
                    // Verify the Firebase ID token using Firebase Admin SDK
                    val principal = FirebaseAuthService.verifyTokenOrNull(credential.token)

                    if (principal == null) {
                        application.log.warn("Firebase authentication failed: Invalid or expired token")
                    }

                    principal
                } catch (e: FirebaseAuthException) {
                    application.log.warn("Firebase authentication error: ${e.message}")
                    null
                } catch (e: Exception) {
                    application.log.error("Unexpected Firebase authentication error", e)
                    null
                }
            }
        }
    }

    log.info("Firebase authentication configured successfully")
}
