package com.eros.common.services

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner
import com.amazonaws.services.cloudfront.util.SignerUtils
import com.eros.common.cache.InMemoryUrlCache
import com.eros.common.cache.UrlCache
import com.eros.common.config.S3Config
import org.slf4j.LoggerFactory
import java.io.File
import java.security.PrivateKey
import java.time.Instant
import java.util.Date

/**
 * Service for generating CloudFront signed URLs with time-limited access.
 *
 * Provides access control to S3 resources via CloudFront CDN with expiration.
 * Uses pluggable caching to avoid regenerating identical URLs on repeated requests.
 *
 * Cache implementations can be swapped via dependency injection:
 * - InMemoryUrlCache (default) - Fast, single-instance
 * - RedisUrlCache - Shared cache, multi-instance deployments
 * - Custom implementations via UrlCache interface
 */
class CloudFrontSignerService(
    private val s3Config: S3Config,
    private val urlCache: UrlCache = InMemoryUrlCache()
) {
    private val logger = LoggerFactory.getLogger(CloudFrontSignerService::class.java)

    // Eagerly load private key at construction time (fail-fast principle)
    private val privateKey: PrivateKey

    init {
        require(s3Config.isCloudFrontEnabled()) {
            "CloudFront signed URLs are not enabled. Check configuration: " +
            "cloudFrontKeyPairId, cloudFrontPrivateKeyPath, and cloudFrontDistributionDomain must be set."
        }

        // Load private key immediately - fail fast if configuration is invalid
        privateKey = loadPrivateKey()
    }

    /**
     * Generates a CloudFront signed URL with canned policy (simple expiration).
     *
     * URLs are cached in memory to avoid regenerating identical URLs when users
     * refresh their daily batches. Cache key includes object key and expiry hours.
     *
     * @param objectKey S3 object key (e.g., "photos/user123/abc.jpg")
     * @param expiryHours How long the URL should remain valid (default: 48 hours)
     * @return CloudFront signed URL with expiration parameters (cached or freshly generated)
     * @throws IllegalStateException if CloudFront is not properly configured
     */
    fun generateSignedUrl(objectKey: String, expiryHours: Long = 48): String {
        val cacheKey = "$objectKey:$expiryHours"

        return urlCache.getOrGenerate(cacheKey, expiryHours) {
            val resourceUrl = "${s3Config.getBaseUrl()}/$objectKey"
            val expiresAt = Date.from(Instant.now().plusSeconds(expiryHours * 3600))

            logger.debug("Generating signed URL for: $resourceUrl (expires in ${expiryHours}h)")

            try {
                CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
                    resourceUrl,
                    s3Config.cloudFrontKeyPairId,
                    privateKey,
                    expiresAt
                )
            } catch (e: Exception) {
                logger.error("Failed to generate CloudFront signed URL for: $objectKey", e)
                throw IllegalStateException("Failed to generate signed URL", e)
            }
        }
    }

    /**
     * Generates a CloudFront signed URL with custom policy (advanced options).
     *
     * Allows IP restrictions, custom date ranges, etc.
     *
     * @param objectKey S3 object key
     * @param expiryHours Expiration time in hours
     * @param ipRange Optional IP range restriction (CIDR notation, e.g., "203.0.113.0/24")
     * @return CloudFront signed URL
     */
    fun generateSignedUrlWithCustomPolicy(
        objectKey: String,
        expiryHours: Long = 48,
        ipRange: String? = null
    ): String {
        val resourceUrl = "${s3Config.getBaseUrl()}/$objectKey"
        val expiresAt = Date.from(Instant.now().plusSeconds(expiryHours * 3600))

        logger.debug("Generating custom signed URL for: $resourceUrl")

        return try {
            // Build custom policy JSON
            val policy = buildCustomPolicy(resourceUrl, expiresAt, ipRange)

            CloudFrontUrlSigner.getSignedURLWithCustomPolicy(
                resourceUrl,
                s3Config.cloudFrontKeyPairId,
                privateKey,
                policy
            )
        } catch (e: Exception) {
            logger.error("Failed to generate custom CloudFront signed URL for: $objectKey", e)
            throw IllegalStateException("Failed to generate signed URL", e)
        }
    }

    /**
     * Builds a custom CloudFront policy for URL signing.
     */
    private fun buildCustomPolicy(
        resourceUrl: String,
        expiresAt: Date,
        ipRange: String?
    ): String {
        val epochExpires = expiresAt.time / 1000
        val ipCondition = if (ipRange != null) {
            ""","IpAddress":{"AWS:SourceIp":"$ipRange"}"""
        } else {
            ""
        }

        return """
        {
            "Statement": [{
                "Resource": "$resourceUrl",
                "Condition": {
                    "DateLessThan": {"AWS:EpochTime": $epochExpires}$ipCondition
                }
            }]
        }
        """.trimIndent()
    }

    /**
     * Loads the CloudFront private key from the configured path.
     *
     * Supports both PEM and DER formats.
     */
    private fun loadPrivateKey(): PrivateKey {
        val keyPath = s3Config.cloudFrontPrivateKeyPath
            ?: throw IllegalStateException("CloudFront private key path not configured")

        val keyFile = File(keyPath)
        if (!keyFile.exists()) {
            throw IllegalStateException("CloudFront private key file not found: $keyPath")
        }

        logger.info("Loading CloudFront private key from: $keyPath")

        return try {
            // Read PEM file and convert to PrivateKey
            SignerUtils.loadPrivateKey(keyFile)
        } catch (e: Exception) {
            logger.error("Failed to load CloudFront private key from: $keyPath", e)
            throw IllegalStateException("Failed to load CloudFront private key", e)
        }
    }

    /**
     * Extracts the S3 object key from a full URL.
     *
     * Handles CloudFront URLs, CDN URLs, and S3 direct URLs.
     */
    fun extractObjectKey(url: String): String {
        val baseUrl = s3Config.getBaseUrl().trimEnd('/')
        return when {
            url.startsWith(baseUrl) -> url.removePrefix("$baseUrl/")
            url.startsWith("https://") || url.startsWith("http://") -> {
                // Extract path from URL (everything after domain)
                url.substringAfter("://").substringAfter("/")
            }
            else -> url // Already an object key
        }
    }

    /**
     * Invalidates cached signed URLs for a specific object key.
     *
     * Call this when a user updates or deletes a photo to ensure fresh URLs are generated.
     *
     * @param objectKey S3 object key to invalidate (e.g., "photos/user123/abc.jpg")
     */
    fun invalidateCache(objectKey: String) {
        // Invalidate all expiry variants of this object key
        listOf(24L, 48L, 30 * 24L, 90 * 24L).forEach { expiryHours ->
            urlCache.invalidate("$objectKey:$expiryHours")
        }
        logger.debug("Invalidated cache for: $objectKey")
    }

    /**
     * Invalidates all cached signed URLs for a specific user.
     *
     * Call this when a user updates their profile photos.
     *
     * @param userId The user whose URLs should be invalidated
     */
    fun invalidateUserCache(userId: String) {
        urlCache.invalidateUser(userId)
        logger.debug("Invalidated cache for user: $userId")
    }

    /**
     * Returns cache statistics for monitoring.
     */
    fun getCacheStats(): UrlCache.CacheStats {
        return urlCache.getStats()
    }
}
