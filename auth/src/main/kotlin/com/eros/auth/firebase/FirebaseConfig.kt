package com.eros.auth.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.server.application.*
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicBoolean

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

    private val initialized = AtomicBoolean(false)

    /**
     * Initialize Firebase Admin SDK
     *
     * @param settings Firebase configuration settings
     * @throws IllegalStateException if Firebase is already initialized or configuration is invalid
     */
    fun initialize(settings: FirebaseSettings) {
        if (!initialized.compareAndSet(false, true)) {
            throw IllegalStateException("Firebase is already initialized")
        }

        if (settings.projectId.isBlank() || settings.projectId == "your-project-id") {
            throw IllegalStateException(
                "FIREBASE_PROJECT_ID must be configured with a valid Firebase project ID. " +
                "Please set the FIREBASE_PROJECT_ID environment variable."
            )
        }

        val options = FileInputStream(settings.serviceAccountPath).use { serviceAccount ->
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setProjectId(settings.projectId)
                .build()
        }

        FirebaseApp.initializeApp(options)
    }

    /**
     * Check if Firebase has been initialized
     */
    fun isInitialized(): Boolean = initialized.get()
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
