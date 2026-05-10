# CloudFront Signed URLs Implementation Guide

## Overview

This document outlines the implementation of **CloudFront with Origin Access Control (OAC)** and **Signed URLs** for secure, time-limited access to user profile photos in the Eros dating platform.

### Solution Architecture

```
┌─────────────┐      ┌──────────────────┐      ┌─────────────┐      ┌─────────────┐
│   Client    │─────▶│  Ktor Backend    │─────▶│ CloudFront  │─────▶│  S3 Bucket  │
│ (iOS/Web)   │      │ (Signed URL Gen) │      │   (CDN)     │      │  (Private)  │
└─────────────┘      └──────────────────┘      └─────────────┘      └─────────────┘
                              │                        ▲
                              │                        │
                              └────────────────────────┘
                          CloudFront Private Key
                          (Signs URLs with expiry)
```

**Key Benefits:**
- ✅ **Security**: S3 bucket remains private, only CloudFront has access via OAC
- ✅ **Performance**: Global CDN with edge caching for fast image delivery
- ✅ **Access Control**: Time-limited URLs per user with different expiry policies
- ✅ **Privacy**: URLs expire automatically, preventing permanent sharing
- ✅ **Scalability**: Handles millions of requests with automatic scaling
- ✅ **Cost-Effective**: CloudFront cheaper than S3 direct access at scale

---

## Part 1: AWS Infrastructure Setup

### Step 1: Create CloudFront Key Pair

CloudFront signed URLs require a trusted key pair. You need to create this using the **root AWS account**.

#### 1.1 Generate RSA Key Pair Locally

```bash
# Generate 2048-bit RSA private key
openssl genrsa -out cloudfront-private-key.pem 2048

# Extract public key
openssl rsa -pubout -in cloudfront-private-key.pem -out cloudfront-public-key.pem

# Verify the keys
openssl rsa -text -in cloudfront-private-key.pem -noout
```

**Security Note:**
- Store `cloudfront-private-key.pem` securely (AWS Secrets Manager, encrypted environment variable)
- Never commit private key to version control
- Add to `.gitignore`: `*.pem`, `cloudfront-private-key.*`

#### 1.2 Upload Public Key to AWS

1. **Log in to AWS Console as root user** (CloudFront key pairs require root access)
2. Navigate to: **Account Menu → Security Credentials → CloudFront key pairs**
3. Click **Create new key pair**
4. Upload `cloudfront-public-key.pem`
5. AWS will provide a **Key Pair ID** (format: `APKAXXXXXXXXXXXXXXXX`)
6. **Save this Key Pair ID** - you'll need it for configuration

**Alternative (AWS CLI):**
```bash
# Note: This requires root credentials
aws cloudfront create-public-key --public-key-config \
  Name=eros-cloudfront-key,\
  CallerReference=$(date +%s),\
  EncodedKey=$(cat cloudfront-public-key.pem)
```

---

### Step 2: Create CloudFront Distribution

#### 2.1 Via AWS Console

1. **Navigate to CloudFront** → Distributions → Create Distribution

2. **Origin Settings:**
   - **Origin domain**: Select your S3 bucket (`eros-photos.s3.eu-west-2.amazonaws.com`)
   - **Origin path**: Leave blank
   - **Name**: `S3-eros-photos`
   - **Origin access**: **Origin access control settings (recommended)**
   - **Origin access control**: Create new OAC
     - **Name**: `eros-photos-oac`
     - **Signing behavior**: Sign requests (recommended)
     - **Origin type**: S3

3. **Default Cache Behavior:**
   - **Viewer protocol policy**: Redirect HTTP to HTTPS
   - **Allowed HTTP methods**: GET, HEAD, OPTIONS
   - **Cache policy**: CachingOptimized (recommended)
   - **Origin request policy**: None
   - **Response headers policy**: CORS-with-preflight (if needed)

4. **Distribution Settings:**
   - **Price class**: Use all edge locations (best performance) OR Use only North America and Europe (cost savings)
   - **Alternate domain names (CNAMEs)**: Optional - add custom domain like `cdn.eros.app`
   - **Custom SSL certificate**: If using custom domain, request ACM certificate
   - **Default root object**: Leave blank (not needed for API-driven image delivery)

5. **Click Create Distribution**

