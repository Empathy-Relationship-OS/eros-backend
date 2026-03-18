package com.eros.users

import com.eros.common.errors.ForbiddenException

class ProfileAccessControl(
    private val matchAccessChecker: MatchAccessChecker
) {

    suspend fun hasPublicProfileAccess(userId: String, targetUserId: String): Boolean {

        if (userId != targetUserId) {
            // todo also check that they like one another. If they have a pending date then they can also see one anothers profile up to the end of date
            val hasMatch = matchAccessChecker.hasServedMatch(userId, targetUserId)
            val currentBatch = matchAccessChecker.isInCurrentBatch(userId, targetUserId)

            if (!hasMatch && !currentBatch) {
                throw ForbiddenException("You do not have access to this profile")
            }
        }

        return true
    }
}
