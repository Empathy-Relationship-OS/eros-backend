package com.eros.users.routes

import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.common.errors.ConflictException
import com.eros.common.errors.NotFoundException
import com.eros.users.models.CreateCityRequest
import com.eros.users.models.UpdateCityRequest
import com.eros.users.service.CityService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

class CityRoutes {

    fun Route.cityRoutes(cityService: CityService) {
        route("/city") {
            requireRoles("ADMIN","EMPLOYEE")

            post{
                val request = call.receive<CreateCityRequest>()

                if (cityService.doesExists(request.cityName))
                    throw ConflictException("City already exists")

                val city = cityService.createCity(request)
                call.respond(HttpStatusCode.OK, city)
            }

            get("/{id}"){
                val id = call.parameters["id"]?.toLong()
                    ?: throw BadRequestException("Invalid city ID provided.")

                val city = cityService.findByCityId(id) ?: throw NotFoundException("City not found.")
                call.respond(HttpStatusCode.OK, city)
            }

            patch("/{id}") {
                val id = call.parameters["id"]?.toLong()
                    ?: throw BadRequestException("Invalid city ID provided.")

                val request = call.receive<UpdateCityRequest>()

                val updated = cityService.updateCity(id, request) ?: throw NotFoundException("City not found")

                call.respond(HttpStatusCode.OK, updated)
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toLong()
                    ?:  throw BadRequestException("Invalid city ID provided.")

                val deleted = cityService.deleteCity(id)
                if (deleted > 0) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, "City not found")
                }
            }
        }
    }
}