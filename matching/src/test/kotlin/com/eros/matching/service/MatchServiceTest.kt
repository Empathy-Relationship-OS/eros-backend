package com.eros.matching.service

import com.eros.common.errors.NotFoundException
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
import java.time.ZoneId
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
        batchDate: LocalDate = LocalDate.ofInstant(fixedInstant, ZoneId.of("UTC")),
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

            val exception = assertThrows<NotFoundException> {
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

        @Test
        fun `should not allow action on unserved match`() = runTest {
            val match = createTestMatch(
                matchId = 1L,
                user1Id = "user1",
                user2Id = "user2",
                liked = null,
                servedAt = null  // Match has not been served yet
            )

            coEvery { matchRepository.findById(1L) } returns match

            val exception = assertThrows<com.eros.common.errors.ConflictException> {
                matchService.matchAction(1L, "user1", true)
            }

            // Assert on the exception behavior - cannot act on unserved match
            assertTrue(exception.message!!.contains("Cannot act on a match that has not been served yet"))
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

            val exception = assertThrows<NotFoundException> {
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
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns emptyList()
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
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns emptyList()
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns emptyList()

            val exception = assertThrows<NoMatchesAvailableException> {
                matchService.fetchDailyBatch("user1")
            }

            // Assert on the exception behavior
            assertTrue(exception.message!!.contains("No matches available"))
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
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns emptyList()
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
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns emptyList()
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
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns emptyList()
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

        @Test
        fun `should return carryover matches and fill with new matches to reach batch size`() = runTest {
            val today = LocalDate.now()
            val servedAt = Instant.now().minus(java.time.Duration.ofDays(1)) // Served yesterday

            // 2 carryover matches (served but not acted upon)
            val servedUnactedMatches = listOf(
                createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", servedAt = servedAt, liked = null),
                createTestMatch(matchId = 2L, user1Id = "user1", user2Id = "user3", servedAt = servedAt, liked = null)
            )

            // 5 new matches to fill remaining slots (7 - 2 = 5)
            val newMatches = listOf(
                createTestMatch(matchId = 3L, user1Id = "user1", user2Id = "user4", servedAt = null),
                createTestMatch(matchId = 4L, user1Id = "user1", user2Id = "user5", servedAt = null),
                createTestMatch(matchId = 5L, user1Id = "user1", user2Id = "user6", servedAt = null),
                createTestMatch(matchId = 6L, user1Id = "user1", user2Id = "user7", servedAt = null),
                createTestMatch(matchId = 7L, user1Id = "user1", user2Id = "user8", servedAt = null)
            )

            val userData1 = createTestUserMatchProfileData(userId = "user2", name = "Alice", age = 28)
            val userData2 = createTestUserMatchProfileData(userId = "user3", name = "Bob", age = 32)
            val userData3 = createTestUserMatchProfileData(userId = "user4", name = "Charlie", age = 30)
            val userData4 = createTestUserMatchProfileData(userId = "user5", name = "Diana", age = 27)
            val userData5 = createTestUserMatchProfileData(userId = "user6", name = "Eve", age = 29)
            val userData6 = createTestUserMatchProfileData(userId = "user7", name = "Frank", age = 31)
            val userData7 = createTestUserMatchProfileData(userId = "user8", name = "Grace", age = 26)
            val dailyBatch = createTestDailyBatch(batchCount = 1)

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns servedUnactedMatches
            coEvery { matchRepository.findUnservedMatches("user1", 5) } returns newMatches // Only fetches 5 to fill
            coEvery { matchRepository.markAsServed(any(), any()) } returns 5
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData("user2") } returns userData1
            coEvery { userService.getUserMatchProfileData("user3") } returns userData2
            coEvery { userService.getUserMatchProfileData("user4") } returns userData3
            coEvery { userService.getUserMatchProfileData("user5") } returns userData4
            coEvery { userService.getUserMatchProfileData("user6") } returns userData5
            coEvery { userService.getUserMatchProfileData("user7") } returns userData6
            coEvery { userService.getUserMatchProfileData("user8") } returns userData7

            val result = matchService.fetchDailyBatch("user1")

            // Assert: batch count incremented, carryover + new matches = 7 profiles
            assertEquals(1, result.batchNumber) // batchCount was 0, now 1
            assertEquals(2, result.remainingBatches) // 3 - 1 = 2 remaining
            assertEquals(7, result.profiles.size) // 2 carryover + 5 new = 7 total
            assertEquals("Alice", result.profiles[0].name) // Carryover first
            assertEquals("Bob", result.profiles[1].name) // Carryover second
            assertEquals("Charlie", result.profiles[2].name) // New matches follow
            assertEquals("Grace", result.profiles[6].name) // Last new match

            // Verify that batch count was incremented and both carryover and new matches were marked as served
            coVerify(exactly = 1) { dailyBatchRepository.incrementBatchCount("user1", today) }
            coVerify(exactly = 1) { matchRepository.markAsServed(match { it.size == 2 }, any()) } // 2 carryover matches re-marked
            coVerify(exactly = 1) { matchRepository.markAsServed(match { it.size == 5 }, any()) } // 5 new matches marked
            coVerify(exactly = 1) { matchRepository.findUnservedMatches("user1", 5) } // Fetched exactly 5
        }

        @Test
        fun `should return only carryover matches when they fill entire batch size`() = runTest {
            val today = LocalDate.now()
            val servedAt = Instant.now().minus(java.time.Duration.ofDays(1)) // Served yesterday

            // 7 carryover matches - exactly batch size
            val servedUnactedMatches = List(7) { index ->
                createTestMatch(
                    matchId = index.toLong() + 1,
                    user1Id = "user1",
                    user2Id = "user${index + 2}",
                    servedAt = servedAt,
                    liked = null
                )
            }

            val dailyBatch = createTestDailyBatch(batchCount = 1)

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns servedUnactedMatches
            coEvery { matchRepository.markAsServed(any(), any()) } returns 7 // Re-mark carryover matches with today's timestamp
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData(any(), any()) } returns createTestUserMatchProfileData()

            val result = matchService.fetchDailyBatch("user1")

            // Assert: batch count incremented, only carryover matches returned, no new matches fetched
            assertEquals(1, result.batchNumber)
            assertEquals(2, result.remainingBatches)
            assertEquals(7, result.profiles.size)

            // Verify carryover matches were re-marked with today's timestamp, but no new matches were fetched
            coVerify(exactly = 1) { dailyBatchRepository.incrementBatchCount("user1", today) }
            coVerify(exactly = 0) { matchRepository.findUnservedMatches(any(), any()) }
            coVerify(exactly = 1) { matchRepository.markAsServed(match { it.size == 7 }, any()) } // Re-mark all 7 carryovers
        }

        @Test
        fun `should handle partial carryover when some carryover profiles are unavailable`() = runTest {
            val today = LocalDate.now()
            val servedAt = Instant.now().minus(java.time.Duration.ofDays(1)) // Served yesterday

            // 3 carryover matches (but one will have unavailable user data)
            val servedUnactedMatches = listOf(
                createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", servedAt = servedAt, liked = null),
                createTestMatch(matchId = 2L, user1Id = "user1", user2Id = "user3", servedAt = servedAt, liked = null),
                createTestMatch(matchId = 3L, user1Id = "user1", user2Id = "user4", servedAt = servedAt, liked = null)
            )

            // Code calculates slotsRemaining based on VALID carryover profiles (2), not match count (3)
            // So it will request 5 new matches (7 - 2 = 5) to fill the batch
            val newMatches = listOf(
                createTestMatch(matchId = 4L, user1Id = "user1", user2Id = "user5", servedAt = null),
                createTestMatch(matchId = 5L, user1Id = "user1", user2Id = "user6", servedAt = null),
                createTestMatch(matchId = 6L, user1Id = "user1", user2Id = "user7", servedAt = null),
                createTestMatch(matchId = 7L, user1Id = "user1", user2Id = "user8", servedAt = null),
                createTestMatch(matchId = 8L, user1Id = "user1", user2Id = "user9", servedAt = null)
            )

            val dailyBatch = createTestDailyBatch(batchCount = 1)

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns servedUnactedMatches
            coEvery { matchRepository.findUnservedMatches("user1", 5) } returns newMatches // 7 - 2 valid profiles = 5
            coEvery { matchRepository.markAsServed(any(), any()) } returns 5
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch

            // user2 profile not found, user3 and user4 are valid
            coEvery { userService.getUserMatchProfileData(eq("user2"), any()) } returns null
            coEvery { userService.getUserMatchProfileData(eq("user3"), any()) } returns createTestUserMatchProfileData(userId = "user3", name = "Bob")
            coEvery { userService.getUserMatchProfileData(eq("user4"), any()) } returns createTestUserMatchProfileData(userId = "user4", name = "Charlie")
            coEvery { userService.getUserMatchProfileData(eq("user5"), any()) } returns createTestUserMatchProfileData(userId = "user5", name = "Diana")
            coEvery { userService.getUserMatchProfileData(eq("user6"), any()) } returns createTestUserMatchProfileData(userId = "user6", name = "Eve")
            coEvery { userService.getUserMatchProfileData(eq("user7"), any()) } returns createTestUserMatchProfileData(userId = "user7", name = "Frank")
            coEvery { userService.getUserMatchProfileData(eq("user8"), any()) } returns createTestUserMatchProfileData(userId = "user8", name = "Grace")
            coEvery { userService.getUserMatchProfileData(eq("user9"), any()) } returns createTestUserMatchProfileData(userId = "user9", name = "Helen")

            val result = matchService.fetchDailyBatch("user1")

            // Assert: 2 valid carryover + 5 new = 7 total profiles (user2 was filtered out)
            assertEquals(1, result.batchNumber)
            assertEquals(2, result.remainingBatches)
            assertEquals(7, result.profiles.size)
            assertEquals("Bob", result.profiles[0].name)
            assertEquals("Charlie", result.profiles[1].name)
            assertEquals("Diana", result.profiles[2].name)
            assertEquals("Helen", result.profiles[6].name)
        }

        @Test
        fun `should throw NoMatchesAvailableException when only carryovers exist but all profiles unavailable`() = runTest {
            val today = LocalDate.now()
            val servedAt = Instant.now().minus(java.time.Duration.ofDays(1)) // Served yesterday

            val servedUnactedMatches = listOf(
                createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", servedAt = servedAt, liked = null),
                createTestMatch(matchId = 2L, user1Id = "user1", user2Id = "user3", servedAt = servedAt, liked = null)
            )

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns servedUnactedMatches
            coEvery { matchRepository.markAsServed(any(), any()) } returns 2 // Re-mark carryovers
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns emptyList() // 7 - 0 valid profiles = 7
            coEvery { userService.getUserMatchProfileData(eq("user2"), any()) } returns null
            coEvery { userService.getUserMatchProfileData(eq("user3"), any()) } returns null

            val exception = assertThrows<NoMatchesAvailableException> {
                matchService.fetchDailyBatch("user1")
            }

            assertTrue(exception.message!!.contains("No matches available"))
        }

        @Test
        fun `should return same batch without incrementing when called multiple times with unacted matches from today`() = runTest {
            val today = LocalDate.now()
            val servedAt = Instant.now().minusSeconds(3600) // Served 1 hour ago (today)

            // 3 matches served today but not acted upon
            val servedUnactedMatches = listOf(
                createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", servedAt = servedAt, liked = null),
                createTestMatch(matchId = 2L, user1Id = "user1", user2Id = "user3", servedAt = servedAt, liked = null),
                createTestMatch(matchId = 3L, user1Id = "user1", user2Id = "user4", servedAt = servedAt, liked = null)
            )

            val dailyBatch = createTestDailyBatch(batchCount = 1)

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 1
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns servedUnactedMatches
            coEvery { dailyBatchRepository.findByUserAndDate("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData(eq("user2"), any()) } returns createTestUserMatchProfileData(userId = "user2", name = "Alice")
            coEvery { userService.getUserMatchProfileData(eq("user3"), any()) } returns createTestUserMatchProfileData(userId = "user3", name = "Bob")
            coEvery { userService.getUserMatchProfileData(eq("user4"), any()) } returns createTestUserMatchProfileData(userId = "user4", name = "Charlie")

            val result = matchService.fetchDailyBatch("user1")

            // Assert: batch count NOT incremented, same batch returned
            assertEquals(1, result.batchNumber) // Still batch 1
            assertEquals(2, result.remainingBatches) // 3 - 1 = 2 remaining
            assertEquals(3, result.profiles.size)
            assertEquals("Alice", result.profiles[0].name)
            assertEquals("Bob", result.profiles[1].name)
            assertEquals("Charlie", result.profiles[2].name)

            // Verify that batch count was NOT incremented and NO matches were marked as served
            coVerify(exactly = 0) { dailyBatchRepository.incrementBatchCount(any(), any()) }
            coVerify(exactly = 0) { matchRepository.markAsServed(any(), any()) }
            coVerify(exactly = 0) { matchRepository.findUnservedMatches(any(), any()) }
        }

        @Test
        fun `should fetch new batch when all served matches have been acted upon`() = runTest {
            val today = LocalDate.now()
            val newMatches = listOf(
                createTestMatch(matchId = 3L, user1Id = "user1", user2Id = "user4", servedAt = null)
            )
            val userData = createTestUserMatchProfileData(userId = "user4", name = "Charlie", age = 29)
            val dailyBatch = createTestDailyBatch(batchCount = 2)

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 1
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns emptyList()
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns newMatches
            coEvery { matchRepository.markAsServed(any(), any()) } returns 1
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData("user4") } returns userData

            val result = matchService.fetchDailyBatch("user1")

            // Assert: batch count incremented, new matches served
            assertEquals(2, result.batchNumber) // batchCount was 1, now 2
            assertEquals(1, result.remainingBatches) // 3 - 2 = 1 remaining
            assertEquals(1, result.profiles.size)
            assertEquals("Charlie", result.profiles[0].name)

            // Verify that batch count was incremented and new matches were marked as served
            coVerify(exactly = 1) { dailyBatchRepository.incrementBatchCount("user1", today) }
            coVerify(exactly = 1) { matchRepository.markAsServed(any(), any()) }
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
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns emptyList()
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
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns emptyList()
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

    // -------------------------------------------------------------------------
    // Context-aware photo expiry tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `Context-aware photo expiry` {

        @Test
        fun `fetchDailyBatch should use 48h photo expiry for unmatched profiles`() = runTest {
            // Given: A match ready to be served in daily batch
            val today = LocalDate.now(ZoneId.of("UTC"))
            val match = createTestMatch(user2Id = "user2", createdAt = fixedInstant)
            val userData = createTestUserMatchProfileData(userId = "user2")
            val dailyBatch = createTestDailyBatch(batchDate = today)

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns emptyList()
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns listOf(match)
            coEvery { matchRepository.markAsServed(any(), any()) } returns 1
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData("user2", any()) } returns userData

            // When: Fetch daily batch
            matchService.fetchDailyBatch("user1")

            // Then: Should call getUserMatchProfileData with 48h expiry (default for daily batches)
            coVerify(exactly = 1) {
                userService.getUserMatchProfileData(
                    userId = "user2",
                    photoExpiryHours = 48 // Daily batch: 48-hour expiry
                )
            }
        }

        @Test
        fun `getPassesInLast24Hours should use 24h photo expiry for reconsideration`() = runTest {
            // Given: A passed match from last 24 hours
            val passedMatch = createTestMatch(
                user2Id = "user2",
                liked = false,
                createdAt = fixedInstant,
                servedAt = fixedInstant.plusSeconds(3600) // Served 1 hour after creation
            )
            val userData = createTestUserMatchProfileData(userId = "user2")

            coEvery { matchRepository.findPassesInLast24Hours("user1") } returns listOf(passedMatch)
            coEvery { userService.getUserMatchProfileData("user2", any()) } returns userData

            // When: Get passes for reconsideration
            matchService.getPassesInLast24Hours("user1")

            // Then: Should call getUserMatchProfileData with 24h expiry (reconsideration)
            coVerify(exactly = 1) {
                userService.getUserMatchProfileData(
                    userId = "user2",
                    photoExpiryHours = 24 // Reconsideration: 24-hour expiry
                )
            }
        }

        @Test
        fun `fetchDailyBatch should apply 48h expiry to all profiles in batch`() = runTest {
            // Given: Multiple matches in daily batch
            val today = LocalDate.now(ZoneId.of("UTC"))
            val match1 = createTestMatch(matchId = 1L, user2Id = "user2", createdAt = fixedInstant)
            val match2 = createTestMatch(matchId = 2L, user2Id = "user3", createdAt = fixedInstant)
            val match3 = createTestMatch(matchId = 3L, user2Id = "user4", createdAt = fixedInstant)

            val userData2 = createTestUserMatchProfileData(userId = "user2", name = "Alice")
            val userData3 = createTestUserMatchProfileData(userId = "user3", name = "Bob")
            val userData4 = createTestUserMatchProfileData(userId = "user4", name = "Charlie")
            val dailyBatch = createTestDailyBatch(batchDate = today)

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns emptyList()
            coEvery { matchRepository.findUnservedMatches("user1", 7) } returns listOf(match1, match2, match3)
            coEvery { matchRepository.markAsServed(any(), any()) } returns 1
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData("user2", any()) } returns userData2
            coEvery { userService.getUserMatchProfileData("user3", any()) } returns userData3
            coEvery { userService.getUserMatchProfileData("user4", any()) } returns userData4

            // When: Fetch daily batch with multiple profiles
            val result = matchService.fetchDailyBatch("user1")

            // Then: All profiles should have 48h expiry
            assertEquals(3, result.profiles.size)

            coVerify(exactly = 1) {
                userService.getUserMatchProfileData(userId = "user2", photoExpiryHours = 48)
            }
            coVerify(exactly = 1) {
                userService.getUserMatchProfileData(userId = "user3", photoExpiryHours = 48)
            }
            coVerify(exactly = 1) {
                userService.getUserMatchProfileData(userId = "user4", photoExpiryHours = 48)
            }
        }

        @Test
        fun `getPassesInLast24Hours should apply 24h expiry to all passed profiles`() = runTest {
            // Given: Multiple passed matches
            val pass1 = createTestMatch(
                matchId = 1L,
                user2Id = "user2",
                liked = false,
                createdAt = fixedInstant,
                servedAt = fixedInstant.plusSeconds(3600) // Served 1 hour after creation
            )
            val pass2 = createTestMatch(
                matchId = 2L,
                user2Id = "user3",
                liked = false,
                createdAt = fixedInstant,
                servedAt = fixedInstant.plusSeconds(7200) // Served 2 hours after creation
            )

            val userData2 = createTestUserMatchProfileData(userId = "user2", name = "Alice")
            val userData3 = createTestUserMatchProfileData(userId = "user3", name = "Bob")

            coEvery { matchRepository.findPassesInLast24Hours("user1") } returns listOf(pass1, pass2)
            coEvery { userService.getUserMatchProfileData("user2", any()) } returns userData2
            coEvery { userService.getUserMatchProfileData("user3", any()) } returns userData3

            // When: Get all passes for reconsideration
            val result = matchService.getPassesInLast24Hours("user1")

            // Then: All profiles should have 24h expiry
            assertEquals(2, result.size)

            coVerify(exactly = 1) {
                userService.getUserMatchProfileData(userId = "user2", photoExpiryHours = 24)
            }
            coVerify(exactly = 1) {
                userService.getUserMatchProfileData(userId = "user3", photoExpiryHours = 24)
            }
        }
    }

    // -------------------------------------------------------------------------
    // fetchDailyBatch ordering tests
    // -------------------------------------------------------------------------

    @Nested
    inner class `fetchDailyBatch ordering` {

        @Test
        fun `should return profiles ordered by servedAt when combining carryover and new matches`() = runTest {
            val today = LocalDate.now()

            // Carryover match served at an earlier time
            val earlierServedAt = fixedInstant.minusSeconds(3600) // 1 hour ago
            val carryoverMatch = createTestMatch(
                matchId = 1L,
                user1Id = "user1",
                user2Id = "user2",
                servedAt = earlierServedAt,
                liked = null
            )

            // New matches will be served "now" (fixedInstant)
            val newMatch1 = createTestMatch(matchId = 2L, user1Id = "user1", user2Id = "user3", servedAt = null)
            val newMatch2 = createTestMatch(matchId = 3L, user1Id = "user1", user2Id = "user4", servedAt = null)

            val userData1 = createTestUserMatchProfileData(userId = "user2", name = "Alice")
            val userData2 = createTestUserMatchProfileData(userId = "user3", name = "Bob")
            val userData3 = createTestUserMatchProfileData(userId = "user4", name = "Charlie")
            val dailyBatch = createTestDailyBatch(batchCount = 1)

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 0
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns listOf(carryoverMatch)
            coEvery { matchRepository.findUnservedMatches("user1", 6) } returns listOf(newMatch1, newMatch2)
            coEvery { matchRepository.markAsServed(any(), any()) } returns 2
            coEvery { dailyBatchRepository.incrementBatchCount("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData("user2", any()) } returns userData1
            coEvery { userService.getUserMatchProfileData("user3", any()) } returns userData2
            coEvery { userService.getUserMatchProfileData("user4", any()) } returns userData3

            val result = matchService.fetchDailyBatch("user1")

            // Profiles should be sorted by servedAt
            assertEquals(3, result.profiles.size)
            // First profile should be the carryover (earlierServedAt < fixedInstant)
            assertEquals("user2", result.profiles[0].userId)
            assertEquals("Alice", result.profiles[0].name)
            assertEquals(earlierServedAt, result.profiles[0].servedAt)

            // New matches follow (both have fixedInstant as servedAt)
            assertEquals("user3", result.profiles[1].userId)
            assertEquals("user4", result.profiles[2].userId)
        }

        @Test
        fun `should maintain consistent order when re-entering matches tab with current window matches`() = runTest {
            val today = LocalDate.now()

            // Matches already served in current window with different servedAt times
            val servedAt1 = fixedInstant.minusSeconds(3600) // 1 hour ago
            val servedAt2 = fixedInstant.minusSeconds(1800) // 30 minutes ago
            val servedAt3 = fixedInstant.minusSeconds(900)  // 15 minutes ago

            val currentWindowMatches = listOf(
                createTestMatch(matchId = 3L, user1Id = "user1", user2Id = "user4", servedAt = servedAt3, liked = null),
                createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", servedAt = servedAt1, liked = null),
                createTestMatch(matchId = 2L, user1Id = "user1", user2Id = "user3", servedAt = servedAt2, liked = null)
            )

            val userData1 = createTestUserMatchProfileData(userId = "user2", name = "Alice")
            val userData2 = createTestUserMatchProfileData(userId = "user3", name = "Bob")
            val userData3 = createTestUserMatchProfileData(userId = "user4", name = "Charlie")
            val dailyBatch = createTestDailyBatch(batchCount = 1)

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 1
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns currentWindowMatches
            coEvery { dailyBatchRepository.findByUserAndDate("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData("user2", any()) } returns userData1
            coEvery { userService.getUserMatchProfileData("user3", any()) } returns userData2
            coEvery { userService.getUserMatchProfileData("user4", any()) } returns userData3

            val result = matchService.fetchDailyBatch("user1")

            // Profiles should be sorted by servedAt ascending (oldest first)
            assertEquals(3, result.profiles.size)
            assertEquals("user2", result.profiles[0].userId) // servedAt1 (oldest)
            assertEquals(servedAt1, result.profiles[0].servedAt)
            assertEquals("user3", result.profiles[1].userId) // servedAt2 (middle)
            assertEquals(servedAt2, result.profiles[1].servedAt)
            assertEquals("user4", result.profiles[2].userId) // servedAt3 (newest)
            assertEquals(servedAt3, result.profiles[2].servedAt)
        }

        @Test
        fun `should maintain order across multiple calls to fetchDailyBatch`() = runTest {
            val today = LocalDate.now()

            val servedAt1 = fixedInstant.minusSeconds(3600)
            val servedAt2 = fixedInstant.minusSeconds(1800)

            val currentWindowMatches = listOf(
                createTestMatch(matchId = 1L, user1Id = "user1", user2Id = "user2", servedAt = servedAt1, liked = null),
                createTestMatch(matchId = 2L, user1Id = "user1", user2Id = "user3", servedAt = servedAt2, liked = null)
            )

            val userData1 = createTestUserMatchProfileData(userId = "user2", name = "Alice")
            val userData2 = createTestUserMatchProfileData(userId = "user3", name = "Bob")
            val dailyBatch = createTestDailyBatch(batchCount = 1)

            coEvery { dailyBatchRepository.getBatchCount("user1", today) } returns 1
            coEvery { matchRepository.findServedUnactedMatches("user1", 7) } returns currentWindowMatches
            coEvery { dailyBatchRepository.findByUserAndDate("user1", today) } returns dailyBatch
            coEvery { userService.getUserMatchProfileData("user2", any()) } returns userData1
            coEvery { userService.getUserMatchProfileData("user3", any()) } returns userData2

            // Call twice to simulate user refreshing
            val result1 = matchService.fetchDailyBatch("user1")
            val result2 = matchService.fetchDailyBatch("user1")

            // Both results should have identical ordering
            assertEquals(result1.profiles.size, result2.profiles.size)
            assertEquals(result1.profiles[0].userId, result2.profiles[0].userId)
            assertEquals(result1.profiles[0].servedAt, result2.profiles[0].servedAt)
            assertEquals(result1.profiles[1].userId, result2.profiles[1].userId)
            assertEquals(result1.profiles[1].servedAt, result2.profiles[1].servedAt)
        }
    }
}
