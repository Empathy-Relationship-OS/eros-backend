package com.eros.marketing.service

import com.eros.common.errors.ForbiddenException
import com.eros.marketing.models.UserMarketingConsent
import com.eros.marketing.repository.MarketingRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for MarketingPreferenceService.
 *
 * Tests business logic for marketing consent CRUD operations.
 */
class MarketingPreferenceServiceTest {

    private lateinit var marketingRepository: MarketingRepository
    private lateinit var marketingPreferenceService: MarketingPreferenceService

    private val fixedInstant = Instant.parse("2024-01-15T10:00:00Z")
    private val fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    @BeforeEach
    fun setup() {
        marketingRepository = mockk()
        marketingPreferenceService = MarketingPreferenceService(
            marketingRepository,
            fixedClock
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // -------------------------------------------------------------------------
    // Helper functions
    // -------------------------------------------------------------------------

    private fun createTestConsent(
        userId: String = "user1",
        marketingConsent: Boolean = false,
        createdAt: Instant = fixedInstant,
        updatedAt: Instant = fixedInstant
    ) = UserMarketingConsent(
        userId = userId,
        marketingConsent = marketingConsent,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    // -------------------------------------------------------------------------
    // findMarketingPreference function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `findMarketingPreference function` {

        @Test
        fun `should return existing consent record when found`() = runTest {
            val consent = createTestConsent(userId = "user1", marketingConsent = true)
            coEvery { marketingRepository.findById("user1") } returns consent

            val result = marketingPreferenceService.findMarketingPreference("user1")

            assertNotNull(result)
            assertEquals("user1", result.userId)
            assertTrue(result.marketingConsent)
            coVerify { marketingRepository.findById("user1") }
        }

        @Test
        fun `should return null when no record exists`() = runTest {
            coEvery { marketingRepository.findById("user1") } returns null

            val result = marketingPreferenceService.findMarketingPreference("user1")

            assertEquals(null, result)
            coVerify { marketingRepository.findById("user1") }
        }
    }

    // -------------------------------------------------------------------------
    // getMarketingPreference function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `getMarketingPreference function` {

        @Test
        fun `should return existing consent record when found`() = runTest {
            val consent = createTestConsent(userId = "user1", marketingConsent = true)
            coEvery { marketingRepository.findById("user1") } returns consent

            val result = marketingPreferenceService.getMarketingPreference("user1")

            assertNotNull(result)
            assertEquals("user1", result.userId)
            assertTrue(result.marketingConsent)
        }

        @Test
        fun `should create and persist default consent when no record exists`() = runTest {
            val defaultConsent = createTestConsent(userId = "user1", marketingConsent = false)

            coEvery { marketingRepository.findById("user1") } returns null
            coEvery { marketingRepository.upsert(any()) } returns defaultConsent

            val result = marketingPreferenceService.getMarketingPreference("user1")

            assertNotNull(result)
            assertEquals("user1", result.userId)
            assertFalse(result.marketingConsent)
            assertEquals(fixedInstant, result.createdAt)
            assertEquals(fixedInstant, result.updatedAt)

            // Verify it was persisted to the database
            coVerify { marketingRepository.upsert(any()) }
        }
    }

    // -------------------------------------------------------------------------
    // createMarketingPreference function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `createMarketingPreference function` {

        @Test
        fun `should create consent record when user creates their own preference`() = runTest {
            val consent = createTestConsent(userId = "user1", marketingConsent = true)
            coEvery { marketingRepository.doesExist("user1") } returns false
            coEvery { marketingRepository.create(any()) } returns consent

            val result = marketingPreferenceService.createMarketingPreference(
                userId = "user1",
                requestingUserId = "user1",
                marketingConsent = true
            )

            assertNotNull(result)
            assertEquals("user1", result.userId)
            assertTrue(result.marketingConsent)
            coVerify { marketingRepository.create(any()) }
        }

        @Test
        fun `should throw ForbiddenException when user tries to create preference for another user`() = runTest {
            val exception = assertThrows<ForbiddenException> {
                marketingPreferenceService.createMarketingPreference(
                    userId = "user1",
                    requestingUserId = "user2",
                    marketingConsent = true
                )
            }

            assertTrue(exception.message!!.contains("You can only create your own marketing preferences"))
            coVerify(exactly = 0) { marketingRepository.create(any()) }
        }

        @Test
        fun `should create consent with false value`() = runTest {
            val consent = createTestConsent(userId = "user1", marketingConsent = false)
            coEvery { marketingRepository.doesExist("user1") } returns false
            coEvery { marketingRepository.create(any()) } returns consent

            val result = marketingPreferenceService.createMarketingPreference(
                userId = "user1",
                requestingUserId = "user1",
                marketingConsent = false
            )

            assertNotNull(result)
            assertEquals("user1", result.userId)
            assertFalse(result.marketingConsent)
        }
    }

