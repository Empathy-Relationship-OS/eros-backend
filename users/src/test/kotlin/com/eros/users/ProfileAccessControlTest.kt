package com.eros.users

import com.eros.common.errors.ForbiddenException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for ProfileAccessControl.hasPublicProfileAccess()
 *
 * Tests focus on BEHAVIOR rather than implementation details:
 * - WHAT access should be granted or denied
 * - NOT HOW the access checks are performed internally
 *
 * This approach allows the implementation to evolve (e.g., adding new access rules,
 * reordering checks, caching) without requiring test changes.
 */
class ProfileAccessControlTest {

    private val mockMatchAccessChecker = mockk<MatchAccessChecker>()
    private val accessControl = ProfileAccessControl(mockMatchAccessChecker)

    @Nested
    inner class `Access Granted Scenarios` {

        @Test
        fun `allows user to view their own profile`() = runTest {
            val userId = "user-123"

            val result = accessControl.hasPublicProfileAccess(userId, userId)

            assertTrue(result)
        }

        @Test
        fun `allows viewing profile when users are matched and in current batch`() = runTest {
            val viewerId = "viewer"
            val targetId = "target"

            setupMatchedUsersInCurrentBatch(viewerId, targetId)

            val result = accessControl.hasPublicProfileAccess(viewerId, targetId)

            assertTrue(result)
        }

        @Test
        fun `allows viewing profile with UUID format IDs when matched`() = runTest {
            val viewerId = "550e8400-e29b-41d4-a716-446655440000"
            val targetId = "660e8400-e29b-41d4-a716-446655440001"

            setupMatchedUsersInCurrentBatch(viewerId, targetId)

            val result = accessControl.hasPublicProfileAccess(viewerId, targetId)

            assertTrue(result)
        }
    }

    @Nested
    inner class `Access Denied Scenarios` {

        @Test
        fun `denies access when users have no match relationship`() = runTest {
            val viewerId = "viewer"
            val targetId = "target"

            setupNoMatchRelationship(viewerId, targetId)

            assertFalse { accessControl.hasPublicProfileAccess(viewerId, targetId) }
        }

        @Test
        fun `denies access when match exists but not in current batch`() = runTest {
            val viewerId = "viewer"
            val targetId = "target"

            setupExpiredMatch(viewerId, targetId)

            assertFalse { accessControl.hasPublicProfileAccess(viewerId, targetId) }
        }

        @Test
        fun `denies access when in current batch but no match exists`() = runTest {
            val viewerId = "viewer"
            val targetId = "target"

            setupInBatchButNoMatch(viewerId, targetId)

            assertFalse { accessControl.hasPublicProfileAccess(viewerId, targetId) }
        }
    }

    @Nested
    inner class `Access Directionality` {

        @Test
        fun `access is directional - viewer can see target but not vice versa`() = runTest {
            val userId1 = "user-1"
            val userId2 = "user-2"

            // user-1 can view user-2
            setupMatchedUsersInCurrentBatch(userId1, userId2)
            // user-2 cannot view user-1
            setupNoMatchRelationship(userId2, userId1)

            val result1 = accessControl.hasPublicProfileAccess(userId1, userId2)
            assertTrue(result1)

            assertFalse { accessControl.hasPublicProfileAccess(userId2, userId1) }
        }
    }

    @Nested
    inner class `Edge Cases` {

        @Test
        fun `allows same user with empty string ID`() = runTest {
            val result = accessControl.hasPublicProfileAccess("", "")

            assertTrue(result)
        }
    }

    // Test Data Builders - encapsulate mock setup to reduce coupling

    private fun setupMatchedUsersInCurrentBatch(viewerId: String, targetId: String) {
        coEvery { mockMatchAccessChecker.hasServedMatch(viewerId, targetId) } returns true
        coEvery { mockMatchAccessChecker.isInCurrentBatch(viewerId, targetId) } returns true
    }

    private fun setupNoMatchRelationship(viewerId: String, targetId: String) {
        coEvery { mockMatchAccessChecker.hasServedMatch(viewerId, targetId) } returns false
        coEvery { mockMatchAccessChecker.isInCurrentBatch(viewerId, targetId) } returns false
    }

    private fun setupExpiredMatch(viewerId: String, targetId: String) {
        coEvery { mockMatchAccessChecker.hasServedMatch(viewerId, targetId) } returns true
        coEvery { mockMatchAccessChecker.isInCurrentBatch(viewerId, targetId) } returns false
    }

    private fun setupInBatchButNoMatch(viewerId: String, targetId: String) {
        coEvery { mockMatchAccessChecker.hasServedMatch(viewerId, targetId) } returns false
        coEvery { mockMatchAccessChecker.isInCurrentBatch(viewerId, targetId) } returns true
    }
}
