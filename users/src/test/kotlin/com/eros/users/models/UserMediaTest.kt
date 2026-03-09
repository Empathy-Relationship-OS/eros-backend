package com.eros.users.models

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.time.Instant

class UserMediaTest {

    @Nested
    inner class `UserMediaItem validation` {

        @Test
        fun `should throw exception when displayOrder is less than 1`() {
            val exception = assertThrows<IllegalArgumentException> {
                createMediaItem(displayOrder = 0)
            }
            assertEquals("Display order must be between 1 and 6", exception.message)
        }

        @Test
        fun `should throw exception when displayOrder is greater than 6`() {
            val exception = assertThrows<IllegalArgumentException> {
                createMediaItem(displayOrder = 7)
            }
            assertEquals("Display order must be between 1 and 6", exception.message)
        }

        @Test
        fun `should throw exception when mediaUrl is blank`() {
            val exception = assertThrows<IllegalArgumentException> {
                createMediaItem(mediaUrl = "  ")
            }
            assertEquals("Media URL is required", exception.message)
        }

        @Test
        fun `should create media item successfully with valid displayOrder range`() {
            for (order in 1..6) {
                val mediaItem = createMediaItem(displayOrder = order)
                assertEquals(order, mediaItem.displayOrder)
            }
        }

        @Test
        fun `should create media item with null thumbnailUrl`() {
            val item = createMediaItem(thumbnailUrl = null)
            assertNull(item.thumbnailUrl)
        }

        @Test
        fun `should create media item with thumbnailUrl`() {
            val item = createMediaItem(thumbnailUrl = "https://example.com/thumb.jpg")
            assertEquals("https://example.com/thumb.jpg", item.thumbnailUrl)
        }
    }

    @Nested
    inner class `PresignedUploadRequest validation` {

        @Test
        fun `should throw exception when displayOrder is out of range`() {
            val exception = assertThrows<IllegalArgumentException> {
                PresignedUploadRequest(
                    fileName = "file.jpg",
                    contentType = "image/jpeg",
                    fileSizeBytes = 1_000_000L,
                    displayOrder = 0
                )
            }
            assertEquals("Display order must be between 1 and 6", exception.message)
        }

        @Test
        fun `should create request successfully with valid data`() {
            val request = PresignedUploadRequest(
                fileName = "file.jpg",
                contentType = "image/png",
                fileSizeBytes = 2_000_000L,
                displayOrder = 3,
                isPrimary = true
            )
            assertEquals("image/png", request.contentType)
            assertEquals(2_000_000L, request.fileSizeBytes)
            assertEquals(3, request.displayOrder)
            assertTrue(request.isPrimary)
        }
    }

    @Nested
    inner class `ConfirmUploadRequest validation` {

        @Test
        fun `should throw exception when displayOrder is out of range`() {
            val exception = assertThrows<IllegalArgumentException> {
                ConfirmUploadRequest(objectKey = "photos/uid/abc.jpg", displayOrder = 10)
            }
            assertEquals("Display order must be between 1 and 6", exception.message)
        }

        @Test
        fun `should throw exception when objectKey is blank`() {
            val exception = assertThrows<IllegalArgumentException> {
                ConfirmUploadRequest(objectKey = "  ", displayOrder = 1)
            }
            assertEquals("Object key is required", exception.message)
        }

        @Test
        fun `should create request successfully with valid data`() {
            val request = ConfirmUploadRequest(
                objectKey = "photos/uid/abc.jpg",
                displayOrder = 2,
                isPrimary = false
            )
            assertEquals("photos/uid/abc.jpg", request.objectKey)
            assertEquals(2, request.displayOrder)
            assertFalse(request.isPrimary)
        }
    }

    @Nested
    inner class `UserMediaCollection behaviour` {

        @Test
        fun `hasMinimumMedia should return true when count is 3 or more`() {
            val collection = UserMediaCollection(
                userId = "user-123",
                media = createMediaList(3),
                totalCount = 3
            )
            assertTrue(collection.hasMinimumMedia())
        }

        @Test
        fun `hasMinimumMedia should return false when count is less than 3`() {
            val collection = UserMediaCollection(
                userId = "user-123",
                media = createMediaList(2),
                totalCount = 2
            )
            assertFalse(collection.hasMinimumMedia())
        }

        @Test
        fun `hasReachedMaximum should return true when count is 6`() {
            val collection = UserMediaCollection(
                userId = "user-123",
                media = createMediaList(6),
                totalCount = 6
            )
            assertTrue(collection.hasReachedMaximum())
        }

        @Test
        fun `hasReachedMaximum should return false when count is less than 6`() {
            val collection = UserMediaCollection(
                userId = "user-123",
                media = createMediaList(5),
                totalCount = 5
            )
            assertFalse(collection.hasReachedMaximum())
        }

        @Test
        fun `getPrimaryMedia should return primary media item`() {
            val primary = createMediaItem(id = 2L, displayOrder = 2, isPrimary = true)
            val collection = UserMediaCollection(
                userId = "user-123",
                media = listOf(
                    createMediaItem(id = 1L, displayOrder = 1, isPrimary = false),
                    primary,
                    createMediaItem(id = 3L, displayOrder = 3, isPrimary = false)
                ),
                totalCount = 3
            )
            assertEquals(primary, collection.getPrimaryMedia())
        }

        @Test
        fun `getPrimaryMedia should return null when no primary exists`() {
            val collection = UserMediaCollection(
                userId = "user-123",
                media = listOf(
                    createMediaItem(id = 1L, displayOrder = 1, isPrimary = false),
                    createMediaItem(id = 2L, displayOrder = 2, isPrimary = false)
                ),
                totalCount = 2
            )
            assertNull(collection.getPrimaryMedia())
        }

        @Test
        fun `isValidCollection should return true for counts 3-6`() {
            for (count in 3..6) {
                val collection = UserMediaCollection(
                    userId = "user-123",
                    media = createMediaList(count),
                    totalCount = count
                )
                assertTrue(collection.isValidCollection(), "Failed for count: $count")
            }
        }

        @Test
        fun `isValidCollection should return false when count is less than 3`() {
            val collection = UserMediaCollection(
                userId = "user-123",
                media = createMediaList(2),
                totalCount = 2
            )
            assertFalse(collection.isValidCollection())
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun createMediaItem(
        id: Long = 1L,
        mediaUrl: String = "https://example.com/photo.jpg",
        thumbnailUrl: String? = null,
        mediaType: MediaType = MediaType.PHOTO,
        displayOrder: Int = 1,
        isPrimary: Boolean = false
    ): UserMediaItem = UserMediaItem(
        id           = id,
        userId  = UUID.randomUUID().toString(),
        mediaUrl     = mediaUrl,
        thumbnailUrl = thumbnailUrl,
        mediaType    = mediaType,
        displayOrder = displayOrder,
        isPrimary    = isPrimary,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private fun createMediaList(count: Int): List<UserMediaItem> =
        (1..count).map { index -> createMediaItem(id = index.toLong(), displayOrder = index) }
}
