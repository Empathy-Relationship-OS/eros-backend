package com.eros.users.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.common.errors.ConflictException
import com.eros.common.errors.ForbiddenException
import com.eros.common.errors.NotFoundException
import com.eros.users.ProfileAccessControl
import com.eros.users.models.CreateUserRequest
import com.eros.users.models.MediaType
import com.eros.users.models.PublicProfileResponse
import com.eros.users.models.Role
import com.eros.users.models.UpdateUserRequest
import com.eros.users.models.UserMediaCollection
import com.eros.users.models.UserMediaItem
import com.eros.users.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

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
         * Base route /users.
         */
        route("/users") {
            requireRoles("ADMIN", "USER", "EMPLOYEE")
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
            post {
                val principal = call.requireFirebasePrincipal()
                val request = call.receive<CreateUserRequest>()

                if (request.userId != principal.uid)
                    throw ForbiddenException("Cannot create profile for another user")

                if (userService.userExists(request.userId))
                    throw ConflictException("User profile already exists")

                val user = userService.createUser(request)
                call.respond(HttpStatusCode.Created, user)
            }


            //TODO:REMOVE ONCE MEDIASERVICE HAS BEEN CREATED / PUSHED
            fun createMediaItem(
                id: Long = 1L,
                userId: String = "user-123",
                mediaUrl: String = "https://example.com/photo${id}.jpg",
                mediaType: MediaType = MediaType.PHOTO,
                displayOrder: Int = 1,
                isPrimary: Boolean = false
            ): UserMediaItem {
                return UserMediaItem(
                    id = id,
                    userId = userId,
                    mediaUrl = mediaUrl,
                    mediaType = mediaType,
                    displayOrder = displayOrder,
                    isPrimary = isPrimary,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            }

            fun createMediaList(count: Int): List<UserMediaItem> {
                return (1..count).map { index ->
                    createMediaItem(id = index.toLong(), displayOrder = index, isPrimary = index == 1)
                }
            }

            /**
             * GET /users/{id}/public
             *
             * Retrieves a user's public profile by ID.
             *
             * Request Headers:
             * - Authorization: Bearer <firebase-id-token>
             *
             * Response: User JSON
             */
            get("/id/{id}/public") {
                //todo: Alter with matches, and media collection
                val principal = call.requireFirebasePrincipal()
                val targetUserId = call.parameters["id"]
                    ?: throw BadRequestException("User ID is required")

                // Ensure the user has access to the account or not.
                ProfileAccessControl.hasPublicProfileAccess(principal.uid, targetUserId)

                val user = userService.findByUserId(targetUserId)
                    ?: throw NotFoundException("User profile not found")

                //todo: Alter with media service
                val media = UserMediaCollection(
                    user.userId,
                    createMediaList(2),
                    2
                )//userMediaService.getMediaForUser(targetUserId)
                //todo: Alter with match service
                val sharedInterests =
                    listOf("Walks", "Shopping", "Running")//matchService.getSharedInterests(principal.uid, targetUserId)
                call.respond(HttpStatusCode.OK, PublicProfileResponse.from(user, media, sharedInterests))
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
            get("/me") {
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
            get("/exists") {
                val principal = call.requireFirebasePrincipal()
                val exists = userService.userExists(principal.uid)
                call.respond(HttpStatusCode.OK, UserExistsResponse(exists = exists, userId = principal.uid))
            }


            /**
             * GET /users/{id}
             *
             * Retrieves a user full profile by ID.
             *
             * Request Headers:
             * - Authorization: Bearer <firebase-id-token>
             *
             * Response: User JSON
             */
            get("/id/{id}") {
                val principal = call.requireFirebasePrincipal()
                val userId = call.parameters["id"] ?: throw BadRequestException("User ID is required")
                val user = userService.findByUserId(userId) ?: throw NotFoundException("User profile not found")
                call.respond(HttpStatusCode.OK, user)
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
            put("/me") {
                val principal = call.requireFirebasePrincipal()
                val request = call.receive<UpdateUserRequest>()
                val user = userService.updateUser(principal.uid, request)
                    ?: throw NotFoundException("User profile not found")
                call.respond(HttpStatusCode.OK, user)
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
            delete("/me") {
                val principal = call.requireFirebasePrincipal()
                val rowsDeleted = userService.deleteUser(principal.uid)

                if (rowsDeleted == 0) throw NotFoundException("User profile not found")

                call.application.log.info("User account deleted: ${principal.uid}")
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}