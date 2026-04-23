package com.eros

import com.eros.common.config.S3Config
import com.eros.matching.repository.DailyBatchRepositoryImpl
import com.eros.matching.repository.MatchRepositoryImpl
import com.eros.matching.routes.matchRoutes
import com.eros.matching.service.MatchService
import com.eros.matching.transaction.DatabaseTransactionManager
import com.eros.users.ProfileAccessControl
import com.eros.users.repository.*
import com.eros.users.routes.*
import com.eros.users.service.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.swagger.*
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

    val transactionManager = DatabaseTransactionManager()

    // Initialize repositories
    val userRepository = UserRepositoryImpl()
    val photoRepository = PhotoRepositoryImpl()

    val cityRepositoryImpl = CityRepositoryImpl()
    val preferenceRepositoryImpl = PreferenceRepositoryImpl()

    val qaRepository = UserQARepositoryImpl()
    val questionRepository = QuestionRepositoryImpl()

    val matchRepository = MatchRepositoryImpl()
    val dailyBatchRepository = DailyBatchRepositoryImpl()

    // Initialize configs
    val s3Config = S3Config.fromApplicationConfig(environment.config)

    // Initialize services
    val photoService = PhotoService(photoRepository, s3Config)
    val qaService = QAService(questionRepository, qaRepository)
    val userService = UserService(userRepository, photoService, qaService)
    val cityService = CityService(cityRepositoryImpl)
    val preferenceService = PreferenceService(preferenceRepositoryImpl, userService)
    val matchService = MatchService(matchRepository, dailyBatchRepository, userService, transactionManager)

    val matchAccessChecker = MatchAccessCheckerImpl(matchService)
    val profileAccessControl = ProfileAccessControl(matchAccessChecker)
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        // Swagger UI endpoint
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml") {
            version = "5.32.1"
        }

        // OpenAPI documentation endpoint
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")

        // All routes require Firebase authentication
        authenticate("firebase-auth") {
            // User profile routes (handles role requirements internally)
            userProfileRoutes(userService, profileAccessControl)

            // Photo management routes (all require roles)
            userPhotoRoutes(photoService)

            cityRoutes(cityService)

            userPreferenceRoutes(preferenceService)

            qaRoutes(qaService,profileAccessControl)

            questionRoutes(qaService)

            matchRoutes(matchService)
        }
    }
}
