package com.eros.users.routes

import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.common.errors.NotFoundException
import com.eros.users.models.CreateCityRequest
import com.eros.users.models.UpdateCityRequest
import com.eros.users.models.toDTO
import com.eros.users.service.CityService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.cityRoutes(cityService: CityService) {
    // User routes
    route("/city") {
        requireRoles("USER", "ADMIN", "EMPLOYEE")
        get("/all"){
            val cities = cityService.getAllCities().map { it.toDTO() }
            call.respond(HttpStatusCode.OK, cities)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLong()
                ?: throw BadRequestException("Invalid city ID provided.")

            val city = cityService.findByCityId(id)
                ?: throw NotFoundException("City not found.")
            call.respond(HttpStatusCode.OK, city.toDTO())
        }

        /**
         * Path to retrieve the nearest SINGLE/MULTIPLE nearest city/s.
         */
        get("/nearest"){
            throw NotImplementedError("/city/nearest has not been implemented")
            /*
            Provide NearestCityRequest (lat, long)
            Return Single or list of nearest cities
             */
        }
    }

    route("/city/admin") {
        // Protected endpoints - only ADMIN, EMPLOYEE
        requireRoles("ADMIN", "EMPLOYEE")
        patch("/{id}") {
            val id = call.parameters["id"]?.toLong()
                ?: throw BadRequestException("Invalid city ID provided.")

            val request = call.receive<UpdateCityRequest>()
            val updated = cityService.updateCity(id, request)
                ?: throw NotFoundException("City not found")

            call.respond(HttpStatusCode.OK, updated.toDTO())
        }
        post {
            val request = call.receive<CreateCityRequest>()
            // Database unique constraint handles duplicate prevention
            // ConflictException is thrown from repository layer on duplicate key violation
            val city = cityService.createCity(request)
            call.respond(HttpStatusCode.Created, city.toDTO())
        }

        delete("/{id}"){
            val id = call.parameters["id"]?.toLong()
                ?: throw BadRequestException("Invalid city ID provided.")

            val deleted = cityService.deleteCity(id)
            if (deleted > 0) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, "City not found")
            }
        }
    }

}
