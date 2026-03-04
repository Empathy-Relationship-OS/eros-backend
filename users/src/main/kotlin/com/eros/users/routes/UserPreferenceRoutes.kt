package com.eros.users.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.common.errors.ConflictException
import com.eros.common.errors.ForbiddenException
import com.eros.common.errors.NotFoundException
import com.eros.users.models.CreatePreferenceRequest
import com.eros.users.models.UpdatePreferenceRequest
import com.eros.users.models.toDTO
import com.eros.users.service.PreferenceService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.delete
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.LoggerFactory


private val logger = LoggerFactory.getLogger(Route::class.java)

fun Route.userPreferenceRoutes(userPreferenceService: PreferenceService) {
    route("/preference") {
        requireRoles("USER", "ADMIN", "EMPLOYEE")

        post {
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<CreatePreferenceRequest>()

            if (request.userId != principal.uid)
                throw ForbiddenException("Cannot create preferences for another user")

            // Database primary key constraint handles duplicate prevention
            // ConflictException is thrown from repository layer on duplicate key violation
            val userPreferences = userPreferenceService.createPreferences(request)
            logger.info("Successful preference creation: ${userPreferences.toDTO()}")
            call.respond(HttpStatusCode.Created, userPreferences.toDTO())
        }

        patch("/me") {
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<UpdatePreferenceRequest>()

            if (request.userId != principal.uid)
                throw ForbiddenException("Cannot create preferences for another user")

            val preferences = userPreferenceService.updatePreferences(request.userId, request)
                ?: throw NotFoundException("User preferences not found.")
            logger.info("Successful preference update: ${preferences.toDTO()}")
            call.respond(HttpStatusCode.OK, preferences.toDTO())
        }

        get("/me") {
            val principal = call.requireFirebasePrincipal()
            val preferences = userPreferenceService.findByUserId(principal.uid)
                ?: throw NotFoundException("User preferences not found.")
            logger.info("Successful preference retrieval: ${preferences.toDTO()}")
            call.respond(HttpStatusCode.OK, preferences.toDTO())
        }


        get("/id/{id}"){
            val principal = call.requireFirebasePrincipal()
            val targetUserId = call.parameters["id"] ?: throw BadRequestException("User ID is required")

            //todo: Ensure the user has a match
            val hasMatch = true //matchService.findMatch(principal.uid, targetUserId)
            val preferences = userPreferenceService.findByUserId(targetUserId)
                ?: throw NotFoundException("User preferences not found.")
            logger.info("Successful preference retrieval of another user: ${preferences.toDTO()}")
            logger.info(
                "Creating preferences for user",
                keyValue("userId", preferences.userId),
                keyValue("cityCount", preferences.dateCities.size)
            )
            call.respond(HttpStatusCode.OK, preferences.toDTO())
        }


        /**
         * Delete a users preferences
         *
         * Return 204 upon successful deletion.
         */
        delete("/me") {
            val principal = call.requireFirebasePrincipal()

            // Check if preferences exist
            if (!userPreferenceService.doesExist(principal.uid)) {
                throw NotFoundException("User preferences not found.")
            }

            // Delete the preferences.
            val deleted = userPreferenceService.delete(principal.uid)

            if (deleted > 0) {
                logger.info("Successful preference deletion for user: ${principal.uid}")
                call.respond(HttpStatusCode.NoContent, "User Preference successfully deleted")
            } else {
                throw NotFoundException("User preferences not found.")
            }
        }
    }
}

