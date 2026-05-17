package com.eros.common.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.RedisClient
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Distributed cache implementation using Lettuce client.
 *
 * Works with both Valkey and Redis (protocol-compatible).
 * Handles failures gracefully and logs errors without throwing exceptions.
 *
 * All operations use Dispatchers.IO to avoid blocking the coroutine thread pool.
 *
 * Example usage:
 * ```kotlin
 * val client = RedisClient.create("redis://localhost:6379")
 * val cache = DistributedCache(client, CacheBackend.VALKEY)
 *
 * // Store with TTL
 * cache.set("user:123:profile", profileJson, ttlSeconds = 3600)
 *
 * // Retrieve
 * val profile = cache.get("user:123:profile")
 * ```
 */
class DistributedCache(
    private val client: RedisClient,
    private val backend: CacheBackend
) : Cache {

    companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * Redacts sensitive key/pattern data for logging by computing a SHA-256 hash.
         * Returns a truncated hash representation to prevent log injection while maintaining debuggability.
         */
        private fun redactKeyForLogging(key: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(key.toByteArray())
            val hashHex = hashBytes.joinToString("") { "%02x".format(it) }
            return "[REDACTED:${hashHex.take(12)}]"
        }
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        try {
            client.connect().use { connection ->
                connection.sync().get(key)
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to get key: ${redactKeyForLogging(key)}" }
            null
        }
    }

    override suspend fun set(key: String, value: String, ttlSeconds: Long): Unit = withContext(Dispatchers.IO) {
        try {
            client.connect().use { connection ->
                connection.sync().setex(key, ttlSeconds, value)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to set key: ${redactKeyForLogging(key)} with TTL: $ttlSeconds" }
        }
    }

    override suspend fun set(key: String, value: String): Unit = withContext(Dispatchers.IO) {
        try {
            client.connect().use { connection ->
                connection.sync().set(key, value)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to set key: ${redactKeyForLogging(key)}" }
        }
    }

    override suspend fun delete(key: String): Unit = withContext(Dispatchers.IO) {
        try {
            client.connect().use { connection ->
                connection.sync().del(key)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete key: ${redactKeyForLogging(key)}" }
        }
    }

    override suspend fun deleteByPattern(pattern: String) = withContext(Dispatchers.IO) {
        try {
            client.connect().use { connection ->
                val commands = connection.sync()

                // Use SCAN for safe pattern deletion (doesn't block server)
                // SCAN is O(N) but doesn't block like KEYS command
                var cursor = ScanCursor.INITIAL
                val keys = mutableListOf<String>()

                do {
                    val scanResult = commands.scan(cursor, ScanArgs.Builder.matches(pattern))
                    keys.addAll(scanResult.keys)
                    cursor = ScanCursor.of(scanResult.cursor)
                } while (!scanResult.isFinished)

                if (keys.isNotEmpty()) {
                    commands.del(*keys.toTypedArray())
                    logger.debug { "Deleted ${keys.size} keys matching pattern: ${redactKeyForLogging(pattern)}" }
                } else {
                    logger.debug { "No keys found matching pattern: ${redactKeyForLogging(pattern)}" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete by pattern: ${redactKeyForLogging(pattern)}" }
        }
    }

    override suspend fun ttl(key: String): Long? = withContext(Dispatchers.IO) {
        try {
            client.connect().use { connection ->
                val ttl = connection.sync().ttl(key)

                // Redis/Valkey returns -2 if key doesn't exist, -1 if no expiry
                when {
                    ttl < 0 -> null
                    else -> ttl
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to get TTL for key: ${redactKeyForLogging(key)}" }
            null
        }
    }

    override suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            client.connect().use { connection ->
                connection.sync().ping() == "PONG"
            }
        } catch (e: Exception) {
            logger.warn(e) { "Ping failed" }
            false
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        try {
            client.connect().use { connection ->
                connection.sync().flushdb()
            }
            logger.warn { "Cache cleared (FLUSHDB executed)" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to clear cache" }
        }
    }

    override suspend fun getStats(): Cache.CacheStats = withContext(Dispatchers.IO) {
        try {
            client.connect().use { connection ->
                val info = connection.sync().info() //"stats", "keyspace"

                // Parse INFO response for statistics
                val keyCount = parseInfoKeyCount(info)
                val memoryUsed = parseInfoMemoryUsed(info)

                Cache.CacheStats(
                    backend = backend.name.lowercase(),
                    connected = true,
                    keyCount = keyCount,
                    memoryUsedBytes = memoryUsed
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to get stats" }
            Cache.CacheStats(
                backend = backend.name.lowercase(),
                connected = false
            )
        }
    }

    /**
     * Parses key count from Redis/Valkey INFO response.
     *
     * Example INFO keyspace output:
     * ```
     * # Keyspace
     * db0:keys=123,expires=45,avg_ttl=3600
     * ```
     */
    private fun parseInfoKeyCount(info: String): Long? {
        val regex = """keys=(\d+)""".toRegex()
        return regex.find(info)?.groupValues?.get(1)?.toLongOrNull()
    }

    /**
     * Parses memory usage from Redis/Valkey INFO response.
     *
     * Example INFO memory output:
     * ```
     * # Memory
     * used_memory:1234567
     * ```
     */
    private fun parseInfoMemoryUsed(info: String): Long? {
        val regex = """used_memory:(\d+)""".toRegex()
        return regex.find(info)?.groupValues?.get(1)?.toLongOrNull()
    }

    /**
     * Shuts down the Lettuce client and closes all connections.
     *
     * Should be called when the application is stopping.
     */
    fun shutdown() {
        try {
            logger.info { "Shutting down ${backend.displayName} client..." }
            client.shutdown()
            logger.info { "${backend.displayName} client shut down successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Error shutting down ${backend.displayName} client" }
        }
    }
}
