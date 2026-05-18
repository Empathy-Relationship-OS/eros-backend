package com.eros.common.cache

/**
 * Generic distributed cache interface.
 *
 * Supports multiple backends: Valkey, Redis, Memcached, In-Memory, etc.
 * Implementations should be thread-safe and handle failures gracefully.
 *
 * This abstraction allows caching arbitrary data (not just URLs) and makes it easy
 * to swap cache backends without changing application code.
 *
 * Example usage:
 * ```kotlin
 * val cache: Cache = DistributedCache(...)
 *
 * // Store with TTL
 * cache.set("user:123:profile", profileJson, ttlSeconds = 3600)
 *
 * // Retrieve
 * val profile = cache.get("user:123:profile")
 *
 * // Invalidate by pattern
 * cache.deleteByPattern("user:123:*")
 * ```
 */
interface Cache {

    /**
     * Gets a value from cache.
     *
     * @param key Cache key
     * @return Cached value or null if not found/expired
     */
    suspend fun get(key: String): String?

    /**
     * Sets a value in cache with TTL.
     *
     * @param key Cache key
     * @param value Value to cache (must be serializable to string)
     * @param ttlSeconds Time-to-live in seconds
     */
    suspend fun set(key: String, value: String, ttlSeconds: Long)

    /**
     * Sets a value in cache without expiry.
     *
     * @param key Cache key
     * @param value Value to cache
     */
    suspend fun set(key: String, value: String)

    /**
     * Deletes a specific key.
     *
     * @param key The cache key to delete
     */
    suspend fun delete(key: String)

    /**
     * Deletes all keys matching a pattern.
     *
     * Pattern syntax:
     * - `user:123:*` - All keys starting with "user:123:"
     * - `*:profile` - All keys ending with ":profile"
     * - `user:*:settings` - All keys matching pattern
     *
     * @param pattern Pattern to match (glob-style with * wildcard)
     */
    suspend fun deleteByPattern(pattern: String)

    /**
     * Gets remaining TTL for a key.
     *
     * @param key Cache key
     * @return Remaining TTL in seconds, or null if key doesn't exist
     */
    suspend fun ttl(key: String): Long?

    /**
     * Checks if cache is healthy and reachable.
     *
     * @return true if cache is operational
     */
    suspend fun ping(): Boolean

    /**
     * Clears all entries (use with caution!).
     *
     * This operation should be used sparingly, typically only in testing
     * or maintenance scenarios.
     */
    suspend fun clear()

    /**
     * Returns cache statistics for monitoring.
     *
     * @return CacheStats with backend info and metrics
     */
    suspend fun getStats(): CacheStats

    /**
     * Cache statistics for monitoring and observability.
     *
     * @property backend The cache backend name ("valkey", "redis", "in-memory", etc.)
     * @property connected Whether the cache is currently connected and operational
     * @property keyCount Number of keys in the cache (if available)
     * @property memoryUsedBytes Memory used by cache in bytes (if available)
     * @property hitRate Cache hit rate as percentage 0.0-1.0 (if available)
     */
    data class CacheStats(
        val backend: String,
        val connected: Boolean,
        val keyCount: Long? = null,
        val memoryUsedBytes: Long? = null,
        val hitRate: Double? = null
    )
}
