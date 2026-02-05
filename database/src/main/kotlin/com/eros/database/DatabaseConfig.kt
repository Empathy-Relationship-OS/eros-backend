package com.eros.database

import io.ktor.server.config.*

/**
 * Configuration data class for database connection settings.
 *
 * **What**: Holds all necessary parameters for establishing and managing database connections
 * via HikariCP connection pool.
 *
 * **How**: Loads configuration from Ktor's ApplicationConfig (application.yaml) with support
 * for environment variable substitution. Generates JDBC URLs dynamically based on host, port, and database name.
 *
 * **Why**: Centralizes database configuration in a type-safe, immutable data class. This approach
 * enables environment-specific configuration through environment variables while providing
 * sensible defaults for local development.
 *
 * @property host Database server hostname (e.g., "localhost", "db.example.com")
 * @property port Database server port (default PostgreSQL: 5432)
 * @property name Database name/schema to connect to
 * @property user Database username for authentication
 * @property password Database password for authentication
 * @property poolSize Maximum number of connections in the HikariCP pool
 * @property maxLifetime Maximum lifetime of a connection in milliseconds (10 minutes recommended)
 * @property connectionTimeout Maximum time to wait for a connection in milliseconds (30 seconds recommended)
 */
data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
    val poolSize: Int,
    val maxLifetime: Long,
    val connectionTimeout: Long
) {
    /**
     * Computed JDBC URL for PostgreSQL connection.
     *
     * **What**: Generates the JDBC connection string in PostgreSQL format.
     *
     * **How**: Combines host, port, and database name into standard PostgreSQL JDBC URL format.
     *
     * **Why**: Eliminates manual URL construction and ensures consistent format across the application.
     *
     * @return JDBC URL in format: `jdbc:postgresql://host:port/name`
     */
    val jdbcUrl: String
        get() = "jdbc:postgresql://$host:$port/$name"

    companion object {
        /**
         * Factory method to create DatabaseConfig from Ktor's ApplicationConfig.
         *
         * **What**: Extracts database configuration from application.yaml and constructs a DatabaseConfig instance.
         *
         * **How**: Reads properties from the "database.*" namespace in ApplicationConfig, converting
         * string values to appropriate types (Int, Long). Supports environment variable substitution
         * via Ktor's `${VAR:default}` syntax.
         *
         * **Why**: Provides a single source of truth for configuration loading. By using Ktor's
         * ApplicationConfig, we automatically get environment variable support and validation.
         *
         * Configuration example in application.yaml:
         * ```yaml
         * database:
         *   host: ${DB_HOST:localhost}
         *   port: ${DB_PORT:5432}
         *   name: ${DB_NAME:eros}
         * ```
         *
         * @param config Ktor ApplicationConfig instance (typically from Application.environment.config)
         * @return DatabaseConfig instance populated from configuration
         * @throws ApplicationConfigurationException if required properties are missing
         */
        fun fromApplicationConfig(config: ApplicationConfig): DatabaseConfig {
            return DatabaseConfig(
                host = config.property("database.host").getString(),
                port = config.property("database.port").getString().toInt(),
                name = config.property("database.name").getString(),
                user = config.property("database.user").getString(),
                password = config.property("database.password").getString(),
                poolSize = config.property("database.poolSize").getString().toInt(),
                maxLifetime = config.property("database.maxLifetime").getString().toLong(),
                connectionTimeout = config.property("database.connectionTimeout").getString().toLong()
            )
        }
    }
}
