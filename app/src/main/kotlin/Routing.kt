package com.eros

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(RequestValidation) {
        validate<String> { bodyText ->
            if (!bodyText.startsWith("Hello"))
                ValidationResult.Invalid("Body text should start with 'Hello'")
            else ValidationResult.Valid
        }
    }
    install(StatusPages) {
        // Handle specific application exceptions
        // Authentication failures are handled by the Authentication plugin's challenge mechanism
        exception<IllegalArgumentException> { call, cause ->
            call.respondText(text = "400: ${cause.message}" , status = HttpStatusCode.BadRequest)
        }
        exception<IllegalStateException> { call, cause ->
            call.respondText(text = "500: ${cause.message}" , status = HttpStatusCode.InternalServerError)
        }
        // Catch-all handler for any uncaught exceptions
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respondText(
                text = "500: Internal Server Error - ${cause.message ?: "An unexpected error occurred"}",
                status = HttpStatusCode.InternalServerError
            )
        }
    }
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        // Protected routes requiring JWT authentication
        authenticate("jwt-auth") {
            // TODO: Implement /users/me endpoint
            // This endpoint should return the authenticated user's profile information
            // Access the JWT payload via: val payload = call.principal<JwtPayload>()
            // Example response: { "userId": "...", "email": "...", "profile": {...} }
            get("/users/me") {
                val payload = requireNotNull(call.principal<JwtPayload>()) {
                    "No authentication principal found"
                }

                // Placeholder response - actual implementation should fetch user data from database
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "userId" to payload.userId.toString(),
                        "email" to payload.email,
                        "message" to "TODO: Implement user profile retrieval from database"
                    )
                )
            }
        }
    }
}
