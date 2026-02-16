package com.eros.users.models

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserMediaTest {

    @Nested
    inner class `UserMediaItem validation` {

        @Test
        fun `should throw exception when displayOrder is less than 1`() {
            val exception = assertThrows<IllegalArgumentException> {
                UserMediaItem(
                    id = 1L,
                    userId = "user-123",
                    mediaUrl = "https://example.com/photo.jpg",
                    mediaType = MediaType.PHOTO,
                    displayOrder = 0,
                    isPrimary = false,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            }
            assertEquals("Display order must be between 1 and 6", exception.message)
        }

        @Test
        fun `should throw exception when displayOrder is greater than 6`() {
            val exception = assertThrows<IllegalArgumentException> {
                UserMediaItem(
                    id = 1L,
                    userId = "user-123",
                    mediaUrl = "https://example.com/photo.jpg",
                    mediaType = MediaType.PHOTO,
                    displayOrder = 7,
                    isPrimary = false,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            }
            assertEquals("Display order must be between 1 and 6", exception.message)
        }

        @Test
        fun `should throw exception when mediaUrl is blank`() {
            val exception = assertThrows<IllegalArgumentException> {
                UserMediaItem(
                    id = 1L,
                    userId = "user-123",
                    mediaUrl = "  ",
                    mediaType = MediaType.PHOTO,
                    displayOrder = 1,
                    isPrimary = false,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            }
            assertEquals("Media URL is required", exception.message)
        }

        @Test
        fun `should create media item successfully with valid displayOrder range`() {
            for (order in 1..6) {
                val mediaItem = UserMediaItem(
                    id = 1L,
                    userId = "user-123",
                    mediaUrl = "https://example.com/photo.jpg",
                    mediaType = MediaType.PHOTO,
                    displayOrder = order,
                    isPrimary = false,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
                assertEquals(order, mediaItem.displayOrder)
            }
        }
    }

    @Nested
    inner class `AddUserMediaRequest validation` {

        @Test
        fun `should throw exception when displayOrder is out of range`() {
            val exception = assertThrows<IllegalArgumentException> {
                AddUserMediaRequest(
                    userId = 1L,
                    mediaUrl = "https://example.com/photo.jpg",
                    mediaType = MediaType.PHOTO,
                    displayOrder = 10
                )
            }
            assertEquals("Display order must be between 1 and 6", exception.message)
        }

        @Test
        fun `should throw exception when mediaUrl is blank`() {
            val exception = assertThrows<IllegalArgumentException> {
                AddUserMediaRequest(
                    userId = 1L,
                    mediaUrl = "",
                    mediaType = MediaType.PHOTO,
                    displayOrder = 1
                )
            }
            assertEquals("Media URL is required", exception.message)
        }

        @Test
        fun `should create request successfully with valid data`() {
            val request = AddUserMediaRequest(
                userId = 1L,
                mediaUrl = "https://example.com/photo.jpg",
                mediaType = MediaType.VIDEO,
                displayOrder = 3,
                isPrimary = true
            )

            assertEquals(1L, request.userId)
            assertEquals("https://example.com/photo.jpg", request.mediaUrl)
            assertEquals(MediaType.VIDEO, request.mediaType)
            assertEquals(3, request.displayOrder)
            assertTrue(request.isPrimary)
        }
    }

    @Nested
    inner class `UpdateUserMediaRequest validation` {

        @Test
        fun `should throw exception when displayOrder is out of range`() {
            val exception = assertThrows<IllegalArgumentException> {
                UpdateUserMediaRequest(displayOrder = 0)
            }
            assertEquals("Display order must be between 1 and 6", exception.message)
        }

        @Test
        fun `should create update request successfully with null fields`() {
            val request = UpdateUserMediaRequest()

            assertNull(request.mediaUrl)
            assertNull(request.displayOrder)
            assertNull(request.isPrimary)
        }

        @Test
        fun `should create update request successfully with valid displayOrder`() {
            val request = UpdateUserMediaRequest(displayOrder = 4)

            assertEquals(4, request.displayOrder)
        }
    }

    @Nested
    inner class UserMediaCollection {

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
        fun `hasReachedMaximum should return true when count is 6 or more`() {
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
        fun `getPrimaryMedia should return first primary media item`() {
            val primaryMedia = createMediaItem(id = 2L, isPrimary = true)
            val collection = UserMediaCollection(
                userId = "user-123",
                media = listOf(
                    createMediaItem(id = 1L, isPrimary = false),
                    primaryMedia,
                    createMediaItem(id = 3L, isPrimary = false)
                ),
                totalCount = 3
            )

            assertEquals(primaryMedia, collection.getPrimaryMedia())
        }

        @Test
        fun `getPrimaryMedia should return null when no primary media exists`() {
            val collection = UserMediaCollection(
                userId = "user-123",
                media = listOf(
                    createMediaItem(id = 1L, isPrimary = false),
                    createMediaItem(id = 2L, isPrimary = false)
                ),
                totalCount = 2
            )

            assertNull(collection.getPrimaryMedia())
        }

        @Test
        fun `isValidCollection should return true when count is between 3 and 6`() {
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

        @Test
        fun `isValidCollection should return false when count is more than 6`() {
            assertThrows<IllegalArgumentException> {
                UserMediaCollection(
                    userId = "user-123",
                    media = createMediaList(7),
                    totalCount = 7
                )
            }
        }
    }

    // Helper functions
    private fun createMediaItem(
        id: Long = 1L,
        userId: String = "user-123",
        mediaUrl: String = "https://example.com/photo.jpg",
        mediaType: MediaType = MediaType.PHOTO,
        displayOrder: Int = 1,
        isPrimary: Boolean = false
    ): UserMediaItem {
        return UserMediaItem(
            id = id,
            userId = userId,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            displayOrder = displayOrder,
            isPrimary = isPrimary,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createMediaList(count: Int): List<UserMediaItem> {
        return (1..count).map { index ->
            createMediaItem(id = index.toLong(), displayOrder = index)
        }
    }
}