6. **Important:** Copy the **Policy statement** that CloudFront provides. It looks like:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": {
       "Sid": "AllowCloudFrontServicePrincipalReadOnly",
       "Effect": "Allow",
       "Principal": {
         "Service": "cloudfront.amazonaws.com"
       },
       "Action": "s3:GetObject",
       "Resource": "arn:aws:s3:::eros-photos/*",
       "Condition": {
         "StringEquals": {
           "AWS:SourceArn": "arn:aws:cloudfront::123456789012:distribution/E1ABCDEFGHIJK"
         }
       }
     }
   }
   ```

7. **Save the CloudFront Distribution ID** (format: `E1ABCDEFGHIJK`) and **Distribution Domain** (format: `d1234567890.cloudfront.net`)

#### 2.2 Via Terraform (Alternative)

```hcl
# terraform/cloudfront.tf

# CloudFront Origin Access Control
resource "aws_cloudfront_origin_access_control" "eros_photos" {
  name                              = "eros-photos-oac"
  description                       = "OAC for Eros photos S3 bucket"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# CloudFront Distribution
resource "aws_cloudfront_distribution" "eros_cdn" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "Eros Photos CDN"
  price_class         = "PriceClass_100" # US, Canada, Europe

  origin {
    domain_name              = aws_s3_bucket.eros_photos.bucket_regional_domain_name
    origin_id                = "S3-eros-photos"
    origin_access_control_id = aws_cloudfront_origin_access_control.eros_photos.id
  }

  default_cache_behavior {
    target_origin_id       = "S3-eros-photos"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true

    cache_policy_id = "658327ea-f89d-4fab-a63d-7e88639e58f6" # CachingOptimized

    # For CORS support
    response_headers_policy_id = "60669652-455b-4ae9-85a7-c5c1f2c8e776" # CORS-with-preflight
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
    # For custom domain:
    # acm_certificate_arn      = aws_acm_certificate.cdn.arn
    # ssl_support_method       = "sni-only"
    # minimum_protocol_version = "TLSv1.2_2021"
  }

  # Optional: Custom domain
  # aliases = ["cdn.eros.app"]

  tags = {
    Environment = "production"
    Project     = "eros"
  }
}

output "cloudfront_distribution_id" {
  value = aws_cloudfront_distribution.eros_cdn.id
}

output "cloudfront_domain_name" {
  value = aws_cloudfront_distribution.eros_cdn.domain_name
}
```

---

### Step 3: Update S3 Bucket Policy

The S3 bucket must allow CloudFront OAC to access objects.

#### 3.1 Via AWS Console

1. Navigate to **S3** → Select `eros-photos` bucket → **Permissions** → **Bucket Policy**
2. Replace or add the policy CloudFront provided (from Step 2.1, item 6):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowCloudFrontServicePrincipalReadOnly",
      "Effect": "Allow",
      "Principal": {
        "Service": "cloudfront.amazonaws.com"
      },
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::eros-photos/*",
      "Condition": {
        "StringEquals": {
          "AWS:SourceArn": "arn:aws:cloudfront::YOUR_ACCOUNT_ID:distribution/YOUR_DISTRIBUTION_ID"
        }
      }
    }
  ]
}
```

**Replace:**
- `YOUR_ACCOUNT_ID`: Your 12-digit AWS account ID
- `YOUR_DISTRIBUTION_ID`: CloudFront distribution ID from Step 2

3. Click **Save changes**

#### 3.2 Verify Bucket is Private

Ensure the bucket **blocks all public access**:
- Navigate to **S3** → `eros-photos` → **Permissions** → **Block public access**
- Ensure all 4 checkboxes are **enabled**:
  - ✅ Block all public access
  - ✅ Block public access to buckets and objects granted through new access control lists (ACLs)
  - ✅ Block public access to buckets and objects granted through any access control lists (ACLs)
  - ✅ Block public access to buckets and objects granted through new public bucket or access point policies
  - ✅ Block public and cross-account access to buckets and objects through any public bucket or access point policies

---

### Step 4: Test CloudFront Distribution

#### 4.1 Wait for Deployment

CloudFront distributions take **10-15 minutes** to deploy globally. Check status:
- **Console**: CloudFront → Distributions → Status should be "Enabled"
- **CLI**: `aws cloudfront get-distribution --id YOUR_DISTRIBUTION_ID`

#### 4.2 Test Public Access (Should Fail)

