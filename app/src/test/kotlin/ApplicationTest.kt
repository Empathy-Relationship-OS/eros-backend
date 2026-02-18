package com.eros

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Application test.json suite using Testcontainers for database isolation.
 *
 * Spins up a PostgreSQL container for each test.json to ensure clean, isolated database state.
 * Requires Docker to be running. Container lifecycle is fully automated.
 *
 * Note: On macOS, ensure Docker Desktop is running before executing tests.
 */
@Disabled("Disabling while setting up firebase auth")
class ApplicationTest : IntegrationTestBase() {

    @Test
    fun testRoot() = testApplication {
        environment {
            config = MapApplicationConfig(
                // Database configuration
                "database.host" to postgres.host,
                "database.port" to postgres.firstMappedPort.toString(),
                "database.name" to postgres.databaseName,
                "database.user" to postgres.username,
                "database.password" to postgres.password,
                "database.poolSize" to "5",
                "database.maxLifetime" to "600000",
                "database.connectionTimeout" to "30000",

                // JWT configuration (required for authentication module)
                "jwt.secret" to "test.json-secret-key-for-application-test.json",
                "jwt.domain" to "https://test-issuer/",
                "jwt.audience" to "test.json-audience",
                "jwt.realm" to "test.json realm",

                // Firebase configuration - dummy values for testing
                "firebase.serviceAccountPath" to "app/src/test/resources/test.json",
                "firebase.projectId" to "test-project-id"
            )
        }
        application {
            module()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

}
