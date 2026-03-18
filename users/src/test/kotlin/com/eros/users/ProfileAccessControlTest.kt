package com.eros.users

import com.eros.common.errors.ForbiddenException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for ProfileAccessControl.hasPublicProfileAccess()
 *
 * Tests the access control logic that determines whether a user (viewer) can access
 * another user's (target) public profile based on match status and batch serving.
 */
class ProfileAccessControlTest {

    private val mockMatchAccessChecker = mockk<MatchAccessChecker>()
    private val accessControl = ProfileAccessControl(mockMatchAccessChecker)

    @Nested
    inner class `Same User Access` {

        @Test
        fun `returns true when viewer is the profile owner`() = runTest {
            val userId = "user-123"

            val result = accessControl.hasPublicProfileAccess(userId, userId)

            assertTrue(result, "User should always have access to their own profile")
            // Should not call match checker when viewing own profile
            coVerify(exactly = 0) { mockMatchAccessChecker.hasServedMatch(any(), any()) }
            coVerify(exactly = 0) { mockMatchAccessChecker.isInCurrentBatch(any(), any()) }
        }

        @Test
        fun `returns true for same user with UUID format ID`() = runTest {
            val userId = "550e8400-e29b-41d4-a716-446655440000"

            val result = accessControl.hasPublicProfileAccess(userId, userId)

            assertTrue(result)
            coVerify(exactly = 0) { mockMatchAccessChecker.hasServedMatch(any(), any()) }
        }

        @Test
        fun `returns true for same user with empty string ID`() = runTest {
            val result = accessControl.hasPublicProfileAccess("", "")

            assertTrue(result, "Empty string IDs are equal, so should return true")
        }
    }

    @Nested
    inner class `Different User Access - Has Served Match` {

        @Test
        fun `returns true when viewer has been served the target as a match`() = runTest {
            val viewerId = "viewer"
            val targetId = "target"

            coEvery { mockMatchAccessChecker.hasServedMatch(viewerId, targetId) } returns true
            coEvery { mockMatchAccessChecker.isInCurrentBatch(viewerId, targetId) } returns false

            val result = accessControl.hasPublicProfileAccess(viewerId, targetId)

            assertTrue(result, "Access should be granted when user has been served the match")
            coVerify { mockMatchAccessChecker.hasServedMatch(viewerId, targetId) }
        }

        @Test
        fun `returns true when both hasMatch and currentBatch are true`() = runTest {
            val viewerId = "viewer"
            val targetId = "target"

            coEvery { mockMatchAccessChecker.hasServedMatch(viewerId, targetId) } returns true
            coEvery { mockMatchAccessChecker.isInCurrentBatch(viewerId, targetId) } returns true

            val result = accessControl.hasPublicProfileAccess(viewerId, targetId)

            assertTrue(result)
        }
    }

    @Nested
    inner class `Different User Access - In Current Batch` {

        @Test
        fun `returns true when target is in viewer's current batch`() = runTest {
            val viewerId = "viewer"
            val targetId = "target"

            coEvery { mockMatchAccessChecker.hasServedMatch(viewerId, targetId) } returns false
            coEvery { mockMatchAccessChecker.isInCurrentBatch(viewerId, targetId) } returns true

            val result = accessControl.hasPublicProfileAccess(viewerId, targetId)

            assertTrue(result, "Access should be granted when target is in current batch")
            coVerify { mockMatchAccessChecker.hasServedMatch(viewerId, targetId) }
            coVerify { mockMatchAccessChecker.isInCurrentBatch(viewerId, targetId) }
        }
    }

    @Nested
    inner class `Access Denied Scenarios` {

        @Test
        fun `throws ForbiddenException when no match and not in current batch`() = runTest {
            val viewerId = "viewer"
            val targetId = "target"

            coEvery { mockMatchAccessChecker.hasServedMatch(viewerId, targetId) } returns false
            coEvery { mockMatchAccessChecker.isInCurrentBatch(viewerId, targetId) } returns false

            val exception = assertFailsWith<ForbiddenException> {
                accessControl.hasPublicProfileAccess(viewerId, targetId)
            }

            assertEquals("You do not have access to this profile", exception.message)
            coVerify { mockMatchAccessChecker.hasServedMatch(viewerId, targetId) }
            coVerify { mockMatchAccessChecker.isInCurrentBatch(viewerId, targetId) }
        }

        @Test
        fun `throws ForbiddenException for unmatched users not in batch`() = runTest {
            val viewerId = "user-1"
            val targetId = "user-2"

            coEvery { mockMatchAccessChecker.hasServedMatch(viewerId, targetId) } returns false
            coEvery { mockMatchAccessChecker.isInCurrentBatch(viewerId, targetId) } returns false

            assertFailsWith<ForbiddenException> {
                accessControl.hasPublicProfileAccess(viewerId, targetId)
            }
        }
    }

    @Nested
    inner class `Return Value Verification` {

        @Test
        fun `consistently returns true for repeated calls with same parameters`() = runTest {
            val viewerId = "user-1"
            val targetId = "user-2"

            coEvery { mockMatchAccessChecker.hasServedMatch(viewerId, targetId) } returns true
            coEvery { mockMatchAccessChecker.isInCurrentBatch(viewerId, targetId) } returns false

            val result1 = accessControl.hasPublicProfileAccess(viewerId, targetId)
            val result2 = accessControl.hasPublicProfileAccess(viewerId, targetId)
            val result3 = accessControl.hasPublicProfileAccess(viewerId, targetId)

            assertTrue(result1)
            assertTrue(result2)
            assertTrue(result3)
            assertEquals(result1, result2)
            assertEquals(result2, result3)
        }

        @Test
        fun `access is directional based on match direction`() = runTest {
            val userId1 = "user-1"
            val userId2 = "user-2"

            // user-1 has been served user-2
            coEvery { mockMatchAccessChecker.hasServedMatch(userId1, userId2) } returns true
            coEvery { mockMatchAccessChecker.isInCurrentBatch(userId1, userId2) } returns false

            // user-2 has NOT been served user-1
            coEvery { mockMatchAccessChecker.hasServedMatch(userId2, userId1) } returns false
            coEvery { mockMatchAccessChecker.isInCurrentBatch(userId2, userId1) } returns false

            val result1 = accessControl.hasPublicProfileAccess(userId1, userId2)
            assertTrue(result1, "user-1 should have access to user-2")

            assertFailsWith<ForbiddenException> {
                accessControl.hasPublicProfileAccess(userId2, userId1)
            }
        }
    }
}
