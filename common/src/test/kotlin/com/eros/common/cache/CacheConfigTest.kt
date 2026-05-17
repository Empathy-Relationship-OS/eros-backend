package com.eros.common.cache

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.ApplicationConfigurationException
import io.ktor.server.config.MapApplicationConfig
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for CacheConfig.
 *
 * Tests configuration loading from ApplicationConfig with various scenarios:
 * - Valid configurations
 * - Missing required fields
 * - Environment variable defaults
 * - TLS configuration
 * - Pool configuration
 * - URI building
 */
class CacheConfigTest {

    @Nested
    inner class FromApplicationConfig {

        @Test
        fun `should load complete configuration`() {
            // Given
            val config = createMockConfig(
                mapOf(
                    "cache.enabled" to "true",
                    "cache.backend" to "valkey",
                    "cache.host" to "localhost",
                    "cache.port" to "6379",
                    "cache.password" to "test-password",
                    "cache.database" to "0",
                    "cache.tls.enabled" to "false",
                    "cache.tls.verifyPeer" to "true",
                    "cache.pool.maxTotal" to "50",
                    "cache.pool.maxIdle" to "10",
                    "cache.pool.minIdle" to "5",
                    "cache.timeoutMs" to "2000"
                )
            )

            // When
            val result = CacheConfig.fromApplicationConfig(config)

            // Then
            assertTrue(result.enabled)
            assertEquals(CacheBackend.VALKEY, result.backend)
            assertEquals("localhost", result.host)
            assertEquals(6379, result.port)
            assertEquals("test-password", result.password)
            assertEquals(0, result.database)
            assertFalse(result.tls.enabled)
            assertTrue(result.tls.verifyPeer)
            assertEquals(50, result.pool.maxTotal)
            assertEquals(10, result.pool.maxIdle)
            assertEquals(5, result.pool.minIdle)
            assertEquals(2000L, result.timeoutMs)
        }

        @Test
        fun `should handle disabled cache`() {
            // Given
            val config = createMockConfig(
                mapOf(
                    "cache.enabled" to "false",
                    "cache.backend" to "in-memory",
                    "cache.host" to "localhost",
                    "cache.port" to "6379",
                    "cache.database" to "0",
                    "cache.pool.maxTotal" to "50",
                    "cache.pool.maxIdle" to "10",
                    "cache.pool.minIdle" to "5",
                    "cache.timeoutMs" to "2000"
                )
            )

            // When
            val result = CacheConfig.fromApplicationConfig(config)

            // Then
            assertFalse(result.enabled)
            assertEquals(CacheBackend.IN_MEMORY, result.backend)
        }

        @Test
        fun `should handle empty password as null`() {
            // Given
            val config = createMockConfig(
                mapOf(
                    "cache.enabled" to "true",
                    "cache.backend" to "valkey",
                    "cache.host" to "localhost",
                    "cache.port" to "6379",
                    "cache.password" to "",  // Empty string
                    "cache.database" to "0",
                    "cache.pool.maxTotal" to "50",
                    "cache.pool.maxIdle" to "10",
                    "cache.pool.minIdle" to "5",
                    "cache.timeoutMs" to "2000"
                )
            )

            // When
            val result = CacheConfig.fromApplicationConfig(config)

            // Then
            assertNull(result.password)
        }

        @Test
        fun `should handle null password`() {
            // Given
            val config = createMockConfig(
                mapOf(
                    "cache.enabled" to "true",
                    "cache.backend" to "valkey",
                    "cache.host" to "localhost",
                    "cache.port" to "6379",
                    // password not provided
                    "cache.database" to "0",
                    "cache.pool.maxTotal" to "50",
                    "cache.pool.maxIdle" to "10",
                    "cache.pool.minIdle" to "5",
                    "cache.timeoutMs" to "2000"
                )
            )

            // When
            val result = CacheConfig.fromApplicationConfig(config)

            // Then
            assertNull(result.password)
        }

        @Test
        fun `should default TLS settings when not provided`() {
            // Given
            val config = createMockConfig(
                mapOf(
                    "cache.enabled" to "true",
                    "cache.backend" to "valkey",
                    "cache.host" to "localhost",
                    "cache.port" to "6379",
                    "cache.database" to "0",
                    // TLS settings not provided
                    "cache.pool.maxTotal" to "50",
                    "cache.pool.maxIdle" to "10",
                    "cache.pool.minIdle" to "5",
                    "cache.timeoutMs" to "2000"
                )
            )

            // When
            val result = CacheConfig.fromApplicationConfig(config)

            // Then
            assertFalse(result.tls.enabled) // Default false
            assertTrue(result.tls.verifyPeer) // Default true
        }

        @Test
        fun `should support all backend types`() {
            // Test each backend
            val backends = mapOf(
                "valkey" to CacheBackend.VALKEY,
                "redis" to CacheBackend.REDIS,
                "in-memory" to CacheBackend.IN_MEMORY
            )

            backends.forEach { (backendName, expectedEnum) ->
                // Given
                val config = createMockConfig(
                    mapOf(
                        "cache.enabled" to "true",
                        "cache.backend" to backendName,
                        "cache.host" to "localhost",
                        "cache.port" to "6379",
                        "cache.database" to "0",
                        "cache.pool.maxTotal" to "50",
                        "cache.pool.maxIdle" to "10",
                        "cache.pool.minIdle" to "5",
                        "cache.timeoutMs" to "2000"
                    )
                )

                // When
                val result = CacheConfig.fromApplicationConfig(config)

                // Then
                assertEquals(expectedEnum, result.backend)
            }
        }

        @Test
        fun `should throw exception for invalid backend`() {
            // Given
            val config = createMockConfig(
                mapOf(
                    "cache.enabled" to "true",
                    "cache.backend" to "invalid-backend",
                    "cache.host" to "localhost",
                    "cache.port" to "6379",
                    "cache.database" to "0",
                    "cache.pool.maxTotal" to "50",
                    "cache.pool.maxIdle" to "10",
                    "cache.pool.minIdle" to "5",
                    "cache.timeoutMs" to "2000"
                )
            )

            // When/Then
            assertThrows<IllegalArgumentException> {
                CacheConfig.fromApplicationConfig(config)
            }
        }

        @Test
        fun `should throw exception for missing required field`() {
            // Given - missing host
            val config = createMockConfig(
                mapOf(
                    "cache.enabled" to "true",
                    "cache.backend" to "valkey",
                    // host missing
                    "cache.port" to "6379",
                    "cache.database" to "0",
                    "cache.pool.maxTotal" to "50",
                    "cache.pool.maxIdle" to "10",
                    "cache.pool.minIdle" to "5",
                    "cache.timeoutMs" to "2000"
                )
            )

            // When/Then
            val exception = assertThrows<ApplicationConfigurationException> {
                CacheConfig.fromApplicationConfig(config)
            }
            assertEquals(exception.message?.contains("cache.host"), true)
        }
    }

