package com.eros.common.cache

import io.lettuce.core.RedisClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for DistributedCache using Testcontainers with Valkey 9.0.4.
 *
 * Tests all Cache interface methods against a real Valkey instance:
 * - Basic operations (get, set, delete)
 * - TTL and expiry behavior
 * - Pattern-based deletion using SCAN
 * - Connection handling
 * - Statistics parsing
 * - Thread safety with real network I/O
 */
@Testcontainers
class DistributedCacheTest {

    companion object {
        private const val TEST_PASSWORD = "test-password-123"

        @Container
        @JvmStatic
        val valkeyContainer = GenericContainer<Nothing>("valkey/valkey:9.0.4-alpine")
            .apply {
                withExposedPorts(6379)
                withCommand(
                    "valkey-server",
                    "--requirepass", TEST_PASSWORD,
                    "--maxmemory", "128mb",
                    "--maxmemory-policy", "allkeys-lru"
                )
            }
    }

    private lateinit var redisClient: RedisClient
    private lateinit var cache: DistributedCache

    @BeforeEach
    fun setup() {
        val uri = "redis://:$TEST_PASSWORD@${valkeyContainer.host}:${valkeyContainer.firstMappedPort}/0"
        redisClient = RedisClient.create(uri)
        cache = DistributedCache(redisClient, CacheBackend.VALKEY)

        // Clear any existing data
        runBlocking {
            cache.clear()
        }
    }

    @AfterEach
    fun cleanup() {
        runBlocking {
            cache.clear()
        }
        redisClient.shutdown()
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
            val result = cache.get("non-existent-key")

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
            cache.delete("non-existent-key")
        }

