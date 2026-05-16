package com.eros

import com.eros.common.cache.Cache
import com.eros.common.cache.CacheClientFactory
import com.eros.common.cache.CacheConfig
import com.eros.common.cache.DistributedCache
import com.eros.common.cache.InMemoryCache
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import kotlinx.serialization.Serializable


/**
 * Configures the cache subsystem for the application.
 *
 * Follows the same pattern as configureDatabase():
 * - Loads configuration from application.yaml
 * - Creates cache client based on backend type
 * - Stores cache in application attributes
 * - Registers shutdown hooks
 * - Adds health check endpoint
 *
 * The cache is stored in application attributes and can be accessed by other modules:
 * ```kotlin
 * val cache = application.attributes[CacheKey]
 * ```
 */
fun Application.configureCache() {
    val config = try {
        CacheConfig.fromApplicationConfig(environment.config)
    } catch (e: Exception) {
        log.error("Failed to load cache configuration", e)
        throw e
    }

    if (!config.enabled) {
        log.info("Cache is disabled (cache.enabled=false)")
        // Store in-memory cache even when disabled for consistent API
        attributes.put(CacheKey, InMemoryCache())
        return
    }

    log.info("Initializing cache with backend: ${config.backend.displayName}")

    // Create cache client (with automatic fallback to in-memory on failure)
    val cache = CacheClientFactory.create(config)

    // Store cache in application attributes for access by other modules
    attributes.put(CacheKey, cache)

    // Register shutdown hook for distributed caches
    monitor.subscribe(ApplicationStopped) {
        if (cache is DistributedCache) {
            log.info("Shutting down distributed cache...")
            cache.shutdown()
        }
    }

    // Add health check endpoint
    routing {
        get("/health/cache") {
            val healthy = cache.ping()
            val stats = cache.getStats()

            if (healthy) {
                call.respond(
                    HttpStatusCode.OK,
                    CacheHealthResponse(
                        status = "healthy",
                        backend = stats.backend,
                        connected = stats.connected,
                        keyCount = stats.keyCount,
                        memoryUsedBytes = stats.memoryUsedBytes
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    CacheHealthResponse(
                        status = "unhealthy",
                        backend = stats.backend,
                        connected = stats.connected,
                        keyCount = null,
                        memoryUsedBytes = null
                    )
                )
            }
        }
    }

    log.info("Cache initialized successfully (backend: ${config.backend.displayName})")
}

/**
 * Attribute key for accessing the cache from application attributes.
 *
 * Usage:
 * ```kotlin
 * val cache = application.attributes[CacheKey]
 * ```
 */
val CacheKey = AttributeKey<Cache>("Cache")

/**
 * Cache health check response.
 */
@Serializable
data class CacheHealthResponse(
    val status: String,
    val backend: String,
    val connected: Boolean,
    val keyCount: Long? = null,
    val memoryUsedBytes: Long? = null
)
