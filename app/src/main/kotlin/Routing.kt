package com.eros

import com.eros.users.routes.qaRoutes
import com.eros.users.routes.cityRoutes
import com.eros.users.routes.questionRoutes
import com.eros.users.routes.userPhotoRoutes
import com.eros.users.routes.userPreferenceRoutes
import com.eros.users.routes.userProfileRoutes
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.requestvalidation.*
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

    // Get the services from the ServiceContainer.kt
    val services = this.services

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        // All routes require Firebase authentication
        authenticate("firebase-auth") {
            userProfileRoutes(services.userService, services.profileAccessControl)
            userPhotoRoutes(services.photoService)
            cityRoutes(services.cityService)
            userPreferenceRoutes(services.preferenceService)
            qaRoutes(services.qaService, services.profileAccessControl)
            questionRoutes(services.qaService)
        }
    }
}
