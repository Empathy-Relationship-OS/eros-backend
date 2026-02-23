package com.eros.users

import com.eros.common.errors.ForbiddenException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ProfileAccessControlTest {


}   /*
    private val matchService = mockk<MatchService>()
    private val accessControl = ProfileAccessControl(matchService)

    @Test
    fun `same user always has access`() {
        val result = accessControl.hasPublicProfileAccess("user-1", "user-1")
        assertTrue(result)
    }

    @Test
    fun `has match allows access`() {
        every { matchService.hasMatch("user-1", "user-2") } returns true
        every { matchService.currentBatch("user-1") } returns emptyList()

        val result = accessControl.hasPublicProfileAccess("user-1", "user-2")

        assertTrue(result)
    }

    @Test
    fun `not matched but not in batch allows access`() {
        every { matchService.hasMatch("user-1", "user-2") } returns false
        every { matchService.currentBatch("user-1") } returns emptyList()

        val result = accessControl.hasPublicProfileAccess("user-1", "user-2")

        assertTrue(result)
    }

    @Test
    fun `not matched but in current batch throws ForbiddenException`() {
        every { matchService.hasMatch("user-1", "user-2") } returns false
        every { matchService.currentBatch("user-1") } returns listOf("user-2")

        assertFailsWith<ForbiddenException> {
            accessControl.hasPublicProfileAccess("user-1", "user-2")
        }
    }
}
*/