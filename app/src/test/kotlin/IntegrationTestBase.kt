package com.eros

import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base class for integration tests requiring PostgreSQL database.
 *
 * Provides shared Testcontainers lifecycle management for all integration tests.
 * Automatically starts a PostgreSQL container before each test and stops it after.
 *
 * Usage:
 * ```kotlin
 * class MyTest : IntegrationTestBase() {
 *     @Test
 *     fun myTest() = testApplication {
 *         setupTestEnvironment()
 *         // Your test code here
 *     }
 * }
 * ```
 *
 * Note: On macOS, Docker Desktop must be running before executing tests.
 */
abstract class IntegrationTestBase {

    protected lateinit var postgres: PostgreSQLContainer<*>

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
}
