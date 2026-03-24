package com.eros

import com.eros.matching.service.MatchService
import com.eros.users.MatchAccessChecker

/**
 * Implementation of MatchAccessChecker that delegates to MatchService.
 *
 * This adapter resolves the circular dependency between users and matching modules.
 */
class MatchAccessCheckerImpl(
    private val matchService: MatchService
) : MatchAccessChecker {

    override suspend fun hasServedMatch(userId: String, targetUserId: String): Boolean {
        return matchService.hasServedMatch(userId, targetUserId)
    }

    override suspend fun isInCurrentBatch(userId: String, targetUserId: String): Boolean {
        return matchService.isInCurrentBatch(userId, targetUserId)
    }
}