```bash
# This should return 403 Forbidden (bucket is private)
curl -I https://eros-photos.s3.eu-west-2.amazonaws.com/photos/test/image.jpg

# This should return 403 MissingKey (no signed URL)
curl -I https://d1234567890.cloudfront.net/photos/test/image.jpg
```

#### 4.3 Upload Test Image

```bash
# Upload a test image to S3
aws s3 cp test-image.jpg s3://eros-photos/photos/test/image.jpg \
  --content-type image/jpeg
```

---

## Part 2: Backend Integration

### Step 1: Add Dependencies

Update `build.gradle.kts` to include CloudFront SDK:

```kotlin
// common/build.gradle.kts or app/build.gradle.kts
dependencies {
    // Existing S3 dependencies
    implementation("software.amazon.awssdk:s3:2.20.26")
    implementation("software.amazon.awssdk:s3-transfer-manager:2.20.26")

    // Add CloudFront SDK for signed URLs
    implementation("software.amazon.awssdk:cloudfront:2.20.26")

    // For URL signing (utilities)
    implementation("com.amazonaws:aws-java-sdk-cloudfront:1.12.529")
}
```

**Note:** AWS SDK v2 doesn't have built-in signed URL utilities, so we use v1 SDK for `CloudFrontUrlSigner` utility class.

---

### Step 2: Update S3Config

Add CloudFront configuration fields:

```kotlin
// common/src/main/kotlin/com/eros/common/config/S3Config.kt

package com.eros.common.config

import io.ktor.server.config.*

data class S3Config(
    val region: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val bucketName: String,
    val cdnBaseUrl: String?,
    val presignedUrlTtlMinutes: Long,

    // New fields for CloudFront signed URLs
    val cloudFrontKeyPairId: String?,
    val cloudFrontPrivateKeyPath: String?,
    val cloudFrontDistributionDomain: String?
) {
    override fun toString(): String =
        "S3Config(region=$region, bucketName=$bucketName, cdnBaseUrl=$cdnBaseUrl, " +
        "presignedUrlTtlMinutes=$presignedUrlTtlMinutes, cloudFrontEnabled=${isCloudFrontEnabled()})"

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
            !cloudFrontDistributionDomain.isNullOrBlank() ->
                "https://${cloudFrontDistributionDomain.trimStart('/')}"
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
     * Use PhotoService.generateSignedUrl() for access-controlled URLs.
     */
    fun publicUrlFor(key: String): String {
        return "${getBaseUrl()}/$key"
    }

    companion object {
        fun fromApplicationConfig(config: ApplicationConfig): S3Config {
            return S3Config(
                region = config.property("aws.region").getString(),
                accessKeyId = config.property("aws.accessKeyId").getString(),
                secretAccessKey = config.property("aws.secretAccessKey").getString(),
                bucketName = config.property("aws.s3BucketName").getString(),
                cdnBaseUrl = config.propertyOrNull("aws.cdnBaseUrl")?.getString()?.ifBlank { null },
                presignedUrlTtlMinutes = config.property("aws.presignedUrlTtlMinutes")
                    .getString().toLongOrNull()
                    ?: error("aws.presignedUrlTtlMinutes must be a valid integer"),

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
```

---

### Step 3: Create CloudFront Signer Service

Create a new service to handle CloudFront signed URL generation:

