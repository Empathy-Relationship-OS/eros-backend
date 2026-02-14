package com.eros.auth.routes

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.auth.models.SyncProfileResponse
import com.eros.auth.models.UserSummary
import com.eros.auth.repository.AuthRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import java.sql.SQLException

/**
 * Configure authentication routes for Firebase Auth integration.
 *
 * These routes handle syncing Firebase-authenticated users with the backend database.
 * All routes require a valid Firebase ID token in the Authorization header.
 *
 * @param authRepository Repository for user data operations
 */
fun Route.authRoutes(authRepository: AuthRepository) {

    // All auth routes require Firebase authentication
    authenticate("firebase-auth") {

        /**
         * POST /auth/sync-profile
         *
         * Syncs a Firebase-authenticated user with the backend database.
         * Creates a new user record if it doesn't exist, or updates if it does.
         *
         * This endpoint should be called after successful Firebase authentication
         * on the client side to ensure the user exists in the backend database.
         *
         * Request Headers:
         * - Authorization: Bearer <firebase-id-token>
         *
         * Response: SyncProfileResponse with user data and isNewUser flag
         */
        post("/sync-profile") {
            val principal = call.principal<FirebaseUserPrincipal>()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "unauthorized", "message" to "Firebase authentication required")
                )

            try {
                // Create or update user in database (atomic operation)
                val upsertResult = authRepository.createOrUpdateUser(
                    firebaseUid = principal.uid,
                    email = principal.email ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "invalid_token", "message" to "Firebase token must contain email")
                    ),
                    phone = principal.phoneNumber
                )

                // Update last active timestamp
                authRepository.updateLastActiveAt(principal.uid)

                val response = SyncProfileResponse(
                    user = UserSummary(
                        userId = upsertResult.user.id,
                        email = upsertResult.user.email,
                        phone = upsertResult.user.phone,
                        emailVerified = principal.emailVerified
                    ),
                    isNewUser = upsertResult.wasCreated
                )

                call.respond(HttpStatusCode.OK, response)
            } catch (e: IllegalArgumentException) {
                call.application.log.warn("Invalid input while syncing user profile", e)
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "invalid_input", "message" to (e.message ?: "Invalid input"))
                )
            } catch (e: ExposedSQLException) {
                call.application.log.error("Database error syncing user profile", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "database_error", "message" to "Database operation failed")
                )
            } catch (e: SQLException) {
                call.application.log.error("SQL error syncing user profile", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "database_error", "message" to "Database operation failed")
                )
            } catch (e: Exception) {
                call.application.log.error("Error syncing user profile", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "server_error", "message" to "Failed to sync user profile")
                )
            }
        }

        /**
         * GET /auth/me
         *
         * Retrieves the current authenticated user's profile.
         *
         * Request Headers:
         * - Authorization: Bearer <firebase-id-token>
         *
         * Response: UserSummary with user data
         */
        get("/me") {
            val principal = call.principal<FirebaseUserPrincipal>()
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "unauthorized", "message" to "Firebase authentication required")
                )

            try {
                val user = authRepository.findByFirebaseUid(principal.uid)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf(
                            "error" to "user_not_found",
                            "message" to "User profile not found. Call POST /auth/sync-profile first."
                        )
                    )

                // Update last active timestamp
                authRepository.updateLastActiveAt(principal.uid)

                val userSummary = UserSummary(
                    userId = user.id,
                    email = user.email,
                    phone = user.phone,
                    emailVerified = principal.emailVerified
                )

                call.respond(HttpStatusCode.OK, userSummary)
            } catch (e: ExposedSQLException) {
                call.application.log.error("Database error fetching user profile", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "database_error", "message" to "Database operation failed")
                )
            } catch (e: SQLException) {
                call.application.log.error("SQL error fetching user profile", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "database_error", "message" to "Database operation failed")
                )
            } catch (e: Exception) {
                call.application.log.error("Error fetching user profile", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "server_error", "message" to "Failed to fetch user profile")
                )
            }
        }

        /**
         * DELETE /auth/delete-account
         *
         * Deletes the current authenticated user's account from the backend.
         *
         * Note: This only deletes the backend user data. The Firebase Auth account
         * must be deleted separately on the client side using Firebase SDK.
         *
         * Request Headers:
         * - Authorization: Bearer <firebase-id-token>
         *
         * Response: 204 No Content on success
         */
        delete("/delete-account") {
            val principal = call.principal<FirebaseUserPrincipal>()
                ?: return@delete call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "unauthorized", "message" to "Firebase authentication required")
                )

            try {
                val rowsDeleted = authRepository.deleteUser(principal.uid)

                if (rowsDeleted == 0) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "user_not_found", "message" to "User profile not found")
                    )
                } else {
                    call.application.log.info("User account deleted: ${principal.uid}")
                    call.respond(HttpStatusCode.NoContent)
                }
            } catch (e: ExposedSQLException) {
                call.application.log.error("Database error deleting user account", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "database_error", "message" to "Database operation failed")
                )
            } catch (e: SQLException) {
                call.application.log.error("SQL error deleting user account", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "database_error", "message" to "Database operation failed")
                )
            } catch (e: Exception) {
                call.application.log.error("Error deleting user account", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "server_error", "message" to "Failed to delete user account")
                )
            }
        }
    }
}
