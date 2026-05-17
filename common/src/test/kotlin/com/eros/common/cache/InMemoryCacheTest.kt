package com.eros.common.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for InMemoryCache.
 *
 * Tests all Cache interface methods with various scenarios including:
 * - Basic operations (get, set, delete)
 * - TTL and expiry behavior
 * - Pattern-based deletion
 * - Thread safety
 * - Statistics
 */
class InMemoryCacheTest {

    private lateinit var cache: InMemoryCache

    @BeforeEach
    fun setup() {
        cache = InMemoryCache()
    }

    @Nested
    inner class BasicOperations {

        @Test
        fun `should store and retrieve value`() = runTest {
            // Given
            val key = "test-key"
            val value = "test-value"

            // When
            cache.set(key, value)
            val result = cache.get(key)

            // Then
            assertEquals(value, result)
        }

        @Test
        fun `should return null for non-existent key`() = runTest {
            // When
            val result = cache.get("non-existent")

            // Then
            assertNull(result)
        }

        @Test
        fun `should overwrite existing value`() = runTest {
            // Given
            val key = "test-key"
            cache.set(key, "old-value")

            // When
            cache.set(key, "new-value")
            val result = cache.get(key)

            // Then
            assertEquals("new-value", result)
        }

        @Test
        fun `should delete existing key`() = runTest {
            // Given
            val key = "test-key"
            cache.set(key, "test-value")

            // When
            cache.delete(key)
            val result = cache.get(key)

            // Then
            assertNull(result)
        }

        @Test
        fun `should not fail when deleting non-existent key`() = runTest {
            // When/Then - should not throw
            cache.delete("non-existent")
        }
    }

    @Nested
    inner class TTLBehavior {

        @Test
        fun `should store value with TTL`() = runTest {
            // Given
            val key = "test-key"
            val value = "test-value"

            // When
            cache.set(key, value, ttlSeconds = 10)
            val result = cache.get(key)

            // Then
            assertEquals(value, result)
        }

        @Test
        fun `should return null for expired key`() = runBlocking {
            // Given
            val key = "test-key"
            cache.set(key, "test-value", ttlSeconds = 1)

            // When
            Thread.sleep(1500) // Wait for expiry with margin (using real time, not virtual)
            val result = cache.get(key)

            // Then
            assertNull(result)
        }

        @Test
        fun `should not expire key without TTL`() = runTest {
            // Given
            val key = "test-key"
            cache.set(key, "test-value") // No TTL

            // When
            delay(100)
            val result = cache.get(key)

            // Then
            assertEquals("test-value", result)
        }

        @Test
        fun `should return correct TTL for key`() = runTest {
            // Given
            val key = "test-key"
            cache.set(key, "test-value", ttlSeconds = 10)

            // When
            val ttl = cache.ttl(key)

            // Then
            assertNotNull(ttl)
            assertTrue(ttl in 8..10) // Allow some margin
        }

        @Test
        fun `should return null TTL for non-existent key`() = runTest {
            // When
            val ttl = cache.ttl("non-existent")

            // Then
            assertNull(ttl)
        }

        @Test
        fun `should return null TTL for key without expiry`() = runTest {
            // Given
            cache.set("test-key", "test-value") // No TTL

            // When
            val ttl = cache.ttl("test-key")

            // Then
            assertNull(ttl)
        }

        @Test
        fun `should return null TTL for expired key`() = runBlocking {
            // Given
            cache.set("test-key", "test-value", ttlSeconds = 1)
            Thread.sleep(1500) // Wait for expiry with margin (using real time, not virtual)

            // When
            val ttl = cache.ttl("test-key")

            // Then
            assertNull(ttl)
        }
    }