```kotlin
// common/src/main/kotlin/com/eros/common/services/CloudFrontSignerService.kt

package com.eros.common.services

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner
import com.amazonaws.services.cloudfront.util.SignerUtils
import com.eros.common.config.S3Config
import org.slf4j.LoggerFactory
import java.io.File
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Date

/**
 * Service for generating CloudFront signed URLs with time-limited access.
 *
 * Provides access control to S3 resources via CloudFront CDN with expiration.
 */
class CloudFrontSignerService(
    private val s3Config: S3Config
) {
    private val logger = LoggerFactory.getLogger(CloudFrontSignerService::class.java)

    // Lazy-load private key (only load once on first use)
    private val privateKey: PrivateKey by lazy {
        loadPrivateKey()
    }

    /**
     * Generates a CloudFront signed URL with canned policy (simple expiration).
     *
     * @param objectKey S3 object key (e.g., "photos/user123/abc.jpg")
     * @param expiryHours How long the URL should remain valid (default: 48 hours)
     * @return CloudFront signed URL with expiration parameters
     * @throws IllegalStateException if CloudFront is not properly configured
     */
    fun generateSignedUrl(objectKey: String, expiryHours: Long = 48): String {
        require(s3Config.isCloudFrontEnabled()) {
            "CloudFront signed URLs are not enabled. Check configuration."
        }

        val resourceUrl = "${s3Config.getBaseUrl()}/$objectKey"
        val expiresAt = Date.from(Instant.now().plusSeconds(expiryHours * 3600))

        logger.debug("Generating signed URL for: $resourceUrl (expires in ${expiryHours}h)")

        return try {
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
        require(s3Config.isCloudFrontEnabled()) {
            "CloudFront signed URLs are not enabled. Check configuration."
        }

        val resourceUrl = "${s3Config.getBaseUrl()}/$objectKey"
        val expiresAt = Date.from(Instant.now().plusSeconds(expiryHours * 3600))

        logger.debug("Generating custom signed URL for: $resourceUrl")

        return try {
            CloudFrontUrlSigner.getSignedURLWithCustomPolicy(
                resourceUrl,
                s3Config.cloudFrontKeyPairId,
                privateKey,
                expiresAt,
                null, // activeFrom (null = active immediately)
                ipRange
            )
        } catch (e: Exception) {
            logger.error("Failed to generate custom CloudFront signed URL for: $objectKey", e)
            throw IllegalStateException("Failed to generate signed URL", e)
        }
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
        require(keyFile.exists()) {
            "CloudFront private key file not found: $keyPath"
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
}
```

---

### Step 4: Update PhotoService

Integrate CloudFront signed URL generation into `PhotoService`:

```kotlin
// users/src/main/kotlin/com/eros/users/service/PhotoService.kt

import com.eros.common.services.CloudFrontSignerService

class PhotoService(
    private val photoRepository: PhotoRepository,
    private val s3Config: S3Config,
    private val s3Client: S3Client = buildS3Client(s3Config),
    private val s3Presigner: S3Presigner = buildS3Presigner(s3Config),
    private val cloudFrontSigner: CloudFrontSignerService? = if (s3Config.isCloudFrontEnabled()) {
        CloudFrontSignerService(s3Config)
    } else null
) {
    // ... existing code ...

    /**
     * Generates a time-limited access URL for an image.
     *
     * Prefers CloudFront signed URLs if available, falls back to S3 presigned URLs.
     *
     * @param mediaUrl The stored media URL (from database)
     * @param expiryHours How long the URL should be valid
     * @return Time-limited access URL
     */
    suspend fun generateAccessUrl(mediaUrl: String, expiryHours: Long = 48): String {
        return if (cloudFrontSigner != null && s3Config.isCloudFrontEnabled()) {
            // Use CloudFront signed URL (preferred)
            val objectKey = cloudFrontSigner.extractObjectKey(mediaUrl)
            cloudFrontSigner.generateSignedUrl(objectKey, expiryHours)
        } else {
            // Fall back to S3 presigned URL
            val objectKey = urlToObjectKey(mediaUrl)
            generateS3PresignedUrl(objectKey, expiryHours)
        }
    }

    /**
     * Generates an S3 presigned GET URL (fallback when CloudFront not configured).
     *
     * @param objectKey S3 object key
     * @param expiryHours Expiration time in hours
     * @return S3 presigned GET URL
     */
    private fun generateS3PresignedUrl(objectKey: String, expiryHours: Long): String {
        val getObjectRequest = software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
            .bucket(s3Config.bucketName)
            .key(objectKey)
            .build()

        val presignRequest = software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
            .signatureDuration(java.time.Duration.ofHours(expiryHours))
            .getObjectRequest(getObjectRequest)
            .build()

        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }

    // ... rest of existing code ...
}
```

---

### Step 5: Update UserService

Modify `getUserMatchProfileData()` to generate signed URLs with appropriate expiry:

