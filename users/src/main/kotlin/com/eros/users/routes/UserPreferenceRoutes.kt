package com.eros.users.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.ConflictException
import com.eros.common.errors.ForbiddenException
import com.eros.common.errors.NotFoundException
import com.eros.users.models.CreatePreferenceRequest
import com.eros.users.models.UpdatePreferenceRequest
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

class UserPreferenceRoutes {

    fun Route.userPreferenceRoutes(userPreferenceService: PreferenceService) {
        route("/preference") {
            requireRoles("USER", "ADMIN", "EMPLOYEE")

            post {
                val principal = call.requireFirebasePrincipal()
                val request = call.receive<CreatePreferenceRequest>()

                if (request.userId != principal.uid)
                    throw ForbiddenException("Cannot create preferences for another user")

                if (userPreferenceService.doesExist(request.userId))
                    throw ConflictException("User preferences already exist")

                val userPreferences = userPreferenceService.createPreferences(request)
                call.respond(HttpStatusCode.OK, userPreferences)
            }

            patch("/me"){
                val principal = call.requireFirebasePrincipal()
                val request = call.receive<UpdatePreferenceRequest>()

                if (request.userId != principal.uid)
                    throw ForbiddenException("Cannot create preferences for another user")

                val preferences = userPreferenceService.updatePreferences(request.id, request) ?:
                    throw NotFoundException("User preferences not found.")
                call.respond(HttpStatusCode.OK, preferences)
            }

            get("/me"){
                val principal = call.requireFirebasePrincipal()
                val preferences = userPreferenceService.findByUserId(principal.uid) ?:
                throw NotFoundException("User preferences not found.")
                call.respond(HttpStatusCode.OK, preferences)
            }


            delete("/me") {
                val principal = call.requireFirebasePrincipal()

                // Check if preferences exist
                if (!userPreferenceService.doesExist(principal.uid)) {
                    throw NotFoundException("User preferences not found.")
                }

                // Delete the preferences.
                val deleted = userPreferenceService.delete(principal.uid)

                if (deleted > 0) {
                    call.respond(HttpStatusCode.OK, "User Preference successfully deleted")
                } else {
                    throw NotFoundException("User preferences not found.")
                }
            }
        }
    }

}