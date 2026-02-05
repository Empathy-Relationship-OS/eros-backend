package com.eros.database

import io.ktor.server.config.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for DatabaseConfig configuration loading and validation.
 *
 * **What**: Verifies that DatabaseConfig correctly loads configuration from ApplicationConfig
 * and generates proper JDBC URLs.
 *
 * **How**: Uses MapApplicationConfig to simulate application.yaml configuration and validates
 * that all properties are correctly extracted and type-converted.
 *
 * **Why**: Ensures configuration loading is robust and catches issues before deployment.
 * Tests both default local development settings and custom production-like configurations.
 */
class DatabaseConfigTest {

    /**
     * Tests DatabaseConfig loading with default local development values.
     *
     * **What**: Verifies configuration loading with typical localhost development settings.
     *
     * **How**: Creates a MapApplicationConfig with default values (localhost:5432, postgres user)
     * and validates that fromApplicationConfig correctly extracts all properties.
     *
     * **Why**: Default configuration is the most common use case during development.
     * This test ensures developers can start the application without custom environment variables.
     */
    @Test
    fun testDatabaseConfigFromApplicationConfigWithDefaults() {
        // Create a test configuration with default values
        val configMap = mapOf(
            "database.host" to "localhost",
            "database.port" to "5432",
            "database.name" to "eros",
            "database.user" to "postgres",
            "database.password" to "postgres",
            "database.poolSize" to "10",
            "database.maxLifetime" to "600000",
            "database.connectionTimeout" to "30000"
        )

        val applicationConfig = MapApplicationConfig(
            configMap.map { it.key to it.value }
        )

        val databaseConfig = DatabaseConfig.fromApplicationConfig(applicationConfig)

        assertEquals("localhost", databaseConfig.host)
        assertEquals(5432, databaseConfig.port)
        assertEquals("eros", databaseConfig.name)
        assertEquals("postgres", databaseConfig.user)
        assertEquals("postgres", databaseConfig.password)
        assertEquals(10, databaseConfig.poolSize)
        assertEquals(600000L, databaseConfig.maxLifetime)
        assertEquals(30000L, databaseConfig.connectionTimeout)
    }

    /**
     * Tests JDBC URL generation from DatabaseConfig properties.
     *
     * **What**: Verifies that the jdbcUrl computed property generates correct PostgreSQL JDBC URLs.
     *
     * **How**: Creates a DatabaseConfig with custom values and checks that the jdbcUrl property
     * returns the expected format: `jdbc:postgresql://host:port/name`
     *
     * **Why**: JDBC URL format must be exact for PostgreSQL driver to accept the connection.
     * This test prevents URL format regressions.
     */
    @Test
    fun testDatabaseConfigJdbcUrl() {
        val config = DatabaseConfig(
            host = "testhost",
            port = 5433,
            name = "testdb",
            user = "testuser",
            password = "testpass",
            poolSize = 5,
            maxLifetime = 500000L,
            connectionTimeout = 20000L
        )

        assertEquals("jdbc:postgresql://testhost:5433/testdb", config.jdbcUrl)
    }

    /**
     * Tests DatabaseConfig loading with custom production-like values.
     *
     * **What**: Verifies configuration loading with non-default values simulating a production environment.
     *
     * **How**: Creates a MapApplicationConfig with custom host, port, larger pool size, and longer timeouts.
     * Validates that all custom values are correctly extracted and the JDBC URL is properly generated.
     *
     * **Why**: Production environments use different settings than development (remote host, larger pools,
     * longer timeouts). This test ensures environment variable overrides work correctly for production deployment.
     */
    @Test
    fun testDatabaseConfigFromApplicationConfigWithCustomValues() {
        // Create a test configuration with custom values
        val configMap = mapOf(
            "database.host" to "db.example.com",
            "database.port" to "5433",
            "database.name" to "production_db",
            "database.user" to "admin",
            "database.password" to "secretpass",
            "database.poolSize" to "20",
            "database.maxLifetime" to "900000",
            "database.connectionTimeout" to "60000"
        )

        val applicationConfig = MapApplicationConfig(
            configMap.map { it.key to it.value }
        )

        val databaseConfig = DatabaseConfig.fromApplicationConfig(applicationConfig)

        assertEquals("db.example.com", databaseConfig.host)
        assertEquals(5433, databaseConfig.port)
        assertEquals("production_db", databaseConfig.name)
        assertEquals("admin", databaseConfig.user)
        assertEquals("secretpass", databaseConfig.password)
        assertEquals(20, databaseConfig.poolSize)
        assertEquals(900000L, databaseConfig.maxLifetime)
        assertEquals(60000L, databaseConfig.connectionTimeout)
        assertEquals("jdbc:postgresql://db.example.com:5433/production_db", databaseConfig.jdbcUrl)
    }
}
