package com.eros.common.services

import com.eros.common.TestFixtures
import com.eros.common.cache.InMemoryUrlCache
import com.eros.common.cache.UrlCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudFrontSignerServiceTest {

    @Nested
    inner class `generateSignedUrl()` {

        @Nested
        inner class `URL generation` {

            @Test
            fun `should generate CloudFront signed URL with correct structure`() {
                val s3Config = TestFixtures.testS3Config()
                val signer = CloudFrontSignerService(s3Config)

                val signedUrl = signer.generateSignedUrl("photos/user123/test.jpg", 48)

                // Verify URL structure
                assertTrue(signedUrl.startsWith("https://d123test.cloudfront.net/photos/user123/test.jpg"),
                    "URL should start with CloudFront domain and object key")
                assertTrue(signedUrl.contains("Expires="),
                    "URL should contain Expires parameter")
                assertTrue(signedUrl.contains("Signature="),
                    "URL should contain Signature parameter")
                assertTrue(signedUrl.contains("Key-Pair-Id="),
                    "URL should contain Key-Pair-Id parameter")
            }

            @Test
            fun `should include correct Key-Pair-Id in URL`() {
                val s3Config = TestFixtures.testS3Config(
                    cloudFrontKeyPairId = "APKATEST123456"
                )
                val signer = CloudFrontSignerService(s3Config)

                val signedUrl = signer.generateSignedUrl("photos/test.jpg", 48)

                assertTrue(signedUrl.contains("Key-Pair-Id=APKATEST123456"),
                    "URL should contain the correct Key-Pair-Id")
            }

            @Test
            fun `should generate different signatures for different object keys`() {
                val s3Config = TestFixtures.testS3Config()
                val signer = CloudFrontSignerService(s3Config)

                val url1 = signer.generateSignedUrl("photos/user1/photo1.jpg", 48)
                val url2 = signer.generateSignedUrl("photos/user2/photo2.jpg", 48)

                // URLs should be different (different object keys = different signatures)
                assertFalse(url1 == url2, "Different object keys should produce different signed URLs")
            }
        }

        @Nested
        inner class `caching behavior` {

            @Test
            fun `should use cached URL when available`() {
                val mockCache = mockk<UrlCache>()
                val s3Config = TestFixtures.testS3Config()
                val signer = CloudFrontSignerService(s3Config, mockCache)

                val cachedUrl = "https://d123test.cloudfront.net/photos/test.jpg?Expires=123&Signature=cached&Key-Pair-Id=TEST"

                every { mockCache.getOrGenerate(any(), any(), any()) } returns cachedUrl

                val result = signer.generateSignedUrl("photos/test.jpg", 48)

                assertEquals(cachedUrl, result, "Should return cached URL")
                verify(exactly = 1) {
                    mockCache.getOrGenerate("photos/test.jpg:48", 48, any())
                }
            }

            @Test
            fun `should cache generated URLs for repeated calls`() {
                val s3Config = TestFixtures.testS3Config()
                val cache = InMemoryUrlCache()
                val signer = CloudFrontSignerService(s3Config, cache)

                // First call - generates and caches
                val url1 = signer.generateSignedUrl("photos/user123/test.jpg", 48)

                // Second call - should return cached URL (same result)
                val url2 = signer.generateSignedUrl("photos/user123/test.jpg", 48)

                assertEquals(url1, url2, "Should return same URL from cache")
            }

            @Test
            fun `should regenerate URL after cache invalidation`() {
                val s3Config = TestFixtures.testS3Config()
                val cache = InMemoryUrlCache()
                val signer = CloudFrontSignerService(s3Config, cache)

                // Generate and cache URL
                val url1 = signer.generateSignedUrl("photos/user123/test.jpg", 48)

                // Invalidate cache
                signer.invalidateCache("photos/user123/test.jpg")

                // Should generate new URL (note: signature will be different due to timestamp)
                val url2 = signer.generateSignedUrl("photos/user123/test.jpg", 48)

                // We can't compare URLs directly because timestamp changes,
                // but we can verify the structure is correct
                assertTrue(url2.startsWith("https://d123test.cloudfront.net/photos/user123/test.jpg"))
                assertTrue(url2.contains("Expires="))
            }
        }

        @Nested
        inner class `different expiry times` {

            @Test
            fun `should generate different cache keys for different expiry times`() {
                val s3Config = TestFixtures.testS3Config()
                val cache = InMemoryUrlCache()
                val signer = CloudFrontSignerService(s3Config, cache)

                // Generate URLs with different expiry times
                val url48h = signer.generateSignedUrl("photos/user123/test.jpg", 48)
                val url24h = signer.generateSignedUrl("photos/user123/test.jpg", 24)

                // URLs should be different (different expiry = different signature)
                assertFalse(url48h == url24h,
                    "Different expiry times should produce different URLs")

                // Verify both are cached separately
                val stats = cache.getStats()
                assertEquals(2, stats.size, "Should have 2 separate cache entries")
                assertTrue(stats.entries.contains("photos/user123/test.jpg:48"))
                assertTrue(stats.entries.contains("photos/user123/test.jpg:24"))
            }
        }
    }

    @Nested
    inner class `extractObjectKey()` {

        @Test
        fun `should extract object key from CloudFront URL`() {
            val s3Config = TestFixtures.testS3Config(
                cloudFrontDistributionDomain = "d123test.cloudfront.net"
            )
            val signer = CloudFrontSignerService(s3Config)

            val cloudFrontUrl = "https://d123test.cloudfront.net/photos/user123/test.jpg"
            val objectKey = signer.extractObjectKey(cloudFrontUrl)

            assertEquals("photos/user123/test.jpg", objectKey)
        }

        @Test
        fun `should extract object key from S3 URL`() {
            val s3Config = TestFixtures.testS3Config()
            val signer = CloudFrontSignerService(s3Config)

            val s3Url = "https://test-bucket.s3.eu-west-2.amazonaws.com/photos/user123/test.jpg"
            val objectKey = signer.extractObjectKey(s3Url)

            assertEquals("photos/user123/test.jpg", objectKey)
        }

        @Test
        fun `should return plain object key unchanged`() {
            val s3Config = TestFixtures.testS3Config()
            val signer = CloudFrontSignerService(s3Config)

            val plainKey = "photos/user123/test.jpg"
            val objectKey = signer.extractObjectKey(plainKey)

            assertEquals("photos/user123/test.jpg", objectKey)
        }
    }

    @Nested
    inner class `cache invalidation` {

        @Test
        fun `should invalidate cache for all expiry variants when invalidating object`() {
            val s3Config = TestFixtures.testS3Config()
            val cache = InMemoryUrlCache()
            val signer = CloudFrontSignerService(s3Config, cache)

            // Generate URLs with different expiry times
            signer.generateSignedUrl("photos/user123/test.jpg", 24)
            signer.generateSignedUrl("photos/user123/test.jpg", 48)
            signer.generateSignedUrl("photos/user123/test.jpg", 720) // 30 days

            // Verify all are cached
            assertEquals(3, cache.getStats().size)

            // Invalidate all variants
            signer.invalidateCache("photos/user123/test.jpg")

            // All should be invalidated
            assertEquals(0, cache.getStats().size, "All expiry variants should be invalidated")
        }

        @Test
        fun `should invalidate all user photos when invalidating user`() {
            val s3Config = TestFixtures.testS3Config()
            val cache = InMemoryUrlCache()
            val signer = CloudFrontSignerService(s3Config, cache)

            // Generate URLs for multiple photos from same user
            signer.generateSignedUrl("photos/user123/photo1.jpg", 48)
            signer.generateSignedUrl("photos/user123/photo2.jpg", 48)
            signer.generateSignedUrl("photos/user123/photo3.jpg", 24)

            // Generate URLs for another user (should not be affected)
            signer.generateSignedUrl("photos/user456/photo1.jpg", 48)

            // Invalidate all for user123
            signer.invalidateUserCache("user123")

            val stats = cache.getStats()

            // Only user456's URL should remain
            assertEquals(1, stats.size, "Only user456's cache entry should remain")
            assertTrue(stats.entries.any { it.contains("user456") },
                "user456's entry should remain")
            assertFalse(stats.entries.any { it.contains("user123") },
                "All user123 entries should be removed")
        }
    }

    @Nested
    inner class `statistics` {

        @Test
        fun `should return cache statistics`() {
            val s3Config = TestFixtures.testS3Config()
            val cache = InMemoryUrlCache()
            val signer = CloudFrontSignerService(s3Config, cache)

            // Generate some URLs
            signer.generateSignedUrl("photos/user1/test.jpg", 48)
            signer.generateSignedUrl("photos/user2/test.jpg", 48)

            val stats = signer.getCacheStats()

            assertEquals(2, stats.size, "Stats should reflect cached entries")
            assertEquals("in-memory", stats.implementation)
        }
    }

    @Nested
    inner class `generateSignedUrlWithCustomPolicy()` {

        @Test
        fun `should generate CloudFront signed URL with custom policy`() {
            val s3Config = TestFixtures.testS3Config()
            val signer = CloudFrontSignerService(s3Config)

            val signedUrl = signer.generateSignedUrlWithCustomPolicy(
                "photos/user123/test.jpg",
                expiryHours = 48,
                ipRange = null
            )

            // Verify URL structure
            assertTrue(signedUrl.startsWith("https://d123test.cloudfront.net/photos/user123/test.jpg"),
                "URL should start with CloudFront domain and object key")
            assertTrue(signedUrl.contains("Policy="),
                "URL should contain Policy parameter for custom policy")
            assertTrue(signedUrl.contains("Signature="),
                "URL should contain Signature parameter")
            assertTrue(signedUrl.contains("Key-Pair-Id="),
                "URL should contain Key-Pair-Id parameter")
        }

        @Test
        fun `should generate custom policy URL with IP restriction`() {
            val s3Config = TestFixtures.testS3Config()
            val signer = CloudFrontSignerService(s3Config)

            val signedUrl = signer.generateSignedUrlWithCustomPolicy(
                "photos/user123/test.jpg",
                expiryHours = 48,
                ipRange = "203.0.113.0/24"
            )

            // URL should contain policy parameter
            assertTrue(signedUrl.contains("Policy="),
                "URL with IP restriction should contain Policy parameter")
        }

        @Test
        fun `should properly escape JSON special characters in resource URL`() {
            val s3Config = TestFixtures.testS3Config()
            val signer = CloudFrontSignerService(s3Config)

            // Resource URL with JSON special characters (quotes, backslashes)
            val objectKeyWithSpecialChars = "photos/user123/test\"quote'single\\backslash.jpg"

            // Should not throw exception due to malformed JSON
            val signedUrl = signer.generateSignedUrlWithCustomPolicy(
                objectKeyWithSpecialChars,
                expiryHours = 48,
                ipRange = null
            )

            // Verify URL was generated successfully
            assertTrue(signedUrl.contains("Policy="),
                "Should generate valid URL even with special characters in object key")
            assertTrue(signedUrl.contains("Signature="),
                "Should contain signature despite special characters")
        }

        @Test
        fun `should properly escape JSON special characters in IP range`() {
            val s3Config = TestFixtures.testS3Config()
            val signer = CloudFrontSignerService(s3Config)

            // IP range with potential injection attempt
            val maliciousIpRange = "192.168.1.0/24\",\"malicious\":\"injection"

            // Should not throw exception or allow injection
            val signedUrl = signer.generateSignedUrlWithCustomPolicy(
                "photos/user123/test.jpg",
                expiryHours = 48,
                ipRange = maliciousIpRange
            )

            // Verify URL was generated successfully (JSON library escapes the quotes)
            assertTrue(signedUrl.contains("Policy="),
                "Should generate valid URL with escaped IP range")
            assertTrue(signedUrl.contains("Signature="),
                "Should contain valid signature")
        }

        @Test
        fun `should handle Unicode characters in resource URL`() {
            val s3Config = TestFixtures.testS3Config()
            val signer = CloudFrontSignerService(s3Config)

            // Resource URL with Unicode characters
            val objectKeyWithUnicode = "photos/user123/café-☕-日本語.jpg"

            val signedUrl = signer.generateSignedUrlWithCustomPolicy(
                objectKeyWithUnicode,
                expiryHours = 48,
                ipRange = null
            )

            // Verify URL was generated successfully
            assertTrue(signedUrl.contains("Policy="),
                "Should generate valid URL with Unicode characters")
        }
    }

    @Nested
    inner class `error handling` {

        @Test
        fun `should throw exception when CloudFront not configured`() {
            val s3Config = TestFixtures.testS3ConfigWithoutCloudFront()

            val exception = assertThrows<IllegalArgumentException> {
                CloudFrontSignerService(s3Config)
            }

            assertTrue(exception.message!!.contains("CloudFront signed URLs are not enabled"),
                "Exception should indicate CloudFront is not configured")
        }

        @Test
        fun `should throw exception when private key file not found`() {
            val s3Config = TestFixtures.testS3Config(
                cloudFrontPrivateKeyPath = "/nonexistent/path/to/key.pem"
            )

            val exception = assertThrows<IllegalStateException> {
                CloudFrontSignerService(s3Config)
            }

            assertTrue(exception.message!!.contains("CloudFront private key file not found"),
                "Exception should indicate private key file not found")
        }

        @Test
        fun `should throw exception when private key is invalid`() {
            // Create a file with invalid key content
            val tempFile = kotlin.io.path.createTempFile("invalid-key", ".pem").toFile()
            tempFile.deleteOnExit()
            tempFile.writeText("INVALID KEY CONTENT")

            val s3Config = TestFixtures.testS3Config(
                cloudFrontPrivateKeyPath = tempFile.absolutePath
            )

            val exception = assertThrows<IllegalStateException> {
                CloudFrontSignerService(s3Config)
            }

            assertTrue(exception.message!!.contains("Failed to load CloudFront private key"),
                "Exception should indicate private key loading failed")
        }
    }
}
