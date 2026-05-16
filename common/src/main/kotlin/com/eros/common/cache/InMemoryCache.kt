package com.eros.common.cache

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple in-memory cache implementation using ConcurrentHashMap.
 *
 * Thread-safe but not distributed across application instances.
 * Automatically used as fallback when distributed cache is unavailable.
 *
 * Features:
 * - Thread-safe (ConcurrentHashMap)
 * - TTL support with automatic expiry checking
 * - Pattern-based deletion (glob-style wildcards)
 * - No external dependencies
 *
 * Limitations:
 * - Not shared across multiple application instances
 * - Data lost on application restart
 * - Memory usage grows with cache size (no automatic eviction)
 *
 * Example usage:
 * ```kotlin
 * val cache = InMemoryCache()
 *
 * // Store with TTL
 * cache.set("user:123:profile", profileJson, ttlSeconds = 3600)
 *
 * // Retrieve
 * val profile = cache.get("user:123:profile")
 * ```
 */
class InMemoryCache : Cache {

    private data class CacheEntry(
        val value: String,
        val expiresAt: Long? = null  // null = no expiry, otherwise epoch seconds
    ) {
        /**
         * Checks if this entry has expired.
         */
        fun isExpired(): Boolean {
            return expiresAt != null && Instant.now().epochSecond >= expiresAt
        }
    }

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    override suspend fun get(key: String): String? {
        val entry = cache[key] ?: return null

        // Check expiry
        if (entry.isExpired()) {
            cache.remove(key)
            return null
        }

        return entry.value
    }

    override suspend fun set(key: String, value: String, ttlSeconds: Long) {
        require(ttlSeconds > 0) { "TTL must be positive, got: $ttlSeconds" }

        val now = Instant.now().epochSecond
        val expiresAt = try {
            Math.addExact(now, ttlSeconds)
        } catch (e: ArithmeticException) {
            throw IllegalArgumentException("TTL value too large: $ttlSeconds seconds would cause overflow", e)
        }
        cache[key] = CacheEntry(value, expiresAt)
    }

    override suspend fun set(key: String, value: String) {
        cache[key] = CacheEntry(value, expiresAt = null)
    }

    override suspend fun delete(key: String) {
        cache.remove(key)
    }

    override suspend fun deleteByPattern(pattern: String) {
        // Convert glob pattern to regex
        // Example: "user:123:*" -> "user:123:.*"
        val regexPattern = pattern
            .replace(".", "\\.")  // Escape dots
            .replace("*", ".*")   // Convert * to .*
            .replace("?", ".")    // Convert ? to .
        val regex = regexPattern.toRegex()

        // Find and delete matching keys
        val keysToDelete = cache.keys.filter { regex.matches(it) }
        keysToDelete.forEach { cache.remove(it) }
    }

    override suspend fun ttl(key: String): Long? {
        val entry = cache[key] ?: return null

        if (entry.isExpired()) {
            cache.remove(key)
            return null
        }

        return entry.expiresAt?.let {
            val remaining = it - Instant.now().epochSecond
            if (remaining > 0) remaining else null
        }
    }

    override suspend fun ping(): Boolean = true

    override suspend fun clear() {
        cache.clear()
    }

    override suspend fun getStats(): Cache.CacheStats {
        // Clean up expired entries before returning stats
        cleanupExpiredEntries()

        return Cache.CacheStats(
            backend = "in-memory",
            connected = true,
            keyCount = cache.size.toLong()
        )
    }

    /**
     * Removes all expired entries from the cache.
     *
     * Called during getStats() to provide accurate key count.
     * Could also be called periodically as a cleanup task.
     */
    private fun cleanupExpiredEntries() {
        val expiredKeys = cache.entries
            .filter { it.value.isExpired() }
            .map { it.key }

        expiredKeys.forEach { cache.remove(it) }
    }
}
