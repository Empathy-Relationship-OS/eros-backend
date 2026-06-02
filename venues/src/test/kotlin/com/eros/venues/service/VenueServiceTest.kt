package com.eros.venues.service

import com.eros.venues.models.Venue
import com.eros.venues.models.createTestCreateVenueRequest
import com.eros.venues.models.createTestVenue
import com.eros.venues.repository.VenueRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VenueServiceTest {

    private val venueRepository: VenueRepository = mockk()
    private val fixedInstant = Instant.parse("2024-01-15T10:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
    private val service = VenueService(venueRepository, clock)

    @BeforeAll
    fun setup() {
        // Connect to dummy database to allow for dbQuery to work correctly.
        Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )
    }

    @BeforeEach
    fun resetMocks(){
        clearMocks(venueRepository)
    }


    @Nested
    inner class `CREATE Venue` {

        @Test
        fun `successful venue creation maps request correctly`() = runTest {
            val request = createTestCreateVenueRequest()
            val expectedVenue = createTestVenue(
                createdAt = fixedInstant,
                updatedAt = fixedInstant
            )

            coEvery { venueRepository.create(any()) } returns expectedVenue

            val result = service.createVenue(request)

            assertEquals(expectedVenue, result)
        }

        @Test
        fun `venue creation sets createdAt and updatedAt from clock`() = runTest {
            val request = createTestCreateVenueRequest()

            coEvery { venueRepository.create(any()) } answers {
                firstArg<Venue>()
            }

            val result = service.createVenue(request)

            assertEquals(fixedInstant, result.createdAt)
            assertEquals(fixedInstant, result.updatedAt)
        }

        @Test
        fun `venue creation passes correct fields to repository`() = runTest {
            val request = createTestCreateVenueRequest(
                name = "My Venue",
                cityId = 5L,
                maxCapacity = 200
            )

            coEvery { venueRepository.create(any()) } answers { firstArg<Venue>() }

            val result = service.createVenue(request)

            assertEquals("My Venue", result.name)
            assertEquals(5L, result.cityId)
            assertEquals(200, result.maxCapacity)
        }

        @Test
        fun `venue creation calls repository exactly once`() = runTest {
            val request = createTestCreateVenueRequest()
            coEvery { venueRepository.create(any()) } answers { firstArg<Venue>() }

            service.createVenue(request)

            coVerify(exactly = 1) { venueRepository.create(any()) }
        }

        @Test
        fun `venue creation propagates repository exception`() = runTest {
            val request = createTestCreateVenueRequest()
            coEvery { venueRepository.create(any()) } throws RuntimeException("DB error")

            assertThrows<RuntimeException> {
                service.createVenue(request)
            }
        }
    }
}