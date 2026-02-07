package com.eros.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource

/**
 * Factory object for creating and configuring HikariCP database connection pools.
 *
 * **What**: Provides a centralized location for DataSource creation with optimized
 * HikariCP configuration for PostgreSQL connections.
 *
 * **How**: Configures HikariCP with production-ready settings including connection pooling,
 * transaction isolation, and timeout management. Uses the builder pattern via `apply { }`
 * to configure HikariConfig before creating the DataSource.
 *
 * **Why**: HikariCP is the fastest and most reliable connection pool for JDBC. Centralizing
 * DataSource creation ensures consistent configuration across the application and simplifies
 * testing by allowing easy DataSource mocking.
 */
object DatabaseFactory {
    /**
     * Creates a configured HikariCP DataSource for PostgreSQL connections.
     *
     * **What**: Initializes a HikariCP connection pool with PostgreSQL-specific settings
     * optimized for consistency and performance.
     *
     * **How**:
     * - Configures HikariCP with provided database credentials and connection settings
     * - Sets `autoCommit = false` to require explicit transaction management (prevents accidental commits)
     * - Uses `TRANSACTION_REPEATABLE_READ` isolation level for consistent reads within transactions
     * - Validates configuration before creating the DataSource to fail fast on misconfiguration
     *
     * **Why**:
     * - **Connection Pooling**: Reuses connections instead of creating new ones, improving performance
     * - **No Auto-Commit**: Ensures all database operations are explicitly wrapped in transactions via Exposed
     * - **Repeatable Read**: Prevents phantom reads and ensures consistency during multi-step operations
     * - **Timeout Management**: Prevents hanging connections with configurable timeout
     * - **Max Lifetime**: Recycles old connections to avoid stale connections and database-side timeouts
     *
     * Configuration parameters:
     * - `jdbcUrl`: PostgreSQL JDBC connection string
     * - `username/password`: Database credentials
     * - `maximumPoolSize`: Maximum number of connections in pool (default: 10)
     * - `maxLifetime`: Maximum connection lifetime in ms (default: 600000 = 10 minutes)
     * - `connectionTimeout`: Maximum time to wait for connection in ms (default: 30000 = 30 seconds)
     *
     * @param config DatabaseConfig containing connection parameters
     * @return Configured HikariDataSource ready for use with Exposed ORM
     * @throws IllegalArgumentException if HikariConfig validation fails
     */
    fun createDataSource(config: DatabaseConfig): DataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            username = config.user
            password = config.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = config.poolSize
            maxLifetime = config.maxLifetime
            connectionTimeout = config.connectionTimeout
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(hikariConfig)
    }
}
