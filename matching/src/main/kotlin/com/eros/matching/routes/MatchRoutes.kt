package com.eros.matching.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.matching.models.MatchActionRequest
import com.eros.matching.service.DailyBatchLimitExceededException
import com.eros.matching.service.MatchService
import com.eros.matching.service.NoMatchesAvailableException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route


fun Route.matchRoutes(matchService: MatchService) {
    route("/match/admin") {
        requireRoles("ADMIN", "EMPLOYEE")

        get("/user/{uid}") {
            // get all matchs for user
            // query params here allow pagination
        }

        get("/user/{uid}/{secondaryUid}") {
            // this is used when matchId is unknown but you know the user who did the action against the other user
        }

        get("/{matchId}"){
            val id = call.parameters["matchId"]?.toLong()
                ?: throw BadRequestException("Invalid matchId provided.")
        }

        post("/") {
            // gives admin ability to create if system goes down
            // realistically this should never happen or need to be used
        }

        patch("/{matchId}"){
            // update the match with the body details.
        }

        delete("/{matchId}"){
            // if faulty match in db allows an admin/employee to delete
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
         * - 200 OK: List<UserMatchProfile> - Successfully fetched batch
         * - 204 No Content: No unserved matches available
         * - 429 Too Many Requests: Daily batch limit (3) exceeded
         * - 401 Unauthorized: Not authenticated
         */
        get("/") {
            val principal = call.requireFirebasePrincipal()

            try {
                val matches = matchService.fetchDailyBatch(principal.uid)
                call.respond(HttpStatusCode.OK, matches)
            } catch (e: NoMatchesAvailableException) {
                call.respond(HttpStatusCode.NoContent)
            } catch (e: DailyBatchLimitExceededException) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to e.message))
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