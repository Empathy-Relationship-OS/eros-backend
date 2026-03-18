package com.eros.matching.service

import com.eros.matching.models.DailyBatch
import com.eros.matching.models.Match
import com.eros.matching.repository.DailyBatchRepository
import com.eros.matching.repository.MatchRepository
import com.eros.matching.transaction.NoOpTransactionManager
import com.eros.users.service.UserMatchProfileData
import com.eros.users.service.UserService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for MatchService.
 *
 * Uses NoOpTransactionManager to bypass database transaction requirements,
 * allowing pure unit testing with mocked dependencies.
 */
class MatchServiceTest {

    private lateinit var matchRepository: MatchRepository
    private lateinit var dailyBatchRepository: DailyBatchRepository
    private lateinit var userService: UserService
    private lateinit var matchService: MatchService

    private val fixedInstant = Instant.parse("2024-01-15T10:00:00Z")

    @BeforeEach
    fun setup() {
        matchRepository = mockk()
        dailyBatchRepository = mockk()
        userService = mockk()
        matchService = MatchService(
            matchRepository,
            dailyBatchRepository,
            userService,
            NoOpTransactionManager() // ← No database required!
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // -------------------------------------------------------------------------
    // Helper functions
    // -------------------------------------------------------------------------

    private fun createTestMatch(
        matchId: Long = 1L,
        user1Id: String = "user1",
        user2Id: String = "user2",
        liked: Boolean = false,
        createdAt: Instant = fixedInstant,
        updatedAt: Instant = fixedInstant,
        servedAt: Instant? = null
    ) = Match(
        matchId = matchId,
        user1Id = user1Id,
        user2Id = user2Id,
        liked = liked,
        createdAt = createdAt,
        updatedAt = updatedAt,
        servedAt = servedAt
    )

    private fun createTestUserMatchProfileData(
        userId: String = "user2",
        name: String = "Test User",
        age: Int = 25,
        thumbnailUrl: String? = "https://example.com/photo.jpg",
        badges: Set<String>? = setOf("VERIFIED")
    ) = UserMatchProfileData(
        userId = userId,
        name = name,
        age = age,
        thumbnailUrl = thumbnailUrl,
        badges = badges
    )

    private fun createTestDailyBatch(
        userId: String = "user1",
        batchDate: LocalDate = LocalDate.now(),
        batchCount: Int = 1
    ) = DailyBatch(
        userId = userId,
        batchDate = batchDate,
        batchCount = batchCount,
        createdAt = fixedInstant,
        updatedAt = fixedInstant
    )

    // -------------------------------------------------------------------------
    // matchAction function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `matchAction function` {

        @Test
        fun `should update match with like action`() = runTest {
            val match = createTestMatch(liked = false)
            val updatedMatch = match.copy(liked = true)

            coEvery { matchRepository.findById(1L) } returns match
            coEvery { matchRepository.update(1L, any()) } returns updatedMatch

            val result = matchService.matchAction(1L, true)

            assertNotNull(result)
            assertTrue(result.liked)
            assertEquals(1L, result.matchId)

            coVerify { matchRepository.findById(1L) }
            coVerify { matchRepository.update(1L, match.copy(liked = true)) }
        }

        @Test
        fun `should update match with dislike action`() = runTest {
            val match = createTestMatch(liked = true)
            val updatedMatch = match.copy(liked = false)

            coEvery { matchRepository.findById(1L) } returns match
            coEvery { matchRepository.update(1L, any()) } returns updatedMatch

            val result = matchService.matchAction(1L, false)

            assertNotNull(result)
            assertFalse(result.liked)
            assertEquals(1L, result.matchId)

            coVerify { matchRepository.findById(1L) }
            coVerify { matchRepository.update(1L, match.copy(liked = false)) }
        }

        @Test
        fun `should throw exception when match does not exist`() = runTest {
            coEvery { matchRepository.findById(999L) } returns null

            val exception = assertThrows<IllegalArgumentException> {
                matchService.matchAction(999L, true)
            }

            assertEquals("The match does not exist.", exception.message)
            coVerify { matchRepository.findById(999L) }
            coVerify(exactly = 0) { matchRepository.update(any(), any()) }
        }

        @Test
        fun `should preserve match metadata when updating action`() = runTest {
            val match = createTestMatch(
                matchId = 5L,
                user1Id = "userA",
                user2Id = "userB",
                liked = false,
                createdAt = fixedInstant.minusSeconds(7200),  // Earlier than servedAt
                updatedAt = fixedInstant.minusSeconds(7200),
                servedAt = fixedInstant.minusSeconds(3600)
            )
            val updatedMatch = match.copy(liked = true)

            coEvery { matchRepository.findById(5L) } returns match
            coEvery { matchRepository.update(5L, any()) } returns updatedMatch

            val result = matchService.matchAction(5L, true)

            assertEquals("userA", result.user1Id)
            assertEquals("userB", result.user2Id)
            assertEquals(fixedInstant.minusSeconds(3600), result.servedAt)

            coVerify { matchRepository.findById(5L) }
        }
    }

    // -------------------------------------------------------------------------
    // isMutualMatch function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `isMutualMatch function` {

        @Test
        fun `should return true when both users liked each other`() = runTest {
            val match1 = createTestMatch(user1Id = "user1", user2Id = "user2", liked = true)
            val match2 = createTestMatch(user1Id = "user2", user2Id = "user1", liked = true)

            coEvery { matchRepository.getLikeMatch("user1", "user2") } returns match1
            coEvery { matchRepository.getLikeMatch("user2", "user1") } returns match2

            val result = matchService.isMutualMatch("user1", "user2")

            assertTrue(result)

            coVerify { matchRepository.getLikeMatch("user1", "user2") }
            coVerify { matchRepository.getLikeMatch("user2", "user1") }
        }

        @Test
        fun `should return false when only one user liked`() = runTest {
            val match1 = createTestMatch(user1Id = "user1", user2Id = "user2", liked = true)

            coEvery { matchRepository.getLikeMatch("user1", "user2") } returns match1
            coEvery { matchRepository.getLikeMatch("user2", "user1") } returns null

            val result = matchService.isMutualMatch("user1", "user2")

            assertFalse(result)

            coVerify { matchRepository.getLikeMatch("user1", "user2") }
            coVerify { matchRepository.getLikeMatch("user2", "user1") }
        }

        @Test
        fun `should return false when first user did not like`() = runTest {
            val match2 = createTestMatch(user1Id = "user2", user2Id = "user1", liked = true)

            coEvery { matchRepository.getLikeMatch("user1", "user2") } returns null
            coEvery { matchRepository.getLikeMatch("user2", "user1") } returns match2

            val result = matchService.isMutualMatch("user1", "user2")

            assertFalse(result)

            coVerify { matchRepository.getLikeMatch("user1", "user2") }
            // Should short-circuit and not check second match
            coVerify(exactly = 0) { matchRepository.getLikeMatch("user2", "user1") }
        }

        @Test
        fun `should return false when neither user has liked`() = runTest {
            coEvery { matchRepository.getLikeMatch("user1", "user2") } returns null
            coEvery { matchRepository.getLikeMatch("user2", "user1") } returns null

            val result = matchService.isMutualMatch("user1", "user2")

            assertFalse(result)

            coVerify { matchRepository.getLikeMatch("user1", "user2") }
            coVerify(exactly = 0) { matchRepository.getLikeMatch("user2", "user1") }
        }

        @Test
        fun `should return false when first match exists but liked is false`() = runTest {
            // getLikeMatch returns null when liked is false (it only returns liked matches)
            coEvery { matchRepository.getLikeMatch("user1", "user2") } returns null

            val result = matchService.isMutualMatch("user1", "user2")

            assertFalse(result)

            coVerify { matchRepository.getLikeMatch("user1", "user2") }
            coVerify(exactly = 0) { matchRepository.getLikeMatch("user2", "user1") }
        }
    }

