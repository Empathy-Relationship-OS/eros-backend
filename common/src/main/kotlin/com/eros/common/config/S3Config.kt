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
 */
data class S3Config(
    val region: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val bucketName: String,
    val cdnBaseUrl: String?,
    val presignedUrlTtlMinutes: Long
) {
    /**
     * Returns the public URL for a given S3 object key.
     * Uses CDN base URL if configured, otherwise falls back to a direct S3 URL.
     */
    fun publicUrlFor(key: String): String {
        val base = cdnBaseUrl?.trimEnd('/')
        return if (!base.isNullOrBlank()) {
            "$base/$key"
        } else {
            "https://$bucketName.s3.$region.amazonaws.com/$key"
        }
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
                presignedUrlTtlMinutes = config.property("aws.presignedUrlTtlMinutes").getString().toLong()
            )
        }
    }
}
