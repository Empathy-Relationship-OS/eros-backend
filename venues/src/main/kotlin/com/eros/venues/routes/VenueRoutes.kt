package com.eros.venues.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.common.errors.ForbiddenException
import com.eros.venues.models.AdminUpdateVenueRequest
import com.eros.venues.models.CreateVenueRequest
import com.eros.venues.models.UpdateVenueRequest
import com.eros.venues.models.toDTO
import com.eros.venues.service.VenueService
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.venueRoutes(venueService: VenueService){

    //todo: Implement once the venue contact information included
    route("/venue"){
        requireRoles("ADMIN","BUSINESS","EMPLOYEE")
        /**
         * Route for creating a venue.
         */
        post {

            val principal = call.requireFirebasePrincipal()
            val request = call.receive<CreateVenueRequest>()


            //if (principal.role == "BUSINESS" && request.venueId != principal.uid)
            //    throw ForbiddenException("Cannot create profile for another user")

            val venue = venueService.createVenue(request)

            call.respond(HttpStatusCode.Created, venue.toDTO())

        }

        /**
         * Route for updating a venue - Limited to certain fields.
         */
        patch{
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<UpdateVenueRequest>()

            val TEMP = 0L

            val updatedVenue = venueService.updateVenue(TEMP, request)
                ?: throw NotFoundException("Venue could not be found.")

            call.respond(HttpStatusCode.OK, updatedVenue.toDTO())
        }

    }

    /**
     * Admin routes for venues.
     */
    route("/venue/{id}/admin"){
        requireRoles("ADMIN","EMPLOYEE")
        /**
         * Route for an admin to update a venue.
         */
        patch{
            val targetVenueId = call.parameters["id"]?.toLongOrNull()
                ?: throw BadRequestException("Venue ID is required")

            val request = call.receive<AdminUpdateVenueRequest>()

            val updatedVenue = venueService.updateVenueAdmin(targetVenueId, request)
                ?: throw NotFoundException("Could not update venue with id: $targetVenueId")

            call.respond(HttpStatusCode.OK, updatedVenue.toDTO())
        }

    }


}