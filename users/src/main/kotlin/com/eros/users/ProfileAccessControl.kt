package com.eros.users

import com.eros.common.errors.ForbiddenException

object ProfileAccessControl {

    fun hasPublicProfileAccess(userId : String, targetUserId : String) : Boolean{

        if (userId != targetUserId) {
            val hasMatch = true//matchService.hasMatch(principal.uid, targetUserId)
            val currentBatch = true//matchService.currentBatch(userId).contains(targetUserId)
            if (!hasMatch && currentBatch) throw ForbiddenException("You do not have access to this profile")
        }
        return true
    }

}