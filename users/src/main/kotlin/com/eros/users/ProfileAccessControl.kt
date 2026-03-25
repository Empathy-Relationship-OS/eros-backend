package com.eros.users

import com.eros.common.errors.ForbiddenException

class ProfileAccessControl(
    private val matchAccessChecker: MatchAccessChecker
) {

    suspend fun hasPublicProfileAccess(userId: String, targetUserId: String): Boolean {

        if (userId == targetUserId) return true

        // TODO: also check that they like one another. If they have a pending date then they can also see one another's profile up to the end of date
        val hasMatch = matchAccessChecker.hasServedMatch(userId, targetUserId)
        if (!hasMatch) return false

        val currentBatch = matchAccessChecker.isInCurrentBatch(userId, targetUserId)
        return currentBatch
    }
}
