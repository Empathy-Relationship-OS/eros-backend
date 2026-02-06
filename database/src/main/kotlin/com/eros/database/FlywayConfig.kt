package com.eros.database

import org.flywaydb.core.Flyway
import javax.sql.DataSource

/**
 * Configuration object for Flyway database migrations.
 *
 * Provides methods to manage database schema versioning through SQL migration scripts.
 * Flyway tracks applied migrations in the `flyway_schema_history` table.
 *
 * Migration files follow the naming convention: `V{version}__{description}.sql`
 * Example: `V1__auth_tables.sql`, `V2__user_profiles.sql`
 */
object FlywayConfig {

    private const val MIGRATION_LOCATION = "classpath:db/migration"
    private const val BASELINE_VERSION = "0"

    /**
     * Runs all pending database migrations.
     *
     * Applies migrations in version order from the `db/migration` directory.
     * If the database is not versioned, creates a baseline at version 0 before applying migrations.
     *
     * @param dataSource HikariCP DataSource for database connection
     * @throws org.flywaydb.core.api.FlywayException if migration fails
     */
    fun migrate(dataSource: DataSource) {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(MIGRATION_LOCATION)
            .baselineOnMigrate(true)
            .baselineVersion(BASELINE_VERSION)
            .load()

        flyway.migrate()
    }

    /**
     * Drops all database objects (tables, views, procedures) managed by Flyway.
     *
     * **WARNING**: This is destructive and should only be used in development/testing.
     * Production environments should have `cleanDisabled = true` in Flyway configuration.
     *
     * @param dataSource HikariCP DataSource for database connection
     */
    fun clean(dataSource: DataSource) {
        val env = System.getenv("ENVIRONMENT") ?: "development"
        require(env != "production") { "Flyway clean is not allowed in production environment" }

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(MIGRATION_LOCATION)
            .cleanDisabled(false)
            .load()

        flyway.clean()
    }

    /**
     * Returns migration status information as a formatted string.
     *
     * Shows all migrations (pending, applied, failed) with their version, description, and state.
     * Useful for debugging migration issues or verifying database version.
     *
     * @param dataSource HikariCP DataSource for database connection
     * @return Formatted string with migration status (one migration per line)
     */
    fun info(dataSource: DataSource): String {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(MIGRATION_LOCATION)
            .load()

        val info = flyway.info()
        return info.all().joinToString("\n") { migration ->
            "${migration.version} - ${migration.description} [${migration.state}]"
        }
    }
}
