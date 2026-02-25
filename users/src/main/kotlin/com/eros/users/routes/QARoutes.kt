package com.eros.users.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.common.errors.NotFoundException
import com.eros.common.errors.UnauthorizedException
import com.eros.users.ProfileAccessControl
import com.eros.users.models.AddUserQARequest
import com.eros.users.models.CreateQuestionRequest
import com.eros.users.models.DeleteUserQARequest
import com.eros.users.models.UpdateQuestionRequest
import com.eros.users.models.UpdateUserQARequest
import com.eros.users.models.UserQACollection
import com.eros.users.models.UserQACollectionResponse
import com.eros.users.models.toDTO
import com.eros.users.service.QAService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.QARoutes(qaService : QAService, profileAccessControl: ProfileAccessControl) {

    /**
     * Base route /qa.
     */
    route("/qa") {

        // Add a single QA to the authorized user
        put{
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<AddUserQARequest>()

            if (principal.uid != request.userId) throw UnauthorizedException("Can't add a QA for another user.")

            val userQA = qaService.createUserQA(request)
            call.respond(HttpStatusCode.OK, userQA.toDTO())
        }

        // Get all the QA for this user
        get("/me") {
            val principal = call.requireFirebasePrincipal()
            val userQAs = qaService.getAllQAs(principal.uid)
            val collection = UserQACollectionResponse(principal.uid, userQAs.map{it.toDTO()},userQAs.size)
            call.respond(HttpStatusCode.OK, collection)
        }

        // Update a single QA for authorized user.
        patch("/me"){
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<UpdateUserQARequest>()

            if (principal.uid != request.userId) throw UnauthorizedException("Can't add a QA for another user.")

            val userQA = qaService.updateUserQA(request)
            call.respond(HttpStatusCode.OK, userQA.toDTO())
        }

        // Get all the QA's for a single user.
        get("/{id}") {//get someone else's qa - if match or in batch
            val principal = call.requireFirebasePrincipal()
            val targetUserId = call.parameters["id"]
                ?: throw BadRequestException("User ID is required")

            // Ensure the user has access to the account or not.
            //todo: Replace the true values in method once matchService created
            val accessRights = profileAccessControl.hasPublicProfileAccess(principal.uid, targetUserId)
            if (!accessRights) throw UnauthorizedException("User does not have access to $targetUserId QA's.")

            // Retrieve target users QA, form collection and return.
            val userQAs = qaService.getAllQAs(targetUserId)
            val collection = UserQACollectionResponse(targetUserId, userQAs.map{it.toDTO()},userQAs.size)
            call.respond(HttpStatusCode.OK, collection)
        }

        // Delete a single record.
        delete{
            val principal = call.requireFirebasePrincipal()
            val request = call.receive<DeleteUserQARequest>()

            if (principal.uid != request.userId) throw UnauthorizedException("User does not have access to delete ${request.userId} QA.")

            val deleted = qaService.deleteUserQA(request.userId, request.questionId)

            if (deleted == 0) throw NotFoundException("User QA could not be found.")
            call.respond(HttpStatusCode.NoContent)

        }

    }

}