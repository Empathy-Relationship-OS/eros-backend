package com.eros.common.cache

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for CacheBackedUrlCache adapter.
 *
 * Tests the adapter that bridges UrlCache interface to Cache interface:
 * - getOrGenerate with cache hit/miss
 * - 5-minute expiry buffer behavior
 * - Invalidation methods
 * - Key prefix handling
 * - Statistics
 */
class CacheBackedUrlCacheTest {

    private lateinit var backingCache: InMemoryCache
    private lateinit var urlCache: CacheBackedUrlCache

    @BeforeEach
    fun setup() {
        backingCache = InMemoryCache()
        urlCache = CacheBackedUrlCache(backingCache, bufferMinutes = 5)
    }

    @Nested
    inner class GetOrGenerate {

        @Test
        fun `should generate value on cache miss`() = runTest {
            // Given
            val key = "test-key"
            var generatorCalled = false

            // When
            val result = urlCache.getOrGenerate(key, expiryHours = 1) {
                generatorCalled = true
                "generated-value"
            }

            // Then
            assertEquals("generated-value", result)
            assertTrue(generatorCalled)
        }

        @Test
        fun `should return cached value on cache hit`() = runTest {
            // Given
            val key = "test-key"
            urlCache.getOrGenerate(key, expiryHours = 1) { "first-value" }

            var generatorCalled = false

            // When
            val result = urlCache.getOrGenerate(key, expiryHours = 1) {
                generatorCalled = true
                "second-value"
            }

            // Then
            assertEquals("first-value", result) // Should return cached value
            assertTrue(!generatorCalled) // Generator should not be called
        }

        @Test
        fun `should cache generated value with correct TTL`() = runTest {
            // Given
            val key = "test-key"
            val expiryHours = 2L

            // When
            urlCache.getOrGenerate(key, expiryHours = expiryHours) { "value" }

            // Then
            val fullKey = "cloudfront:signed-url:$key"
            val ttl = backingCache.ttl(fullKey)
            assertNotNull(ttl)
            // TTL should be approximately 2 hours (7200 seconds)
            assertTrue(ttl in 7150..7200)
        }

        @Test
        fun `should use key prefix for all operations`() = runTest {
            // Given
            val key = "my-key"

            // When
            urlCache.getOrGenerate(key, expiryHours = 1) { "value" }

            // Then
            val fullKey = "cloudfront:signed-url:$key"
            assertEquals("value", backingCache.get(fullKey))
        }

        @Test
        fun `should handle different keys independently`() = runTest {
            // When
            val result1 = urlCache.getOrGenerate("key1", expiryHours = 1) { "value1" }
            val result2 = urlCache.getOrGenerate("key2", expiryHours = 1) { "value2" }

            // Then
            assertEquals("value1", result1)
            assertEquals("value2", result2)
        }
    }

    @Nested
    inner class ExpiryBuffer {

        @Test
        fun `should regenerate when TTL is below buffer threshold`() = runTest {
            // Given - manually set cache entry with low TTL
            val key = "test-key"
            val fullKey = "cloudfront:signed-url:$key"
            backingCache.set(fullKey, "old-value", ttlSeconds = 60) // 1 minute (< 5 minute buffer)

            var generatorCalled = false

            // When
            val result = urlCache.getOrGenerate(key, expiryHours = 1) {
                generatorCalled = true
                "new-value"
            }

            // Then
            assertEquals("new-value", result)
            assertTrue(generatorCalled) // Should regenerate due to low TTL
        }

        @Test
        fun `should not regenerate when TTL is above buffer threshold`() = runTest {
            // Given - manually set cache entry with high TTL
            val key = "test-key"
            val fullKey = "cloudfront:signed-url:$key"
            backingCache.set(fullKey, "old-value", ttlSeconds = 600) // 10 minutes (> 5 minute buffer)

            var generatorCalled = false

            // When
            val result = urlCache.getOrGenerate(key, expiryHours = 1) {
                generatorCalled = true
                "new-value"
            }

            // Then
            assertEquals("old-value", result)
            assertTrue(!generatorCalled) // Should use cached value
        }

        @Test
        fun `should use custom buffer minutes`() = runTest {
            // Given - custom 2-minute buffer
            val customUrlCache = CacheBackedUrlCache(backingCache, bufferMinutes = 2)
            val key = "test-key"
            val fullKey = "cloudfront:signed-url:$key"
            backingCache.set(fullKey, "old-value", ttlSeconds = 150) // 2.5 minutes

            var generatorCalled = false

            // When - TTL (2.5 min) > buffer (2 min), should use cached
            val result = customUrlCache.getOrGenerate(key, expiryHours = 1) {
                generatorCalled = true
                "new-value"
            }

            // Then
            assertEquals("old-value", result)
            assertTrue(!generatorCalled)
        }
    }

