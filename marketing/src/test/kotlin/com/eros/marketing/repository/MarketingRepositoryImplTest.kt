package com.eros.marketing.repository

import com.eros.database.dbQuery
import com.eros.marketing.models.UserMarketingConsent
import com.eros.marketing.tables.UserMarketingConsent as UserMarketingConsentTable
import com.eros.users.table.Users
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MarketingRepositoryImplTest {

    companion object {
        @Container
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("test_db")
            withUsername("test_user")
            withPassword("test_password")
        }
    }

    private lateinit var repository: MarketingRepositoryImpl
    private val fixedInstant = Instant.parse("2024-01-15T10:00:00Z")

    @BeforeAll
    fun setup() {
        Database.connect(
            url = postgresContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgresContainer.username,
            password = postgresContainer.password
        )

        transaction {
            SchemaUtils.create(Users, UserMarketingConsentTable)
        }
    }

    @BeforeEach
    fun setupEach() {
        repository = MarketingRepositoryImpl()

        transaction {
            UserMarketingConsentTable.deleteAll()
            Users.deleteAll()

            // Create test users
            createTestUser("user1")
            createTestUser("user2")
            createTestUser("user3")
        }
    }

    @AfterAll
    fun tearDown() {
        transaction {
            SchemaUtils.drop(UserMarketingConsentTable, Users)
        }
    }

    // -------------------------------------------------------------------------
    // Helper functions
    // -------------------------------------------------------------------------

    private fun createTestUser(userId: String) {
        transaction {
            Users.insert {
                it[Users.userId] = userId
                it[firstName] = "Test"
                it[lastName] = "User"
                it[email] = "$userId@example.com"
                it[heightCm] = 170
                it[dateOfBirth] = LocalDate.of(1990, 1, 1)
                it[city] = "London"
                it[educationLevel] = "Bachelor"
                it[gender] = "Male"
                it[occupation] = "Engineer"
                it[profileStatus] = "ACTIVE"
                it[eloScore] = 1000
                it[photoValidationStatus] = "UNVALIDATED"
                it[role] = "USER"
                it[profileCompleteness] = 50
                it[coordinatesLongitude] = 0.0
                it[coordinatesLatitude] = 0.0
                it[interests] = listOf("Test")
                it[traits] = listOf("Test")
                it[preferredLanguage] = "ENGLISH"
                it[spokenLanguages] = listOf("ENGLISH")
                it[ethnicity] = listOf("OTHER")
                it[createdAt] = fixedInstant
                it[updatedAt] = fixedInstant
            }
        }
    }

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
    // CRUD operations tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `create function` {

        @Test
        fun `should create new consent record`() = runTest {
            val consent = createTestConsent(userId = "user1", marketingConsent = true)

            val created = dbQuery {
                repository.create(consent)
            }

            assertNotNull(created)
            assertEquals("user1", created.userId)
            assertTrue(created.marketingConsent)
            assertEquals(fixedInstant, created.createdAt)
            assertEquals(fixedInstant, created.updatedAt)
        }

        @Test
        fun `should create consent with false value`() = runTest {
            val consent = createTestConsent(userId = "user1", marketingConsent = false)

            val created = dbQuery { repository.create(consent) }

            assertNotNull(created)
            assertEquals("user1", created.userId)
            assertFalse(created.marketingConsent)
        }
    }

    @Nested
    inner class `findById function` {

        @Test
        fun `should return consent record when found`() = runTest {
            val consent = createTestConsent(userId = "user1", marketingConsent = true)
            dbQuery { repository.create(consent) }

            val found = dbQuery { repository.findById("user1") }

            assertNotNull(found)
            assertEquals("user1", found.userId)
            assertTrue(found.marketingConsent)
        }

        @Test
        fun `should return null when record does not exist`() = runTest {
            val found = dbQuery { repository.findById("nonexistent") }

            assertNull(found)
        }
    }

    @Nested
    inner class `update function` {

        @Test
        fun `should update existing consent record`() = runTest {
            val consent = createTestConsent(userId = "user1", marketingConsent = false)
            dbQuery { repository.create(consent) }

            val updated = consent.copy(marketingConsent = true, updatedAt = fixedInstant.plusSeconds(100))
            val result = dbQuery { repository.update("user1", updated) }

            assertNotNull(result)
            assertEquals("user1", result.userId)
            assertTrue(result.marketingConsent)
            assertEquals(fixedInstant.plusSeconds(100), result.updatedAt)
        }

        @Test
        fun `should return null when updating non-existent record`() = runTest {
            val consent = createTestConsent(userId = "nonexistent", marketingConsent = true)

            val result = dbQuery { repository.update("nonexistent", consent) }

            assertNull(result)
        }
    }

    @Nested
    inner class `delete function` {

        @Test
        fun `should delete existing consent record`() = runTest {
            val consent = createTestConsent(userId = "user1", marketingConsent = true)
            dbQuery { repository.create(consent) }

            val deleted = dbQuery { repository.delete("user1") }

            assertEquals(1, deleted)
            assertNull(dbQuery { repository.findById("user1") })
        }

        @Test
        fun `should return zero when deleting non-existent record`() = runTest {
            val deleted = dbQuery { repository.delete("nonexistent") }

            assertEquals(0, deleted)
        }
    }

    @Nested
    inner class `findAll function` {

        @Test
        fun `should return all consent records`() = runTest {
            dbQuery {
                repository.create(createTestConsent(userId = "user1", marketingConsent = true))
                repository.create(createTestConsent(userId = "user2", marketingConsent = false))
                repository.create(createTestConsent(userId = "user3", marketingConsent = true))
            }

            val all = dbQuery { repository.findAll() }

            assertEquals(3, all.size)
        }

        @Test
        fun `should return empty list when no records exist`() = runTest {
            val all = dbQuery { repository.findAll() }

            assertTrue(all.isEmpty())
        }
    }

    // -------------------------------------------------------------------------
    // Marketing-specific query methods tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `findAllConsented function` {

        @Test
        fun `should return only users who consented to marketing`() = runTest {
            dbQuery {
                repository.create(createTestConsent(userId = "user1", marketingConsent = true))
                repository.create(createTestConsent(userId = "user2", marketingConsent = false))
                repository.create(createTestConsent(userId = "user3", marketingConsent = true))
            }

            val consented = dbQuery { repository.findAllConsented() }

            assertEquals(2, consented.size)
            assertTrue(consented.all { it.marketingConsent })
            assertTrue(consented.any { it.userId == "user1" })
            assertTrue(consented.any { it.userId == "user3" })
            assertFalse(consented.any { it.userId == "user2" })
        }

        @Test
        fun `should return empty list when no users consented`() = runTest {
            dbQuery {
                repository.create(createTestConsent(userId = "user1", marketingConsent = false))
                repository.create(createTestConsent(userId = "user2", marketingConsent = false))
            }

            val consented = dbQuery { repository.findAllConsented() }

            assertTrue(consented.isEmpty())
        }
    }

    @Nested
    inner class `countConsented function` {

        @Test
        fun `should count users who consented to marketing`() = runTest {
            dbQuery {
                repository.create(createTestConsent(userId = "user1", marketingConsent = true))
                repository.create(createTestConsent(userId = "user2", marketingConsent = false))
                repository.create(createTestConsent(userId = "user3", marketingConsent = true))
            }

            val count = dbQuery { repository.countConsented() }

            assertEquals(2L, count)
        }

        @Test
        fun `should return zero when no users consented`() = runTest {
            dbQuery { repository.create(createTestConsent(userId = "user1", marketingConsent = false)) }

            val count = dbQuery { repository.countConsented() }

            assertEquals(0L, count)
        }
    }

    // -------------------------------------------------------------------------
    // Upsert function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `upsert function` {

        @Test
        fun `should insert new record when none exists`() = runTest {
            val consent = createTestConsent(userId = "user1", marketingConsent = true)

            val result = dbQuery { repository.upsert(consent) }

            assertNotNull(result)
            assertEquals("user1", result.userId)
            assertTrue(result.marketingConsent)
            assertEquals(fixedInstant, result.createdAt)
            assertEquals(fixedInstant, result.updatedAt)

            // Verify it was actually persisted
            val found = dbQuery { repository.findById("user1") }
            assertNotNull(found)
            assertTrue(found.marketingConsent)
        }

        @Test
        fun `should update existing record when one exists`() = runTest {
            // Create initial record with consent = false
            val initial = createTestConsent(userId = "user1", marketingConsent = false)
            dbQuery { repository.create(initial) }

            // Upsert with consent = true
            val laterInstant = Instant.parse("2024-01-15T12:00:00Z")
            val updated = createTestConsent(
                userId = "user1",
                marketingConsent = true,
                createdAt = fixedInstant,
                updatedAt = laterInstant
            )

            val result = dbQuery { repository.upsert(updated) }

            assertNotNull(result)
            assertEquals("user1", result.userId)
            assertTrue(result.marketingConsent)
            assertEquals(fixedInstant, result.createdAt) // createdAt should remain unchanged
            assertEquals(laterInstant, result.updatedAt) // updatedAt should be updated

            // Verify only one record exists
            val all = dbQuery { repository.findAll() }
            assertEquals(1, all.size)
        }

        @Test
        fun `should be atomic and prevent race conditions`() = runTest {
            // This test verifies that concurrent upserts don't cause duplicate key errors
            val consent1 = createTestConsent(userId = "user1", marketingConsent = true)
            val consent2 = createTestConsent(userId = "user1", marketingConsent = false)

            // First upsert
            val result1 = dbQuery { repository.upsert(consent1) }
            assertNotNull(result1)

            // Second upsert on same user (simulating race condition)
            val result2 = dbQuery { repository.upsert(consent2) }
            assertNotNull(result2)
            assertFalse(result2.marketingConsent)

            // Verify only one record exists
            val all = dbQuery { repository.findAll() }
            assertEquals(1, all.size)
            assertEquals("user1", all[0].userId)
            assertFalse(all[0].marketingConsent)
        }

        @Test
        fun `should handle multiple users with upsert`() = runTest {
            val user1Consent = createTestConsent(userId = "user1", marketingConsent = true)
            val user2Consent = createTestConsent(userId = "user2", marketingConsent = false)

            // Insert both
            dbQuery {
                repository.upsert(user1Consent)
                repository.upsert(user2Consent)
            }

            // Update user1
            val laterInstant = Instant.parse("2024-01-15T12:00:00Z")
            val user1Updated = createTestConsent(
                userId = "user1",
                marketingConsent = false,
                createdAt = fixedInstant,
                updatedAt = laterInstant
            )

            val result = dbQuery { repository.upsert(user1Updated) }

            assertNotNull(result)
            assertFalse(result.marketingConsent)

            // Verify both records exist with correct values
            val all = dbQuery { repository.findAll() }
            assertEquals(2, all.size)

            val user1 = all.find { it.userId == "user1" }
            assertNotNull(user1)
            assertFalse(user1.marketingConsent)

            val user2 = all.find { it.userId == "user2" }
            assertNotNull(user2)
            assertFalse(user2.marketingConsent)
        }
    }
}
