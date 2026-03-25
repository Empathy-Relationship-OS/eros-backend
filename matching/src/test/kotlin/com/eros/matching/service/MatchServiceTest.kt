package com.eros.matching.service

import com.eros.matching.models.DailyBatch
import com.eros.matching.models.Match
import com.eros.matching.repository.DailyBatchRepository
import com.eros.matching.repository.MatchRepository
import com.eros.matching.transaction.NoOpTransactionManager
import com.eros.users.models.UserMatchProfileData
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
        liked: Boolean? = null,
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
        fun `should return match with liked true when user likes`() = runTest {
            val match = createTestMatch(
                matchId = 1L,
                user1Id = "user1",
                user2Id = "user2",
                liked = null,
                servedAt = fixedInstant
            )
            val updatedMatch = match.recordAction(true, fixedInstant.plusSeconds(60))

            coEvery { matchRepository.findById(1L) } returns match
            coEvery { matchRepository.update(1L, any()) } returns updatedMatch

            val result = matchService.matchAction(1L, "user1", true)

            // Assert on the outcome, not the implementation
            assertNotNull(result)
            assertEquals(true, result.liked)
            assertEquals(1L, result.matchId)
            assertEquals("user1", result.user1Id)
            assertEquals("user2", result.user2Id)
        }

        @Test
        fun `should return match with liked false when user passes`() = runTest {
            val match = createTestMatch(
                matchId = 1L,
                user1Id = "user1",
                user2Id = "user2",
                liked = null,
                servedAt = fixedInstant
            )
            val updatedMatch = match.recordAction(false, fixedInstant.plusSeconds(60))

            coEvery { matchRepository.findById(1L) } returns match
            coEvery { matchRepository.update(1L, any()) } returns updatedMatch

            val result = matchService.matchAction(1L, "user1", false)

            // Assert on the outcome
            assertNotNull(result)
            assertEquals(false, result.liked)
            assertEquals(1L, result.matchId)
        }

        @Test
        fun `should throw NotFoundException when match does not exist`() = runTest {
            coEvery { matchRepository.findById(999L) } returns null

            val exception = assertThrows<com.eros.common.errors.NotFoundException> {
                matchService.matchAction(999L, "user1", true)
            }

            // Assert on the exception behavior
            assertTrue(exception.message!!.contains("Match with ID 999 not found"))
        }

        @Test
        fun `should throw ForbiddenException when user doesn't own match`() = runTest {
            val match = createTestMatch(
                matchId = 1L,
                user1Id = "userA",
                user2Id = "userB",
                servedAt = fixedInstant
            )

            coEvery { matchRepository.findById(1L) } returns match

            val exception = assertThrows<com.eros.common.errors.ForbiddenException> {
                matchService.matchAction(1L, "wrongUser", true)
            }

            // Assert on the exception behavior
            assertTrue(exception.message!!.contains("You do not have permission"))
        }

        @Test
        fun `should throw ConflictException when attempting to change a previous like`() = runTest {
            val match = createTestMatch(
                matchId = 1L,
                user1Id = "user1",
                user2Id = "user2",
                liked = true,
                createdAt = fixedInstant,
                updatedAt = fixedInstant.plusSeconds(120), // Updated after served
                servedAt = fixedInstant
            )

            coEvery { matchRepository.findById(1L) } returns match

            val exception = assertThrows<com.eros.common.errors.ConflictException> {
                matchService.matchAction(1L, "user1", false)
            }

            // Assert on the exception behavior - like→pass is prevented
            assertTrue(exception.message!!.contains("Cannot change from like to pass"))
        }

        @Test
        fun `should allow changing pass to like within 24 hours`() = runTest {
            // Use a recent timestamp (within last 24 hours)
            val recentServedAt = Instant.now().minusSeconds(3600) // 1 hour ago

            val match = createTestMatch(
                matchId = 1L,
                user1Id = "user1",
                user2Id = "user2",
                liked = false,
                createdAt = recentServedAt.minusSeconds(60),
                updatedAt = recentServedAt.plusSeconds(120), // Previously passed
                servedAt = recentServedAt
            )
            val updatedMatch = match.recordAction(true, Instant.now())

            coEvery { matchRepository.findById(1L) } returns match
            coEvery { matchRepository.update(1L, any()) } returns updatedMatch

            val result = matchService.matchAction(1L, "user1", true)

            // Assert user can change from pass to like
            assertNotNull(result)
            assertEquals(true, result.liked)
        }

        @Test
        fun `should allow action on served match that hasn't been acted on`() = runTest {
            val match = createTestMatch(
                matchId = 1L,
                user1Id = "user1",
                user2Id = "user2",
                liked = null,
                createdAt = fixedInstant,
                updatedAt = fixedInstant,
                servedAt = fixedInstant
            )
            val updatedMatch = match.recordAction(true, fixedInstant.plusSeconds(60))

            coEvery { matchRepository.findById(1L) } returns match
            coEvery { matchRepository.update(1L, any()) } returns updatedMatch

            val result = matchService.matchAction(1L, "user1", true)

            // Assert first action is allowed
            assertNotNull(result)
            assertEquals(true, result.liked)
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

            // Assert on the outcome: mutual match detected
            assertTrue(result)
        }

        @Test
        fun `should return false when only one user liked`() = runTest {
            val match1 = createTestMatch(user1Id = "user1", user2Id = "user2", liked = true)

            coEvery { matchRepository.getLikeMatch("user1", "user2") } returns match1
            coEvery { matchRepository.getLikeMatch("user2", "user1") } returns null

            val result = matchService.isMutualMatch("user1", "user2")

            // Assert on the outcome: no mutual match
            assertFalse(result)
        }

        @Test
        fun `should return false when first user did not like`() = runTest {
            coEvery { matchRepository.getLikeMatch("user1", "user2") } returns null

            val result = matchService.isMutualMatch("user1", "user2")

            // Assert on the outcome: no mutual match
            assertFalse(result)
        }

        @Test
        fun `should return false when neither user has liked`() = runTest {
            coEvery { matchRepository.getLikeMatch("user1", "user2") } returns null
            coEvery { matchRepository.getLikeMatch("user2", "user1") } returns null

            val result = matchService.isMutualMatch("user1", "user2")

            // Assert on the outcome: no mutual match
            assertFalse(result)
        }

        @Test
        fun `should return false when match exists but not liked`() = runTest {
            // getLikeMatch returns null when liked is false (it only returns liked matches)
            coEvery { matchRepository.getLikeMatch("user1", "user2") } returns null

            val result = matchService.isMutualMatch("user1", "user2")

            // Assert on the outcome: no mutual match
            assertFalse(result)
        }
    }

    // -------------------------------------------------------------------------
    // matchUser function tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `matchUser function` {

        @Test
        fun `should return MutualMatchInfo when both users liked each other`() = runTest {
            val match = createTestMatch(
                matchId = 1L,
                user1Id = "user1",
                user2Id = "user2",
                liked = null,
                servedAt = fixedInstant
            )
            val updatedMatch = match.recordAction(true, fixedInstant.plusSeconds(60))
            val reverseMatch = createTestMatch(user1Id = "user2", user2Id = "user1", liked = true)

            coEvery { matchRepository.findById(1L) } returns match
            coEvery { matchRepository.update(1L, any()) } returns updatedMatch
            coEvery { matchRepository.getLikeMatch("user1", "user2") } returns updatedMatch
            coEvery { matchRepository.getLikeMatch("user2", "user1") } returns reverseMatch

            val result = matchService.matchUser(1L, "user1", true)

            // Assert on the outcome: mutual match info is returned
            assertNotNull(result)
            assertEquals(1L, result.matchId)
            assertEquals("user1", result.user1Id)
            assertEquals("user2", result.user2Id)
            assertNotNull(result.matchedAt)
        }

        @Test
        fun `should return null when user likes but not a mutual match`() = runTest {
            val match = createTestMatch(
                matchId = 1L,
                user1Id = "user1",
                user2Id = "user2",
                liked = null,
                servedAt = fixedInstant
            )
            val updatedMatch = match.recordAction(true, fixedInstant.plusSeconds(60))

            coEvery { matchRepository.findById(1L) } returns match
            coEvery { matchRepository.update(1L, any()) } returns updatedMatch
            coEvery { matchRepository.getLikeMatch("user1", "user2") } returns updatedMatch
            coEvery { matchRepository.getLikeMatch("user2", "user1") } returns null

            val result = matchService.matchUser(1L, "user1", true)

            // Assert on the outcome: no mutual match info returned
            assertNull(result)
        }

        @Test
        fun `should return null when user passes`() = runTest {
            val match = createTestMatch(
                matchId = 1L,
                user1Id = "user1",
                user2Id = "user2",
                liked = null,
                servedAt = fixedInstant
            )
            val updatedMatch = match.recordAction(false, fixedInstant.plusSeconds(60))

            coEvery { matchRepository.findById(1L) } returns match
            coEvery { matchRepository.update(1L, any()) } returns updatedMatch

            val result = matchService.matchUser(1L, "user1", false)

            // Assert on the outcome: no mutual match check when passing
            assertNull(result)
        }

        @Test
        fun `should throw NotFoundException when match does not exist`() = runTest {
            coEvery { matchRepository.findById(999L) } returns null

            val exception = assertThrows<com.eros.common.errors.NotFoundException> {
                matchService.matchUser(999L, "user1", true)
            }

            // Assert on the exception behavior
            assertTrue(exception.message!!.contains("Match with ID 999 not found"))
        }

        @Test
        fun `should throw ForbiddenException when user doesn't own match`() = runTest {
            val match = createTestMatch(
                matchId = 1L,
                user1Id = "userA",
                user2Id = "userB",
                servedAt = fixedInstant
            )

            coEvery { matchRepository.findById(1L) } returns match

            val exception = assertThrows<com.eros.common.errors.ForbiddenException> {
                matchService.matchUser(1L, "wrongUser", true)
            }

            // Assert on the exception behavior
            assertTrue(exception.message!!.contains("You do not have permission"))
        }

        @Test
        fun `should throw ConflictException when user already took action`() = runTest {
            val match = createTestMatch(
                matchId = 1L,
                user1Id = "user1",
                user2Id = "user2",
                liked = true,
                createdAt = fixedInstant,
                updatedAt = fixedInstant.plusSeconds(120),
                servedAt = fixedInstant
            )

            coEvery { matchRepository.findById(1L) } returns match

            val exception = assertThrows<com.eros.common.errors.ConflictException> {
                matchService.matchUser(1L, "user1", true)
            }

            // Assert on the exception behavior - can't like again when already liked
            assertTrue(exception.message!!.contains("already liked"))
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
            val userData1 = createTestUserMatchProfileData(userId = "user2", name = "Alice", age = 28)
            val userData2 = createTestUserMatchProfileData(userId = "user3", name = "Bob", age = 32)
            val dailyBatch = createTestDailyBatch(batchCount = 1)

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns matches
            coEvery { matchRepository.markAsServed(any(), any()) } returns 2
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData("user2") } returns userData1
            coEvery { userService.getUserMatchProfileData("user3") } returns userData2

            val result = matchService.fetchDailyBatch("user1")

            // Assert on the outcome: correct response structure and metadata
            assertEquals(1, result.batchNumber) // batchCount was 0, so this is batch #1
            assertEquals(2, result.remainingBatches) // 3 - 1 = 2 remaining
            assertEquals(2, result.profiles.size)
            assertEquals(1L, result.profiles[0].matchId)
            assertEquals("user2", result.profiles[0].userId)
            assertEquals("Alice", result.profiles[0].name)
            assertEquals(28, result.profiles[0].age)
            assertEquals(2L, result.profiles[1].matchId)
            assertEquals("user3", result.profiles[1].userId)
            assertEquals("Bob", result.profiles[1].name)
            assertEquals(32, result.profiles[1].age)
            assertNotNull(result.profiles[0].servedAt)
            assertNotNull(result.profiles[1].servedAt)
        }

        @Test
        fun `should throw DailyBatchLimitExceededException when limit reached`() = runTest {
            val today = LocalDate.now()

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 3

            val exception = assertThrows<DailyBatchLimitExceededException> {
                matchService.fetchDailyBatch("user1")
            }

            // Assert on the exception behavior and properties
            assertTrue(exception.message!!.contains("Daily batch limit of 3 exceeded"))
            assertEquals("user1", exception.userId)
            assertEquals(3, exception.batchesUsed)
            assertEquals(3, exception.maxBatches)
            assertNotNull(exception.resetAt)
        }

        @Test
        fun `should throw NoMatchesAvailableException when no unserved matches`() = runTest {
            val today = LocalDate.now()

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns emptyList()

            val exception = assertThrows<NoMatchesAvailableException> {
                matchService.fetchDailyBatch("user1")
            }

            // Assert on the exception behavior
            assertTrue(exception.message!!.contains("No unserved matches available"))
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

            // Assert on the outcome: only profiles with valid user data
            assertEquals(1, result.batchNumber)
            assertEquals(2, result.remainingBatches)
            assertEquals(1, result.profiles.size)
            assertEquals(1L, result.profiles[0].matchId)
            assertEquals("Alice", result.profiles[0].name)
        }

        @Test
        fun `should return up to 7 matches per batch`() = runTest {
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

            val result = matchService.fetchDailyBatch("user1")

            // Assert on the outcome: batch size respected
            assertEquals(1, result.batchNumber)
            assertEquals(2, result.remainingBatches)
            assertEquals(7, result.profiles.size)
        }

        @Test
        fun `should allow fetching batch when under daily limit`() = runTest {
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

            // Assert on the outcome: batch fetched successfully at limit boundary
            assertEquals(3, result.batchNumber) // batchCount was 2, so this is batch #3
            assertEquals(0, result.remainingBatches) // 3 - 3 = 0 remaining
            assertEquals(1, result.profiles.size)
        }
    }

    // -------------------------------------------------------------------------
    // buildUserMatchProfile private function (tested indirectly)
    // -------------------------------------------------------------------------

    @Nested
    inner class `buildUserMatchProfile function` {

        @Test
        fun `should correctly map UserMatchProfileData to UserMatchProfile with all fields`() = runTest {
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

            // Assert on the outcome: profile correctly mapped with all fields
            assertEquals(1, result.batchNumber)
            assertEquals(2, result.remainingBatches)
            assertEquals(1, result.profiles.size)
            val profile = result.profiles[0]
            assertEquals(1L, profile.matchId)
            assertEquals("user2", profile.userId)
            assertEquals("Alice", profile.name)
            assertEquals(28, profile.age)
            assertEquals("https://example.com/photo.jpg", profile.thumbnailUrl)
            assertEquals(setOf("VERIFIED", "GOOD_XP"), profile.badges)
            assertNotNull(profile.servedAt)
        }

        @Test
        fun `should handle null thumbnailUrl and badges gracefully`() = runTest {
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

            // Assert on the outcome: null values handled correctly
            assertEquals(1, result.batchNumber)
            assertEquals(2, result.remainingBatches)
            assertEquals(1, result.profiles.size)
            val profile = result.profiles[0]
            assertEquals("Bob", profile.name)
            assertEquals(30, profile.age)
            assertNull(profile.thumbnailUrl)
            assertNull(profile.badges)
        }
    }
}
