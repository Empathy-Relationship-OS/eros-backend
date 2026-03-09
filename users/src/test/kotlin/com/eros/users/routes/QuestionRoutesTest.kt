package com.eros.users.routes

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.common.errors.ConflictException
import com.eros.common.errors.ForbiddenException
import com.eros.common.errors.NotFoundException
import com.eros.common.errors.UnauthorizedException
import com.eros.common.plugins.configureExceptionHandling
import com.eros.users.models.CreateQuestionRequest
import com.eros.users.models.Question
import com.eros.users.models.QuestionDTO
import com.eros.users.models.UpdateQuestionRequest
import com.eros.users.service.QAService
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

class QuestionRoutesTest {

    private val mockQAService = mockk<QAService>()

    @Nested
    inner class `POST Question`{

        @Test
        fun `successfully POST a question`() = testApplication(){
            setupTestApp()
            val client = configuredClient()

            val request = createValidQuestionRequest()
            val question = createValidQuestion(question = request.question)

            coEvery { mockQAService.createNewQuestion(request) } returns question

            val response = client.post("/questions/admin") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Created, response.status)
            coVerify { mockQAService.createNewQuestion(request) }
        }

        @Test
        fun `Returns 403 when user tried to make question`() = testApplication{
            setupTestApp("USER")
            val client = configuredClient()

            val request = createValidQuestionRequest()

            coEvery { mockQAService.createNewQuestion(request) } throws ForbiddenException("Idk")

            val response = client.post("/questions/admin") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
            coVerify(exactly = 0) { mockQAService.createNewQuestion(request) }

        }

        @Test
        fun `Returns 401 when unauthorized`() = testApplication{
            setupTestApp()
            val client = configuredClient()

            val request = createValidQuestionRequest()

            coEvery { mockQAService.createNewQuestion(request) } throws UnauthorizedException("Idk")

            val response = client.post("/questions/admin") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            coVerify(exactly = 0) { mockQAService.createNewQuestion(request) }

        }

