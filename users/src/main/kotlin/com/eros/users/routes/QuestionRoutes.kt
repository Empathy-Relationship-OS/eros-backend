package com.eros.users.routes

import com.eros.auth.extensions.requireRoles
import com.eros.common.errors.BadRequestException
import com.eros.common.errors.NotFoundException
import com.eros.users.models.CreateQuestionRequest
import com.eros.users.models.QuestionDTO
import com.eros.users.models.toDTO
import com.eros.users.service.QAService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlin.text.toLong

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
             * /question/admin
             */
            post{
                val request = call.receive<CreateQuestionRequest>()
                val question = qaService.createNewQuestion(request)

                call.respond(HttpStatusCode.Created, question.toDTO())
            }

            /**
             * /question/{id}
             *
             * Updates a question in the database.
             */
            patch{
                val request = call.receive<QuestionDTO>()
                val question = qaService.updateQuestion(request) ?: throw NotFoundException("Question not found.")
                call.respond(HttpStatusCode.OK, question.toDTO())
            }

            /**
             * /question/{id}
             *
             * Deletes a question from the database.
             */
            delete("/{id}"){
                val targetQuestionId = call.parameters["id"]
                    ?: throw BadRequestException("User ID is required")
                val deleted = qaService.deleteQuestion(targetQuestionId.toLong())

                if (deleted == 0){ throw NotFoundException("Question not found.")}

                call.respond(HttpStatusCode.NoContent)
            }

        }
    }

}