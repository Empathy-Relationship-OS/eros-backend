package com.eros.users.routes

import com.eros.auth.extensions.requireFirebasePrincipal
import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.common.errors.NotFoundException
import com.eros.users.models.CreateQuestionRequest
import com.eros.users.models.UpdateQuestionRequest
import com.eros.users.models.toDTO
import com.eros.users.service.QAService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.questionRoutes(qaService: QAService) {

    /**
     * Base route /questions.
     */
    route("/questions") {
        requireRoles("USER","ADMIN","EMPLOYEE")

        /**
         * /questions
         *
         * Gets all the questions from the database.
         */
        get{
            val questions = qaService.getAllQuestions()
            call.respond(HttpStatusCode.OK, questions.map { it.toDTO() })
        }

        //Admin only routes
        route("/admin"){
            requireRoles("ADMIN","EMPLOYEE")

            /**
             * /questions/admin
             */
            post{
                val request = call.receive<CreateQuestionRequest>()
                val question = qaService.createNewQuestion(request)

                call.application.log.info("Admin create performed on question ${question.questionId} by ${call.requireFirebasePrincipal().uid}")
                call.respond(HttpStatusCode.Created, question.toDTO())
            }

            /**
             * /questions/admin
             *
             * Updates a question in the database.
             */
            patch{
                val request = call.receive<UpdateQuestionRequest>()
                val question = qaService.updateQuestion(request) ?: throw NotFoundException("Question not found.")
                call.application.log.info("Admin updated performed on question ${question.questionId} by ${call.requireFirebasePrincipal().uid}")
                call.respond(HttpStatusCode.OK, question.toDTO())
            }

            /**
             * /questions/admin/{id}
             *
             * Deletes a question from the database.
             */
            delete("/{id}"){
                val targetQuestionId = call.parameters["id"]?.toLongOrNull()
                    ?: throw BadRequestException("Invalid questionId provided")
                val deleted = qaService.deleteQuestion(targetQuestionId)

                if (deleted == 0){ throw NotFoundException("Question not found.")}

                call.application.log.info("Admin delete performed on question $targetQuestionId by ${call.requireFirebasePrincipal().uid}")
                call.respond(HttpStatusCode.NoContent)
            }

        }
    }

}