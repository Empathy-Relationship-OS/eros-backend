package com.eros.matching.repository

import com.eros.database.dbQuery
import com.eros.matching.models.Match
import com.eros.matching.tables.Matches
import com.eros.users.table.Users
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MatchRepositoryImplTest {

    companion object {
        @Container
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("test_db")
            withUsername("test_user")
            withPassword("test_password")
        }
    }

    private lateinit var repository: MatchRepositoryImpl
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
            SchemaUtils.create(Users, Matches)
        }
    }

    @BeforeEach
    fun setupEach() {
        repository = MatchRepositoryImpl()

        transaction {
            Matches.deleteAll()
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
            SchemaUtils.drop(Matches, Users)
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

    private fun createMatch(
        user1Id: String,
        user2Id: String,
        liked: Boolean? = null,
        servedAt: Instant? = null,
        createdAt: Instant = fixedInstant,
        updatedAt: Instant = fixedInstant
    ): Match = runBlocking {
        dbQuery {
            repository.create(
                Match(
                    matchId = 0,
                    user1Id = user1Id,
                    user2Id = user2Id,
                    liked = liked ?: false,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    servedAt = servedAt
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
        fun `should create match with correct fields`() = runTest {
            val match = dbQuery {
                repository.create(
                    Match(
                        matchId = 0,
                        user1Id = "user1",
                        user2Id = "user2",
                        liked = false,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant,
                        servedAt = null
                    )
                )
            }

            assertNotNull(match.matchId)
            assertTrue(match.matchId > 0)
            assertEquals("user1", match.user1Id)
            assertEquals("user2", match.user2Id)
            assertFalse(match.liked!!)
            assertEquals(fixedInstant, match.createdAt)
            assertEquals(fixedInstant, match.updatedAt)
            assertNull(match.servedAt)
        }

        @Test
        fun `should auto-increment match IDs`() = runTest {
            val match1 = createMatch("user1", "user2")
            val match2 = createMatch("user1", "user3")

            assertTrue(match2.matchId > match1.matchId)
        }

        @Test
        fun `should create match with liked set to true`() = runTest {
            val match = createMatch("user1", "user2", liked = true)

            assertEquals(true, match.liked)
        }

        @Test
        fun `should create match with servedAt timestamp`() = runTest {
            val servedAt = fixedInstant.plusSeconds(3600)
            val match = createMatch("user1", "user2", servedAt = servedAt)

            assertEquals(servedAt, match.servedAt)
        }

        @Test
        fun `should enforce unique user pair constraint`() {
            runTest {
                createMatch("user1", "user2")

                assertFails {
                    createMatch("user1", "user2")
                }
            }
        }

        @Test
        fun `should allow different user pairs`() = runTest {
            val match1 = createMatch("user1", "user2")
            val match2 = createMatch("user2", "user1")

            assertNotNull(match1)
            assertNotNull(match2)
            assertNotEquals(match1.matchId, match2.matchId)
        }
    }

    // -------------------------------------------------------------------------
    // findById function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `findById function` {

        @Test
        fun `should return match when found`() = runTest {
            val createdMatch = createMatch("user1", "user2")

            val foundMatch = dbQuery {
                repository.findById(createdMatch.matchId)
            }

            assertNotNull(foundMatch)
            assertEquals(createdMatch.matchId, foundMatch.matchId)
            assertEquals(createdMatch.user1Id, foundMatch.user1Id)
            assertEquals(createdMatch.user2Id, foundMatch.user2Id)
        }

        @Test
        fun `should return null when match not found`() = runTest {
            val result = dbQuery {
                repository.findById(999L)
            }

            assertNull(result)
        }

        @Test
        fun `should return correct match among multiple matches`() = runTest {
            val match1 = createMatch("user1", "user2")
            createMatch("user1", "user3")
            createMatch("user2", "user3")

            val foundMatch = dbQuery {
                repository.findById(match1.matchId)
            }

            assertNotNull(foundMatch)
            assertEquals("user1", foundMatch.user1Id)
            assertEquals("user2", foundMatch.user2Id)
        }
    }

    // -------------------------------------------------------------------------
    // update function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `update function` {

        @Test
        fun `should update liked status successfully`() = runTest {
            val match = createMatch("user1", "user2", liked = false)

            val updatedMatch = dbQuery {
                repository.update(match.matchId, match.copy(liked = true))
            }

            assertNotNull(updatedMatch)
            assertEquals(true, updatedMatch.liked)
            assertEquals(match.matchId, updatedMatch.matchId)
        }

        @Test
        fun `should update servedAt timestamp`() = runTest {
            val match = createMatch("user1", "user2", servedAt = null)
            val newServedAt = fixedInstant.plusSeconds(7200)

            val updatedMatch = dbQuery {
                repository.update(match.matchId, match.copy(servedAt = newServedAt))
            }

            assertNotNull(updatedMatch)
            assertEquals(newServedAt, updatedMatch.servedAt)
        }

        @Test
        fun `should update updatedAt timestamp`() = runTest {
            val match = createMatch("user1", "user2")
            val newUpdatedAt = fixedInstant.plusSeconds(3600)

            val updatedMatch = dbQuery {
                repository.update(match.matchId, match.copy(updatedAt = newUpdatedAt))
            }

            assertNotNull(updatedMatch)
            assertEquals(newUpdatedAt, updatedMatch.updatedAt)
            assertEquals(fixedInstant, updatedMatch.createdAt) // createdAt unchanged
        }

        @Test
        fun `should return null when updating non-existent match`() = runTest {
            val result = dbQuery {
                repository.update(
                    999L,
                    Match(
                        matchId = 999L,
                        user1Id = "user1",
                        user2Id = "user2",
                        liked = false,
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant
                    )
                )
            }

            assertNull(result)
        }
    }

    // -------------------------------------------------------------------------
    // delete function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `delete function` {

        @Test
        fun `should delete match and return 1 when match exists`() = runTest {
            val match = createMatch("user1", "user2")

            val deleted = dbQuery {
                repository.delete(match.matchId)
            }

            assertEquals(1, deleted)

            // Verify match is actually deleted
            val found = dbQuery {
                repository.findById(match.matchId)
            }
            assertNull(found)
        }

        @Test
        fun `should return 0 when match does not exist`() = runTest {
            val deleted = dbQuery {
                repository.delete(999L)
            }

            assertEquals(0, deleted)
        }

        @Test
        fun `should only delete specified match`() = runTest {
            val match1 = createMatch("user1", "user2")
            val match2 = createMatch("user1", "user3")

            dbQuery {
                repository.delete(match1.matchId)
            }

            val match1Found = dbQuery { repository.findById(match1.matchId) }
            val match2Found = dbQuery { repository.findById(match2.matchId) }

            assertNull(match1Found)
            assertNotNull(match2Found)
        }
    }

    // -------------------------------------------------------------------------
    // getLikeMatch function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `getLikeMatch function` {

        @Test
        fun `should return match when users have liked each other`() = runTest {
            createMatch("user1", "user2", liked = true)

            val match = dbQuery {
                repository.getLikeMatch("user1", "user2")
            }

            assertNotNull(match)
            assertEquals("user1", match.user1Id)
            assertEquals("user2", match.user2Id)
            assertEquals(true, match.liked)
        }

        @Test
        fun `should return null when liked is false`() = runTest {
            createMatch("user1", "user2", liked = false)

            val match = dbQuery {
                repository.getLikeMatch("user1", "user2")
            }

            assertNull(match)
        }

        @Test
        fun `should return null when liked is null`() = runTest {
            createMatch("user1", "user2", liked = null)

            val match = dbQuery {
                repository.getLikeMatch("user1", "user2")
            }

            assertNull(match)
        }

        @Test
        fun `should return null when no match exists`() = runTest {
            val match = dbQuery {
                repository.getLikeMatch("user1", "user2")
            }

            assertNull(match)
        }

        @Test
        fun `should distinguish between user1 and user2 direction`() = runTest {
            createMatch("user1", "user2", liked = true)
            createMatch("user2", "user1", liked = false)

            val forwardMatch = dbQuery {
                repository.getLikeMatch("user1", "user2")
            }
            val reverseMatch = dbQuery {
                repository.getLikeMatch("user2", "user1")
            }

            assertNotNull(forwardMatch)
            assertNull(reverseMatch) // liked is false
        }
    }

    // -------------------------------------------------------------------------
    // findUnservedMatches function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `findUnservedMatches function` {

        @Test
        fun `should return unserved matches for user`() = runTest {
            createMatch("user1", "user2", servedAt = null)
            createMatch("user1", "user3", servedAt = null)

            val matches = dbQuery {
                repository.findUnservedMatches("user1", 10)
            }

            assertEquals(2, matches.size)
            assertTrue(matches.all { it.user1Id == "user1" })
            assertTrue(matches.all { it.servedAt == null })
        }

        @Test
        fun `should not return served matches`() = runTest {
            createMatch("user1", "user2", servedAt = null)
            createMatch("user1", "user3", servedAt = fixedInstant)

            val matches = dbQuery {
                repository.findUnservedMatches("user1", 10)
            }

            assertEquals(1, matches.size)
            assertEquals("user2", matches[0].user2Id)
        }

        @Test
        fun `should respect limit parameter`() = runTest {
            createMatch("user1", "user2", servedAt = null)
            createMatch("user1", "user3", servedAt = null)

            val matches = dbQuery {
                repository.findUnservedMatches("user1", 1)
            }

            assertEquals(1, matches.size)
        }

        @Test
        fun `should return empty list when no unserved matches`() = runTest {
            createMatch("user1", "user2", servedAt = fixedInstant)

            val matches = dbQuery {
                repository.findUnservedMatches("user1", 10)
            }

            assertTrue(matches.isEmpty())
        }

        @Test
        fun `should only return matches for specified user as user1`() = runTest {
            createMatch("user1", "user2", servedAt = null)
            createMatch("user2", "user1", servedAt = null)

            val matches = dbQuery {
                repository.findUnservedMatches("user1", 10)
            }

            assertEquals(1, matches.size)
            assertEquals("user1", matches[0].user1Id)
        }
    }

    // -------------------------------------------------------------------------
    // markAsServed function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `markAsServed function` {

        @Test
        fun `should mark single match as served`() = runTest {
            val match = createMatch("user1", "user2", servedAt = null)
            val servedAt = fixedInstant.plusSeconds(3600)

            val count = dbQuery {
                repository.markAsServed(listOf(match.matchId), servedAt)
            }

            assertEquals(1, count)

            val updatedMatch = dbQuery {
                repository.findById(match.matchId)
            }
            assertNotNull(updatedMatch)
            assertEquals(servedAt, updatedMatch.servedAt)
        }

        @Test
        fun `should mark multiple matches as served`() = runTest {
            val match1 = createMatch("user1", "user2", servedAt = null)
            val match2 = createMatch("user1", "user3", servedAt = null)
            val servedAt = fixedInstant.plusSeconds(3600)

            val count = dbQuery {
                repository.markAsServed(listOf(match1.matchId, match2.matchId), servedAt)
            }

            assertEquals(2, count)

            val updatedMatch1 = dbQuery { repository.findById(match1.matchId) }
            val updatedMatch2 = dbQuery { repository.findById(match2.matchId) }

            assertNotNull(updatedMatch1)
            assertNotNull(updatedMatch2)
            assertEquals(servedAt, updatedMatch1.servedAt)
            assertEquals(servedAt, updatedMatch2.servedAt)
        }

        @Test
        fun `should return 0 when marking non-existent matches`() = runTest {
            val count = dbQuery {
                repository.markAsServed(listOf(999L), fixedInstant)
            }

            assertEquals(0, count)
        }

        @Test
        fun `should handle empty list`() = runTest {
            val count = dbQuery {
                repository.markAsServed(emptyList(), fixedInstant)
            }

            assertEquals(0, count)
        }
    }

    // -------------------------------------------------------------------------
    // countServedToday function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `countServedToday function` {

        @Test
        fun `should count matches served on specified date`() = runTest {
            val today = LocalDate.of(2024, 1, 15)
            val todayInstant = fixedInstant // Use fixedInstant which is in the correct date

            createMatch("user1", "user2", servedAt = todayInstant)
            createMatch("user1", "user3", servedAt = todayInstant.plusSeconds(3600))

            val count = dbQuery {
                repository.countServedToday("user1", today)
            }

            assertEquals(2, count)
        }

        @Test
        fun `should not count matches served on different dates`() = runTest {
            val today = LocalDate.of(2024, 1, 15)
            val yesterday = LocalDate.of(2024, 1, 14)
            val todayInstant = fixedInstant
            val yesterdayInstant = yesterday.atStartOfDay(ZoneId.of("UTC")).toInstant().plusSeconds(36000) // 10:00 AM yesterday

            // Create match with yesterday's createdAt
            val yesterdayCreated = yesterday.atStartOfDay(ZoneId.of("UTC")).toInstant().plusSeconds(36000)
            dbQuery {
                repository.create(
                    Match(
                        matchId = 0,
                        user1Id = "user1",
                        user2Id = "user3",
                        liked = false,
                        createdAt = yesterdayCreated,
                        updatedAt = yesterdayCreated,
                        servedAt = yesterdayInstant
                    )
                )
            }

            createMatch("user1", "user2", servedAt = todayInstant)

            val count = dbQuery {
                repository.countServedToday("user1", today)
            }

            assertEquals(1, count)
        }

        @Test
        fun `should not count unserved matches`() = runTest {
            val today = LocalDate.of(2024, 1, 15)
            createMatch("user1", "user2", servedAt = null)

            val count = dbQuery {
                repository.countServedToday("user1", today)
            }

            assertEquals(0, count)
        }

        @Test
        fun `should return 0 when no matches for user`() = runTest {
            val today = LocalDate.of(2024, 1, 15)

            val count = dbQuery {
                repository.countServedToday("user1", today)
            }

            assertEquals(0, count)
        }

        @Test
        fun `should only count matches for specified user`() = runTest {
            val today = LocalDate.of(2024, 1, 15)
            val todayInstant = fixedInstant

            createMatch("user1", "user2", servedAt = todayInstant)
            createMatch("user2", "user3", servedAt = todayInstant)

            val count = dbQuery {
                repository.countServedToday("user1", today)
            }

            assertEquals(1, count)
        }
    }

    // -------------------------------------------------------------------------
    // findByUserPair function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `findByUserPair function` {

        @Test
        fun `should find match by user pair`() = runTest {
            createMatch("user1", "user2")

            val match = dbQuery {
                repository.findByUserPair("user1", "user2")
            }

            assertNotNull(match)
            assertEquals("user1", match.user1Id)
            assertEquals("user2", match.user2Id)
        }

        @Test
        fun `should return null when no match exists for pair`() = runTest {
            val match = dbQuery {
                repository.findByUserPair("user1", "user2")
            }

            assertNull(match)
        }

        @Test
        fun `should distinguish between different user pair directions`() = runTest {
            createMatch("user1", "user2")
            createMatch("user2", "user1")

            val forwardMatch = dbQuery {
                repository.findByUserPair("user1", "user2")
            }
            val reverseMatch = dbQuery {
                repository.findByUserPair("user2", "user1")
            }

            assertNotNull(forwardMatch)
            assertNotNull(reverseMatch)
            assertNotEquals(forwardMatch.matchId, reverseMatch.matchId)
        }

        @Test
        fun `should find correct match among multiple matches`() = runTest {
            createMatch("user1", "user2")
            createMatch("user1", "user3")
            createMatch("user2", "user3")

            val match = dbQuery {
                repository.findByUserPair("user1", "user3")
            }

            assertNotNull(match)
            assertEquals("user1", match.user1Id)
            assertEquals("user3", match.user2Id)
        }
    }

    // -------------------------------------------------------------------------
    // Integration tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `integration tests` {

        @Test
        fun `should handle full match lifecycle`() = runTest {
            // Create unserved match
            val match = createMatch("user1", "user2", servedAt = null)
            assertNotNull(match.matchId)
            assertNull(match.servedAt)

            // Mark as served
            val servedAt = fixedInstant.plusSeconds(3600)
            dbQuery {
                repository.markAsServed(listOf(match.matchId), servedAt)
            }

            // User takes action (like)
            val actionTime = fixedInstant.plusSeconds(7200)
            val updatedMatch = dbQuery {
                repository.update(
                    match.matchId,
                    match.copy(liked = true, updatedAt = actionTime, servedAt = servedAt)
                )
            }

            assertNotNull(updatedMatch)
            assertEquals(true, updatedMatch.liked)
            assertEquals(servedAt, updatedMatch.servedAt)
            assertEquals(actionTime, updatedMatch.updatedAt)

            // Check if it's a like match
            val likeMatch = dbQuery {
                repository.getLikeMatch("user1", "user2")
            }
            assertNotNull(likeMatch)
        }

        @Test
        fun `should handle batch serving workflow`() = runTest {
            // Create multiple unserved matches
            listOf(
                createMatch("user1", "user2", servedAt = null),
                createMatch("user1", "user3", servedAt = null)
            )

            // Fetch unserved matches
            val unservedMatches = dbQuery {
                repository.findUnservedMatches("user1", 7)
            }
            assertEquals(2, unservedMatches.size)

            // Mark as served
            val servedAt = fixedInstant.plusSeconds(3600)
            val matchIds = unservedMatches.map { it.matchId }
            dbQuery {
                repository.markAsServed(matchIds, servedAt)
            }

            // Verify no more unserved matches
            val remainingUnserved = dbQuery {
                repository.findUnservedMatches("user1", 7)
            }
            assertTrue(remainingUnserved.isEmpty())

            // Verify count served today
            val today = LocalDate.of(2024, 1, 15)
            val count = dbQuery {
                repository.countServedToday("user1", today)
            }
            assertEquals(2, count)
        }

        @Test
        fun `should handle mutual match scenario`() = runTest {
            // Both users like each other
            createMatch("user1", "user2", liked = true)
            createMatch("user2", "user1", liked = true)

            val user1LikesUser2 = dbQuery {
                repository.getLikeMatch("user1", "user2")
            }
            val user2LikesUser1 = dbQuery {
                repository.getLikeMatch("user2", "user1")
            }

            assertNotNull(user1LikesUser2)
            assertNotNull(user2LikesUser1)
            assertTrue(user1LikesUser2.liked!!)
            assertTrue(user2LikesUser1.liked!!)
        }
    }
}
