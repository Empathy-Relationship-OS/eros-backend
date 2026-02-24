package com.eros.users

import com.eros.common.errors.ForbiddenException

class ProfileAccessControl(
    //private val matchService: MatchService
) {

    fun hasPublicProfileAccess(userId: String, targetUserId: String): Boolean {

        if (userId != targetUserId) {
            val hasMatch = true//matchService.hasMatch(userId, targetUserId)
            val currentBatch = true//matchService.currentBatch(userId) .contains(targetUserId)

            if (!hasMatch && !currentBatch) {
                throw ForbiddenException("You do not have access to this profile")
            }
        }

        return true
    }
}