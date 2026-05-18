package com.eros.common

import com.eros.common.config.S3Config
import java.nio.file.Files

/**
 * Test fixtures and builders for common test data.
 */
object TestFixtures {

    /**
     * Creates a test S3Config with CloudFront enabled.
     */
    fun testS3Config(
        region: String = "eu-west-2",
        bucketName: String = "test-bucket",
        cloudFrontDistributionDomain: String = "d123test.cloudfront.net",
        cloudFrontKeyPairId: String = "APKATESTKEY123",
        cloudFrontPrivateKeyPath: String? = null
    ): S3Config {
        return S3Config(
            region = region,
            accessKeyId = "test-access-key",
            secretAccessKey = "test-secret-key",
            bucketName = bucketName,
            cdnBaseUrl = null,
            presignedUrlTtlMinutes = 15,
            cloudFrontKeyPairId = cloudFrontKeyPairId,
            cloudFrontPrivateKeyPath = cloudFrontPrivateKeyPath ?: createTestPrivateKeyFile(),
            cloudFrontDistributionDomain = cloudFrontDistributionDomain
        )
    }

    /**
     * Creates a test S3Config WITHOUT CloudFront (not enabled).
     */
    fun testS3ConfigWithoutCloudFront(
        region: String = "eu-west-2",
        bucketName: String = "test-bucket"
    ): S3Config {
        return S3Config(
            region = region,
            accessKeyId = "test-access-key",
            secretAccessKey = "test-secret-key",
            bucketName = bucketName,
            cdnBaseUrl = null,
            presignedUrlTtlMinutes = 15,
            cloudFrontKeyPairId = null,
            cloudFrontPrivateKeyPath = null,
            cloudFrontDistributionDomain = null
        )
    }

    /**
     * Creates a temporary test private key file for CloudFront signing.
     *
     * This is a real RSA private key in PKCS#8 PEM format (2048 bits) that can be used
     * for testing CloudFront URL signing.
     */
    fun createTestPrivateKeyFile(): String {
        val tempFile = Files.createTempFile("test-cloudfront-key", ".pem").toFile()
        tempFile.deleteOnExit()

        // Real RSA private key (2048 bits) - for testing only!
        val privateKeyPem = """
-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCy6v8HBgb9tXjE
eqEZjKbFF1uyAnHcgIKZfju9hItMKf4sk7YejmwMP5Too90djn3S/fM19VB6sje/
ch3iBYikAh0829opFjx9qY2iW/nqlBYxC/FJB6C7jYRqWX2NXw+QBdMUum0dyABQ
PDxch0UntYSSJNDz3mGkI0q81mgNdEW89eEEX2QO9xa9bPF0X5j7WO3Uqkbxnzah
xz03+3ozfUifegeaczmruvjnLcHstZh8fwVWeAQlbMhCfKE9fd5hUZpucNv6b72G
J8Qt5e8rNXJyQvuULhaXi+96KYDPlGrXdXZQJ9pjJtbyzn+Z0mawbr00X3zpmTqP
p9pDfaavAgMBAAECggEAEAOO4lLh8vkNqfmsjAFgoeO703x63XjBImTqivAfRFNp
NRXe0fBUb7+MdUrJYo5bk7Qi0ycLANhiZf0cUJvHMErM0XgqGCMDNLTQK60zerwD
b84OVseF1ZOj5khD6mXiRN/zQHfehrEH2iacuSQVsVQCss6fHIdrJ4pNyQYgbRJe
87T/gP2zMXFkMrUGiqId5JqpIwkj6emeoRVDZ9acqjqJdTdrwETMF9xNSR484ihg
6FptkyraiosKufEud0x6FCP7mnhvptAwWT1fRJaiMKAkMGnNwhLS0B4ZWwf0AVcg
5asz4cnQ4TVkyBC6Kc/yLRb5mxTUoFyMtCZ5wDe6IQKBgQD1jw5vdGITnQg/f8u6
yoNk0k/wtwGefR1mKSYbcjeqxvDccE5BITuR7UGAvKPWKlI4dLis2N5t0C7oe3Tx
5Glugh5a1FOnhVcWE8t4vGfK8q9rKez9nK4ipJO2/NfhIu8qpca0b86Hfz97s2jN
oczH8Qpdy5gwmGNO3SMqXEbZ4QKBgQC6hotXYJ0nhEJYPJC2izk0dMZBifA/o04U
30CYVnBwMhSbvH+It+D1Qsns5EXwt6Im749ySzQDvfrvvnrnw50jXICo4mu6I2PM
rUwliMIuwaYIp6iqWIGjW8CBPGqI5ujmokL1Pnovw6H3FvoBJFnK5dWnKZgYzagw
SZYkJNwyjwKBgQDOv6sMxjXJg76tECFtXRLx8W1jJAKV+YumN2EhXLJn9GZZg6kv
hxaLz/IFvU5IhIuDgyr2RQZJ+S11inS1MXZfl+iM2xawBkGDkhkPx7mwE1ME7GDg
S2oGOwEv4YS1xt8NzlQWGckPJmFB+pV0BRXRBi6POFskFzoN7XytLtJsoQKBgEyx
s6ua0yODc1Aanxofxsa12Srrj7hq6lkUrte0ewj23phmej359WqDKsnX1pYyu16t
E9tX5qw/OmXHeXvAZ+U5TMGkT806bfejrfKk98sH3sAiR+y69Zr4x/+bmuZto4fp
YgS6yrzHANq7PNKpYq5VsHLKXHqeXTB1oWF9MnkrAoGAfZFxIr6sDSr1B55B33/t
1puAWAq09HY/+RwytzLfdUqO6XgC8L0VMlOSgKNLEyswykdmxyEpzc2N4xpPYmTx
O28PE7clQdnoXcaOzuNEw7aavZ7zXa+dw5JxtGizs0/bwVprHx01qfFsQqVrgzXw
ZaoMla8fzoEfYduTZ0wHczo=
-----END PRIVATE KEY-----
        """.trimIndent()

        tempFile.writeText(privateKeyPem)
        return tempFile.absolutePath
    }

    /**
     * Sample object keys for testing.
     */
    object ObjectKeys {
        const val USER_PHOTO = "photos/user123/abc123.jpg"
        const val USER_PHOTO_2 = "photos/user123/def456.jpg"
        const val ANOTHER_USER_PHOTO = "photos/user456/ghi789.jpg"
    }

    /**
     * Sample CloudFront URLs for testing.
     */
    object Urls {
        const val CLOUDFRONT_BASE = "https://d123test.cloudfront.net"
        const val S3_BASE = "https://test-bucket.s3.eu-west-2.amazonaws.com"

        fun cloudFrontUrl(objectKey: String) = "$CLOUDFRONT_BASE/$objectKey"
        fun s3Url(objectKey: String) = "$S3_BASE/$objectKey"
    }
}