```kotlin
// users/src/main/kotlin/com/eros/users/service/UserService.kt

/**
 * Retrieves lightweight profile data for matching, with time-limited photo access.
 *
 * @param userId The user whose profile to retrieve
 * @param photoExpiryHours How long the photo URL should be valid (default: 48h for daily batches)
 * @return UserMatchProfileData containing basic profile info, or null if user not found
 */
suspend fun getUserMatchProfileData(
    userId: String,
    photoExpiryHours: Long = 48 // Default: 48 hours for daily batch matches
): UserMatchProfileData? {
    val user = dbQuery { userRepository.findById(userId) } ?: return null
    val photos = photoService.getUserMedia(userId).media
    val primaryPhoto = photos.firstOrNull { it.isPrimary }

    // Generate time-limited access URL for thumbnail
    val thumbnailUrl = primaryPhoto?.let { photo ->
        photoService.generateAccessUrl(
            mediaUrl = photo.mediaUrl,
            expiryHours = photoExpiryHours
        )
    }

    return UserMatchProfileData(
        userId = user.userId,
        name = user.firstName,
        age = user.getAge(),
        thumbnailUrl = thumbnailUrl, // CloudFront signed URL with expiration
        badges = user.badges?.map { it.name }?.toSet()
    )
}
```

---

### Step 6: Update MatchService

Use different expiry times based on match context:

```kotlin
// matching/src/main/kotlin/com/eros/matching/service/MatchService.kt

/**
 * Builds a lightweight UserMatchProfile from a match and user data.
 *
 * @param match The match record
 * @param servedAt The timestamp when the match was served
 * @param photoExpiryHours How long the photo URLs should be valid (default: 48h)
 * @return UserMatchProfile or null if user not found
 */
private suspend fun buildUserMatchProfile(
    match: Match,
    servedAt: Instant,
    photoExpiryHours: Long = 48 // Default: 48 hours for unmatched profiles
): UserMatchProfile? {
    val userData = userService.getUserMatchProfileData(
        userId = match.user2Id,
        photoExpiryHours = photoExpiryHours
    ) ?: return null

    return UserMatchProfile(
        matchId = match.matchId,
        userId = userData.userId,
        name = userData.name,
        age = userData.age,
        thumbnailUrl = userData.thumbnailUrl,
        badges = userData.badges,
        servedAt = servedAt
    )
}

/**
 * Fetches the next batch of daily matches for a user.
 *
 * Photos in daily batches have 48-hour access (enough time for user to decide).
 */
suspend fun fetchDailyBatch(userId: String): DailyBatchResponse = transactionManager.execute {
    // ... existing code ...

    // Build lightweight profile responses with 48-hour photo access
    val servedAt = Instant.now(clock)
    val successfulResults = unservedMatches.mapNotNull { match ->
        buildUserMatchProfile(
            match = match,
            servedAt = servedAt,
            photoExpiryHours = 48 // Daily batch: 48-hour expiry
        )?.let { profile ->
            match.matchId to profile
        }
    }

    // ... rest of existing code ...
}

/**
 * Fetches all profiles that the user passed on in the last 24 hours.
 *
 * Photos have 24-hour access (shorter since user already saw them).
 */
suspend fun getPassesInLast24Hours(userId: String): List<UserMatchProfile> = transactionManager.execute {
    val passedMatches = matchRepository.findPassesInLast24Hours(userId)

    // Build lightweight profile responses with 24-hour photo access
    passedMatches.mapNotNull { match ->
        val servedAt = match.servedAt ?: return@mapNotNull null
        buildUserMatchProfile(
            match = match,
            servedAt = servedAt,
            photoExpiryHours = 24 // Reconsideration: 24-hour expiry
        )
    }
}
```

---

### Step 7: Add Mutual Match Extended Access

For mutual matches, users should have extended access to view each other's profiles:

```kotlin
// dates/src/main/kotlin/com/eros/dates/service/DateService.kt (future module)

/**
 * Gets the profile of a mutual match with extended photo access.
 *
 * Once users are matched, they get 30-day access to view each other's profiles.
 */
suspend fun getMutualMatchProfile(userId: String, matchedUserId: String): PublicProfile? {
    // Verify mutual match exists
    val isMutual = matchService.isMutualMatch(userId, matchedUserId)
    if (!isMutual) return null

    // Get full profile with extended photo access
    val user = userService.getUser(matchedUserId) ?: return null
    val photos = photoService.getUserMedia(matchedUserId).media

    // Generate 30-day access URLs for all photos
    val photoUrls = photos.map { photo ->
        photoService.generateAccessUrl(
            mediaUrl = photo.mediaUrl,
            expiryHours = 30 * 24 // 30 days for mutual matches
        )
    }

    return PublicProfile(
        userId = user.userId,
        name = user.firstName,
        age = user.getAge(),
        photos = photoUrls,
        // ... other profile fields
    )
}
```