        @Test
        fun `throws conflict if question already exists`() = testApplication{
            setupTestApp()
            val client = configuredClient()

            val request = createValidQuestionRequest()
            coEvery { mockQAService.createNewQuestion(request) } throws ConflictException("Question already exists.")

            val response = client.post("/questions/admin") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            assertEquals(HttpStatusCode.Conflict, response.status)
        }
    }

    @Nested
    inner class `PATCH Questions` {

        @Test
        fun `successful patch a question`() = testApplication{
            setupTestApp()
            val client = configuredClient()

            val question = "does this code even get reviewed"
            val request = UpdateQuestionRequest(1L,question)
            val updatedQuestion = createValidQuestion(1L,question)
            coEvery { mockQAService.updateQuestion(request) } returns updatedQuestion

            val response = client.patch("/questions/admin") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val returnedBody = response.body<QuestionDTO>()

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(question, returnedBody.question)
        }

        @Test
        fun `throws forbidden when user attempts patch`() = testApplication{
            setupTestApp("USER")
            val client = configuredClient()

            val question = "does this code even get reviewed"
            val request = UpdateQuestionRequest(1L,question)
            coEvery { mockQAService.updateQuestion(request) } throws ForbiddenException("Only admin can update question.")

            val response = client.patch("/questions/admin") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

        @Test
        fun `throws 404 error if question not found`()= testApplication{
            setupTestApp()
            val client = configuredClient()
            val request = UpdateQuestionRequest(999L,"Blanket question goes here?")

            coEvery { mockQAService.updateQuestion(request) } throws NotFoundException("Question not found.")

            val response = client.patch("/questions/admin") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    }

    @Nested
    inner class `GET Questions` {

        @Test
        fun `successful ADMIN retrieval of all questions`() = testApplication{
            setupTestApp()
            val client = configuredClient()
            val q1 = createValidQuestion()
            val q2 = createValidQuestion(2L,"Blah blah blah")
            val q3 = createValidQuestion(4L,"If your music taste could talk, what would it say?")
            coEvery { mockQAService.getAllQuestions() } returns listOf(q1,q2,q3)

            val response = client.get("/questions") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.OK, response.status)

            val questions = response.body<List<QuestionDTO>>()

            assertEquals(questions.size, 3)
            assertEquals(questions[0].question, q1.question)
            assertEquals(questions[1].questionId, q2.questionId)
        }

        @Test
        fun `successful USER retrieval of all questions`() = testApplication{
            setupTestApp("USER")
            val client = configuredClient()
            val q1 = createValidQuestion()
            val q2 = createValidQuestion(2L,"Blah blah blah")
            val q3 = createValidQuestion(4L,"If your music taste could talk, what would it say?")
            coEvery { mockQAService.getAllQuestions() } returns listOf(q1,q2,q3)

            val response = client.get("/questions") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.OK, response.status)

            val questions = response.body<List<QuestionDTO>>()

            assertEquals(questions.size, 3)
            assertEquals(questions[0].question, q1.question)
            assertEquals(questions[1].questionId, q2.questionId)
        }

        @Test
        fun `successful retrieval of EMPTY questions`() = testApplication{
            setupTestApp("USER")
            val client = configuredClient()

            coEvery { mockQAService.getAllQuestions() } returns emptyList()

            val response = client.get("/questions") {
                setAuthenticatedUser("test-user-id")
            }
            assertEquals(HttpStatusCode.OK, response.status)

            val questions = response.body<List<QuestionDTO>>()

            assertEquals(questions.size, 0)
        }

    }

    @Nested
    inner class `DELETE Question` {

        @Test
        fun `successful DELETE question`() = testApplication{
            setupTestApp()
            val client = configuredClient()

            val q1 = createValidQuestion()

            coEvery { mockQAService.deleteQuestion(q1.questionId) } returns 1

            val response = client.delete("/questions/admin/${q1.questionId}") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.NoContent, response.status)

        }

        @Test
        fun `Returns 404 when DELETE question that doesn't exist`()= testApplication{
            setupTestApp()
            val client = configuredClient()
            val questionId = 5L

            coEvery { mockQAService.deleteQuestion(questionId) } returns 0

            val response = client.delete("/questions/admin/$questionId") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        @Test
        fun `Return 403 if USER attempts DELETE`()= testApplication{
            setupTestApp("USER")
            val client = configuredClient()
            val questionId = 5L

            val response = client.delete("/questions/admin/$questionId") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

        @Test
        fun `Return 401 if unathorized`()= testApplication {
            setupTestApp()
            val client = configuredClient()
            val questionId = 5L

            val response = client.delete("/questions/admin/$questionId") {
                contentType(ContentType.Application.Json)
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }


    // Helper functions

    private fun createValidQuestion(questionId : Long = 1L,question : String = "If you had 3 genie wishes, what would they be?") : Question{
        val now = Instant.now()
        return Question(questionId, question, now, now)
    }

    private fun createValidQuestionRequest(question : String = "If you had 3 genie wishes, what would they be?") : CreateQuestionRequest {
        return CreateQuestionRequest(question)
    }

    /**
     * Creates a configured HTTP client for tests with JSON content negotiation.
     * Each test should create its own client instance to ensure proper serialization.
     */
    private fun ApplicationTestBuilder.configuredClient() = createClient {
        install(ClientContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    private fun ApplicationTestBuilder.setupTestApp(role : String = "ADMIN") {
        application {
            configureExceptionHandling()
            // Install server-side content negotiation
            install(ServerContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }

            // Install authentication
            install(Authentication) {
                bearer("firebase-auth") {
                    realm = "test-realm"
                    authenticate { credential ->
                        // Return mock principal based on credential token
                        // Token format: "user-{userId}"
                        val userId = credential.token.removePrefix("user-")
                        val mockToken = mockk<com.google.firebase.auth.FirebaseToken>(relaxed = true) {
                            coEvery { uid } returns userId
                        }
                        FirebaseUserPrincipal(
                            uid = userId,
                            email = "$userId@example.com",
                            phoneNumber = null,
                            emailVerified = true,
                            token = mockToken,
                            role = role
                        )
                    }
                }
            }

            routing {
                authenticate("firebase-auth") {
                    questionRoutes(mockQAService)
                }
            }
        }
    }

    private fun HttpRequestBuilder.setAuthenticatedUser(userId: String) {
        header(HttpHeaders.Authorization, "Bearer user-$userId")
    }

}