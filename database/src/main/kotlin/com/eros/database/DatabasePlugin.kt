package com.eros.database

import io.ktor.server.application.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import javax.sql.DataSource

/**
 * Ktor plugin that integrates database connectivity, Exposed ORM, and Flyway migrations
 * into the application lifecycle.
 *
 * Initializes on application start and closes connections on application stop.
 * Runs Flyway migrations automatically to ensure schema is up-to-date.
 */
class DatabasePlugin(private val config: DatabaseConfig) {

    lateinit var dataSource: DataSource
        private set

    /**
     * Initializes database connectivity and runs migrations.
     *
     * Creates HikariCP connection pool, connects Exposed ORM, and applies pending Flyway migrations.
     * Called automatically on ApplicationStarted event.
     */
    fun initialize() {
        // Create HikariCP connection pool
        dataSource = DatabaseFactory.createDataSource(config)

        // Connect Exposed to the database
        Database.connect(dataSource)

        // Run Flyway migrations
        FlywayConfig.migrate(dataSource)
    }

    /**
     * Closes database connections and releases resources.
     *
     * Called automatically on ApplicationStopped event.
     */
    fun close() {
        if (dataSource is AutoCloseable) {
            (dataSource as AutoCloseable).close()
        }
    }

    companion object Plugin : BaseApplicationPlugin<Application, DatabaseConfig, DatabasePlugin> {
        override val key = AttributeKey<DatabasePlugin>("Database")

        override fun install(pipeline: Application, configure: DatabaseConfig.() -> Unit): DatabasePlugin {
            val config = DatabaseConfig.fromApplicationConfig(pipeline.environment.config).apply(configure)
            val plugin = DatabasePlugin(config)

            // Initialize database on application start
            pipeline.monitor.subscribe(ApplicationStarted) {
                plugin.initialize()
                pipeline.log.info("Database initialized: ${config.jdbcUrl}")
                pipeline.log.info("Flyway migrations completed successfully")
            }

            // Close database connections on application stop
            pipeline.monitor.subscribe(ApplicationStopped) {
                plugin.close()
                pipeline.log.info("Database connections closed")
            }

            return plugin
        }
    }
}

/**
 * Extension function for executing database queries in a suspending transaction.
 *
 * Wraps database operations in Exposed's transaction context on the IO dispatcher.
 * All database operations should use this wrapper to ensure proper transaction management.
 *
 * Example:
 * ```
 * suspend fun getUser(id: UUID): User? = dbQuery {
 *     Users.select { Users.id eq id }.singleOrNull()?.toUser()
 * }
 * ```
 *
 * @param block Suspending lambda containing database operations
 * @return Result of the database operation
 */
suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }
