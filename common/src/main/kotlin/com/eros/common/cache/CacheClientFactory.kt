package com.eros.common.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SocketOptions
import io.lettuce.core.SslOptions
import java.time.Duration

/**
 * Factory for creating cache clients based on configuration.
 *
 * Supports multiple backends (Valkey, Redis, In-Memory) and handles:
 * - TLS/SSL for AWS ElastiCache
 * - Connection pooling
 * - Fail-fast behavior (throws exception if distributed cache fails to connect)
 *
 * Example usage:
 * ```kotlin
 * val config = CacheConfig.fromApplicationConfig(environment.config)
 * val cache = CacheClientFactory.create(config)  // Throws if connection fails
 * ```
 */
object CacheClientFactory {

    private val logger = KotlinLogging.logger {}

    /**
     * Creates a cache instance based on configuration.
     *
     * Returns InMemoryCache only if:
     * - Cache is disabled (`enabled = false`)
     * - Backend is explicitly set to `IN_MEMORY`
     *
     * For distributed backends (Valkey/Redis):
     * - Throws exception if connection fails (fail-fast behavior)
     * - This prevents silent fallback and ensures configuration issues are caught early
     *
     * @param config Cache configuration
     * @return Cache implementation
     * @throws Exception if distributed cache connection fails
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
     * - Connection health check (PING command)
     *
     * @param config Cache configuration
     * @return DistributedCache
     * @throws Exception if connection fails or PING fails
     */
    private fun createDistributedCache(config: CacheConfig): Cache {
        return try {
            logger.info {
                "Connecting to ${config.backend.displayName} at ${config.host}:${config.port} " +
                "(TLS: ${config.tls.enabled}, Verify Peer: ${config.tls.verifyPeer}, Database: ${config.database})"
            }

            // Build connection URI with proper TLS peer verification
            val uriBuilder = RedisURI.Builder.redis(config.host, config.port)
                .withDatabase(config.database)

            // Apply password if configured
            config.password?.let { uriBuilder.withPassword(it.toCharArray()) }

            // Apply TLS settings (including peer verification)
            if (config.tls.enabled) {
                uriBuilder.withSsl(true)
                uriBuilder.withVerifyPeer(config.tls.verifyPeer)
            }

            val uri = uriBuilder.build()

            // Create Lettuce client (works for both Valkey and Redis - protocol compatible)
            val client = RedisClient.create(uri)

            // Configure client options
            val clientOptions = buildClientOptions(config)
            client.options = clientOptions

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
