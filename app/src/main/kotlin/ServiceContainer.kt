package com.eros

import com.eros.common.config.S3Config
import com.eros.users.ProfileAccessControl
import com.eros.users.repository.*
import com.eros.users.service.*
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.AttributeKey

class ServiceContainer(environment: ApplicationEnvironment) {
    // Configs
    private val s3Config = S3Config.fromApplicationConfig(environment.config)

    // Repositories
    val userRepository: UserRepository by lazy { UserRepositoryImpl() }
    val photoRepository: PhotoRepository by lazy { PhotoRepositoryImpl() }
    val cityRepository: CityRepository by lazy { CityRepositoryImpl() }
    val preferenceRepository: PreferenceRepository by lazy { PreferenceRepositoryImpl() }
    val qaRepository: UserQARepository by lazy { UserQARepositoryImpl() }
    val questionRepository: QuestionRepository by lazy { QuestionRepositoryImpl() }

    // Services using repositories.
    val photoService: PhotoService by lazy {
        PhotoService(photoRepository, s3Config)
    }
    val userService: UserService by lazy {
        UserService(userRepository, photoService)
    }
    val cityService: CityService by lazy {
        CityService(cityRepository)
    }
    val preferenceService: PreferenceService by lazy {
        PreferenceService(preferenceRepository, userService)
    }
    val qaService: QAService by lazy {
        QAService(questionRepository, qaRepository)
    }

    // Access control
    val profileAccessControl: ProfileAccessControl by lazy {
        ProfileAccessControl()
    }
}

// Extension property for easy access
val Application.services: ServiceContainer
    get() = attributes.computeIfAbsent(ServiceContainerKey) {
        ServiceContainer(environment)
    }

private val ServiceContainerKey = AttributeKey<ServiceContainer>("ServiceContainer")