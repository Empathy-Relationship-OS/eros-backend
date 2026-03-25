package com.eros.matching.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.matching.models.DailyBatchLimitError
import com.eros.matching.models.MatchActionRequest
import com.eros.matching.service.DailyBatchLimitExceededException
import com.eros.matching.service.MatchService
import com.eros.matching.service.NoMatchesAvailableException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Clock
import java.time.Duration
import java.time.Instant


fun Route.matchRoutes(matchService: MatchService, clock: Clock = Clock.systemUTC()) {
    route("/match/admin") {
        requireRoles("ADMIN", "EMPLOYEE")

        // TODO implement admin endpoints
        get("/user/{uid}") {
            // get all matches for user
            // query params here allow pagination
            call.respond(HttpStatusCode.NotImplemented, "Not yet implemented")
        }

        get("/user/{uid}/{secondaryUid}") {
            // this is used when matchId is unknown but you know the user who did the action against the other user
            call.respond(HttpStatusCode.NotImplemented, "Not yet implemented")
        }

        get("/{matchId}"){
            call.parameters["matchId"]?.toLong()
                ?: throw BadRequestException("Invalid matchId provided.")
            call.respond(HttpStatusCode.NotImplemented, "Not yet implemented")
        }

        post("/") {
            // gives admin ability to create if system goes down
            // realistically this should never happen or need to be used
            call.respond(HttpStatusCode.NotImplemented, "Not yet implemented")
        }

        patch("/{matchId}"){
            // update the match with the body details.
            call.respond(HttpStatusCode.NotImplemented, "Not yet implemented")
        }

        delete("/{matchId}"){
            // if faulty match in db allows an admin/employee to delete
            call.respond(HttpStatusCode.NotImplemented, "Not yet implemented")
        }
    }

    route("/match") {
        requireRoles("ADMIN", "USER", "EMPLOYEE")

        /**
         * GET /matches - Fetch daily batch of matches
         *
         * Returns a batch of up to 7 unserved matches for the authenticated user.
         * Users can fetch maximum 3 batches per day (21 total matches).
         *
         * Responses:
         * - 200 OK: DailyBatchResponse - Successfully fetched batch with metadata
         * - 204 No Content: No unserved matches available
         * - 429 Too Many Requests: Daily batch limit (3) exceeded (includes Retry-After header)
         * - 401 Unauthorized: Not authenticated
         */
        get("/") {
            val principal = call.requireFirebasePrincipal()

            try {
                val batchResponse = matchService.fetchDailyBatch(principal.uid)
                call.respond(HttpStatusCode.OK, batchResponse)
            } catch (_: NoMatchesAvailableException) {
                call.respond(HttpStatusCode.NoContent)
            } catch (e: DailyBatchLimitExceededException) {
                // Calculate seconds until midnight UTC (when limit resets)
                val now = Instant.now(clock)
                val secondsUntilReset = maxOf(0, Duration.between(now, e.resetAt).seconds)

                // Add Retry-After header with seconds until reset
                call.response.header(HttpHeaders.RetryAfter, secondsUntilReset.toString())

                // Return structured error response
                val errorResponse = DailyBatchLimitError(
                    error = "Daily batch limit exceeded",
                    batchesUsed = e.batchesUsed,
                    maxBatches = e.maxBatches,
                    resetAt = e.resetAt
                )
                call.respond(HttpStatusCode.TooManyRequests, errorResponse)
            }
        }

        /**
         * GET /match/last-24 - Fetch profiles user passed on in last 24 hours
         *
         * Returns all profiles the authenticated user said "no" to within the last 24 hours,
         * allowing them to reconsider and potentially change their decision to "like".
         * This is a "second chance" feature for accidental swipes.
         *
         * The 24-hour window is calculated from the servedAt timestamp (when the match was shown).
         * Users can change pass→like within this window by calling PATCH /match/action/{matchId}.
         *
         * Responses:
         * - 200 OK: List<UserMatchProfile> - List of passed profiles with matchIds
         * - 200 OK: [] - Empty list if no passes in last 24 hours
         * - 401 Unauthorized: Not authenticated
         */
        get("/last-24") {
            val principal = call.requireFirebasePrincipal()
            val passes = matchService.getPassesInLast24Hours(principal.uid)
            call.respond(HttpStatusCode.OK, passes)
        }

        /**
         * PATCH /match/action/{matchId} - Take action on a potential match
         *
         * Allows a user to like or pass on a match they've been served.
         *
         * Request body:
         * - liked (boolean): true for like, false for pass
         *
         * Responses:
         * - 200 OK: MutualMatchInfo - Both users liked each other (mutual match)
         * - 204 No Content: Action recorded, but no mutual match
         * - 400 Bad Request: Invalid matchId format
         * - 401 Unauthorized: Not authenticated
         * - 403 Forbidden: User doesn't own this match
         * - 409 Conflict: User already took action on this match
         */
        patch("action/{matchId}") {
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<MatchActionRequest>()

            val matchId = call.parameters["matchId"]?.toLongOrNull()
                ?: throw BadRequestException("Invalid matchId provided")

            // Service handles all validation: ownership, conflict detection, mutual match logic
            val mutualMatchInfo = matchService.matchUser(matchId, principal.uid, request.liked)

            if (mutualMatchInfo != null) {
                call.respond(HttpStatusCode.OK, mutualMatchInfo)
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}