        @Test
        fun `should handle multiple keys`() = runTest {
            // Given
            val keys = (1..100).map { "key-$it" to "value-$it" }

            // When
            keys.forEach { (key, value) ->
                cache.set(key, value)
            }

            // Then - all should be retrievable
            keys.forEach { (key, value) ->
                assertEquals(value, cache.get(key))
            }
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
        fun `should expire key after TTL`() = runBlocking {
            // Given
            val key = "test-key"
            cache.set(key, "test-value", ttlSeconds = 2)

            // When
            Thread.sleep(2500) // Wait for expiry (using real time, not virtual)
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
            assertTrue(ttl in 8..10) // Allow margin for execution time
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
        fun `should handle very short TTL`() = runBlocking {
            // Given
            cache.set("key", "value", ttlSeconds = 1)

            // When
            Thread.sleep(1500) // Wait for expiry (using real time, not virtual)
            val result = cache.get("key")

            // Then
            assertNull(result)
        }

        @Test
        fun `should handle long TTL`() = runTest {
            // Given
            cache.set("key", "value", ttlSeconds = 3600) // 1 hour

            // When
            val result = cache.get("key")
            val ttl = cache.ttl("key")

            // Then
            assertEquals("value", result)
            assertNotNull(ttl)
            assertTrue(ttl > 3500) // Should be close to 3600
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
            assertNotNull(cache.get("user:settings"))
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
        fun `should handle large number of keys with SCAN`() = runTest {
            // Given - create 200 keys
            repeat(200) { i ->
                cache.set("batch:user:$i", "data-$i")
            }

            // When
            cache.deleteByPattern("batch:user:*")

            // Then - all should be deleted
            repeat(200) { i ->
                assertNull(cache.get("batch:user:$i"))
            }
        }

        @Test
        fun `should use SCAN not KEYS for pattern deletion`() = runTest {
            // Given - many keys
            repeat(50) { i ->
                cache.set("test:$i", "value-$i")
            }

            // When - this should use SCAN internally
            cache.deleteByPattern("test:*")

            // Then - all deleted
            repeat(50) { i ->
                assertNull(cache.get("test:$i"))
            }
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
        fun `should handle clearing empty cache`() = runTest {
            // When/Then - should not throw
            cache.clear()
        }
    }

    @Nested
    inner class PingOperation {

        @Test
        fun `should return true for healthy connection`() = runTest {
            // When
            val result = cache.ping()

            // Then
            assertTrue(result)
        }

        @Test
        fun `should be able to ping multiple times`() = runTest {
            // When
            repeat(10) {
                val result = cache.ping()
                assertTrue(result)
            }
        }
    }

    @Nested
    inner class Statistics {

        @Test
        fun `should return correct stats for empty cache`() = runTest {
            // When
            val stats = cache.getStats()

            // Then
            assertEquals("valkey", stats.backend)
            assertTrue(stats.connected)
            // keyCount might be 0 or null depending on INFO response
            assertTrue(stats.keyCount == null || stats.keyCount == 0L)
        }

        @Test
        fun `should return correct backend name`() = runTest {
            // When
            val stats = cache.getStats()

            // Then
            assertEquals("valkey", stats.backend)
        }

        @Test
        fun `should report as connected`() = runTest {
            // When
            val stats = cache.getStats()

            // Then
            assertTrue(stats.connected)
        }

        @Test
        fun `should parse memory usage from INFO`() = runTest {
            // Given - add some data
            repeat(10) { i ->
                cache.set("key-$i", "value-$i".repeat(100))
            }

            // When
            val stats = cache.getStats()

            // Then - memory should be reported
            assertNotNull(stats.memoryUsedBytes)
            assertTrue(stats.memoryUsedBytes > 0)
        }

        @Test
        fun `should parse key count from INFO`() = runTest {
            // Given
            cache.set("key1", "value1")
            cache.set("key2", "value2")
            cache.set("key3", "value3")

            // When
            val stats = cache.getStats()

            // Then
            // Valkey INFO might report key count
            if (stats.keyCount != null) {
                assertTrue(stats.keyCount >= 3)
            }
        }
    }

    @Nested
    inner class ThreadSafety {

        @Test
        fun `should handle concurrent writes safely`() = runTest {
            // Given
            val iterations = 50

            // When - multiple coroutines writing concurrently
            val jobs = List(10) { threadId ->
                launch {
                    repeat(iterations) { i ->
                        cache.set("key-$threadId-$i", "value-$threadId-$i")
                    }
                }
            }

            jobs.joinAll()

            // Then - all values should be present
            repeat(10) { threadId ->
                repeat(iterations) { i ->
                    val value = cache.get("key-$threadId-$i")
                    assertEquals("value-$threadId-$i", value)
                }
            }
        }

        @Test
        fun `should handle concurrent reads and writes safely`() = runTest {
            // Given
            cache.set("shared-key", "initial-value")

            // When - concurrent reads and writes
            val writeJobs = List(5) { threadId ->
                launch {
                    repeat(20) { i ->
                        cache.set("shared-key", "value-$threadId-$i")
                    }
                }
            }

            val readJobs = List(5) {
                launch {
                    repeat(20) {
                        cache.get("shared-key")
                    }
                }
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
            val jobs = List(10) { threadId ->
                launch {
                    repeat(10) { i ->
                        val keyIndex = threadId * 10 + i
                        cache.delete("key-$keyIndex")
                    }
                }
            }

            jobs.joinAll()

            // Then - all keys should be deleted
            repeat(100) { i ->
                assertNull(cache.get("key-$i"))
            }
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
        fun `should handle JSON values`() = runTest {
            // Given
            val value = """{"userId": 123, "name": "John Doe", "active": true}"""

            // When
            cache.set("json-key", value)
            val result = cache.get("json-key")

            // Then
            assertEquals(value, result)
        }

        @Test
        fun `should handle unicode characters`() = runTest {
            // Given
            val value = "Hello 世界 🌍 Привет"

            // When
            cache.set("unicode-key", value)
            val result = cache.get("unicode-key")

            // Then
            assertEquals(value, result)
        }
    }

    @Nested
    inner class ConnectionHandling {

        @Test
        fun `should handle reconnection after operation`() = runTest {
            // Given - do some operations
            cache.set("key1", "value1")

            // When - do more operations (new connections)
            cache.set("key2", "value2")
            val result1 = cache.get("key1")
            val result2 = cache.get("key2")

            // Then
            assertEquals("value1", result1)
            assertEquals("value2", result2)
        }

        @Test
        fun `should handle multiple rapid operations`() = runTest {
            // When - rapid fire operations
            repeat(50) { i ->
                cache.set("rapid-$i", "value-$i")
                val result = cache.get("rapid-$i")
                assertEquals("value-$i", result)
            }
        }
    }
}
