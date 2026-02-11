package com.eros.auth.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.server.application.*
import java.io.FileInputStream

/**
 * Firebase configuration settings
 *
 * @property serviceAccountPath Path to Firebase service account JSON file
 * @property projectId Firebase project ID
 */
data class FirebaseSettings(
    val serviceAccountPath: String,
    val projectId: String
)

/**
 * Firebase initialization for the application
 *
 * This object handles the one-time initialization of Firebase Admin SDK.
 * Firebase Auth is used for:
 * - JWT token verification
 * - User identity management
 * - Email/phone verification (handled client-side)
 */
object FirebaseConfig {

    private var initialized = false

    /**
     * Initialize Firebase Admin SDK
     *
     * @param settings Firebase configuration settings
     * @throws IllegalStateException if Firebase is already initialized
     */
    fun initialize(settings: FirebaseSettings) {
        if (initialized) {
            throw IllegalStateException("Firebase is already initialized")
        }

        val serviceAccount = FileInputStream(settings.serviceAccountPath)
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .setProjectId(settings.projectId)
            .build()

        FirebaseApp.initializeApp(options)
        initialized = true
    }

    /**
     * Check if Firebase has been initialized
     */
    fun isInitialized(): Boolean = initialized
}

/**
 * Ktor plugin to configure Firebase authentication
 *
 * This plugin initializes Firebase Admin SDK on application startup.
 */
fun Application.configureFirebase() {
    val serviceAccountPath = environment.config.propertyOrNull("firebase.serviceAccountPath")?.getString()
        ?: throw IllegalStateException("firebase.serviceAccountPath must be configured")

    val projectId = environment.config.propertyOrNull("firebase.projectId")?.getString()
        ?: throw IllegalStateException("firebase.projectId must be configured")

    val settings = FirebaseSettings(
        serviceAccountPath = serviceAccountPath,
        projectId = projectId
    )

    FirebaseConfig.initialize(settings)

    log.info("Firebase initialized successfully for project: $projectId")
}
