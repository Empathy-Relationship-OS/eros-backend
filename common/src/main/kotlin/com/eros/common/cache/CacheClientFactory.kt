package com.eros.common.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.SocketOptions
import io.lettuce.core.SslOptions
import java.time.Duration

/**
 * Factory for creating cache clients based on configuration.
 *
 * Supports multiple backends (Valkey, Redis, In-Memory) and handles:
 * - TLS/SSL for AWS ElastiCache
 * - Connection pooling
 * - Graceful fallback to in-memory cache on failure
 *
 * Example usage:
 * ```kotlin
 * val config = CacheConfig.fromApplicationConfig(environment.config)
 * val cache = CacheClientFactory.create(config)
 * ```
 */
object CacheClientFactory {

    private val logger = KotlinLogging.logger {}

    /**
     * Creates a cache instance based on configuration.
     *
     * Falls back to InMemoryCache if:
     * - Cache is disabled (`enabled = false`)
     * - Distributed backend fails to initialize
     * - Connection test fails
     *
     * @param config Cache configuration
     * @return Cache implementation (distributed or in-memory fallback)
     */
    fun create(config: CacheConfig): Cache {
        if (!config.enabled) {
            logger.info { "Cache is disabled, using in-memory fallback" }
            return InMemoryCache()
        }

        return when (config.backend) {
            CacheBackend.VALKEY, CacheBackend.REDIS -> {
                createDistributedCache(config)
            }
            CacheBackend.IN_MEMORY -> {
                logger.info { "Using in-memory cache (configured explicitly)" }
                InMemoryCache()
            }
        }
    }

    /**
     * Creates a distributed cache (Valkey/Redis) with Lettuce client.
     *
     * Handles:
     * - TLS/SSL configuration for AWS ElastiCache
     * - Connection timeout settings
     * - Connection health check
     * - Automatic fallback to in-memory on failure
     *
     * @param config Cache configuration
     * @return DistributedCache or InMemoryCache (fallback)
     */
    private fun createDistributedCache(config: CacheConfig): Cache {
        return try {
            logger.info {
                "Connecting to ${config.backend.displayName} at ${config.host}:${config.port} " +
                "(TLS: ${config.tls.enabled}, Database: ${config.database})"
            }

            // Build connection URI (handles redis:// or rediss:// protocol)
            val uri = config.buildUri()

            // Create Lettuce client (works for both Valkey and Redis - protocol compatible)
            val client = RedisClient.create(uri)

            // Configure client options
            val clientOptions = buildClientOptions(config)
            client.setOptions(clientOptions)

            // Test connection with PING
            testConnection(client, config)

            logger.info {
                "Successfully connected to ${config.backend.displayName} " +
                "(host: ${config.host}, TLS: ${config.tls.enabled})"
            }

            DistributedCache(client, config.backend)

        } catch (e: Exception) {
            logger.error(e) {
                "Failed to connect to ${config.backend.displayName} at ${config.host}:${config.port}"
            }
            throw e
        }
    }

    /**
     * Builds Lettuce ClientOptions with TLS and timeout configuration.
     *
     * @param config Cache configuration
     * @return Configured ClientOptions
     */
    private fun buildClientOptions(config: CacheConfig): ClientOptions {
        val optionsBuilder = ClientOptions.builder()
            .socketOptions(
                SocketOptions.builder()
                    .connectTimeout(Duration.ofMillis(config.timeoutMs))
                    .build()
            )

        // Configure TLS/SSL if enabled (required for AWS ElastiCache with in-transit encryption)
        if (config.tls.enabled) {
            logger.debug { "Enabling TLS/SSL for cache connection" }
            optionsBuilder.sslOptions(
                SslOptions.builder()
                    .jdkSslProvider()  // Use JDK's built-in SSL provider
                    .build()
            )
        }

        return optionsBuilder.build()
    }

    /**
     * Tests connection to cache backend with PING command.
     *
     * @param client Lettuce RedisClient
     * @param config Cache configuration (for logging)
     * @throws IllegalStateException if PING fails
     * @throws Exception if connection fails
     */
    private fun testConnection(client: RedisClient, config: CacheConfig) {
        logger.debug { "Testing connection to ${config.backend.displayName}..." }

        val connection = client.connect()
        try {
            val testResult = connection.sync().ping()

            if (testResult != "PONG") {
                throw IllegalStateException(
                    "Cache PING failed: expected 'PONG', got '$testResult'"
                )
            }

            logger.debug { "Connection test successful (PONG received)" }
        } finally {
            connection.close()
        }
    }
}