    // -------------------------------------------------------------------------
    // matchUser function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `matchUser function` {

        @Test
        fun `should return MutualMatchInfo when both users liked each other`() = runTest {
            val match = createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", liked = false)
            val updatedMatch = match.copy(liked = true)
            val reverseMatch = createTestMatch(user1Id = "user2", user2Id = "user1", liked = true)

            coEvery { matchRepository.findById(1L) } returns match
            coEvery { matchRepository.update(1L, any()) } returns updatedMatch
            coEvery { matchRepository.getLikeMatch("user1", "user2") } returns updatedMatch
            coEvery { matchRepository.getLikeMatch("user2", "user1") } returns reverseMatch

            val result = matchService.matchUser(1L, true)

            assertNotNull(result)
            assertEquals(1L, result.matchId)
            assertEquals("user1", result.user1Id)
            assertEquals("user2", result.user2Id)
            assertNotNull(result.matchedAt)

            coVerify { matchRepository.findById(1L) }
            coVerify { matchRepository.update(1L, any()) }
            coVerify { matchRepository.getLikeMatch("user1", "user2") }
            coVerify { matchRepository.getLikeMatch("user2", "user1") }
        }

        @Test
        fun `should return null when not a mutual match`() = runTest {
            val match = createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", liked = false)
            val updatedMatch = match.copy(liked = true)

            coEvery { matchRepository.findById(1L) } returns match
            coEvery { matchRepository.update(1L, any()) } returns updatedMatch
            coEvery { matchRepository.getLikeMatch("user1", "user2") } returns updatedMatch
            coEvery { matchRepository.getLikeMatch("user2", "user1") } returns null

            val result = matchService.matchUser(1L, true)

            assertNull(result)

            coVerify { matchRepository.findById(1L) }
            coVerify { matchRepository.update(1L, any()) }
            coVerify { matchRepository.getLikeMatch("user1", "user2") }
            coVerify { matchRepository.getLikeMatch("user2", "user1") }
        }

