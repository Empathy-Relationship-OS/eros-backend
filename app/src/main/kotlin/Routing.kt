package com.eros

import com.eros.auth.repository.AuthRepositoryImpl
import com.eros.auth.routes.authRoutes
import io.ktor.http.*
import io.ktor.server.application.*
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

    // Initialize repositories
    val authRepository = AuthRepositoryImpl()

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        // Auth routes (Firebase authenticated)
        route("/auth") {
            authRoutes(authRepository)
        }
    }
}