    @Nested
    inner class TTLValidation {

        @Test
        fun `should reject zero TTL`() = runTest {
            // When/Then
            val exception = assertThrows<IllegalArgumentException> {
                cache.set("key", "value", ttlSeconds = 0)
            }
            assertTrue(exception.message!!.contains("TTL must be positive"))
        }

        @Test
        fun `should reject negative TTL`() = runTest {
            // When/Then
            val exception = assertThrows<IllegalArgumentException> {
                cache.set("key", "value", ttlSeconds = -1)
            }
            assertTrue(exception.message!!.contains("TTL must be positive"))
        }

        @Test
        fun `should reject TTL that causes overflow`() = runTest {
            // When/Then
            val exception = assertThrows<IllegalArgumentException> {
                cache.set("key", "value", ttlSeconds = Long.MAX_VALUE)
            }
            assertTrue(exception.message!!.contains("TTL value too large"))
            assertTrue(exception.message!!.contains("overflow"))
        }

        @Test
        fun `should reject very large TTL that would overflow`() = runTest {
            // Given - a TTL that would overflow when added to current timestamp
            // Current timestamp is ~1.8 billion seconds (year 2026)
            // Long.MAX_VALUE is 9,223,372,036,854,775,807
            // So we need something close to Long.MAX_VALUE that when added to ~1.8B overflows
            val veryLargeTTL = Long.MAX_VALUE - 1_000_000_000L // Will overflow when added to current time

            // When/Then
            val exception = assertThrows<IllegalArgumentException> {
                cache.set("key", "value", ttlSeconds = veryLargeTTL)
            }
            assertTrue(exception.message!!.contains("overflow"))
        }

        @Test
        fun `should accept reasonable large TTL`() = runTest {
            // Given - 10 years in seconds (reasonable upper bound)
            val tenYears = 10L * 365 * 24 * 60 * 60

            // When/Then - should not throw
            cache.set("key", "value", ttlSeconds = tenYears)
            val result = cache.get("key")
            assertEquals("value", result)
        }
    }

    @Nested
    inner class PatternDeletion {

        @Test
        fun `should delete keys matching wildcard pattern`() = runTest {
            // Given
            cache.set("user:123:profile", "profile-data")
            cache.set("user:123:settings", "settings-data")
            cache.set("user:456:profile", "other-profile")

            // When
            cache.deleteByPattern("user:123:*")

            // Then
            assertNull(cache.get("user:123:profile"))
            assertNull(cache.get("user:123:settings"))
            assertNotNull(cache.get("user:456:profile")) // Should not be deleted
        }

        @Test
        fun `should delete all keys with star wildcard`() = runTest {
            // Given
            cache.set("key1", "value1")
            cache.set("key2", "value2")
            cache.set("key3", "value3")

            // When
            cache.deleteByPattern("*")

            // Then
            assertNull(cache.get("key1"))
            assertNull(cache.get("key2"))
            assertNull(cache.get("key3"))
        }

        @Test
        fun `should delete keys matching suffix pattern`() = runTest {
            // Given
            cache.set("user:profile", "profile1")
            cache.set("admin:profile", "profile2")
            cache.set("user:settings", "settings")

            // When
            cache.deleteByPattern("*:profile")

            // Then
            assertNull(cache.get("user:profile"))
            assertNull(cache.get("admin:profile"))
            assertNotNull(cache.get("user:settings")) // Should not be deleted
        }

        @Test
        fun `should handle pattern with no matches`() = runTest {
            // Given
            cache.set("key1", "value1")

            // When/Then - should not throw
            cache.deleteByPattern("non-matching-*")

            // Verify original key still exists
            assertNotNull(cache.get("key1"))
        }

        @Test
        fun `should handle complex patterns`() = runTest {
            // Given
            cache.set("cache:user:123:data", "data1")
            cache.set("cache:user:456:data", "data2")
            cache.set("cache:admin:123:data", "data3")

            // When
            cache.deleteByPattern("cache:user:*:data")

            // Then
            assertNull(cache.get("cache:user:123:data"))
            assertNull(cache.get("cache:user:456:data"))
            assertNotNull(cache.get("cache:admin:123:data"))
        }
    }

    @Nested
    inner class ClearOperation {

        @Test
        fun `should clear all entries`() = runTest {
            // Given
            cache.set("key1", "value1")
            cache.set("key2", "value2")
            cache.set("key3", "value3", ttlSeconds = 100)

            // When
            cache.clear()

            // Then
            assertNull(cache.get("key1"))
            assertNull(cache.get("key2"))
            assertNull(cache.get("key3"))
        }

        @Test
        fun `should clear empty cache without error`() = runTest {
            // When/Then - should not throw
            cache.clear()
        }
    }

    @Nested
    inner class PingOperation {

        @Test
        fun `should always return true for in-memory cache`() = runTest {
            // When
            val result = cache.ping()

            // Then
            assertTrue(result)
        }
    }

