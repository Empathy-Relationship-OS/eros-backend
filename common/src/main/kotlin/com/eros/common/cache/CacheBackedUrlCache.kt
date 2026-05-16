package com.eros.common.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

/**
 * Adapter that implements UrlCache using the generic Cache interface.
 *
 * This allows existing services (like CloudFrontSignerService) to use the new
 * distributed cache infrastructure without any code changes.
 *
 * The adapter adds the UrlCache-specific behavior (5-minute expiry buffer) on top
 * of the generic Cache interface.
 *
 * Example usage:
 * ```kotlin
 * val cache: Cache = DistributedCache(...)
 * val urlCache: UrlCache = CacheBackedUrlCache(cache)
 *
 * // Now use with CloudFrontSignerService
 * val signerService = CloudFrontSignerService(s3Config, urlCache)
 * ```
 */
class CacheBackedUrlCache(
    private val cache: Cache,
    private val bufferMinutes: Long = 5
) : UrlCache {

    companion object {
        private const val KEY_PREFIX = "cloudfront:signed-url:"
        private val logger = KotlinLogging.logger {}
    }

    override fun getOrGenerate(
        key: String,
        expiryHours: Long,
        generator: () -> String
    ): String = runBlocking {
        val fullKey = "$KEY_PREFIX$key"

        // Try to get from cache
        val cached = cache.get(fullKey)
        val ttl = cache.ttl(fullKey)

        // Check if cached value exists and TTL is sufficient (5-minute buffer)
        // This ensures URLs are regenerated before they expire, preventing edge cases
        // where a cached URL is returned but expires before the client can use it
        if (cached != null && ttl != null && ttl > bufferMinutes * 60) {
            logger.debug { "Cache hit for key: $key (TTL: ${ttl}s remaining)" }
            return@runBlocking cached
        }

        // Cache miss or expiring soon - regenerate
        if (cached != null && ttl != null) {
            logger.debug {
                "Cache hit but expiring soon for key: $key (TTL: ${ttl}s < ${bufferMinutes * 60}s buffer), regenerating"
            }
        } else {
            logger.debug { "Cache miss for key: $key, generating new URL" }
        }

        val generated = generator()

        // Store in cache with TTL in seconds
        cache.set(fullKey, generated, expiryHours * 3600)

        generated
    }

    override fun invalidate(key: String) = runBlocking {
        val fullKey = "$KEY_PREFIX$key"
        cache.delete(fullKey)
        logger.debug { "Invalidated cache key: $key" }
    }

    override fun invalidateUser(userId: String) = runBlocking {
        // Pattern: "cloudfront:signed-url:photos/{userId}/*"
        val pattern = "$KEY_PREFIX*$userId*"
        cache.deleteByPattern(pattern)
        logger.debug { "Invalidated all cache entries for user: $userId" }
    }

    override fun invalidateByPrefix(prefix: String) = runBlocking {
        val pattern = "$KEY_PREFIX$prefix*"
        cache.deleteByPattern(pattern)
        logger.debug { "Invalidated all cache entries matching prefix: $prefix" }
    }

    override fun clear() = runBlocking {
        // Only clear CloudFront signed URL entries, not the entire cache
        cache.deleteByPattern("$KEY_PREFIX*")
        logger.warn { "Cleared all CloudFront signed URL cache entries" }
    }

    override fun getStats(): UrlCache.CacheStats = runBlocking {
        val cacheStats = cache.getStats()

        UrlCache.CacheStats(
            size = cacheStats.keyCount?.toInt() ?: 0,
            entries = emptyList(),  // Would require expensive SCAN operation
            implementation = cacheStats.backend
        )
    }
}