---

### Step 8: Update Application Configuration

Add CloudFront configuration to `application.yaml`:

```yaml
# app/src/main/resources/application.yaml

aws:
  region: ${AWS_REGION:eu-west-2}
  accessKeyId: ${AWS_ACCESS_KEY_ID:}
  secretAccessKey: ${AWS_SECRET_ACCESS_KEY:}
  s3BucketName: ${AWS_S3_BUCKET_NAME:eros-photos}
  presignedUrlTtlMinutes: ${AWS_PRESIGNED_URL_TTL_MINUTES:15}

  # Legacy CDN config (optional, for backward compatibility)
  cdnBaseUrl: ${AWS_CDN_BASE_URL:}

  # CloudFront Signed URLs Configuration
  cloudFrontDistributionDomain: ${CLOUDFRONT_DISTRIBUTION_DOMAIN:}
  cloudFrontKeyPairId: ${CLOUDFRONT_KEY_PAIR_ID:}
  cloudFrontPrivateKeyPath: ${CLOUDFRONT_PRIVATE_KEY_PATH:}
```

Update `.env.example`:

```bash
# .env.example

# AWS S3 Configuration
AWS_REGION=eu-west-2
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_S3_BUCKET_NAME=eros-photos
AWS_PRESIGNED_URL_TTL_MINUTES=15

# CloudFront Configuration (for signed URLs)
CLOUDFRONT_DISTRIBUTION_DOMAIN=d1234567890.cloudfront.net
CLOUDFRONT_KEY_PAIR_ID=APKAXXXXXXXXXXXXXXXX
CLOUDFRONT_PRIVATE_KEY_PATH=/path/to/cloudfront-private-key.pem
```

---

## Part 3: Access Control Policies

### Photo Access Expiry Matrix

| Context | Expiry Time | Rationale |
|---------|-------------|-----------|
| **Daily Match Batch** | 48 hours | User has 2 days to decide on profile |
| **Reconsideration (Passed Profiles)** | 24 hours | Already saw once, shorter window |
| **Mutual Match (Chatting)** | 30 days | Ongoing conversation, need stable access |
| **Scheduled Date** | 90 days | Extended access until date completes |
| **Admin/Moderation** | 7 days | Review window for reported content |

### Implementation Examples

```kotlin
// Enum for access context
enum class PhotoAccessContext(val expiryHours: Long) {
    DAILY_BATCH(48),           // 2 days
    RECONSIDERATION(24),       // 1 day
    MUTUAL_MATCH(30 * 24),     // 30 days
    SCHEDULED_DATE(90 * 24),   // 90 days
    ADMIN_REVIEW(7 * 24)       // 7 days
}

// Usage in services
suspend fun buildUserMatchProfile(
    match: Match,
    context: PhotoAccessContext = PhotoAccessContext.DAILY_BATCH
): UserMatchProfile? {
    val userData = userService.getUserMatchProfileData(
        userId = match.user2Id,
        photoExpiryHours = context.expiryHours
    ) ?: return null

    // ... rest of implementation
}
```

---

## Part 4: Security Considerations

### 1. Private Key Protection

**DO:**
- ✅ Store private key in **AWS Secrets Manager** or encrypted environment variable
- ✅ Use **IAM roles** for EC2/ECS to access Secrets Manager
- ✅ Restrict file permissions: `chmod 400 cloudfront-private-key.pem`
- ✅ Rotate keys periodically (every 90 days)

**DON'T:**
- ❌ Commit private key to Git
- ❌ Store in plain text environment variables in production
- ❌ Share private key across environments (dev/staging/prod should have separate keys)

### 2. URL Expiration Best Practices

