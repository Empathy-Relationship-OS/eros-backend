package com.eros.common.config

import io.ktor.server.config.*

// TODO add more buckets than this need raw data bucket and processed bucket for final images
/**
 * AWS S3 configuration loaded from application.yaml.
 *
 * Environment variables (with defaults):
 * - AWS_REGION                  — AWS region (default: eu-west-2)
 * - AWS_ACCESS_KEY_ID           — IAM access key (blank = use default credentials chain)
 * - AWS_SECRET_ACCESS_KEY       — IAM secret key (blank = use default credentials chain)
 * - AWS_S3_BUCKET_NAME          — S3 bucket name (default: eros-photos)
 * - AWS_CDN_BASE_URL            — CloudFront base URL (optional; blank = return S3 URLs)
 * - AWS_PRESIGNED_URL_TTL_MINUTES — Presigned URL expiry in minutes (default: 15)
 * - CLOUDFRONT_DISTRIBUTION_DOMAIN — CloudFront distribution domain (e.g., d123.cloudfront.net)
 * - CLOUDFRONT_KEY_PAIR_ID      — CloudFront key pair ID (required for signed URLs)
 * - CLOUDFRONT_PRIVATE_KEY_PATH — Path to CloudFront private key PEM file
 */
data class S3Config(
    val region: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val bucketName: String,
    val cdnBaseUrl: String?,
    val presignedUrlTtlMinutes: Long,

    // CloudFront signed URL configuration
    val cloudFrontKeyPairId: String?,
    val cloudFrontPrivateKeyPath: String?,
    val cloudFrontDistributionDomain: String?
) {
    override fun toString(): String =
        "S3Config(region=$region, bucketName=$bucketName, " +
        "accessKeyId=REDACTED, secretAccessKey=REDACTED, " +
        "cdnBaseUrl=${if (cdnBaseUrl.isNullOrBlank()) "null" else "REDACTED"}, " +
        "presignedUrlTtlMinutes=$presignedUrlTtlMinutes, " +
        "cloudFrontKeyPairId=${if (cloudFrontKeyPairId.isNullOrBlank()) "null" else "REDACTED"}, " +
        "cloudFrontPrivateKeyPath=${if (cloudFrontPrivateKeyPath.isNullOrBlank()) "null" else "REDACTED"}, " +
        "cloudFrontDistributionDomain=${cloudFrontDistributionDomain ?: "null"}, " +
        "cloudFrontEnabled=${isCloudFrontEnabled()})"

    /**
     * Check if CloudFront signed URLs are properly configured.
     */
    fun isCloudFrontEnabled(): Boolean {
        return !cloudFrontKeyPairId.isNullOrBlank() &&
               !cloudFrontPrivateKeyPath.isNullOrBlank() &&
               !cloudFrontDistributionDomain.isNullOrBlank()
    }

    /**
     * Returns the base URL for CloudFront distribution.
     * Falls back to cdnBaseUrl or S3 direct URL if CloudFront not configured.
     */
    fun getBaseUrl(): String {
        return when {
            isCloudFrontEnabled() -> {
                require(!cloudFrontDistributionDomain!!.startsWith("/")) {
                    "CloudFront distribution domain must not start with '/'. Got: $cloudFrontDistributionDomain"
                }
                "https://$cloudFrontDistributionDomain"
            }
            !cdnBaseUrl.isNullOrBlank() ->
                cdnBaseUrl.trimEnd('/')
            else ->
                "https://$bucketName.s3.$region.amazonaws.com"
        }
    }

    /**
     * Returns the public URL for a given S3 object key.
     *
     * **Note:** If CloudFront is enabled, this returns an UNSIGNED URL.
     * Use PhotoService.generateAccessUrl() for access-controlled URLs.
     */
    fun publicUrlFor(key: String): String {
        return "${getBaseUrl()}/$key"
    }

    companion object {
        fun fromApplicationConfig(config: ApplicationConfig): S3Config {
            val cdnBaseUrl = config.propertyOrNull("aws.cdnBaseUrl")?.getString()
            return S3Config(
                region                = config.property("aws.region").getString(),
                accessKeyId           = config.property("aws.accessKeyId").getString(),
                secretAccessKey       = config.property("aws.secretAccessKey").getString(),
                bucketName            = config.property("aws.s3BucketName").getString(),
                cdnBaseUrl            = cdnBaseUrl?.ifBlank { null },
                presignedUrlTtlMinutes = config.property("aws.presignedUrlTtlMinutes").getString()
                    .toLongOrNull()
                    ?: error("aws.presignedUrlTtlMinutes must be a valid integer (got: ${config.property("aws.presignedUrlTtlMinutes").getString()})"),

                // CloudFront configuration (optional)
                cloudFrontKeyPairId = config.propertyOrNull("aws.cloudFrontKeyPairId")
                    ?.getString()?.ifBlank { null },
                cloudFrontPrivateKeyPath = config.propertyOrNull("aws.cloudFrontPrivateKeyPath")
                    ?.getString()?.ifBlank { null },
                cloudFrontDistributionDomain = config.propertyOrNull("aws.cloudFrontDistributionDomain")
                    ?.getString()?.ifBlank { null }
            )
        }
    }
}
