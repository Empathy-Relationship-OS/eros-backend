package com.eros.matching.repository

import com.eros.database.dbQuery
import com.eros.matching.models.DailyBatch
import com.eros.matching.tables.UserDailyBatches
import com.eros.users.table.Users
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.*
import org.jetbrains.exposed.v1.jdbc.insert
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DailyBatchRepositoryImplTest {

    companion object {
        @Container
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("test_db")
            withUsername("test_user")
            withPassword("test_password")
        }
    }

    private lateinit var repository: DailyBatchRepositoryImpl
    private val fixedInstant = Instant.parse("2024-01-15T10:00:00Z")
    private val testDate = LocalDate.of(2024, 1, 15)
    private lateinit var testClock: TestClock

    /**
     * A mutable clock for testing that allows advancing time deterministically.
     */
    private class TestClock(private var instant: Instant, private val zone: ZoneId = ZoneId.of("UTC")) : Clock() {
        override fun instant(): Instant = instant
        override fun getZone(): ZoneId = zone
        override fun withZone(zone: ZoneId): Clock = TestClock(instant, zone)

        fun advanceBy(seconds: Long) {
            instant = instant.plusSeconds(seconds)
        }
    }

    @BeforeAll
    fun setup() {
        Database.connect(
            url = postgresContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgresContainer.username,
            password = postgresContainer.password
        )

        transaction {
            SchemaUtils.create(Users, UserDailyBatches)
        }
    }

    @BeforeEach
    fun setupEach() {
        testClock = TestClock(fixedInstant)
        repository = DailyBatchRepositoryImpl(testClock)

        transaction {
            UserDailyBatches.deleteAll()
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
            SchemaUtils.drop(UserDailyBatches, Users)
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

    private fun createDailyBatch(
        userId: String,
        batchDate: LocalDate,
        batchCount: Int = 1,
        createdAt: Instant = fixedInstant,
        updatedAt: Instant = fixedInstant
    ): DailyBatch = runBlocking {
        dbQuery {
            repository.create(
                DailyBatch(
                    userId = userId,
                    batchDate = batchDate,
                    batchCount = batchCount,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            )
        }
    }

    // -------------------------------------------------------------------------
    // create function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `create function` {

        @Test
        fun `should create daily batch with correct fields`() = runTest {
            val dailyBatch = dbQuery {
                repository.create(
                    DailyBatch(
                        userId = "user1",
                        batchDate = testDate,
                        batchCount = 1,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant
                    )
                )
            }

            assertEquals("user1", dailyBatch.userId)
            assertEquals(testDate, dailyBatch.batchDate)
            assertEquals(1, dailyBatch.batchCount)
            assertEquals(fixedInstant, dailyBatch.createdAt)
            assertEquals(fixedInstant, dailyBatch.updatedAt)
        }

        @Test
        fun `should create daily batch with batchCount of 0`() = runTest {
            val dailyBatch = createDailyBatch("user1", testDate, batchCount = 0)

            assertEquals(0, dailyBatch.batchCount)
        }

        @Test
        fun `should create daily batch with batchCount of 3`() = runTest {
            val dailyBatch = createDailyBatch("user1", testDate, batchCount = 3)

            assertEquals(3, dailyBatch.batchCount)
        }

        @Test
        fun `should enforce unique user and date combination`() {
            runTest {
                createDailyBatch("user1", testDate, batchCount = 1)

                assertFails {
                    createDailyBatch("user1", testDate, batchCount = 2)
                }
            }
        }

        @Test
        fun `should allow same user on different dates`() = runTest {
            val batch1 = createDailyBatch("user1", testDate, batchCount = 1)
            val batch2 = createDailyBatch("user1", testDate.plusDays(1), batchCount = 1)

            assertEquals("user1", batch1.userId)
            assertEquals("user1", batch2.userId)
            assertNotEquals(batch1.batchDate, batch2.batchDate)
        }

        @Test
        fun `should allow different users on same date`() = runTest {
            val batch1 = createDailyBatch("user1", testDate, batchCount = 1)
            val batch2 = createDailyBatch("user2", testDate, batchCount = 1)

            assertEquals(testDate, batch1.batchDate)
            assertEquals(testDate, batch2.batchDate)
            assertNotEquals(batch1.userId, batch2.userId)
        }
    }

    // -------------------------------------------------------------------------
    // findByUserAndDate function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `findByUserAndDate function` {

        @Test
        fun `should find daily batch by user and date`() = runTest {
            createDailyBatch("user1", testDate, batchCount = 2)

            val found = dbQuery {
                repository.findByUserAndDate("user1", testDate)
            }

            assertNotNull(found)
            assertEquals("user1", found.userId)
            assertEquals(testDate, found.batchDate)
            assertEquals(2, found.batchCount)
        }

        @Test
        fun `should return null when no batch exists for user and date`() = runTest {
            val found = dbQuery {
                repository.findByUserAndDate("user1", testDate)
            }

            assertNull(found)
        }

        @Test
        fun `should return null when user exists but different date`() = runTest {
            createDailyBatch("user1", testDate, batchCount = 1)

            val found = dbQuery {
                repository.findByUserAndDate("user1", testDate.plusDays(1))
            }

            assertNull(found)
        }

        @Test
        fun `should return null when date exists but different user`() = runTest {
            createDailyBatch("user1", testDate, batchCount = 1)

            val found = dbQuery {
                repository.findByUserAndDate("user2", testDate)
            }

            assertNull(found)
        }

        @Test
        fun `should find correct batch among multiple batches`() = runTest {
            createDailyBatch("user1", testDate, batchCount = 1)
            createDailyBatch("user1", testDate.plusDays(1), batchCount = 2)
            createDailyBatch("user2", testDate, batchCount = 3)

            val found = dbQuery {
                repository.findByUserAndDate("user1", testDate)
            }

            assertNotNull(found)
            assertEquals("user1", found.userId)
            assertEquals(testDate, found.batchDate)
            assertEquals(1, found.batchCount)
        }
    }

    // -------------------------------------------------------------------------
    // update function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `update function` {

        @Test
        fun `should update batch count successfully`() = runTest {
            val batch = createDailyBatch("user1", testDate, batchCount = 1)
            val newUpdatedAt = fixedInstant.plusSeconds(3600)

            val updated = dbQuery {
                repository.update(batch.copy(batchCount = 2, updatedAt = newUpdatedAt))
            }

            assertNotNull(updated)
            assertEquals(2, updated.batchCount)
            assertEquals(newUpdatedAt, updated.updatedAt)
            assertEquals(fixedInstant, updated.createdAt) // createdAt unchanged
        }

        @Test
        fun `should update updatedAt timestamp on update`() = runTest {
            val batch = createDailyBatch("user1", testDate, batchCount = 1)
            val newUpdatedAt = fixedInstant.plusSeconds(7200)

            val updated = dbQuery {
                repository.update(batch.copy(updatedAt = newUpdatedAt))
            }

            assertNotNull(updated)
            assertEquals(fixedInstant, updated.createdAt)
            assertEquals(newUpdatedAt, updated.updatedAt)
        }

        @Test
        fun `should return null when updating non-existent batch`() = runTest {
            val result = dbQuery {
                repository.update(
                    DailyBatch(
                        userId = "nonexistent",
                        batchDate = testDate,
                        batchCount = 1,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant
                    )
                )
            }

            assertNull(result)
        }

        @Test
        fun `should update only specified batch`() = runTest {
            val batch1 = createDailyBatch("user1", testDate, batchCount = 1)
            createDailyBatch("user2", testDate, batchCount = 1)

            dbQuery {
                repository.update(batch1.copy(batchCount = 3))
            }

            val updated1 = dbQuery { repository.findByUserAndDate("user1", testDate) }
            val updated2 = dbQuery { repository.findByUserAndDate("user2", testDate) }

            assertNotNull(updated1)
            assertNotNull(updated2)
            assertEquals(3, updated1.batchCount)
            assertEquals(1, updated2.batchCount) // unchanged
        }
    }

    // -------------------------------------------------------------------------
    // incrementBatchCount function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `incrementBatchCount function` {

        @Test
        fun `should create new batch with count 1 when none exists`() = runTest {
            val batch = dbQuery {
                repository.incrementBatchCount("user1", testDate)
            }

            assertNotNull(batch)
            assertEquals("user1", batch.userId)
            assertEquals(testDate, batch.batchDate)
            assertEquals(1, batch.batchCount)
        }

        @Test
        fun `should increment existing batch count`() = runTest {
            createDailyBatch("user1", testDate, batchCount = 1)

            val batch = dbQuery {
                repository.incrementBatchCount("user1", testDate)
            }

            assertEquals(2, batch.batchCount)
        }

        @Test
        fun `should increment from 2 to 3`() = runTest {
            createDailyBatch("user1", testDate, batchCount = 2)

            val batch = dbQuery {
                repository.incrementBatchCount("user1", testDate)
            }

            assertEquals(3, batch.batchCount)
        }

        @Test
        fun `should throw exception when trying to increment beyond 3`() = runTest {
            createDailyBatch("user1", testDate, batchCount = 3)

            assertFails {
                dbQuery {
                    repository.incrementBatchCount("user1", testDate)
                }
            }
        }


        @Test
        fun `should update updatedAt when incrementing`() = runTest {
            val batch = createDailyBatch("user1", testDate, batchCount = 1)

            // Advance the clock by 1 hour to ensure timestamp difference
            testClock.advanceBy(3600)

            val incremented = dbQuery {
                repository.incrementBatchCount("user1", testDate)
            }

            assertEquals(batch.createdAt, incremented.createdAt) // createdAt unchanged
            assertTrue(incremented.updatedAt.isAfter(batch.updatedAt),
                "updatedAt should be after the original timestamp (original: ${batch.updatedAt}, updated: ${incremented.updatedAt})")
            assertEquals(fixedInstant.plusSeconds(3600), incremented.updatedAt) // Should match advanced clock
        }

        @Test
        fun `should handle multiple users incrementing on same date`() = runTest {
            val batch1 = dbQuery {
                repository.incrementBatchCount("user1", testDate)
            }
            val batch2 = dbQuery {
                repository.incrementBatchCount("user2", testDate)
            }

            assertEquals(1, batch1.batchCount)
            assertEquals(1, batch2.batchCount)
            assertEquals(testDate, batch1.batchDate)
            assertEquals(testDate, batch2.batchDate)
        }

        @Test
        fun `should handle same user incrementing on different dates`() = runTest {
            val batch1 = dbQuery {
                repository.incrementBatchCount("user1", testDate)
            }
            val batch2 = dbQuery {
                repository.incrementBatchCount("user1", testDate.plusDays(1))
            }

            assertEquals(1, batch1.batchCount)
            assertEquals(1, batch2.batchCount)
            assertEquals(testDate, batch1.batchDate)
            assertEquals(testDate.plusDays(1), batch2.batchDate)
        }

        @Test
        fun `should handle concurrent increments atomically without race conditions`() = runTest {
            // This test verifies the upsert implementation prevents race conditions
            // by simulating 3 concurrent increment requests for the same user/date
            val concurrentRequests = 3

            val results = (1..concurrentRequests).map {
                async {
                    dbQuery {
                        repository.incrementBatchCount("user1", testDate)
                    }
                }
            }.awaitAll()

            // All operations should succeed
            assertEquals(concurrentRequests, results.size)

            // Final count should be exactly 3 (no lost updates)
            val finalCount = dbQuery {
                repository.getBatchCount("user1", testDate)
            }
            assertEquals(concurrentRequests, finalCount)

            // Verify no duplicate primary key violations occurred
            val batch = dbQuery {
                repository.findByUserAndDate("user1", testDate)
            }
            assertNotNull(batch)
            assertEquals(concurrentRequests, batch.batchCount)
        }

        @Test
        fun `should handle concurrent increments for multiple users simultaneously`() = runTest {
            // Test concurrent operations across different users
            val users = listOf("user1", "user2", "user3")
            val incrementsPerUser = 2

            val allResults = users.flatMap { userId ->
                (1..incrementsPerUser).map {
                    async {
                        dbQuery {
                            repository.incrementBatchCount(userId, testDate)
                        }
                    }
                }
            }.awaitAll()

            // All operations should succeed
            assertEquals(users.size * incrementsPerUser, allResults.size)

            // Each user should have exactly 2 increments
            users.forEach { userId ->
                val count = dbQuery { repository.getBatchCount(userId, testDate) }
                assertEquals(incrementsPerUser, count, "User $userId should have $incrementsPerUser increments")
            }
        }
    }

    // -------------------------------------------------------------------------
    // getBatchCount function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `getBatchCount function` {

        @Test
        fun `should return 0 when no batch exists`() = runTest {
            val count = dbQuery {
                repository.getBatchCount("user1", testDate)
            }

            assertEquals(0, count)
        }

        @Test
        fun `should return correct batch count when batch exists`() = runTest {
            createDailyBatch("user1", testDate, batchCount = 2)

            val count = dbQuery {
                repository.getBatchCount("user1", testDate)
            }

            assertEquals(2, count)
        }

        @Test
        fun `should return 0 for different date`() = runTest {
            createDailyBatch("user1", testDate, batchCount = 2)

            val count = dbQuery {
                repository.getBatchCount("user1", testDate.plusDays(1))
            }

            assertEquals(0, count)
        }

        @Test
        fun `should return 0 for different user`() = runTest {
            createDailyBatch("user1", testDate, batchCount = 2)

            val count = dbQuery {
                repository.getBatchCount("user2", testDate)
            }

            assertEquals(0, count)
        }

        @Test
        fun `should return correct count for multiple batches`() = runTest {
            createDailyBatch("user1", testDate, batchCount = 1)
            createDailyBatch("user1", testDate.plusDays(1), batchCount = 2)
            createDailyBatch("user2", testDate, batchCount = 3)

            val count1 = dbQuery { repository.getBatchCount("user1", testDate) }
            val count2 = dbQuery { repository.getBatchCount("user1", testDate.plusDays(1)) }
            val count3 = dbQuery { repository.getBatchCount("user2", testDate) }

            assertEquals(1, count1)
            assertEquals(2, count2)
            assertEquals(3, count3)
        }
    }

    // -------------------------------------------------------------------------
    // Integration tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `integration tests` {

        @Test
        fun `should handle full daily batch lifecycle`() = runTest {
            // Day 1: User fetches 3 batches
            val batch1 = dbQuery { repository.incrementBatchCount("user1", testDate) }
            assertEquals(1, batch1.batchCount)

            val batch2 = dbQuery { repository.incrementBatchCount("user1", testDate) }
            assertEquals(2, batch2.batchCount)

            val batch3 = dbQuery { repository.incrementBatchCount("user1", testDate) }
            assertEquals(3, batch3.batchCount)

            // Verify limit reached
            val finalCount = dbQuery { repository.getBatchCount("user1", testDate) }
            assertEquals(3, finalCount)

            // Attempting to fetch 4th batch should fail
            assertFails {
                dbQuery { repository.incrementBatchCount("user1", testDate) }
            }

            // Day 2: User can fetch batches again
            val nextDay = testDate.plusDays(1)
            val nextDayBatch = dbQuery { repository.incrementBatchCount("user1", nextDay) }
            assertEquals(1, nextDayBatch.batchCount)
        }

        @Test
        fun `should handle multiple users with different batch counts`() = runTest {
            // User1: 2 batches
            dbQuery { repository.incrementBatchCount("user1", testDate) }
            dbQuery { repository.incrementBatchCount("user1", testDate) }

            // User2: 1 batch
            dbQuery { repository.incrementBatchCount("user2", testDate) }

            // User3: 3 batches
            dbQuery { repository.incrementBatchCount("user3", testDate) }
            dbQuery { repository.incrementBatchCount("user3", testDate) }
            dbQuery { repository.incrementBatchCount("user3", testDate) }

            val count1 = dbQuery { repository.getBatchCount("user1", testDate) }
            val count2 = dbQuery { repository.getBatchCount("user2", testDate) }
            val count3 = dbQuery { repository.getBatchCount("user3", testDate) }

            assertEquals(2, count1)
            assertEquals(1, count2)
            assertEquals(3, count3)
        }

        @Test
        fun `should track batches across multiple dates`() = runTest {
            val day1 = testDate
            val day2 = testDate.plusDays(1)
            val day3 = testDate.plusDays(2)

            // Day 1: 2 batches
            dbQuery { repository.incrementBatchCount("user1", day1) }
            dbQuery { repository.incrementBatchCount("user1", day1) }

            // Day 2: 3 batches
            dbQuery { repository.incrementBatchCount("user1", day2) }
            dbQuery { repository.incrementBatchCount("user1", day2) }
            dbQuery { repository.incrementBatchCount("user1", day2) }

            // Day 3: 1 batch
            dbQuery { repository.incrementBatchCount("user1", day3) }

            assertEquals(2, dbQuery { repository.getBatchCount("user1", day1) })
            assertEquals(3, dbQuery { repository.getBatchCount("user1", day2) })
            assertEquals(1, dbQuery { repository.getBatchCount("user1", day3) })
        }

        @Test
        fun `should handle update and increment operations together`() = runTest {
            // Create initial batch
            val batch = createDailyBatch("user1", testDate, batchCount = 1)

            // Manual update
            val updated = dbQuery {
                repository.update(batch.copy(batchCount = 2))
            }
            assertNotNull(updated)
            assertEquals(2, updated.batchCount)

            // Increment from 2 to 3
            val incremented = dbQuery {
                repository.incrementBatchCount("user1", testDate)
            }
            assertEquals(3, incremented.batchCount)

            // Verify final count
            val finalCount = dbQuery {
                repository.getBatchCount("user1", testDate)
            }
            assertEquals(3, finalCount)
        }

        @Test
        fun `should maintain data consistency across operations`() = runTest {
            // Create batches for multiple users and dates
            dbQuery { repository.incrementBatchCount("user1", testDate) }
            dbQuery { repository.incrementBatchCount("user1", testDate.plusDays(1)) }
            dbQuery { repository.incrementBatchCount("user2", testDate) }
            dbQuery { repository.incrementBatchCount("user2", testDate) }

            // Update one specific batch
            val user1Today = dbQuery { repository.findByUserAndDate("user1", testDate) }
            assertNotNull(user1Today)
            dbQuery { repository.update(user1Today.copy(batchCount = 3)) }

            // Verify all batches have correct values
            assertEquals(3, dbQuery { repository.getBatchCount("user1", testDate) })
            assertEquals(1, dbQuery { repository.getBatchCount("user1", testDate.plusDays(1)) })
            assertEquals(2, dbQuery { repository.getBatchCount("user2", testDate) })
        }
    }
}
