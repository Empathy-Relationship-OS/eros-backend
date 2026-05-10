package com.eros.common.cache

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of UrlCache using ConcurrentHashMap.
 *
 * Suitable for:
 * - Single-instance deployments
 * - Development/testing environments
 * - Low-latency requirements where network overhead is unacceptable
 *
 * Limitations:
 * - Cache is lost on server restart
 * - Not shared across multiple server instances
 * - Memory usage grows with active users (bounded by TTL)
 *
 * For multi-instance deployments, consider RedisUrlCache instead.
 *
 * Thread-safe implementation using ConcurrentHashMap.
 */
class InMemoryUrlCache : UrlCache {
    private data class CacheEntry(
        val signedUrl: String,
        val expiresAt: Instant
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    /**
     * Gets a cached signed URL or generates a new one.
     *
     * @param key Cache key (e.g., "photos/user123/abc.jpg:48")
     * @param expiryHours How long the URL should be valid
     * @param generator Function to generate the URL if not cached
     * @return Signed URL (cached or freshly generated)
     */
    override fun getOrGenerate(
        key: String,
        expiryHours: Long,
        generator: () -> String
    ): String {
        // Clean expired entries periodically (every time cache is accessed)
        cleanExpiredEntries()

        val existing = cache[key]

        // Return cached URL if still valid (with 5-minute buffer before expiry)
        if (existing != null && existing.expiresAt.isAfter(Instant.now().plusSeconds(300))) {
            return existing.signedUrl
        }

        // Generate new URL and cache it
        val newUrl = generator()
        val expiresAt = Instant.now().plusSeconds(expiryHours * 3600)
        cache[key] = CacheEntry(newUrl, expiresAt)

        return newUrl
    }

    /**
     * Removes expired entries from the cache.
     *
     * Called automatically on each access to prevent memory leaks.
     */
    private fun cleanExpiredEntries() {
        val now = Instant.now()
        cache.entries.removeIf { (_, entry) ->
            entry.expiresAt.isBefore(now)
        }
    }

    /**
     * Invalidates a specific cache entry.
     *
     * Useful when a user updates their photo and we need to regenerate the URL.
     */
    override fun invalidate(key: String) {
        cache.remove(key)
    }

    /**
     * Invalidates all cache entries for a specific user.
     *
     * @param userId The user whose URLs should be invalidated
     */
    override fun invalidateUser(userId: String) {
        cache.keys.removeIf { it.contains("photos/$userId/") }
    }

    /**
     * Clears the entire cache.
     *
     * Useful for testing or maintenance.
     */
    override fun clear() {
        cache.clear()
    }

    /**
     * Returns cache statistics for monitoring.
     */
    override fun getStats(): UrlCache.CacheStats {
        cleanExpiredEntries()
        return UrlCache.CacheStats(
            size = cache.size,
            entries = cache.keys.toList(),
            implementation = "in-memory"
        )
    }
}
