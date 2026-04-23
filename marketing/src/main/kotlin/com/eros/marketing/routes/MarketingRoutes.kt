package com.eros.marketing.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import com.eros.auth.extensions.requireRoles
import com.eros.marketing.models.CreateMarketingConsentRequest
import com.eros.marketing.models.MarketingPreferenceResponse
import com.eros.marketing.models.UpdateMarketingConsentRequest
import com.eros.marketing.service.MarketingPreferenceService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.marketingRoutes(marketingPreferenceService: MarketingPreferenceService) {
    route("/marketing") {
        requireRoles("USER", "ADMIN", "EMPLOYEE")

        /**
         * GET /marketing/preference - Get current user's marketing preference
         *
         * Returns the authenticated user's marketing consent record.
         * If no record exists, returns a default record with marketingConsent = false.
         *
         * Responses:
         * - 200 OK: MarketingPreferenceResponse - User's marketing preference
         * - 401 Unauthorized: Not authenticated
         */
        get("/preference") {
            val principal = call.requireFirebasePrincipal()
            val consent = marketingPreferenceService.getMarketingPreference(principal.uid)
            val response = MarketingPreferenceResponse.fromDomain(consent)
            call.respond(HttpStatusCode.OK, response)
        }

        /**
         * POST /marketing/preference - Create marketing preference
         *
         * Creates a new marketing consent record for the authenticated user.
         * User can only create their own preference record.
         *
         * Request body:
         * - marketingConsent (boolean): Whether user consents to marketing communications
         *
         * Responses:
         * - 201 Created: MarketingPreferenceResponse - Created marketing preference
         * - 400 Bad Request: Invalid request body
         * - 401 Unauthorized: Not authenticated
         * - 403 Forbidden: Attempting to create preference for another user
         * - 409 Conflict: Record already exists (use PUT to update)
         */
        post("/preference") {
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<CreateMarketingConsentRequest>()

            val consent = marketingPreferenceService.createMarketingPreference(
                userId = principal.uid,
                requestingUserId = principal.uid,
                marketingConsent = request.marketingConsent
            )

            val response = MarketingPreferenceResponse.fromDomain(consent)
            call.respond(HttpStatusCode.Created, response)
        }

        /**
         * PUT /marketing/preference - Update marketing preference
         *
         * Updates the authenticated user's marketing consent record.
         * Creates a new record if none exists (upsert behavior).
         * User can only update their own preference record.
         *
         * Request body:
         * - marketingConsent (boolean): New marketing consent value
         *
         * Responses:
         * - 200 OK: MarketingPreferenceResponse - Updated marketing preference
         * - 400 Bad Request: Invalid request body
         * - 401 Unauthorized: Not authenticated
         * - 403 Forbidden: Attempting to update preference for another user
         */
        put("/preference") {
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<UpdateMarketingConsentRequest>()

            val consent = marketingPreferenceService.updateMarketingPreference(
                userId = principal.uid,
                requestingUserId = principal.uid,
                marketingConsent = request.marketingConsent
            )

            val response = MarketingPreferenceResponse.fromDomain(consent)
            call.respond(HttpStatusCode.OK, response)
        }
    }

    // Admin/Employee only routes
    route("marketing/admin") {
        requireRoles("ADMIN", "EMPLOYEE")

        /**
         * GET /marketing/admin/preference/{userId} - Get user's marketing preference (Admin only)
         *
         * Returns the specified user's marketing consent record.
         * If no record exists, returns a default record with marketingConsent = false.
         *
         * Responses:
         * - 200 OK: MarketingPreferenceResponse - User's marketing preference
         * - 401 Unauthorized: Not authenticated
         * - 403 Forbidden: Insufficient permissions (not ADMIN or EMPLOYEE)
         * - 404 Not Found: User not found
         */
        get("/preference/{userId}") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "userId parameter is required")

            val consent = marketingPreferenceService.getMarketingPreference(userId)
            val response = MarketingPreferenceResponse.fromDomain(consent)
            call.respond(HttpStatusCode.OK, response)
        }

        /**
         * DELETE /marketing/admin/preference/{userId} - Delete user's marketing preference (Admin only)
         *
         * Deletes the specified user's marketing consent record.
         * Deletion is idempotent (no error if record doesn't exist).
         *
         * Responses:
         * - 204 No Content: Record deleted successfully (or didn't exist)
         * - 401 Unauthorized: Not authenticated
         * - 403 Forbidden: Insufficient permissions (not ADMIN or EMPLOYEE)
         */
        delete("/preference/{userId}") {
            val userId = call.parameters["userId"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "userId parameter is required")

            marketingPreferenceService.deleteMarketingPreference(userId)
            call.respond(HttpStatusCode.NoContent)
        }

        /**
         * GET /marketing/admin/consented - Get all users who consented to marketing (Admin only)
         *
         * Returns a list of all users who have marketingConsent = true.
         * Useful for generating marketing email lists.
         *
         * Responses:
         * - 200 OK: List<MarketingPreferenceResponse> - All consented users
         * - 401 Unauthorized: Not authenticated
         * - 403 Forbidden: Insufficient permissions (not ADMIN or EMPLOYEE)
         */
        get("/consented") {
            val consented = marketingPreferenceService.getAllConsentedUsers()
            val responses = consented.map { MarketingPreferenceResponse.fromDomain(it) }
            call.respond(HttpStatusCode.OK, responses)
        }
    }
}
