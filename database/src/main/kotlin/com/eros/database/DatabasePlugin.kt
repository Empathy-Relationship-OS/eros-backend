package com.eros.database

import io.ktor.server.application.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
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
 * Extension function for executing database queries in a suspending transaction.
 *
 * Wraps database operations in Exposed's transaction context on the IO dispatcher.
 * All database operations should use this wrapper to ensure proper transaction management.
 *
 * **Use this for DAOs that manage their own transactions internally.**
 *
 * Example:
 * ```
 * suspend fun getUser(id: UUID): User? = dbQuery {
 *     Users.select { Users.id eq id }.singleOrNull()?.toUser()
 * }
 * ```
 *
 * @param block Non-suspending lambda containing database operations
 * @return Result of the database operation
 */
suspend fun <T> dbQuery(block: () -> T): T =
    withContext(Dispatchers.IO) {
        transaction { block() }
    }

/**
 * Extension function for executing database queries with suspend function support.
 *
 * Similar to [dbQuery], but accepts a **suspend lambda**, allowing you to call
 * suspend repository methods from within the transaction block. This is useful
 * for grouping multiple repository calls into a single atomic transaction.
 *
 * **Use this at the SERVICE layer when calling transaction-agnostic DAOs.**
 *
 * Example:
 * ```
 * suspend fun createUserWithProfile(user: User): UserProfile = dbQuerySuspend {
 *     val created = userRepository.create(user)  // suspend call
 *     profileRepository.create(created.toProfile())  // suspend call
 *     // Both calls in same transaction - if second fails, first rolls back
 * }
 * ```
 *
 * @param block Suspending lambda containing database operations
 * @return Result of the database operation
 */
suspend fun <T> dbQuerySuspend(block: suspend () -> T): T =
    withContext(Dispatchers.IO) {
        transaction { kotlinx.coroutines.runBlocking { block() } }
    }
