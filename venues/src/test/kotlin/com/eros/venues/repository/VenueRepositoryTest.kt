package com.eros.venues.repository

import com.eros.database.dbQuery
import com.eros.users.table.Cities
import com.eros.venues.models.createTestVenue
import com.eros.venues.table.Venues
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Assert.assertThrows
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VenueRepositoryTest {


    @Nested
    inner class `CREATE Venue` {

        @Test
        fun `successful venue creation`() = runTest {
            val venue = createTestVenue(cityId = cityId)
            val createdVenue = dbQuery { repository.create(venue) }
            assertEquals(venue.name, createdVenue.name)
        }

        @Test
        fun `created venue has correct all fields`() = runTest {
            val venue = createTestVenue(cityId = cityId)
            val createdVenue = dbQuery { repository.create(venue) }

            assertEquals(venue.name, createdVenue.name)
            assertEquals(venue.description, createdVenue.description)
            assertEquals(venue.address, createdVenue.address)
            assertEquals(venue.cityId, createdVenue.cityId)
            assertEquals(venue.latitude, createdVenue.latitude)
            assertEquals(venue.longitude, createdVenue.longitude)
            assertEquals(venue.priceRange, createdVenue.priceRange)
            assertEquals(venue.maxCapacity, createdVenue.maxCapacity)
            assertEquals(venue.reservationRequired, createdVenue.reservationRequired)
            assertEquals(venue.partnerInstructions, createdVenue.partnerInstructions)
            assertEquals(venue.disabledFriendly, createdVenue.disabledFriendly)
            assertEquals(venue.indoorOutdoor, createdVenue.indoorOutdoor)
            assertEquals(venue.parkingAvailable, createdVenue.parkingAvailable)
            assertEquals(venue.websiteUrl, createdVenue.websiteUrl)
            assertEquals(venue.dressCode, createdVenue.dressCode)
            assertEquals(venue.activeFrom, createdVenue.activeFrom)
            assertEquals(venue.activeTo, createdVenue.activeTo)
        }


        @Test
        fun `created venue has timestamps set`() = runTest {
            val venue = createTestVenue(cityId = cityId, createdAt = fixedInstant, updatedAt = fixedInstant)
            val createdVenue = dbQuery { repository.create(venue) }
            assertEquals(fixedInstant, createdVenue.createdAt)
            assertEquals(fixedInstant, createdVenue.updatedAt)
        }

        @Test
        fun `created venue with null optional fields`() = runTest {
            val venue = createTestVenue(cityId = cityId, websiteUrl = null, activeTo = null)
            val createdVenue = dbQuery { repository.create(venue) }
            assertEquals(null, createdVenue.websiteUrl)
            assertEquals(null, createdVenue.activeTo)
        }

        @Test
        fun `created venue with all optional fields populated`() = runTest {
            val venue = createTestVenue(
                cityId = cityId,
                websiteUrl = "https://myvenue.com",
                activeTo = LocalDate.of(2025, 12, 31)
            )
            val createdVenue = dbQuery { repository.create(venue) }
            assertEquals("https://myvenue.com", createdVenue.websiteUrl)
            assertEquals(LocalDate.of(2025, 12, 31), createdVenue.activeTo)
        }

        @Test
        fun `multiple venues can be created`() = runTest {
            val venue1 = createTestVenue(cityId = cityId, name = "Venue One")
            val venue2 = createTestVenue(cityId = cityId, name = "Venue Two")

            val created1 = dbQuery { repository.create(venue1) }
            val created2 = dbQuery { repository.create(venue2) }

            assertNotEquals(created1.venueId, created2.venueId)
            assertEquals("Venue One", created1.name)
            assertEquals("Venue Two", created2.name)
        }

        @Test
        fun `venue creation with invalid city id throws exception`() = runTest {
            val venue = createTestVenue(cityId = 9999L)
            assertThrows<ExposedSQLException> {
                dbQuery { repository.create(venue) }
            }
        }
    }




    // =============================//
    // ========== Setup ============//
    // =============================//

    companion object {
        @Container
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("test_db")
            withUsername("test_user")
            withPassword("test_password")
        }
    }

    private lateinit var repository: VenueRepositoryImpl
    private lateinit var clock: Clock
    private val fixedInstant = Instant.parse("2024-01-15T10:00:00Z")
    private var cityId: Long = 0L

    @BeforeAll
    fun setup() {
        Database.connect(
            url = postgresContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgresContainer.username,
            password = postgresContainer.password
        )

        transaction {
            SchemaUtils.create(Cities, Venues)
        }
    }

    @BeforeEach
    fun setupEach() {
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
        repository = VenueRepositoryImpl(clock)

        transaction {
            Venues.deleteAll()
            Cities.deleteAll()

            Cities.insert {
                it[cityId] = 0L
                it[cityName] = "London"
                it[latitude] = 51.5074
                it[longitude] = -0.1278
                it[createdAt] = fixedInstant
                it[updatedAt] = fixedInstant
            }

        }
    }

    @AfterAll
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Venues, Cities)
        }
    }

}