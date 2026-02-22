package com.eros.users.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.common.errors.NotFoundException
import com.eros.users.models.ConfirmUploadRequest
import com.eros.users.models.PresignedUploadRequest
import com.eros.users.service.PhotoService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
fun Route.userPhotoRoutes(photoService: PhotoService) {
    route("/users/me/photos") {
        requireRoles("ADMIN", "USER", "EMPLOYEE")
            /**
             * POST /users/me/photos/presigned-url
             *
             * Step 1 of the upload flow. Validates file metadata and returns a presigned
             * S3 PUT URL that the client uses to upload the file directly to S3.
             *
             * Request Headers:
             * - Authorization: Bearer <firebase-id-token>
             *
             * Request Body: PresignedUploadRequest JSON
             * Response: PresignedUploadResponse JSON
             */
            post("/presigned-url") {
                val principal = call.requireFirebasePrincipal()
                val request = call.receive<PresignedUploadRequest>()
                val response = photoService.generatePresignedUploadUrl(principal.uid, request)
                call.respond(HttpStatusCode.OK, response)
            }

            /**
             * POST /users/me/photos
             *
             * Step 2 of the upload flow. Confirms a completed S3 upload, performs a
             * HeadObject check, and persists the media record.
             *
             * Request Headers:
             * - Authorization: Bearer <firebase-id-token>
             *
             * Request Body: ConfirmUploadRequest JSON
             * Response: UserMediaItem JSON
             */
            post {
                val principal = call.requireFirebasePrincipal()
                val request = call.receive<ConfirmUploadRequest>()
                val item = photoService.confirmUpload(principal.uid, request)
                call.respond(HttpStatusCode.Created, item)
            }

            /**
             * GET /users/me/photos
             *
             * Returns all photos for the authenticated user, ordered by displayOrder.
             *
             * Request Headers:
             * - Authorization: Bearer <firebase-id-token>
             *
             * Response: UserMediaCollection JSON
             */
            // TODO user gets profile not directly, but from the PublicProfile. Should only be used for admin purposes
            get {
                val principal = call.requireFirebasePrincipal()
                val collection = photoService.getUserMedia(principal.uid)
                call.respond(HttpStatusCode.OK, collection)
            }

            /**
             * DELETE /users/me/photos/{photoId}
             *
             * Deletes a photo from S3 and the database.
             *
             * Request Headers:
             * - Authorization: Bearer <firebase-id-token>
             *
             * Response: 204 No Content on success
             */
            delete("/{photoId}") {
                val principal = call.requireFirebasePrincipal()
                val photoId = call.parameters["photoId"]?.toLongOrNull()
                    ?: throw BadRequestException("Photo ID must be a number")

                val deleted = photoService.deletePhoto(principal.uid, photoId)
                    ?: throw NotFoundException("Photo not found")

                call.respond(HttpStatusCode.NoContent)
            }

//            /**
//             * PUT /users/me/photos/{photoId}/primary
//             *
//             * Sets the specified photo as the user's primary photo.
//             *
//             * Request Headers:
//             * - Authorization: Bearer <firebase-id-token>
//             *
//             * Response: Updated UserMediaItem JSON
//             */
//            put("/{photoId}/primary") {
//                val principal = call.requireFirebasePrincipal()
//                val photoId = call.parameters["photoId"]?.toLongOrNull()
//                    ?: throw BadRequestException("Photo ID must be a number")
//
//                val updated = photoService.setPrimaryPhoto(principal.uid, photoId)
//                    ?: throw NotFoundException("Photo not found")
//
//                call.respond(HttpStatusCode.OK, updated)
//            }
    }
}
