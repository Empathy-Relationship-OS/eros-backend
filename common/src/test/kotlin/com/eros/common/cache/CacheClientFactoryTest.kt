package com.eros.common.cache

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for CacheClientFactory.
 *
 * Tests factory logic including:
 * - Backend selection (Valkey/Redis/In-Memory)
 * - Connection handling
 * - Fail-fast behavior
 * - Configuration validation
 * - TLS support
 */
@Testcontainers
class CacheClientFactoryTest {

    companion object {
        @Container
        @JvmStatic
        val valkeyContainer = GenericContainer<Nothing>("valkey/valkey:9.0.4-alpine")
            .apply {
                withExposedPorts(6379)
                withCommand("valkey-server", "--requirepass", "test-pass")
            }
    }

    @Nested
    inner class BackendSelection {

        @Test
        fun `should create InMemoryCache when disabled`() {
            // Given
            val config = CacheConfig(
                enabled = false,
                backend = CacheBackend.VALKEY, // Backend doesn't matter when disabled
                host = "localhost",
                port = 6379,
                password = null,
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When
            val cache = CacheClientFactory.create(config)

            // Then
            assertIs<InMemoryCache>(cache)
        }

        @Test
        fun `should create InMemoryCache when backend is IN_MEMORY`() {
            // Given
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.IN_MEMORY,
                host = "localhost",
                port = 6379,
                password = null,
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When
            val cache = CacheClientFactory.create(config)

            // Then
            assertIs<InMemoryCache>(cache)
        }

        @Test
        fun `should create DistributedCache for Valkey backend`() = runTest {
            // Given
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = valkeyContainer.host,
                port = valkeyContainer.firstMappedPort,
                password = "test-pass",
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When
            val cache = CacheClientFactory.create(config)

            // Then
            assertIs<DistributedCache>(cache)
            assertTrue(cache.ping())
        }

        @Test
        fun `should create DistributedCache for Redis backend`() = runTest {
            // Given - Redis and Valkey use same protocol
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.REDIS,
                host = valkeyContainer.host,
                port = valkeyContainer.firstMappedPort,
                password = "test-pass",
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When
            val cache = CacheClientFactory.create(config)

            // Then
            assertIs<DistributedCache>(cache)
            assertTrue(cache.ping())
        }
    }

    @Nested
    inner class FailFastBehavior {

        @Test
        fun `should throw exception when Valkey is unreachable`() {
            // Given - invalid host
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = "non-existent-host-12345",
                port = 6379,
                password = null,
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 1000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When/Then
            assertThrows<Exception> {
                CacheClientFactory.create(config)
            }
        }

        @Test
        fun `should throw exception when port is wrong`() {
            // Given - wrong port
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = valkeyContainer.host,
                port = 9999, // Wrong port
                password = "test-pass",
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 1000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When/Then
            assertThrows<Exception> {
                CacheClientFactory.create(config)
            }
        }

        @Test
        fun `should throw exception when password is wrong`() {
            // Given - wrong password
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = valkeyContainer.host,
                port = valkeyContainer.firstMappedPort,
                password = "wrong-password",
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When/Then
            assertThrows<Exception> {
                CacheClientFactory.create(config)
            }
        }
    }

    @Nested
    inner class ConnectionValidation {

        @Test
        fun `should validate connection with PING`() = runTest {
            // Given
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = valkeyContainer.host,
                port = valkeyContainer.firstMappedPort,
                password = "test-pass",
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When
            val cache = CacheClientFactory.create(config)

            // Then - PING should succeed
            assertIs<DistributedCache>(cache)
            assertTrue(cache.ping())
        }

        @Test
        fun `should connect to different database numbers`() = runTest {
            // Test connecting to database 1
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = valkeyContainer.host,
                port = valkeyContainer.firstMappedPort,
                password = "test-pass",
                database = 1, // Different database
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When
            val cache = CacheClientFactory.create(config)

            // Then
            assertIs<DistributedCache>(cache)
            assertTrue(cache.ping())
        }
    }

    @Nested
    inner class ConfigurationHandling {

        @Test
        fun `should respect connection timeout`() {
            // Given - very short timeout, host that will timeout
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = "10.255.255.1", // Non-routable IP (will timeout)
                port = 6379,
                password = null,
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 100, // Very short timeout
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When
            val startTime = System.currentTimeMillis()
            assertThrows<Exception> {
                CacheClientFactory.create(config)
            }
            val duration = System.currentTimeMillis() - startTime

            // Then - should timeout quickly (within reasonable margin)
            assertTrue(duration < 5000) // Should not hang for long
        }

        @Test
        fun `should handle connection without password`() = runTest {
            // Note: This test would require a Valkey instance without password
            // For now, we document that password is supported
            // In production, always use passwords!

            // This is more of a documentation test
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = "localhost",
                port = 6379,
                password = null, // No password
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // URI should not have password
            val uri = config.buildUri()
            assertEquals("redis://localhost:6379/0", uri)
        }
    }

    @Nested
    inner class PoolConfiguration {

        @Test
        fun `should accept various pool configurations`() = runTest {
            // Given - custom pool config
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = valkeyContainer.host,
                port = valkeyContainer.firstMappedPort,
                password = "test-pass",
                database = 0,
                pool = CacheConfig.PoolConfig(
                    maxTotal = 100,
                    maxIdle = 20,
                    minIdle = 10
                ),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When
            val cache = CacheClientFactory.create(config)

            // Then
            assertIs<DistributedCache>(cache)
            assertTrue(cache.ping())
        }
    }

    @Nested
    inner class TLSConfiguration {

        @Test
        fun `should build correct URI for TLS disabled`() {
            // Given
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = "localhost",
                port = 6379,
                password = "pass",
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When
            val uri = config.buildUri()

            // Then
            assertTrue(uri.startsWith("redis://"))
        }

        @Test
        fun `should build correct URI for TLS enabled`() {
            // Given
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = "master.cache.aws.com",
                port = 6379,
                password = "aws-pass",
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = true)
            )

            // When
            val uri = config.buildUri()

            // Then
            assertTrue(uri.startsWith("rediss://")) // Note the double 's'
        }

        // Note: Testing actual TLS connection requires TLS-enabled Valkey
        // In production, AWS ElastiCache provides TLS endpoints
    }
}