- **Short-lived for sensitive content**: 24-48 hours for unmatched profiles
- **Long-lived for committed relationships**: 30+ days for mutual matches
- **Renewal strategy**: Generate new URLs when user refreshes profile (don't cache expired URLs client-side)

### 3. Monitoring & Alerting

Set up CloudWatch alarms for:
- **4xx errors** (MissingKey, InvalidSignature) - indicates expired or invalid URLs
- **5xx errors** - CloudFront or S3 issues
- **Unusual traffic patterns** - potential URL sharing or scraping

---

## Part 5: Testing

### Manual Testing

#### Test 1: Verify CloudFront OAC

```bash
# Direct S3 access should fail (403 Forbidden)
curl -I https://eros-photos.s3.eu-west-2.amazonaws.com/photos/test/image.jpg
# Expected: HTTP/1.1 403 Forbidden

# CloudFront without signature should fail (403 MissingKey)
curl -I https://d1234567890.cloudfront.net/photos/test/image.jpg
# Expected: HTTP/1.1 403 Forbidden (MissingKey error)
```

#### Test 2: Verify Signed URL Generation

```kotlin
// Unit test in PhotoServiceTest.kt

@Test
fun `should generate CloudFront signed URL with expiration`() = runTest {
    val s3Config = S3Config(
        region = "eu-west-2",
        bucketName = "eros-photos",
        cloudFrontDistributionDomain = "d1234567890.cloudfront.net",
        cloudFrontKeyPairId = "APKATEST",
        cloudFrontPrivateKeyPath = "/path/to/test-key.pem",
        // ... other fields
    )

    val cloudFrontSigner = CloudFrontSignerService(s3Config)
    val signedUrl = cloudFrontSigner.generateSignedUrl("photos/test/image.jpg", expiryHours = 1)

    // Verify URL structure
    assertTrue(signedUrl.startsWith("https://d1234567890.cloudfront.net/photos/test/image.jpg"))
    assertTrue(signedUrl.contains("Expires="))
    assertTrue(signedUrl.contains("Signature="))
    assertTrue(signedUrl.contains("Key-Pair-Id="))
}
```

#### Test 3: End-to-End Integration Test

```bash
# Call your API endpoint to get daily batch
curl -X GET https://api.eros.app/matches/daily-batch \
  -H "Authorization: Bearer YOUR_FIREBASE_TOKEN"

# Response should include signed CloudFront URLs:
{
  "profiles": [
    {
      "matchId": 123,
      "userId": "user456",
      "name": "Alex",
      "age": 28,
      "thumbnailUrl": "https://d1234567890.cloudfront.net/photos/user456/abc.jpg?Expires=1234567890&Signature=...&Key-Pair-Id=APKA...",
      "badges": ["verified"],
      "servedAt": "2023-10-15T14:30:00Z"
    }
  ],
  "batchNumber": 1,
  "remainingBatches": 2
}

# Test accessing the signed URL
curl -I "https://d1234567890.cloudfront.net/photos/user456/abc.jpg?Expires=...&Signature=...&Key-Pair-Id=..."
# Expected: HTTP/1.1 200 OK (image accessible)

# Wait for expiration, then test again
# Expected: HTTP/1.1 403 Forbidden (expired signature)
```

---

## Part 6: Deployment Checklist

### Pre-Deployment

- [ ] CloudFront distribution created and deployed (status: Enabled)
- [ ] S3 bucket policy updated to allow CloudFront OAC
- [ ] S3 bucket public access blocked (all 4 checkboxes enabled)
- [ ] CloudFront key pair created and Key Pair ID saved
- [ ] Private key generated and stored securely (Secrets Manager or encrypted storage)
- [ ] Environment variables configured:
  - [ ] `CLOUDFRONT_DISTRIBUTION_DOMAIN`
  - [ ] `CLOUDFRONT_KEY_PAIR_ID`
  - [ ] `CLOUDFRONT_PRIVATE_KEY_PATH`

### Code Changes

- [ ] `S3Config.kt` updated with CloudFront fields
- [ ] `CloudFrontSignerService.kt` created
- [ ] `PhotoService.kt` updated with `generateAccessUrl()` method
- [ ] `UserService.kt` updated to use signed URLs
- [ ] `MatchService.kt` updated with context-aware expiry times
- [ ] Dependencies added to `build.gradle.kts`
- [ ] Tests written for signed URL generation

### Post-Deployment

- [ ] Verify CloudFront distribution serving content
- [ ] Test signed URL generation via API
- [ ] Monitor CloudWatch logs for errors
- [ ] Test URL expiration behavior
- [ ] Verify old URLs expire as expected
- [ ] Performance testing (response times, CDN cache hit rate)

---

## Part 7: Troubleshooting

### Common Issues

#### Issue 1: "MissingKey" Error on CloudFront

**Symptom:** Accessing CloudFront URL returns 403 with MissingKey error

**Cause:** URL is not signed (missing query parameters)

**Solution:** Ensure `generateAccessUrl()` is called, not `publicUrlFor()`

---

#### Issue 2: "InvalidSignature" Error

**Symptom:** Signed URL returns 403 InvalidSignature

**Possible Causes:**
1. Wrong Key Pair ID
2. Wrong private key
3. URL was modified (query params changed)
4. Clock skew between server and CloudFront

**Solution:**
- Verify `CLOUDFRONT_KEY_PAIR_ID` matches AWS Console
- Verify private key corresponds to uploaded public key
- Check server clock: `date -u` (should be within 5 minutes of actual UTC)

---

#### Issue 3: S3 Bucket Policy Denied

**Symptom:** CloudFront returns 403 AccessDenied from S3

**Cause:** S3 bucket policy doesn't allow CloudFront OAC

**Solution:**
- Verify S3 bucket policy includes CloudFront service principal
- Check `AWS:SourceArn` condition matches your distribution ARN

---

#### Issue 4: Private Key Not Found

**Symptom:** Application crashes on startup with "CloudFront private key file not found"

**Cause:** `CLOUDFRONT_PRIVATE_KEY_PATH` points to non-existent file

**Solution:**
- Verify file path is absolute and accessible by application user
- Check file permissions: `ls -la /path/to/cloudfront-private-key.pem`
- For Docker: ensure private key is mounted as volume or in Secrets Manager

---

## Part 8: Cost Estimation

### CloudFront Pricing (US/EU regions)

| Component | Cost | Example (1M users, 10M photo views/month) |
|-----------|------|------------------------------------------|
| **Data Transfer Out** | $0.085/GB (first 10 TB) | ~500 GB = **$42.50** |
| **HTTP/HTTPS Requests** | $0.0075/10k requests | 10M requests = **$7.50** |
| **Invalidations** | $0.005/path (first 1000 free) | Minimal |
| **Total** | | **~$50/month** |

### S3 Pricing (for comparison)

| Component | Cost | Example (same traffic) |
|-----------|------|------------------------|
| **Data Transfer Out** | $0.09/GB | ~500 GB = **$45.00** |
| **GET Requests** | $0.0004/1k requests | 10M requests = **$4.00** |
| **Storage** | $0.023/GB/month | 100 GB = **$2.30** |
| **Total** | | **~$51.30/month** |

**Savings:** CloudFront provides better performance at similar cost, with significant savings at scale due to caching (fewer S3 GET requests).

---

## Part 9: Future Enhancements

### 1. Custom Domain for CDN

Instead of `d1234567890.cloudfront.net`, use `cdn.eros.app`:

1. Request ACM certificate in `us-east-1` region (required for CloudFront)
2. Add CNAME record in Route 53: `cdn.eros.app` → `d1234567890.cloudfront.net`
3. Update CloudFront distribution with alternate domain name
4. Update `CLOUDFRONT_DISTRIBUTION_DOMAIN=cdn.eros.app`

### 2. Watermarking for Unmatched Profiles

Add subtle watermark to photos shown in daily batches (prevent screenshot sharing):
- Lambda@Edge function adds watermark on-the-fly
- Remove watermark after mutual match

### 3. Adaptive Image Delivery

CloudFront can serve different image sizes based on device:
- Lambda@Edge detects `User-Agent` header
- Returns optimized image size (thumbnail, medium, full)
- Reduces bandwidth and improves load times

### 4. Analytics & Abuse Detection

Track photo access patterns:
- CloudWatch Logs for CloudFront access logs
- Detect unusual access patterns (URL sharing, scraping)
- Automatically revoke or shorten expiry for suspicious activity

---

## Summary

This implementation provides:

1. **Security**: S3 bucket remains private, only CloudFront has access
2. **Access Control**: Time-limited URLs per user with context-aware expiry
3. **Performance**: Global CDN with edge caching for fast image delivery
4. **Privacy**: URLs expire automatically, preventing permanent sharing
5. **Scalability**: Handles millions of requests with automatic scaling
6. **Cost-Effective**: CloudFront cheaper than S3 direct at scale

**Next Steps:**
1. Set up CloudFront distribution with OAC (Part 1)
2. Implement backend code changes (Part 2)
3. Deploy and test (Parts 5-6)
4. Monitor and optimize (Part 7)

For questions or issues, refer to the Troubleshooting section or AWS CloudFront documentation.
