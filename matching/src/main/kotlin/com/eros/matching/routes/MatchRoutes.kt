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

        get("/latest/{uid}") {
            // get the most recent 24 hours action of user from match table, where decision is not null and equal to false on liked
            // this will allow a user to get a view of the last 24 hours, so can like if they accidentally said no
            // due to events they cant unsend a like once sent will have to cancel date in date section
        }

        get("/today/{uid}") {
            // fetch the daily batch's for today
            // a user has 21 potential matchs available a day
            // we will send this in 3 lots of 7
            // they should not be able to get the next batch until all actionable events taken upon the users presented
            // returned value should be a list of userIds with matchId.
            // The system should then call get on the public profile info
            // Or should we return a list of basic info with userId, such as Lightweight Public Profile that will allow a user
            // to see the name, thumbnail and badges until they click and get full profile?
            // probably more optimal for later, but does this cross concerns modules or does it simply quicken up process
        }

        patch("action/{matchId}"){
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<MatchActionRequest>()

            val matchId = call.parameters["matchId"]?.toLong()
                ?: throw BadRequestException("Invalid matchId provided.")

            if (principal.uid != request.fromUserId) {
                throw BadRequestException("Invalid fromUserId provided.")
            }

            if (matchId != request.matchId) {
                throw BadRequestException("Invalid matchId provided.")
            }

            // update match info the only alterable field here is like or not for me
            val mutualMatchInfo = matchService.matchUser(matchId, request.liked)

            if (mutualMatchInfo != null) {
                call.respond(HttpStatusCode.OK, mutualMatchInfo)
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}