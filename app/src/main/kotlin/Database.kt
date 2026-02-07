package com.eros

import com.eros.database.DatabasePlugin
import io.ktor.server.application.*

/**
 * Configures database connectivity with HikariCP, Exposed ORM, and Flyway migrations.
 *
 * Installs the DatabasePlugin which:
 * - Creates a HikariCP connection pool from application.yaml configuration
 * - Connects Exposed ORM to the database
 * - Runs Flyway migrations on application startup
 * - Closes connections gracefully on application shutdown
 *
 * Configuration is loaded from the `database` section in application.yaml.
 */
fun Application.configureDatabase() {
    install(DatabasePlugin)
}