    @Nested
    inner class Invalidation {

        @Test
        fun `should invalidate specific key`() = runTest {
            // Given
            val key = "test-key"
            urlCache.getOrGenerate(key, expiryHours = 1) { "value" }

            // When
            urlCache.invalidate(key)

            // Then - should generate new value
            var generatorCalled = false
            urlCache.getOrGenerate(key, expiryHours = 1) {
                generatorCalled = true
                "new-value"
            }
            assertTrue(generatorCalled)
        }

        @Test
        fun `should not fail when invalidating non-existent key`() = runTest {
            // When/Then - should not throw
            urlCache.invalidate("non-existent-key")
        }

        @Test
        fun `should invalidate user keys`() = runTest {
            // Given - keys for user 123
            urlCache.getOrGenerate("photos/user123/img1.jpg", expiryHours = 1) { "url1" }
            urlCache.getOrGenerate("photos/user123/img2.jpg", expiryHours = 1) { "url2" }
            urlCache.getOrGenerate("photos/user456/img3.jpg", expiryHours = 1) { "url3" }

            // When
            urlCache.invalidateUser("user123")

            // Then - user123 keys should be regenerated, user456 should not
            var generator1Called = false
            var generator2Called = false
            var generator3Called = false

            urlCache.getOrGenerate("photos/user123/img1.jpg", expiryHours = 1) {
                generator1Called = true
                "new-url1"
            }
            urlCache.getOrGenerate("photos/user123/img2.jpg", expiryHours = 1) {
                generator2Called = true
                "new-url2"
            }
            urlCache.getOrGenerate("photos/user456/img3.jpg", expiryHours = 1) {
                generator3Called = true
                "new-url3"
            }

            assertTrue(generator1Called)
            assertTrue(generator2Called)
            assertTrue(!generator3Called) // user456 should still be cached
        }

        @Test
        fun `should invalidate by prefix`() = runTest {
            // Given
            urlCache.getOrGenerate("photos/user123/profile.jpg", expiryHours = 1) { "url1" }
            urlCache.getOrGenerate("photos/user123/cover.jpg", expiryHours = 1) { "url2" }
            urlCache.getOrGenerate("documents/user123/file.pdf", expiryHours = 1) { "url3" }

            // When
            urlCache.invalidateByPrefix("photos/")

            // Then - only photos should be invalidated
            var generator1Called = false
            var generator2Called = false
            var generator3Called = false

            urlCache.getOrGenerate("photos/user123/profile.jpg", expiryHours = 1) {
                generator1Called = true
                "new-url1"
            }
            urlCache.getOrGenerate("photos/user123/cover.jpg", expiryHours = 1) {
                generator2Called = true
                "new-url2"
            }
            urlCache.getOrGenerate("documents/user123/file.pdf", expiryHours = 1) {
                generator3Called = true
                "new-url3"
            }

            assertTrue(generator1Called)
            assertTrue(generator2Called)
            assertTrue(!generator3Called) // documents should still be cached
        }
    }

    @Nested
    inner class Clear {

        @Test
        fun `should clear all URL cache entries`() = runTest {
            // Given
            urlCache.getOrGenerate("key1", expiryHours = 1) { "value1" }
            urlCache.getOrGenerate("key2", expiryHours = 1) { "value2" }

            // When
            urlCache.clear()

            // Then - all should be regenerated
            var generator1Called = false
            var generator2Called = false

            urlCache.getOrGenerate("key1", expiryHours = 1) {
                generator1Called = true
                "new-value1"
            }
            urlCache.getOrGenerate("key2", expiryHours = 1) {
                generator2Called = true
                "new-value2"
            }

            assertTrue(generator1Called)
            assertTrue(generator2Called)
        }

        @Test
        fun `should only clear URL cache entries not all cache`() = runTest {
            // Given - add both URL cache and direct cache entries
            urlCache.getOrGenerate("url-key", expiryHours = 1) { "url-value" }
            backingCache.set("other-key", "other-value") // Not a URL cache entry

            // When
            urlCache.clear()

            // Then - URL cache entries cleared, but other entries remain
            val otherValue = backingCache.get("other-key")
            assertEquals("other-value", otherValue)
        }
    }

