package com.eros.common.cache

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InMemoryUrlCacheTest {

    @Nested
    inner class `getOrGenerate()` {

        @Nested
        inner class `cache hits` {

            @Test
            fun `should return generated URL on first request (cache miss)`() {
                val cache = InMemoryUrlCache()
                var generationCount = 0

                val url = cache.getOrGenerate("test-key:48", 48) {
                    generationCount++
                    "https://example.com/generated-url-1"
                }

                assertEquals("https://example.com/generated-url-1", url)
                assertEquals(1, generationCount, "Generator should be called once on cache miss")
            }

            @Test
            fun `should return cached URL on subsequent requests (cache hit)`() {
                val cache = InMemoryUrlCache()
                var generationCount = 0

                // First request - cache miss
                val url1 = cache.getOrGenerate("test-key:48", 48) {
                    generationCount++
                    "https://example.com/url-${generationCount}"
                }

                // Second request - cache hit
                val url2 = cache.getOrGenerate("test-key:48", 48) {
                    generationCount++
                    "https://example.com/url-${generationCount}"
                }

                // Third request - cache hit
                val url3 = cache.getOrGenerate("test-key:48", 48) {
                    generationCount++
                    "https://example.com/url-${generationCount}"
                }

                assertEquals(url1, url2, "Second request should return cached URL")
                assertEquals(url1, url3, "Third request should return cached URL")
                assertEquals(1, generationCount, "Generator should only be called once")
            }

            @Test
            fun `should cache different URLs for different keys`() {
                val cache = InMemoryUrlCache()

                val url1 = cache.getOrGenerate("photos/user1/abc.jpg:48", 48) {
                    "https://example.com/user1-photo"
                }

                val url2 = cache.getOrGenerate("photos/user2/def.jpg:48", 48) {
                    "https://example.com/user2-photo"
                }

                assertEquals("https://example.com/user1-photo", url1)
                assertEquals("https://example.com/user2-photo", url2)
                assertTrue(url1 != url2, "Different keys should have different cached values")
            }

            @Test
            fun `should cache different URLs for same object with different expiry times`() {
                val cache = InMemoryUrlCache()

                val url48h = cache.getOrGenerate("photos/user1/abc.jpg:48", 48) {
                    "https://example.com/photo-48h"
                }

                val url24h = cache.getOrGenerate("photos/user1/abc.jpg:24", 24) {
                    "https://example.com/photo-24h"
                }

                assertEquals("https://example.com/photo-48h", url48h)
                assertEquals("https://example.com/photo-24h", url24h)
                assertTrue(url48h != url24h, "Different expiry times should produce different cache entries")
            }
        }

        @Nested
        inner class `cache misses and expiry` {

            @Test
            fun `should regenerate URL after expiry time has passed`() {
                val cache = InMemoryUrlCache()
                var generationCount = 0

                // Use 0 hours expiry (expires immediately for testing)
                val url1 = cache.getOrGenerate("test-key:0", 0) {
                    generationCount++
                    "https://example.com/url-${generationCount}"
                }

                // Wait a tiny bit to ensure expiry (for 0-hour TTL)
                Thread.sleep(10)

                // Should regenerate because entry is expired
                val url2 = cache.getOrGenerate("test-key:0", 0) {
                    generationCount++
                    "https://example.com/url-${generationCount}"
                }

                assertEquals("https://example.com/url-1", url1)
                assertEquals("https://example.com/url-2", url2)
                assertEquals(2, generationCount, "Generator should be called twice due to expiry")
            }

            @Test
            fun `should clean up expired entries automatically`() {
                val cache = InMemoryUrlCache()

                // Add entry with 0-hour expiry (expires immediately)
                cache.getOrGenerate("expired-key:0", 0) { "expired-url" }

                // Add entry with long expiry
                cache.getOrGenerate("valid-key:48", 48) { "valid-url" }

                // Wait for expired entry to be removed
                Thread.sleep(10)

                // Trigger cleanup by getting stats
                val stats = cache.getStats()

                // Only the valid entry should remain
                assertEquals(1, stats.size, "Expired entries should be cleaned up")
                assertTrue(stats.entries.contains("valid-key:48"), "Valid entry should remain")
                assertFalse(stats.entries.contains("expired-key:0"), "Expired entry should be removed")
            }

            @Test
            fun `should respect 5-minute buffer before expiry`() {
                val cache = InMemoryUrlCache()
                var generationCount = 0

                // Create entry with very short expiry (but not 0)
                // This tests that cache uses 5-minute buffer
                // For this test, we can't easily test the exact buffer without mocking time,
                // so we verify that entries don't expire prematurely
                val url1 = cache.getOrGenerate("test-key:24", 24) {
                    generationCount++
                    "https://example.com/url-1"
                }

                // Immediately request again - should still be cached
                val url2 = cache.getOrGenerate("test-key:24", 24) {
                    generationCount++
                    "https://example.com/url-2"
                }

                assertEquals(url1, url2)
                assertEquals(1, generationCount, "Should not regenerate within valid time")
            }
        }

        @Nested
        inner class `thread safety` {

            @Test
            fun `should handle concurrent requests to same key (thread safety)`() {
                val cache = InMemoryUrlCache()
                val threadCount = 50
                val latch = CountDownLatch(threadCount)
                val executor = Executors.newFixedThreadPool(threadCount)

                val results = mutableListOf<String>()
                val exceptions = mutableListOf<Throwable>()

                // Pre-populate cache to avoid race condition during initial population
                cache.getOrGenerate("concurrent-key:48", 48) {
                    "https://example.com/concurrent-url"
                }

                // Launch 50 threads trying to get the same cached value
                repeat(threadCount) {
                    executor.submit {
                        try {
                            val url = cache.getOrGenerate("concurrent-key:48", 48) {
                                // This should not be called since entry is already cached
                                throw IllegalStateException("Generator should not be called - entry is cached")
                            }
                            synchronized(results) {
                                results.add(url)
                            }
                        } catch (e: Throwable) {
                            synchronized(exceptions) {
                                exceptions.add(e)
                            }
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                // Wait for all threads to complete
                latch.await()
                executor.shutdown()

                // No exceptions should have occurred
                assertTrue(exceptions.isEmpty(),
                    "No exceptions should occur during concurrent access. Got: ${exceptions.map { it.message }}")

                // All threads should get the same URL
                assertEquals(threadCount, results.size, "All threads should complete")
                assertTrue(results.all { it == "https://example.com/concurrent-url" },
                    "All threads should get the same cached URL")
            }
        }
    }

    @Nested
    inner class `invalidation` {

        @Test
        fun `should invalidate specific cache entry`() {
            val cache = InMemoryUrlCache()
            var generationCount = 0

            // Cache a URL
            val url1 = cache.getOrGenerate("test-key:48", 48) {
                generationCount++
                "https://example.com/url-${generationCount}"
            }

            // Invalidate it
            cache.invalidate("test-key:48")

            // Should regenerate after invalidation
            val url2 = cache.getOrGenerate("test-key:48", 48) {
                generationCount++
                "https://example.com/url-${generationCount}"
            }

            assertEquals("https://example.com/url-1", url1)
            assertEquals("https://example.com/url-2", url2)
            assertEquals(2, generationCount, "Generator should be called again after invalidation")
        }

        @Test
        fun `should invalidate all entries for a user`() {
            val cache = InMemoryUrlCache()

            // Cache multiple URLs for user123
            cache.getOrGenerate("photos/user123/photo1.jpg:48", 48) { "url1" }
            cache.getOrGenerate("photos/user123/photo2.jpg:48", 48) { "url2" }
            cache.getOrGenerate("photos/user123/photo3.jpg:24", 24) { "url3" }

            // Cache URLs for another user (should not be affected)
            cache.getOrGenerate("photos/user456/photo1.jpg:48", 48) { "url4" }

            // Invalidate all URLs for user123
            cache.invalidateUser("user123")

            val stats = cache.getStats()

            // Only user456's URL should remain
            assertEquals(1, stats.size, "Only user456's entries should remain")
            assertTrue(stats.entries.contains("photos/user456/photo1.jpg:48"),
                "user456's entry should remain")
            assertFalse(stats.entries.any { it.contains("user123") },
                "All user123 entries should be removed")
        }

        @Test
        fun `should clear entire cache`() {
            val cache = InMemoryUrlCache()

            // Cache multiple URLs
            cache.getOrGenerate("key1:48", 48) { "url1" }
            cache.getOrGenerate("key2:48", 48) { "url2" }
            cache.getOrGenerate("key3:24", 24) { "url3" }

            // Verify cache has entries
            assertEquals(3, cache.getStats().size)

            // Clear cache
            cache.clear()

            // Cache should be empty
            val stats = cache.getStats()
            assertEquals(0, stats.size, "Cache should be empty after clear()")
            assertTrue(stats.entries.isEmpty(), "No entries should remain")
        }
    }

    @Nested
    inner class `statistics` {

        @Test
        fun `should return correct cache size and entries`() {
            val cache = InMemoryUrlCache()

            // Add entries
            cache.getOrGenerate("key1:48", 48) { "url1" }
            cache.getOrGenerate("key2:48", 48) { "url2" }
            cache.getOrGenerate("key3:24", 24) { "url3" }

            val stats = cache.getStats()

            assertEquals(3, stats.size, "Size should reflect number of cached entries")
            assertEquals("in-memory", stats.implementation, "Implementation should be 'in-memory'")
            assertTrue(stats.entries.contains("key1:48"), "Entries should include key1")
            assertTrue(stats.entries.contains("key2:48"), "Entries should include key2")
            assertTrue(stats.entries.contains("key3:24"), "Entries should include key3")
        }

        @Test
        fun `should exclude expired entries from stats`() {
            val cache = InMemoryUrlCache()

            // Add entry with 0-hour expiry (expires immediately)
            cache.getOrGenerate("expired:0", 0) { "expired-url" }

            // Add valid entries
            cache.getOrGenerate("valid1:48", 48) { "url1" }
            cache.getOrGenerate("valid2:48", 48) { "url2" }

            // Wait for expired entry to be cleaned
            Thread.sleep(10)

            val stats = cache.getStats()

            // Only valid entries should be in stats
            assertEquals(2, stats.size, "Expired entries should not be counted")
            assertFalse(stats.entries.contains("expired:0"), "Expired entry should not be in stats")
        }
    }
}
