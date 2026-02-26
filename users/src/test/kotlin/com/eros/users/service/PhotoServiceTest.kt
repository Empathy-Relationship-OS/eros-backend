package com.eros.users.service

import com.eros.common.config.S3Config
import com.eros.users.models.ConfirmUploadRequest
import com.eros.users.models.MediaType
import com.eros.users.models.PresignedUploadRequest
import com.eros.users.models.UserMediaItem
import com.eros.users.models.UserMediaItemDTO
import com.eros.users.repository.PhotoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URL
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhotoServiceTest {

    // -------------------------------------------------------------------------
    // Test doubles
    // -------------------------------------------------------------------------

    private val mockRepository   = mockk<PhotoRepository>()
    private val mockS3Client     = mockk<S3Client>()
    private val mockS3Presigner  = mockk<S3Presigner>()

    private val s3Config = S3Config(
        region                 = "eu-west-2",
        accessKeyId            = "test-key",
        secretAccessKey        = "test-secret",
        bucketName             = "test-bucket",
        cdnBaseUrl             = null,
        presignedUrlTtlMinutes = 15L
    )

    private val service = PhotoService(
        photoRepository = mockRepository,
        s3Config        = s3Config,
        s3Client        = mockS3Client,
        s3Presigner     = mockS3Presigner
    )

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun mediaItem(
        id: Long = 1L,
        userId: String = "uid-1",
        mediaUrl: String = "https://test-bucket.s3.eu-west-2.amazonaws.com/photos/uid-1/abc.jpg",
        thumbnailUrl: String? = null,
        displayOrder: Int = 1,
        isPrimary: Boolean = false
    ) = UserMediaItem(
        id           = id,
        userId       = userId,
        mediaUrl     = mediaUrl,
        thumbnailUrl = thumbnailUrl,
        mediaType    = MediaType.PHOTO,
        displayOrder = displayOrder,
        isPrimary    = isPrimary,
        createdAt    = Instant.now(),
        updatedAt    = Instant.now()
    )

    private fun mediaItemDTO(
        id: Long = 1L,
        mediaUrl: String = "https://test-bucket.s3.eu-west-2.amazonaws.com/photos/uid-1/abc.jpg",
        thumbnailUrl: String? = null,
        displayOrder: Int = 1,
        isPrimary: Boolean = false
    ) = UserMediaItemDTO(
        id = id,
        mediaUrl     = mediaUrl,
        thumbnailUrl = thumbnailUrl,
        displayOrder = displayOrder,
        mediaType    = MediaType.PHOTO,
        isPrimary    = isPrimary
    )

    // -------------------------------------------------------------------------
    // generatePresignedUploadUrl
    // -------------------------------------------------------------------------

    @Nested
    inner class generatePresignedUploadUrl {

        private fun stubPresigner(url: String = "https://s3.amazonaws.com/presigned") {
            val presignedRequest = mockk<PresignedPutObjectRequest> {
                every { url() } returns URL(url)
            }
            every { mockS3Presigner.presignPutObject(any<PutObjectPresignRequest>()) } returns presignedRequest
        }

        @Test
        fun `should return presigned URL for valid JPEG upload`() = runTest {
            stubPresigner()
            coEvery { mockRepository.countByUserId("uid-1") } returns 0
            coEvery { mockRepository.findByDisplayOrder("uid-1", 1) } returns null

            val request = PresignedUploadRequest(
                fileName = "file.jpg",
                contentType   = "image/jpeg",
                fileSizeBytes = 1_000_000L,
                displayOrder  = 1
            )
            val response = service.generatePresignedUploadUrl("uid-1", request)

            assertNotNull(response.uploadUrl)
            assertNotNull(response.objectKey)
            assertEquals(15L, response.expiresInMinutes)
            assertTrue(response.objectKey.startsWith("photos/uid-1/"))
            assertTrue(response.objectKey.endsWith(".jpg"))
        }

        @Test
        fun `should return presigned URL for valid PNG upload`() = runTest {
            stubPresigner()
            coEvery { mockRepository.countByUserId("uid-1") } returns 2
            coEvery { mockRepository.findByDisplayOrder("uid-1", 2) } returns null

            val request = PresignedUploadRequest(
                fileName = "file.jpg",
                contentType   = "image/png",
                fileSizeBytes = 2_000_000L,
                displayOrder  = 2
            )
            val response = service.generatePresignedUploadUrl("uid-1", request)

            assertTrue(response.objectKey.endsWith(".png"))
        }

        @Test
        fun `should return presigned URL for valid HEIC upload`() = runTest {
            stubPresigner()
            coEvery { mockRepository.countByUserId("uid-1") } returns 1
            coEvery { mockRepository.findByDisplayOrder("uid-1", 1) } returns null

            val request = PresignedUploadRequest(
                fileName = "file.jpg",
                contentType   = "image/heic",
                fileSizeBytes = 3_000_000L,
                displayOrder  = 1
            )
            val response = service.generatePresignedUploadUrl("uid-1", request)

            assertTrue(response.objectKey.endsWith(".heic"))
        }

        @Test
        fun `should throw IllegalArgumentException for unsupported content type`() = runTest {
            // DTO validation rejects unsupported content types during construction
            assertThrows<IllegalArgumentException> {
                PresignedUploadRequest(
                    fileName = "file.jpg",
                    contentType   = "image/gif",
                    fileSizeBytes = 1_000_000L,
                    displayOrder  = 1
                )
            }
        }

        @Test
        fun `should throw IllegalArgumentException when file is below minimum size`() = runTest {
            // Service enforces stricter 500KB minimum
            assertThrows<IllegalArgumentException> {
                PresignedUploadRequest(
                    fileName = "file.jpg",
                    contentType   = "image/jpeg",
                    fileSizeBytes = 100L,  // below 500 KB - DTO allows but service rejects
                    displayOrder  = 1
                )
            }
        }

        @Test
        fun `should throw IllegalArgumentException when file exceeds maximum size`() = runTest {
            // DTO validation rejects invalid file size during construction
            assertThrows<IllegalArgumentException> {
                PresignedUploadRequest(
                    fileName = "file.jpg",
                    contentType   = "image/jpeg",
                    fileSizeBytes = 11_000_000L,  // above 10 MB
                    displayOrder  = 1
                )
            }
        }

        @Test
        fun `should throw IllegalStateException when user has max photos and slot is empty`() = runTest {
            coEvery { mockRepository.countByUserId("uid-1") } returns 6
            coEvery { mockRepository.findByDisplayOrder("uid-1", 1) } returns null

            val request = PresignedUploadRequest(
                fileName = "file.jpg",
                contentType   = "image/jpeg",
                fileSizeBytes = 1_000_000L,
                displayOrder  = 1
            )
            assertThrows<IllegalStateException> {
                service.generatePresignedUploadUrl("uid-1", request)
            }
        }

        @Test
        fun `should allow upload when slot is occupied even if user has max photos`() = runTest {
            // Replacing an existing photo at the same slot is always allowed
            stubPresigner()
            coEvery { mockRepository.countByUserId("uid-1") } returns 6
            coEvery { mockRepository.findByDisplayOrder("uid-1", 1) } returns mediaItem()

            val request = PresignedUploadRequest(
                fileName = "file.jpg",
                contentType   = "image/jpeg",
                fileSizeBytes = 1_000_000L,
                displayOrder  = 1
            )
            // Should not throw
            val response = service.generatePresignedUploadUrl("uid-1", request)
            assertNotNull(response.uploadUrl)
        }
    }

    // -------------------------------------------------------------------------
    // confirmUpload
    // -------------------------------------------------------------------------

    @Nested
    inner class confirmUpload {

        private fun stubHeadObject() {
            every {
                mockS3Client.headObject(any<HeadObjectRequest>())
            } returns HeadObjectResponse.builder().build()
        }

        private fun stubHeadObjectMissing() {
            every {
                mockS3Client.headObject(any<HeadObjectRequest>())
            } throws NoSuchKeyException.builder().message("not found").build()
        }

        @Test
        fun `should persist and return media item when upload is verified`() = runTest {
            stubHeadObject()
            val objectKey = "photos/uid-1/abc.jpg"
            val expectedUrl = "https://test-bucket.s3.eu-west-2.amazonaws.com/$objectKey"
            val item = mediaItem(mediaUrl = expectedUrl)

            coEvery { mockRepository.findByDisplayOrder("uid-1", 1) } returns null
            coEvery {
                mockRepository.insert("uid-1", expectedUrl, MediaType.PHOTO, 1, false)
            } returns item

            val request = ConfirmUploadRequest(objectKey = objectKey, displayOrder = 1, isPrimary = false)
            val result = service.confirmUpload("uid-1", request)

            assertEquals(item, result)
            coVerify { mockRepository.insert("uid-1", expectedUrl, MediaType.PHOTO, 1, false) }
        }

        @Test
        fun `should delete existing photo at display order slot before inserting new one`() = runTest {
            stubHeadObject()
            val objectKey = "photos/uid-1/new.jpg"
            val expectedUrl = "https://test-bucket.s3.eu-west-2.amazonaws.com/$objectKey"
            val existingItem = mediaItem(id = 99L, mediaUrl = "https://test-bucket.s3.eu-west-2.amazonaws.com/photos/uid-1/old.jpg")
            val newItem = mediaItem(mediaUrl = expectedUrl)

            coEvery { mockRepository.findByDisplayOrder("uid-1", 1) } returns existingItem
            coEvery { mockRepository.deleteById(99L) } returns 1
            every { mockS3Client.deleteObject(any<java.util.function.Consumer<software.amazon.awssdk.services.s3.model.DeleteObjectRequest.Builder>>()) } returns mockk()
            coEvery {
                mockRepository.insert("uid-1", expectedUrl, MediaType.PHOTO, 1, false)
            } returns newItem

            val request = ConfirmUploadRequest(objectKey = objectKey, displayOrder = 1, isPrimary = false)
            service.confirmUpload("uid-1", request)

            coVerify { mockRepository.deleteById(99L) }
            coVerify { mockRepository.insert("uid-1", expectedUrl, MediaType.PHOTO, 1, false) }
        }

        @Test
        fun `should set photo as primary when isPrimary is true`() = runTest {
            stubHeadObject()
            val objectKey = "photos/uid-1/abc.jpg"
            val expectedUrl = "https://test-bucket.s3.eu-west-2.amazonaws.com/$objectKey"
            val item = mediaItem(mediaUrl = expectedUrl, isPrimary = true)
            val primaryItem = item.copy(isPrimary = true)

            coEvery { mockRepository.findByDisplayOrder("uid-1", 1) } returns null
            coEvery { mockRepository.insert("uid-1", expectedUrl, MediaType.PHOTO, 1, true) } returns item
            coEvery { mockRepository.setPrimary("uid-1", item.id) } returns primaryItem

            val request = ConfirmUploadRequest(objectKey = objectKey, displayOrder = 1, isPrimary = true)
            val result = service.confirmUpload("uid-1", request)

            assertEquals(true, result.isPrimary)
            coVerify { mockRepository.setPrimary("uid-1", item.id) }
        }

        @Test
        fun `should throw IllegalArgumentException when S3 object does not exist`() = runTest {
            stubHeadObjectMissing()

            val request = ConfirmUploadRequest(
                objectKey    = "photos/uid-1/missing.jpg",
                displayOrder = 1
            )
            val ex = assertThrows<IllegalArgumentException> {
                service.confirmUpload("uid-1", request)
            }
            assertTrue(ex.message!!.contains("Upload not found in S3"))
        }

        @Test
        fun `should throw IllegalArgumentException when object key does not belong to user`() = runTest {
            // Attacker trying to confirm upload with another user's object key
            val request = ConfirmUploadRequest(
                objectKey    = "photos/other-user/malicious.jpg",
                displayOrder = 1
            )
            val ex = assertThrows<IllegalArgumentException> {
                service.confirmUpload("uid-1", request)
            }
            assertEquals("Object key must belong to the authenticated user", ex.message)
        }

        @Test
        fun `should throw IllegalArgumentException when object key has wrong format`() = runTest {
            // Object key not following the expected photos/userId/ format
            val request = ConfirmUploadRequest(
                objectKey    = "invalid/path/file.jpg",
                displayOrder = 1
            )
            val ex = assertThrows<IllegalArgumentException> {
                service.confirmUpload("uid-1", request)
            }
            assertEquals("Object key must belong to the authenticated user", ex.message)
        }
    }

    // -------------------------------------------------------------------------
    // getUserMedia
    // -------------------------------------------------------------------------

    @Nested
    inner class getUserMedia {

        @Test
        fun `should return UserMediaCollection for user`() = runTest {
            val items = listOf(
                mediaItem(id = 1L, displayOrder = 1),
                mediaItem(id = 2L, displayOrder = 2)
            )
            val itemsDto = listOf(
                mediaItemDTO(id = 1L, displayOrder = 1),
                mediaItemDTO(id = 2L, displayOrder = 2)
            )
            coEvery { mockRepository.findByUserId("uid-1") } returns items

            val collection = service.getUserMedia("uid-1")

            assertEquals("uid-1", collection.userId)
            assertEquals(2, collection.totalCount)
            assertEquals(itemsDto, collection.media)
        }

        @Test
        fun `should return empty collection when user has no photos`() = runTest {
            coEvery { mockRepository.findByUserId("uid-1") } returns emptyList()

            val collection = service.getUserMedia("uid-1")

            assertEquals(0, collection.totalCount)
            assertTrue(collection.media.isEmpty())
        }
    }

    // -------------------------------------------------------------------------
    // deletePhoto
    // -------------------------------------------------------------------------

    @Nested
    inner class deletePhoto {

        @Test
        fun `should delete photo from S3 and DB and return deleted item`() = runTest {
            val item = mediaItem(id = 1L, userId = "uid-1")
            coEvery { mockRepository.findById(1L) } returns item
            coEvery { mockRepository.deleteById(1L) } returns 1
            every { mockS3Client.deleteObject(any<java.util.function.Consumer<software.amazon.awssdk.services.s3.model.DeleteObjectRequest.Builder>>()) } returns mockk()

            val result = service.deletePhoto("uid-1", 1L)

            assertEquals(item, result)
            coVerify { mockRepository.deleteById(1L) }
        }

        @Test
        fun `should also delete thumbnail when it exists`() = runTest {
            val item = mediaItem(
                id           = 1L,
                userId       = "uid-1",
                thumbnailUrl = "https://test-bucket.s3.eu-west-2.amazonaws.com/photos/uid-1/thumb.jpg"
            )
            coEvery { mockRepository.findById(1L) } returns item
            coEvery { mockRepository.deleteById(1L) } returns 1
            // deleteFromS3 uses the DSL (Consumer<Builder>) overload of deleteObject
            every { mockS3Client.deleteObject(any<java.util.function.Consumer<software.amazon.awssdk.services.s3.model.DeleteObjectRequest.Builder>>()) } returns mockk()

            service.deletePhoto("uid-1", 1L)

            // Verify deleteObject was called twice: once for original, once for thumbnail
            io.mockk.verify(exactly = 2) {
                mockS3Client.deleteObject(any<java.util.function.Consumer<software.amazon.awssdk.services.s3.model.DeleteObjectRequest.Builder>>())
            }
        }

        @Test
        fun `should return null when photo does not exist`() = runTest {
            coEvery { mockRepository.findById(99L) } returns null

            val result = service.deletePhoto("uid-1", 99L)

            assertNull(result)
        }

        @Test
        fun `should return null when photo belongs to different user`() = runTest {
            val item = mediaItem(id = 1L, userId = "other-user")
            coEvery { mockRepository.findById(1L) } returns item

            val result = service.deletePhoto("uid-1", 1L)

            assertNull(result)
        }
    }

    // -------------------------------------------------------------------------
    // setPrimaryPhoto
    // -------------------------------------------------------------------------

    @Nested
    inner class setPrimaryPhoto {

        @Test
        fun `should set photo as primary and return updated item`() = runTest {
            val item = mediaItem(id = 1L, userId = "uid-1")
            val updatedItem = item.copy(isPrimary = true)

            coEvery { mockRepository.findById(1L) } returns item
            coEvery { mockRepository.setPrimary("uid-1", 1L) } returns updatedItem

            val result = service.setPrimaryPhoto("uid-1", 1L)

            assertEquals(true, result?.isPrimary)
            coVerify { mockRepository.setPrimary("uid-1", 1L) }
        }

        @Test
        fun `should return null when photo does not exist`() = runTest {
            coEvery { mockRepository.findById(99L) } returns null

            val result = service.setPrimaryPhoto("uid-1", 99L)

            assertNull(result)
        }

        @Test
        fun `should return null when photo belongs to different user`() = runTest {
            val item = mediaItem(id = 1L, userId = "other-user")
            coEvery { mockRepository.findById(1L) } returns item

            val result = service.setPrimaryPhoto("uid-1", 1L)

            assertNull(result)
        }
    }

    // -------------------------------------------------------------------------
    // S3Config.publicUrlFor (CDN vs S3 URL construction)
    // -------------------------------------------------------------------------

    @Nested
    inner class `S3Config URL construction` {

        @Test
        fun `should return S3 URL when cdnBaseUrl is null`() {
            val config = s3Config.copy(cdnBaseUrl = null)
            val url = config.publicUrlFor("photos/uid/abc.jpg")
            assertEquals("https://test-bucket.s3.eu-west-2.amazonaws.com/photos/uid/abc.jpg", url)
        }

        @Test
        fun `should return CDN URL when cdnBaseUrl is set`() {
            val config = s3Config.copy(cdnBaseUrl = "https://cdn.example.com")
            val url = config.publicUrlFor("photos/uid/abc.jpg")
            assertEquals("https://cdn.example.com/photos/uid/abc.jpg", url)
        }

        @Test
        fun `should strip trailing slash from cdnBaseUrl`() {
            val config = s3Config.copy(cdnBaseUrl = "https://cdn.example.com/")
            val url = config.publicUrlFor("photos/uid/abc.jpg")
            assertEquals("https://cdn.example.com/photos/uid/abc.jpg", url)
        }
    }
}