    @Nested
    inner class URIBuilding {

        @Test
        fun `should build URI without password`() {
            // Given
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = "localhost",
                port = 6379,
                password = null,
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When
            val uri = config.buildUri()

            // Then
            assertEquals("redis://localhost:6379/0", uri)
        }

        @Test
        fun `should build URI with password`() {
            // Given
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = "localhost",
                port = 6379,
                password = "my-password",
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When
            val uri = config.buildUri()

            // Then
            assertEquals("redis://:my-password@localhost:6379/0", uri)
        }

        @Test
        fun `should build URI with TLS enabled`() {
            // Given
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = "master.cache.amazonaws.com",
                port = 6379,
                password = "aws-password",
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = true)
            )

            // When
            val uri = config.buildUri()

            // Then
            assertEquals("rediss://:aws-password@master.cache.amazonaws.com:6379/0", uri)
        }

        @Test
        fun `should build URI with different database number`() {
            // Given
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = "localhost",
                port = 6379,
                password = null,
                database = 5,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When
            val uri = config.buildUri()

            // Then
            assertEquals("redis://localhost:6379/5", uri)
        }

        @Test
        fun `should build URI with custom port`() {
            // Given
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = "localhost",
                port = 6380,
                password = null,
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When
            val uri = config.buildUri()

            // Then
            assertEquals("redis://localhost:6380/0", uri)
        }

        @Test
        fun `should percent-encode special characters in password`() {
            // Given
            val config = CacheConfig(
                enabled = true,
                backend = CacheBackend.VALKEY,
                host = "localhost",
                port = 6379,
                password = "p@ssw0rd:sp3c!al#chars",
                database = 0,
                pool = CacheConfig.PoolConfig(50, 10, 5),
                timeoutMs = 2000,
                tls = CacheConfig.TlsConfig(enabled = false)
            )

            // When
            val uri = config.buildUri()

            // Then
            assertEquals("redis://:p%40ssw0rd%3Asp3c%21al%23chars@localhost:6379/0", uri)
        }
    }

    @Nested
    inner class CacheBackendEnum {

        @Test
        fun `should identify distributed backends`() {
            assertTrue(CacheBackend.VALKEY.isDistributed)
            assertTrue(CacheBackend.REDIS.isDistributed)
            assertFalse(CacheBackend.IN_MEMORY.isDistributed)
        }

        @Test
        fun `should provide display names`() {
            assertEquals("Valkey", CacheBackend.VALKEY.displayName)
            assertEquals("Redis", CacheBackend.REDIS.displayName)
            assertEquals("In-memory", CacheBackend.IN_MEMORY.displayName)
        }
    }

    // Helper function to create mock ApplicationConfig
    private fun createMockConfig(properties: Map<String, String>): ApplicationConfig {
        return MapApplicationConfig(*properties.map { (key, value) -> key to value }.toTypedArray())
    }
}
