package com.eros.common.cache

/**
 * Interface for caching time-limited URLs (CloudFront signed URLs, S3 presigned URLs, etc.).
 *
 * Implementations can use different caching strategies:
 * - In-memory (InMemoryUrlCache)
 * - Redis (RedisUrlCache)
 * - Memcached (MemcachedUrlCache)
 * - Multi-tier (e.g., in-memory L1 + Redis L2)
 *
 * This abstraction allows swapping cache implementations without changing service code.
 */
interface UrlCache {

    /**
     * Gets a cached URL or generates a new one if not cached or expired.
     *
     * @param key Unique cache key (e.g., "photos/user123/abc.jpg:48")
     * @param expiryHours How long the URL should remain valid (in hours)
     * @param generator Function to generate the URL if not cached
     * @return The URL (cached or freshly generated)
     */
    fun getOrGenerate(
        key: String,
        expiryHours: Long,
        generator: () -> String
    ): String

    /**
     * Invalidates a specific cache entry.
     *
     * @param key The cache key to invalidate
     */
    fun invalidate(key: String)

    /**
     * Invalidates all cache entries for a specific user.
     *
     * @param userId The user whose URLs should be invalidated
     */
    fun invalidateUser(userId: String)

    /**
     * Clears the entire cache.
     *
     * Useful for testing or maintenance operations.
     */
    fun clear()

    /**
     * Returns cache statistics for monitoring.
     *
     * @return CacheStats containing metrics about cache usage
     */
    fun getStats(): CacheStats

    /**
     * Cache statistics for monitoring and observability.
     */
    data class CacheStats(
        val size: Int,
        val entries: List<String>,
        val implementation: String = "unknown"
    )
}