    @Nested
    inner class Statistics {

        @Test
        fun `should return stats from backing cache`() = runTest {
            // Given
            urlCache.getOrGenerate("key1", expiryHours = 1) { "value1" }
            urlCache.getOrGenerate("key2", expiryHours = 1) { "value2" }

            // When
            val stats = urlCache.getStats()

            // Then
            assertEquals("in-memory", stats.implementation)
            assertTrue(stats.size >= 2) // At least 2 entries
        }

        @Test
        fun `should return implementation name from backing cache`() = runTest {
            // When
            val stats = urlCache.getStats()

            // Then
            assertEquals("in-memory", stats.implementation)
        }

        @Test
        fun `should not populate entries list`() = runTest {
            // Given
            urlCache.getOrGenerate("key1", expiryHours = 1) { "value1" }

            // When
            val stats = urlCache.getStats()

            // Then - entries should be empty (expensive to populate)
            assertTrue(stats.entries.isEmpty())
        }
    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `should handle empty key`() = runTest {
            // When
            val result = urlCache.getOrGenerate("", expiryHours = 1) { "value" }

            // Then
            assertEquals("value", result)
        }

        @Test
        fun `should handle keys with special characters`() = runTest {
            // Given
            val key = "path/to/file:with:colons_and_underscores.jpg"

            // When
            val result = urlCache.getOrGenerate(key, expiryHours = 1) { "url" }

            // Then
            assertEquals("url", result)
        }

        @Test
        fun `should handle very long expiry times`() = runTest {
            // Given
            val key = "test-key"

            // When
            val result = urlCache.getOrGenerate(key, expiryHours = 8760) { "value" } // 1 year

            // Then
            assertEquals("value", result)
        }

        @Test
        fun `should reject zero expiry`() = runTest {
            // Given
            val key = "test-key"

            // When/Then - should throw due to zero TTL
            val exception = assertThrows<IllegalArgumentException> {
                urlCache.getOrGenerate(key, expiryHours = 0) { "value" }
            }
            assertTrue(exception.message!!.contains("TTL must be positive"))
        }

        @Test
        fun `should handle generator returning empty string`() = runTest {
            // When
            val result = urlCache.getOrGenerate("key", expiryHours = 1) { "" }

            // Then
            assertEquals("", result)
        }

        @Test
        fun `should handle generator returning large values`() = runTest {
            // When
            val largeValue = "x".repeat(10000)
            val result = urlCache.getOrGenerate("key", expiryHours = 1) { largeValue }

            // Then
            assertEquals(largeValue, result)
        }
    }

    @Nested
    inner class RealWorldScenarios {

        @Test
        fun `should handle CloudFront URL caching scenario`() = runTest {
            // Simulate CloudFront signed URL generation
            val objectKey = "photos/user123/profile.jpg"
            val userId = "user123"
            val expiryHours = 48L

            // First request - cache miss
            val url1 = urlCache.getOrGenerate(objectKey, expiryHours) {
                "https://d123.cloudfront.net/$objectKey?Expires=123&Signature=abc"
            }

            // Second request - cache hit
            val url2 = urlCache.getOrGenerate(objectKey, expiryHours) {
                "https://d123.cloudfront.net/$objectKey?Expires=456&Signature=def"
            }

            // Should return same URL (cached)
            assertEquals(url1, url2)

            // Invalidate user's URLs
            urlCache.invalidateUser(userId)

            // Third request - cache miss (regenerated)
            val url3 = urlCache.getOrGenerate(objectKey, expiryHours) {
                "https://d123.cloudfront.net/$objectKey?Expires=789&Signature=ghi"
            }

            // Should be different from cached URL
            assertTrue(url1 != url3)
        }

        @Test
        fun `should handle multiple users independently`() = runTest {
            // Cache URLs for different users
            urlCache.getOrGenerate("photos/user1/img.jpg", 1) { "url1" }
            urlCache.getOrGenerate("photos/user2/img.jpg", 1) { "url2" }

            // Invalidate user1
            urlCache.invalidateUser("user1")

            // user1 should regenerate, user2 should not
            var user1Generated = false
            var user2Generated = false

            urlCache.getOrGenerate("photos/user1/img.jpg", 1) {
                user1Generated = true
                "new-url1"
            }

            urlCache.getOrGenerate("photos/user2/img.jpg", 1) {
                user2Generated = true
                "new-url2"
            }

            assertTrue(user1Generated)
            assertTrue(!user2Generated)
        }
    }
}