    @Nested
    inner class Statistics {

        @Test
        fun `should return correct stats for empty cache`() = runTest {
            // When
            val stats = cache.getStats()

            // Then
            assertEquals("in-memory", stats.backend)
            assertTrue(stats.connected)
            assertEquals(0L, stats.keyCount)
            assertNull(stats.memoryUsedBytes)
            assertNull(stats.hitRate)
        }

        @Test
        fun `should return correct key count`() = runTest {
            // Given
            cache.set("key1", "value1")
            cache.set("key2", "value2")
            cache.set("key3", "value3")

            // When
            val stats = cache.getStats()

            // Then
            assertEquals(3L, stats.keyCount)
        }

        @Test
        fun `should not count expired entries in stats`() = runBlocking {
            // Given
            cache.set("key1", "value1") // No expiry
            cache.set("key2", "value2", ttlSeconds = 1) // Will expire
            Thread.sleep(1500) // Wait for expiry with margin (using real time, not virtual)

            // When
            val stats = cache.getStats()

            // Then
            assertEquals(1L, stats.keyCount) // Only key1 should count
        }

        @Test
        fun `should return backend as in-memory`() = runTest {
            // When
            val stats = cache.getStats()

            // Then
            assertEquals("in-memory", stats.backend)
        }

        @Test
        fun `should always report as connected`() = runTest {
            // When
            val stats = cache.getStats()

            // Then
            assertTrue(stats.connected)
        }
    }

    @Nested
    inner class ThreadSafety {

        @Test
        fun `should handle concurrent writes safely`() = runTest {
            // Given
            val iterations = 100

            // When - multiple coroutines writing concurrently
            val jobs = withContext(Dispatchers.Default) {
                List(10) { threadId ->
                    launch {
                        repeat(iterations) { i ->
                            cache.set("key-$threadId-$i", "value-$threadId-$i")
                        }
                    }
                }
            }

            jobs.joinAll()

            // Then - all values should be present
            val stats = cache.getStats()
            assertEquals(1000L, stats.keyCount) // 10 threads * 100 iterations
        }

        @Test
        fun `should handle concurrent reads and writes safely`() = runTest {
            // Given
            cache.set("shared-key", "initial-value")

            // When - concurrent reads and writes
            val (writeJobs, readJobs) = withContext(Dispatchers.Default) {
                val writes = List(5) { threadId ->
                    launch {
                        repeat(20) { i ->
                            cache.set("shared-key", "value-$threadId-$i")
                        }
                    }
                }

                val reads = List(5) {
                    launch {
                        repeat(20) {
                            cache.get("shared-key") // Just read, don't care about result
                        }
                    }
                }

                writes to reads
            }

            (writeJobs + readJobs).joinAll()

            // Then - should not crash and key should exist
            assertNotNull(cache.get("shared-key"))
        }

        @Test
        fun `should handle concurrent deletes safely`() = runTest {
            // Given
            repeat(100) { i ->
                cache.set("key-$i", "value-$i")
            }

            // When - concurrent deletes
            val jobs = withContext(Dispatchers.Default) {
                List(10) { threadId ->
                    launch {
                        repeat(10) { i ->
                            val keyIndex = threadId * 10 + i
                            cache.delete("key-$keyIndex")
                        }
                    }
                }
            }

            jobs.joinAll()

            // Then - all keys should be deleted
            val stats = cache.getStats()
            assertEquals(0L, stats.keyCount)
        }
    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `should handle empty string value`() = runTest {
            // Given
            val key = "empty-key"
            val value = ""

            // When
            cache.set(key, value)
            val result = cache.get(key)

            // Then
            assertEquals("", result)
        }

        @Test
        fun `should handle very long values`() = runTest {
            // Given
            val key = "long-key"
            val value = "x".repeat(10000)

            // When
            cache.set(key, value)
            val result = cache.get(key)

            // Then
            assertEquals(value, result)
        }

        @Test
        fun `should handle special characters in keys`() = runTest {
            // Given
            val key = "key:with:colons:and-dashes_and_underscores"
            val value = "test-value"

            // When
            cache.set(key, value)
            val result = cache.get(key)

            // Then
            assertEquals(value, result)
        }

        @Test
        fun `should handle special characters in values`() = runTest {
            // Given
            val value = """{"json": "value", "with": ["special", "chars!"]}"""

            // When
            cache.set("json-key", value)
            val result = cache.get("json-key")

            // Then
            assertEquals(value, result)
        }

        @Test
        fun `should handle very short TTL`() = runBlocking {
            // Given
            cache.set("key", "value", ttlSeconds = 1)

            // When
            Thread.sleep(1500) // Wait for expiry with margin (using real time, not virtual)
            val result = cache.get("key")

            // Then - should be expired
            assertNull(result)
        }

        @Test
        fun `should handle very long TTL without overflow`() = runTest {
            // Given - 1 year TTL (within safe range)
            val oneYearSeconds = 365L * 24 * 60 * 60 // ~31 million seconds
            cache.set("key", "value", ttlSeconds = oneYearSeconds)

            // When
            val result = cache.get("key")
            val ttl = cache.ttl("key")

            // Then
            assertEquals("value", result)
            assertNotNull(ttl)
            assertTrue(ttl > 0)
        }
    }
}
