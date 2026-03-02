package com.eros.database

import io.ktor.server.application.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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
 * Executes a database query within a coroutine-safe transaction context.
 *
 * This function provides a safe way to interact with the database in a suspended context
 * by wrapping database operations in a transaction and executing them on the IO dispatcher.
 *
 * ## Usage Guidelines
 * - **MUST** be called from a suspend function
 * - **MUST** only be used in the Service Layer
 * - **DO NOT** use in Controllers/Routes or Repository classes directly
 *
 * ## Why Service Layer Only?
 * Database queries should be encapsulated in the service layer to:
 * - Maintain separation of concerns
 * - Enable proper transaction management
 * - Facilitate testing and mocking
 * - Keep business logic separate from data access
 *
 * @param T The return type of the database operation
 * @param block The database operation to execute within the transaction
 * @return The result of the database operation
 *
 * @sample
 * ```kotlin
 * class UserService {
 *     suspend fun getUserById(id: String): User? = dbQuery {
 *         UserTable.select { UserTable.id eq id }
 *             .map { it.toUser() }
 *             .singleOrNull()
 *     }
 * }
 * ```
 *
 * @see kotlinx.coroutines.Dispatchers.IO
 * @see org.jetbrains.exposed.sql.transactions.experimental.suspendTransaction
 */
suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T =
    withContext(Dispatchers.IO) {
        suspendTransaction {
            block()
        }
    }