package com.eros

import com.eros.common.config.S3Config
import com.eros.matching.repository.DailyBatchRepositoryImpl
import com.eros.matching.repository.MatchRepositoryImpl
import com.eros.matching.routes.matchRoutes
import com.eros.matching.service.MatchService
import com.eros.matching.transaction.DatabaseTransactionManager
import com.eros.users.ProfileAccessControl
import com.eros.users.repository.PhotoRepositoryImpl
import com.eros.users.repository.PreferenceRepositoryImpl
import com.eros.users.repository.QuestionRepositoryImpl
import com.eros.users.repository.UserQARepositoryImpl
import com.eros.users.repository.UserRepositoryImpl
import com.eros.users.routes.qaRoutes
import com.eros.users.routes.cityRoutes
import com.eros.users.routes.questionRoutes
import com.eros.users.routes.userPhotoRoutes
import com.eros.users.routes.userPreferenceRoutes
import com.eros.users.routes.userProfileRoutes
import com.eros.users.service.CityService
import com.eros.users.service.PhotoService
import com.eros.users.service.PreferenceService
import com.eros.users.service.QAService
import com.eros.users.service.UserService
import com.eros.wallet.repository.TransactionRepositoryImpl
import com.eros.wallet.repository.WalletRepositoryImpl
import com.eros.wallet.routes.walletRoutes
import com.eros.wallet.routes.webhookRoute
import com.eros.wallet.services.PaymentService
import com.eros.wallet.services.TransactionService
import com.eros.wallet.services.WalletService
import com.eros.wallet.stripe.StripeService
import com.eros.wallet.stripe.StripeWebhookHandler
import com.eros.users.repository.*
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

    val transactionManager = DatabaseTransactionManager()

    // Initialize repositories
    val userRepository = UserRepositoryImpl()
    val photoRepository = PhotoRepositoryImpl()

    val cityRepositoryImpl = CityRepositoryImpl()
    val preferenceRepositoryImpl = PreferenceRepositoryImpl()

    val qaRepository = UserQARepositoryImpl()
    val questionRepository = QuestionRepositoryImpl()
    val transactionRepository = TransactionRepositoryImpl()
    val walletRepository = WalletRepositoryImpl()

    val matchRepository = MatchRepositoryImpl()
    val dailyBatchRepository = DailyBatchRepositoryImpl()

    // Initialize configs
    val s3Config = S3Config.fromApplicationConfig(environment.config)

    // Initialize services
    val photoService = PhotoService(photoRepository, s3Config)
    val userService = UserService(userRepository, photoService)
    val cityService = CityService(cityRepositoryImpl)
    val preferenceService = PreferenceService(preferenceRepositoryImpl, userService)
    val qaService = QAService(questionRepository, qaRepository)
    val transactionService = TransactionService(transactionRepository)
    val walletService = WalletService(walletRepository, transactionService)
    val stripeService = StripeService()
    val paymentService = PaymentService(walletService,transactionService, stripeService)
    val webhookHandler = StripeWebhookHandler(walletService, transactionService)
    val matchService = MatchService(matchRepository, dailyBatchRepository, userService, transactionManager)

    val matchAccessChecker = MatchAccessCheckerImpl(matchService)
    val profileAccessControl = ProfileAccessControl(matchAccessChecker)
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        // placed outside the firebase-auth due to external token use.
        webhookRoute(webhookHandler)

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

            walletRoutes(paymentService)

            matchRoutes(matchService)
        }
    }
}