    // -------------------------------------------------------------------------
    // updateMarketingPreference function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `updateMarketingPreference function` {

        @Test
        fun `should update existing consent record`() = runTest {
            val updatedConsent = createTestConsent(userId = "user1", marketingConsent = true)

            coEvery { marketingRepository.upsert(any()) } returns updatedConsent

            val result = marketingPreferenceService.updateMarketingPreference(
                userId = "user1",
                requestingUserId = "user1",
                marketingConsent = true
            )

            assertNotNull(result)
            assertEquals("user1", result.userId)
            assertTrue(result.marketingConsent)
            coVerify { marketingRepository.upsert(any()) }
        }

        @Test
        fun `should create new record when none exists (upsert)`() = runTest {
            val newConsent = createTestConsent(userId = "user1", marketingConsent = true)

            coEvery { marketingRepository.upsert(any()) } returns newConsent

            val result = marketingPreferenceService.updateMarketingPreference(
                userId = "user1",
                requestingUserId = "user1",
                marketingConsent = true
            )

            assertNotNull(result)
            assertEquals("user1", result.userId)
            assertTrue(result.marketingConsent)
            coVerify { marketingRepository.upsert(any()) }
        }

        @Test
        fun `should throw ForbiddenException when user tries to update another user's preference`() = runTest {
            val exception = assertThrows<ForbiddenException> {
                marketingPreferenceService.updateMarketingPreference(
                    userId = "user1",
                    requestingUserId = "user2",
                    marketingConsent = true
                )
            }

            assertTrue(exception.message!!.contains("You can only update your own marketing preferences"))
            coVerify(exactly = 0) { marketingRepository.upsert(any()) }
        }

        @Test
        fun `should allow changing consent from true to false`() = runTest {
            val updatedConsent = createTestConsent(userId = "user1", marketingConsent = false)

            coEvery { marketingRepository.upsert(any()) } returns updatedConsent

            val result = marketingPreferenceService.updateMarketingPreference(
                userId = "user1",
                requestingUserId = "user1",
                marketingConsent = false
            )

            assertNotNull(result)
            assertEquals("user1", result.userId)
            assertFalse(result.marketingConsent)
            coVerify { marketingRepository.upsert(any()) }
        }
    }

    // -------------------------------------------------------------------------
    // deleteMarketingPreference function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `deleteMarketingPreference function` {

        @Test
        fun `should delete existing consent record`() = runTest {
            coEvery { marketingRepository.delete("user1") } returns 1

            val result = marketingPreferenceService.deleteMarketingPreference("user1")

            assertTrue(result)
            coVerify { marketingRepository.delete("user1") }
        }

        @Test
        fun `should return false when record does not exist`() = runTest {
            coEvery { marketingRepository.delete("user1") } returns 0

            val result = marketingPreferenceService.deleteMarketingPreference("user1")

            assertFalse(result)
            coVerify { marketingRepository.delete("user1") }
        }
    }

    // -------------------------------------------------------------------------
    // getAllConsentedUsers function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `GetAllConsentedUsers function` {

        @Test
        fun `should return list of users who consented`() = runTest {
            val consented = listOf(
                createTestConsent(userId = "user1", marketingConsent = true),
                createTestConsent(userId = "user2", marketingConsent = true)
            )
            coEvery { marketingRepository.findAllConsented(any(), any()) } returns consented

            val result = marketingPreferenceService.getAllConsentedUsers()

            assertEquals(2, result.size)
            assertEquals("user1", result[0].userId)
            assertEquals("user2", result[1].userId)
            assertTrue(result.all { it.marketingConsent })
        }

        @Test
        fun `should return empty list when no users consented`() = runTest {
            coEvery { marketingRepository.findAllConsented(any(), any()) } returns emptyList()

            val result = marketingPreferenceService.getAllConsentedUsers()

            assertTrue(result.isEmpty())
        }
    }
}
