package com.eros.common.cache

import io.ktor.server.config.ApplicationConfig


/**
 * Backend-agnostic cache configuration.
 *
 * Supports both local development (Docker) and AWS ElastiCache in production.
 *
 * Configuration is loaded from application.yaml and can be overridden via
 * environment variables.
 *
 * Example usage:
 * ```kotlin
 * val config = CacheConfig.fromApplicationConfig(environment.config)
 * val cache = CacheClientFactory.create(config)
 * ```
 */
data class CacheConfig(
    val enabled: Boolean,
    val backend: CacheBackend,
    val host: String,
    val port: Int,
    val password: String?,
    val database: Int,
    val pool: PoolConfig,
    val timeoutMs: Long,
    val tls: TlsConfig
) {
    /**
     * Connection pool configuration.
     *
     * Controls how many connections are maintained to the cache backend.
     */
    data class PoolConfig(
        val maxTotal: Int,
        val maxIdle: Int,
        val minIdle: Int
    )

    /**
     * TLS/SSL configuration for secure connections.
     *
     * Required for AWS ElastiCache with in-transit encryption enabled.
     *
     * @property enabled Whether to use TLS (rediss:// protocol)
     * @property verifyPeer Whether to verify the server certificate (recommended)
     */
    data class TlsConfig(
        val enabled: Boolean,
        val verifyPeer: Boolean = true
    )

    /**
     * Builds the connection URI based on configuration.
     *
     * Examples:
     * - Local: `redis://localhost:6379/0`
     * - Local with password: `redis://:devpassword@localhost:6379/0`
     * - AWS ElastiCache: `rediss://:auth-token@master.xxx.cache.amazonaws.com:6379/0`
     *
     * @return Redis protocol URI
     */
    fun buildUri(): String {
        val protocol = if (tls.enabled) "rediss" else "redis"
        val auth = password?.let {
            val encoded = percentEncode(it)
            ":$encoded@"
        } ?: ""
        return "$protocol://$auth$host:$port/$database"
    }

    /**
     * Percent-encodes a string per RFC 3986.
     *
     * Encodes all characters except:
     * - Unreserved characters: A-Z, a-z, 0-9, -, ., _, ~
     *
     * This is more conservative than URLEncoder which is designed for
     * form-encoding (application/x-www-form-urlencoded) and encodes
     * spaces as '+' instead of '%20'.
     *
     * @param value The string to encode
     * @return Percent-encoded string suitable for URI components
     */
    private fun percentEncode(value: String): String {
        return buildString(value.length * 2) {
            for (char in value) {
                when {
                    char.isLetterOrDigit() -> append(char)
                    char in "-._~" -> append(char)
                    else -> {
                        // Percent-encode as UTF-8 bytes
                        for (byte in char.toString().toByteArray(Charsets.UTF_8)) {
                            append('%')
                            append(String.format("%02X", byte.toInt() and 0xFF))
                        }
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Loads cache configuration from Ktor ApplicationConfig.
         *
         * Expects configuration under the `cache` section in application.yaml:
         * ```yaml
         * cache:
         *   enabled: true
         *   backend: valkey
         *   host: localhost
         *   port: 6379
         *   password: ""
         *   database: 0
         *   tls:
         *     enabled: false
         *     verifyPeer: true
         *   pool:
         *     maxTotal: 50
         *     maxIdle: 10
         *     minIdle: 5
         *   timeoutMs: 2000
         * ```
         *
         * @param config The Ktor ApplicationConfig
         * @return Parsed CacheConfig
         * @throws IllegalArgumentException if required configuration is missing
         */
        fun fromApplicationConfig(config: ApplicationConfig): CacheConfig {
            return CacheConfig(
                enabled = config.property("cache.enabled").getString().toBoolean(),
                backend = CacheBackend.valueOf(
                    config.property("cache.backend").getString()
                        .replace("-", "_")
                        .uppercase()
                ),
                host = config.property("cache.host").getString(),
                port = config.property("cache.port").getString().toInt(),
                password = config.propertyOrNull("cache.password")?.getString()
                    ?.takeIf { it.isNotBlank() },  // Convert empty string to null
                database = config.property("cache.database").getString().toInt(),
                pool = PoolConfig(
                    maxTotal = config.property("cache.pool.maxTotal").getString().toInt(),
                    maxIdle = config.property("cache.pool.maxIdle").getString().toInt(),
                    minIdle = config.property("cache.pool.minIdle").getString().toInt()
                ),
                timeoutMs = config.property("cache.timeoutMs").getString().toLong(),
                tls = TlsConfig(
                    enabled = config.propertyOrNull("cache.tls.enabled")
                        ?.getString()?.toBoolean() ?: false,
                    verifyPeer = config.propertyOrNull("cache.tls.verifyPeer")
                        ?.getString()?.toBoolean() ?: true
                )
            )
        }
    }
}

/**
 * Supported cache backends.
 *
 * The application is designed to work with any of these backends without
 * code changes - just update the configuration.
 */
enum class CacheBackend {
    /**
     * Valkey - Open-source Redis fork (recommended).
     *
     * Protocol-compatible with Redis, actively maintained by the Linux Foundation.
     * Fully open-source under BSD license.
     *
     * Use this for new projects unless you have specific Redis requirements.
     */
    VALKEY,

    /**
     * Redis - Original implementation.
     *
     * Use Valkey unless you have specific Redis requirements or are migrating
     * from an existing Redis deployment.
     *
     * Note: Redis changed to a restrictive license (RSAL/SSPL). Valkey is
     * the community-driven open-source alternative.
     */
    REDIS,

    /**
     * In-memory - Local JVM cache (ConcurrentHashMap-based).
     *
     * No external dependencies required, but not distributed across instances.
     *
     * Note: The runtime behavior is fail-fast on distributed cache connection
     * failures. In-memory cache is NOT used as an automatic fallback. It is only
     * used when explicitly configured (via `cache.backend=in-memory`) or when
     * caching is disabled (`cache.enabled=false`).
     *
     * Use for:
     * - Local development without Docker
     * - Testing
     * - Single-instance deployments
     *
     * See CacheClientFactory for cache initialization behavior.
     */
    IN_MEMORY;

    /**
     * Whether this backend is distributed (shared across multiple instances).
     */
    val isDistributed: Boolean
        get() = this != IN_MEMORY

    /**
     * Human-readable backend name.
     */
    val displayName: String
        get() = when (this) {
            VALKEY -> "Valkey"
            REDIS -> "Redis"
            IN_MEMORY -> "In-memory"
        }
}