        @Test
        fun `should return null when user dislikes`() = runTest {
            val match = createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", liked = true)
            val updatedMatch = match.copy(liked = false)

            coEvery { matchRepository.findById(1L) } returns match
            coEvery { matchRepository.update(1L, any()) } returns updatedMatch
            coEvery { matchRepository.getLikeMatch("user1", "user2") } returns null

            val result = matchService.matchUser(1L, false)

            assertNull(result)

            coVerify { matchRepository.findById(1L) }
            coVerify { matchRepository.update(1L, any()) }
            coVerify { matchRepository.getLikeMatch("user1", "user2") }
        }

        @Test
        fun `should throw exception when match does not exist`() = runTest {
            coEvery { matchRepository.findById(999L) } returns null

            val exception = assertThrows<IllegalArgumentException> {
                matchService.matchUser(999L, true)
            }

            assertEquals("The match does not exist.", exception.message)

            coVerify { matchRepository.findById(999L) }
            coVerify(exactly = 0) { matchRepository.update(any(), any()) }
        }
    }

    // -------------------------------------------------------------------------
    // fetchDailyBatch function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `fetchDailyBatch function` {

        @Test
        fun `should return batch of UserMatchProfiles when unserved matches exist`() = runTest {
            val today = LocalDate.now()
            val matches = listOf(
                createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", servedAt = null),
                createTestMatch(matchId = 2L, user1Id = "user1", user2Id = "user3", servedAt = null)
            )
            val userData1 = createTestUserMatchProfileData(userId = "user2", name = "Alice")
            val userData2 = createTestUserMatchProfileData(userId = "user3", name = "Bob")
            val dailyBatch = createTestDailyBatch(batchCount = 1)

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns matches
            coEvery { matchRepository.markAsServed(any(), any()) } returns 2
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData("user2") } returns userData1
            coEvery { userService.getUserMatchProfileData("user3") } returns userData2

            val result = matchService.fetchDailyBatch("user1")

            assertEquals(2, result.size)
            assertEquals(1L, result[0].matchId)
            assertEquals("Alice", result[0].name)
            assertEquals(2L, result[1].matchId)
            assertEquals("Bob", result[1].name)

            coVerify { dailyBatchRepository.getBatchCount("user1", today) }
            coVerify { matchRepository.findUnservedMatches("user1", 7) }
            coVerify { matchRepository.markAsServed(listOf(1L, 2L), any()) }
            coVerify { dailyBatchRepository.incrementBatchCount("user1", today) }
            coVerify { userService.getUserMatchProfileData("user2") }
            coVerify { userService.getUserMatchProfileData("user3") }
        }

        @Test
        fun `should throw DailyBatchLimitExceededException when limit reached`() = runTest {
            val today = LocalDate.now()

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 3

            val exception = assertThrows<DailyBatchLimitExceededException> {
                matchService.fetchDailyBatch("user1")
            }

            assertTrue(exception.message!!.contains("Daily batch limit of 3 exceeded"))

            coVerify { dailyBatchRepository.getBatchCount("user1", today) }
            coVerify(exactly = 0) { matchRepository.findUnservedMatches(any(), any()) }
        }

        @Test
        fun `should throw NoMatchesAvailableException when no unserved matches`() = runTest {
            val today = LocalDate.now()

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns emptyList()

            val exception = assertThrows<NoMatchesAvailableException> {
                matchService.fetchDailyBatch("user1")
            }

            assertTrue(exception.message!!.contains("No unserved matches available"))

            coVerify { dailyBatchRepository.getBatchCount("user1", today) }
            coVerify { matchRepository.findUnservedMatches("user1", 7) }
            coVerify(exactly = 0) { matchRepository.markAsServed(any(), any()) }
        }

        @Test
        fun `should filter out matches when user data is not found`() = runTest {
            val today = LocalDate.now()
            val matches = listOf(
                createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", servedAt = null),
                createTestMatch(matchId = 2L, user1Id = "user1", user2Id = "user3", servedAt = null),
                createTestMatch(matchId = 3L, user1Id = "user1", user2Id = "user4", servedAt = null)
            )
            val userData1 = createTestUserMatchProfileData(userId = "user2", name = "Alice")
            val dailyBatch = createTestDailyBatch()

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns matches
            coEvery { matchRepository.markAsServed(any(), any()) } returns 3
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData("user2") } returns userData1
            coEvery { userService.getUserMatchProfileData("user3") } returns null
            coEvery { userService.getUserMatchProfileData("user4") } returns null

            val result = matchService.fetchDailyBatch("user1")

            assertEquals(1, result.size)
            assertEquals(1L, result[0].matchId)
            assertEquals("Alice", result[0].name)

            coVerify { userService.getUserMatchProfileData("user2") }
            coVerify { userService.getUserMatchProfileData("user3") }
            coVerify { userService.getUserMatchProfileData("user4") }
        }

        @Test
        fun `should respect BATCH_SIZE constant when fetching matches`() = runTest {
            val today = LocalDate.now()
            val matches = List(7) { index ->
                createTestMatch(
                    matchId = index.toLong() + 1,
                    user1Id = "user1",
                    user2Id = "user${index + 2}",
                    servedAt = null
                )
            }
            val dailyBatch = createTestDailyBatch()

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns matches
            coEvery { matchRepository.markAsServed(any(), any()) } returns 7
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData(any()) } returns createTestUserMatchProfileData()

            matchService.fetchDailyBatch("user1")

            coVerify { matchRepository.findUnservedMatches("user1", 7) }
        }

        @Test
        fun `should mark all fetched matches as served with timestamp`() = runTest {
            val today = LocalDate.now()
            val matches = listOf(
                createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", servedAt = null),
                createTestMatch(matchId = 2L, user1Id = "user1", user2Id = "user3", servedAt = null)
            )
            val dailyBatch = createTestDailyBatch()

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns matches
            coEvery { matchRepository.markAsServed(any(), any()) } returns 2
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData(any()) } returns createTestUserMatchProfileData()

            matchService.fetchDailyBatch("user1")

            coVerify { matchRepository.markAsServed(listOf(1L, 2L), any()) }
        }

        @Test
        fun `should increment batch count after serving matches`() = runTest {
            val today = LocalDate.now()
            val matches = listOf(
                createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", servedAt = null)
            )
            val dailyBatch = createTestDailyBatch()

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 1
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns matches
            coEvery { matchRepository.markAsServed(any(), any()) } returns 1
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData("user2") } returns createTestUserMatchProfileData()

            matchService.fetchDailyBatch("user1")

            coVerify { dailyBatchRepository.incrementBatchCount("user1", today) }
        }

        @Test
        fun `should handle batch count at limit boundary`() = runTest {
            val today = LocalDate.now()
            val matches = listOf(
                createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", servedAt = null)
            )
            val dailyBatch = createTestDailyBatch(batchCount = 3)

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 2
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns matches
            coEvery { matchRepository.markAsServed(any(), any()) } returns 1
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData("user2") } returns createTestUserMatchProfileData()

            val result = matchService.fetchDailyBatch("user1")

            assertEquals(1, result.size)

            coVerify { dailyBatchRepository.getBatchCount("user1", today) }
            coVerify { matchRepository.findUnservedMatches("user1", 7) }
        }
    }

    // -------------------------------------------------------------------------
    // buildUserMatchProfile private function (tested indirectly)
    // -------------------------------------------------------------------------

    @Nested
    inner class `buildUserMatchProfile function` {

        @Test
        fun `should correctly map UserMatchProfileData to UserMatchProfile`() = runTest {
            val today = LocalDate.now()
            val match = createTestMatch(
                matchId = 1L,
                user1Id = "user1",
                user2Id = "user2",
                servedAt = null
            )
            val userData = UserMatchProfileData(
                userId = "user2",
                name = "Alice",
                age = 28,
                thumbnailUrl = "https://example.com/photo.jpg",
                badges = setOf("VERIFIED", "GOOD_XP")
            )
            val dailyBatch = createTestDailyBatch()

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns listOf(match)
            coEvery { matchRepository.markAsServed(any(), any()) } returns 1
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData("user2") } returns userData

            val result = matchService.fetchDailyBatch("user1")

            assertEquals(1, result.size)
            val profile = result[0]
            assertEquals(1L, profile.matchId)
            assertEquals("user2", profile.userId)
            assertEquals("Alice", profile.name)
            assertEquals(28, profile.age)
            assertEquals("https://example.com/photo.jpg", profile.thumbnailUrl)
            assertEquals(setOf("VERIFIED", "GOOD_XP"), profile.badges)
            assertNotNull(profile.servedAt)
        }

        @Test
        fun `should handle null thumbnailUrl and badges`() = runTest {
            val today = LocalDate.now()
            val match = createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", servedAt = null)
            val userData = UserMatchProfileData(
                userId = "user2",
                name = "Bob",
                age = 30,
                thumbnailUrl = null,
                badges = null
            )
            val dailyBatch = createTestDailyBatch()

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns listOf(match)
            coEvery { matchRepository.markAsServed(any(), any()) } returns 1
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData("user2") } returns userData

            val result = matchService.fetchDailyBatch("user1")

            assertEquals(1, result.size)
            val profile = result[0]
            assertNull(profile.thumbnailUrl)
            assertNull(profile.badges)
        }
    }
}
