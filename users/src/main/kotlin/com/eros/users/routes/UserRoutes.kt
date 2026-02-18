package com.eros.users.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.common.errors.ConflictException
import com.eros.common.errors.ForbiddenException
import com.eros.common.errors.NotFoundException
import com.eros.users.models.CreateUserRequest
import com.eros.users.models.UpdateUserRequest
import com.eros.users.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import java.sql.SQLException

/**
 * Response model for user existence check
 */
@Serializable
data class UserExistsResponse(
    val exists: Boolean,
    val userId: String
)

/**
 * Configure user routes.
 *
 * These routes handle user profile CRUD operations.
 * All routes require Firebase authentication.
 *
 * @param userService Service for user data operations
 */
fun Route.userRoutes(userService: UserService) {

    // All user routes require Firebase authentication
    authenticate("firebase-auth") {

        /**
         * POST /users
         *
         * Creates a new user profile.
         *
         * Request Headers:
         * - Authorization: Bearer <firebase-id-token>
         *
         * Request Body: CreateUserRequest JSON
         * Response: User JSON
         */
        post("/users") {
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<CreateUserRequest>()

            if (request.userId != principal.uid)
                throw ForbiddenException("Cannot create profile for another user")

            if (userService.userExists(request.userId))
                throw ConflictException("User profile already exists")

            val user = userService.createUser(request)
            call.respond(HttpStatusCode.Created, user)
        }

        /**
         * GET /users/me
         *
         * Retrieves the current authenticated user's profile.
         *
         * Request Headers:
         * - Authorization: Bearer <firebase-id-token>
         *
         * Response: User JSON
         */
        get("/users/me") {
            val principal = call.requireFirebasePrincipal()
            val user = userService.findByUserId(principal.uid)
                ?: throw NotFoundException("User profile not found. Create a profile first.")
            call.respond(HttpStatusCode.OK, user)
        }

        /**
         * GET /users/exists
         *
         * Checks if the current authenticated user has a profile.
         *
         * Request Headers:
         * - Authorization: Bearer <firebase-id-token>
         *
         * Response: UserExistsResponse JSON
         */
        get("/users/exists") {
            val principal = call.requireFirebasePrincipal()

            try {
                val exists = userService.userExists(principal.uid)
                call.respond(HttpStatusCode.OK, UserExistsResponse(exists = exists, userId = principal.uid))
            } catch (e: Exception) {
                call.application.log.error("Error checking user existence", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "server_error", "message" to "Failed to check user existence")
                )
            }
        }

        /**
         * GET /users/{id}
         *
         * Retrieves a user profile by ID.
         *
         * Request Headers:
         * - Authorization: Bearer <firebase-id-token>
         *
         * Response: User JSON
         */
        get("/users/{id}") {
            val userId = call.parameters["id"]
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "invalid_request", "message" to "User ID is required")
                )

            try {
                val user = userService.findByUserId(userId)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "user_not_found", "message" to "User profile not found")
                    )

                call.respond(HttpStatusCode.OK, user)
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
         * PUT /users/me
         *
         * Updates the current authenticated user's profile.
         *
         * Request Headers:
         * - Authorization: Bearer <firebase-id-token>
         *
         * Request Body: UpdateUserRequest JSON
         * Response: User JSON
         */
        put("/users/me") {
            val principal = call.requireFirebasePrincipal()

            try {
                val request = call.receive<UpdateUserRequest>()
                val user = userService.updateUser(principal.uid, request)
                    ?: return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "user_not_found", "message" to "User profile not found")
                    )

                call.respond(HttpStatusCode.OK, user)
            } catch (e: IllegalArgumentException) {
                call.application.log.warn("Invalid input while updating user profile", e)
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "invalid_input", "message" to (e.message ?: "Invalid input"))
                )
            } catch (e: ExposedSQLException) {
                call.application.log.error("Database error updating user profile", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "database_error", "message" to "Database operation failed")
                )
            } catch (e: SQLException) {
                call.application.log.error("SQL error updating user profile", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "database_error", "message" to "Database operation failed")
                )
            } catch (e: Exception) {
                call.application.log.error("Error updating user profile", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "server_error", "message" to "Failed to update user profile")
                )
            }
        }

        /**
         * DELETE /users/me
         *
         * Deletes (soft delete) the current authenticated user's account.
         *
         * Request Headers:
         * - Authorization: Bearer <firebase-id-token>
         *
         * Response: 204 No Content on success
         */
        delete("/users/me") {
            val principal = call.requireFirebasePrincipal()

            try {
                val rowsDeleted = userService.deleteUser(principal.uid)

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