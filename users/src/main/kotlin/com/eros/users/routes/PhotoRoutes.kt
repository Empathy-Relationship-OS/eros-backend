package com.eros.users.routes

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.users.models.ConfirmUploadRequest
import com.eros.users.models.PresignedUploadRequest
import com.eros.users.service.PhotoService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// TODO move this into the standardised way of routing being lazy and keeping it here for now
// TODO need to pull in latest exception handling and alter this approach
/**
 * Photo management routes under `/users/me/photos`.
 *
 * All routes require Firebase authentication.
 *
 * ## Upload flow
 * 1. `POST /users/me/photos/presigned-url` — get a presigned S3 PUT URL
 * 2. Client uploads the file directly to S3
 * 3. `POST /users/me/photos` — confirm the upload; backend verifies S3 and saves to DB
 *
 * ## Other operations
 * - `GET  /users/me/photos` — list all photos
 * - `DELETE /users/me/photos/{photoId}` — delete a photo
 * - `PUT /users/me/photos/{photoId}/primary` — set as primary photo
 */
fun Route.photoRoutes(photoService: PhotoService) {

    authenticate("firebase-auth") {
        route("/users/me/photos") {
            /**
             * POST /users/me/photos/presigned-url
             *
             * Step 1 of the upload flow. Validates file metadata and returns a presigned
             * S3 PUT URL that the client uses to upload the file directly to S3.
             *
             * Request body: [PresignedUploadRequest]
             * Response: [com.eros.users.models.PresignedUploadResponse]
             */
            post("/presigned-url") {
                val principal = call.principal<FirebaseUserPrincipal>()
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "unauthorized", "message" to "Firebase authentication required")
                    )

                try {
                    val request = call.receive<PresignedUploadRequest>()
                    val response = photoService.generatePresignedUploadUrl(principal.uid, request)
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: IllegalArgumentException) {
                    call.application.log.warn("Invalid presigned URL request for ${principal.uid}", e)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "invalid_request", "message" to (e.message ?: "Invalid input"))
                    )
                } catch (e: IllegalStateException) {
                    call.application.log.warn("Photo limit reached for ${principal.uid}", e)
                    call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "photo_limit_reached", "message" to (e.message ?: "Photo limit reached"))
                    )
                } catch (e: Exception) {
                    call.application.log.error("Error generating presigned URL for ${principal.uid}", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "server_error", "message" to "Failed to generate upload URL")
                    )
                }
            }

            /**
             * POST /users/me/photos
             *
             * Step 2 of the upload flow. Confirms a completed S3 upload, performs a
             * HeadObject check, and persists the media record.
             *
             * Request body: [ConfirmUploadRequest]
             * Response: [com.eros.users.models.UserMediaItem]
             */
            post {
                val principal = call.principal<FirebaseUserPrincipal>()
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "unauthorized", "message" to "Firebase authentication required")
                    )

                try {
                    val request = call.receive<ConfirmUploadRequest>()
                    val item = photoService.confirmUpload(principal.uid, request)
                    call.respond(HttpStatusCode.Created, item)
                } catch (e: IllegalArgumentException) {
                    call.application.log.warn("Upload confirmation failed for ${principal.uid}", e)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "upload_not_found", "message" to (e.message ?: "Upload not found in S3"))
                    )
                } catch (e: Exception) {
                    call.application.log.error("Error confirming upload for ${principal.uid}", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "server_error", "message" to "Failed to confirm upload")
                    )
                }
            }

            /**
             * GET /users/me/photos
             *
             * Returns all photos for the authenticated user, ordered by displayOrder.
             *
             * Response: [com.eros.users.models.UserMediaCollection]
             */
            get {
                val principal = call.principal<FirebaseUserPrincipal>()
                    ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "unauthorized", "message" to "Firebase authentication required")
                    )

                try {
                    val collection = photoService.getUserMedia(principal.uid)
                    call.respond(HttpStatusCode.OK, collection)
                } catch (e: Exception) {
                    call.application.log.error("Error fetching photos for ${principal.uid}", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "server_error", "message" to "Failed to fetch photos")
                    )
                }
            }

            /**
             * DELETE /users/me/photos/{photoId}
             *
             * Deletes a photo from S3 and the database.
             *
             * Response: 204 No Content on success, 404 if not found.
             */
            delete("/{photoId}") {
                val principal = call.principal<FirebaseUserPrincipal>()
                    ?: return@delete call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "unauthorized", "message" to "Firebase authentication required")
                    )

                val photoId = call.parameters["photoId"]?.toLongOrNull()
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "invalid_request", "message" to "Photo ID must be a number")
                    )

                try {
                    val deleted = photoService.deletePhoto(principal.uid, photoId)
                    if (deleted == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to "photo_not_found", "message" to "Photo not found")
                        )
                    } else {
                        call.respond(HttpStatusCode.NoContent)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Error deleting photo $photoId for ${principal.uid}", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "server_error", "message" to "Failed to delete photo")
                    )
                }
            }

            /**
             * PUT /users/me/photos/{photoId}/primary
             *
             * Sets the specified photo as the user's primary photo.
             *
             * Response: Updated [com.eros.users.models.UserMediaItem], or 404 if not found.
             */
            put("/{photoId}/primary") {
                val principal = call.principal<FirebaseUserPrincipal>()
                    ?: return@put call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "unauthorized", "message" to "Firebase authentication required")
                    )

                val photoId = call.parameters["photoId"]?.toLongOrNull()
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "invalid_request", "message" to "Photo ID must be a number")
                    )

                try {
                    val updated = photoService.setPrimaryPhoto(principal.uid, photoId)
                    if (updated == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to "photo_not_found", "message" to "Photo not found")
                        )
                    } else {
                        call.respond(HttpStatusCode.OK, updated)
                    }
                } catch (e: Exception) {
                    call.application.log.error("Error setting primary photo $photoId for ${principal.uid}", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "server_error", "message" to "Failed to update primary photo")
                    )
                }
            }
        }
    }
}
