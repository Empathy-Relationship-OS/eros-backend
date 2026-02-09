package com.eros

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Application test suite using Testcontainers for database isolation.
 *
 * Spins up a PostgreSQL container for each test to ensure clean, isolated database state.
 * Requires Docker to be running. Container lifecycle is fully automated.
 *
 * Note: On macOS, ensure Docker Desktop is running before executing tests.
 */
class ApplicationTest {

    private lateinit var postgres: PostgreSQLContainer<*>

    /**
     * Starts PostgreSQL container before each test.
     * Container provides isolated database instance with random port assignment.
     */
    @BeforeTest
    fun setUp() {
        // Configure Docker socket location for macOS if needed
        if (System.getProperty("os.name").contains("Mac") && System.getenv("DOCKER_HOST") == null) {
            val dockerSock = System.getProperty("user.home") + "/.docker/run/docker.sock"
            System.setProperty("DOCKER_HOST", "unix://$dockerSock")
        }

        postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("eros_test")
            .withUsername("test")
            .withPassword("test")
        postgres.start()
    }

    /**
     * Stops and removes PostgreSQL container after each test.
     * Ensures clean state for next test run.
     */
    @AfterTest
    fun tearDown() {
        postgres.stop()
    }

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
                "jwt.secret" to "test-secret-key-for-application-test",
                "jwt.domain" to "https://test-issuer/",
                "jwt.audience" to "test-audience",
                "jwt.realm" to "test realm"
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
