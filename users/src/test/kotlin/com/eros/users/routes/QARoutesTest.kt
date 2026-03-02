package com.eros.users.routes

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.common.errors.BadRequestException
import com.eros.common.errors.ConflictException
import com.eros.common.errors.NotFoundException
import com.eros.common.plugins.configureExceptionHandling
import com.eros.users.ProfileAccessControl
import com.eros.users.models.AddUserQARequest
import com.eros.users.models.DeleteUserQARequest
import com.eros.users.models.Question
import com.eros.users.models.QuestionDTO
import com.eros.users.models.UpdateUserQARequest
import com.eros.users.models.UserQACollection
import com.eros.users.models.UserQACollectionDTO
import com.eros.users.models.UserQAItem
import com.eros.users.models.UserQAItemResponse
import com.eros.users.service.QAService
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
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
import kotlin.collections.emptyList
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class QARoutesTest {

    private val mockQAService = mockk<QAService>()
    private val mockProfileAccessControl = mockk<ProfileAccessControl>()

    @Nested
    inner class `POST QA` {
        @Test
        fun `successful POST user QA`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = createValidQARequest()
            val qa = createValidQA()

            coEvery { mockQAService.createUserQA(request) } returns qa
            //coEvery { mockQAService.getUserQAsCount(request.userId)} returns 0

            val response = client.post("/users/qa") {
                setAuthenticatedUser(request.userId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Created, response.status)
            val returnedQA = response.body<UserQAItemResponse>()
            println(returnedQA)

            coVerify { mockQAService.createUserQA(request) }
        }

        @Test
        fun `should return 401 when not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = createValidQARequest()

            val response = client.post("/users/qa") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `should return 403 when userId does not match principal`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = createValidQARequest("unauthorized-test-user")

            val response = client.post("/users/qa") {
                setAuthenticatedUser("not-provided-user")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertTrue(response.bodyAsText().contains("Can't add a QA for another user."))
            coVerify(exactly = 0) { mockQAService.createUserQA(any()) }
        }

        @Test
        fun `should return 400 when user has 3+ QA already`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = createValidQARequest("unauthorized-test-user")

            coEvery { mockQAService.createUserQA(request) } throws BadRequestException("Maximum of 3 Q&A's allowed per user.")

            val response = client.post("/users/qa") {
                setAuthenticatedUser(request.userId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Maximum of 3 Q&A's allowed per user."))
        }

        @Test
        fun `returns 409 when service throws ConflictException`() = testApplication {
            setupTestApp()
            val client = configuredClient()
            val qa = createValidQARequest()
            coEvery { mockQAService.createUserQA(any()) } throws ConflictException("User already has answered this question.")

            val response = client.post("/users/qa") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(qa)
            }

            assertEquals(HttpStatusCode.Conflict, response.status)
        }
    }

    @Nested
    inner class `GET QA`{

        @Test
        fun `GET me returns user's QAs`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-123"
            val qa1 = createValidQA(userId = userId, questionId = 1L)
            val qa2 = createValidQA(userId = userId, questionId = 2L)

            coEvery { mockQAService.getAllUserQAs(userId) } returns listOf(qa1, qa2)

            val response = client.get("/users/qa/me") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val collection = response.body<UserQACollectionDTO>()
            assertEquals(userId, collection.userId)
            assertEquals(2, collection.totalCount)
            assertEquals(2, collection.qas.size)

            coVerify { mockQAService.getAllUserQAs(userId) }
        }

        @Test
        fun `GET me returns empty collection when user has no QAs`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-123"
            coEvery { mockQAService.getAllUserQAs(userId) } returns emptyList()

            val response = client.get("/users/qa/me") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val collection = response.body<UserQACollectionDTO>()
            assertEquals(0, collection.totalCount)
            assertEquals(emptyList(), collection.qas)
        }

        @Test
        fun `GET me returns 401 when user not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/users/qa/me")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            coVerify(exactly = 0) { mockQAService.getAllUserQAs(any()) }
        }

    }

    @Nested
    inner class `PATCH QA`{

        @Test
        fun `PATCH me successfully updates user QA`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-123"
            val request = validUpdateQARequest(userId)
            val updatedQA = createValidQA(userId = userId, questionId = request.question.questionId)

            coEvery { mockQAService.updateUserQA(request) } returns updatedQA

            val response = client.patch("/users/qa/me") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { mockQAService.updateUserQA(request) }
        }

        @Test
        fun `PATCH me returns 403 when trying to update another user's QA`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val authenticatedUser = "user-123"
            val targetUser = "other-user-456"
            val request = validUpdateQARequest(targetUser)

            val response = client.patch("/users/qa/me") {
                setAuthenticatedUser(authenticatedUser)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertTrue(response.bodyAsText().contains("Can't add a QA for another user"))
            coVerify(exactly = 0) { mockQAService.updateUserQA(any()) }
        }

        @Test
        fun `PATCH me returns 404 when QA not found`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-123"
            val request = validUpdateQARequest(userId, questionId = 999L)

            coEvery { mockQAService.updateUserQA(request) } throws NotFoundException("QA not found")

            val response = client.patch("/users/qa/me") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

        @Test
        fun `PATCH me returns 401 when user not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = validUpdateQARequest()

            val response = client.patch("/users/qa/me") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    }

    @Nested
    inner class `DELETE QA`{

        @Test
        fun `DELETE successfully removes user QA`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-123"
            val request = DeleteUserQARequest(userId = userId, questionId = 1L)

            coEvery { mockQAService.deleteUserQA(userId, 1L) } returns 1

            val response = client.delete("/users/qa") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.NoContent, response.status)
            coVerify { mockQAService.deleteUserQA(userId, 1L) }
        }

        @Test
        fun `DELETE returns 401 when trying to delete another user's QA`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val authenticatedUser = "user-123"
            val targetUser = "other-user-456"
            val request = DeleteUserQARequest(userId = targetUser, questionId = 1L)

            val response = client.delete("/users/qa") {
                setAuthenticatedUser(authenticatedUser)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertTrue(response.bodyAsText().contains("does not have access to delete"))
            coVerify(exactly = 0) { mockQAService.deleteUserQA(any(), any()) }
        }

        @Test
        fun `DELETE returns 404 when QA not found`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "test-user-123"
            val request = DeleteUserQARequest(userId = userId, questionId = 999L)

            coEvery { mockQAService.deleteUserQA(userId, 999L) } returns 0

            val response = client.delete("/users/qa") {
                setAuthenticatedUser(userId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertTrue(response.bodyAsText().contains("could not be found"))
        }

        @Test
        fun `DELETE returns 401 when user not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = DeleteUserQARequest(userId = "test-user", questionId = 1L)

            val response = client.delete("/users/qa") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    }

    @Nested
    inner class `GET by userId`{

        @Test
        fun `GET by id returns QAs when user has access`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val authenticatedUser = "user-123"
            val targetUser = "target-user-456"
            val qa1 = createValidQA(userId = targetUser, questionId = 1L)
            val qa2 = createValidQA(userId = targetUser, questionId = 2L)

            coEvery {
                mockProfileAccessControl.hasPublicProfileAccess(authenticatedUser, targetUser)
            } returns true
            coEvery { mockQAService.getAllUserQAs(targetUser) } returns listOf(qa1, qa2)

            val response = client.get("/users/qa/$targetUser") {
                setAuthenticatedUser(authenticatedUser)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val collection = response.body<UserQACollectionDTO>()
            assertEquals(targetUser, collection.userId)
            assertEquals(2, collection.totalCount)

            coVerify { mockProfileAccessControl.hasPublicProfileAccess(authenticatedUser, targetUser) }
            coVerify { mockQAService.getAllUserQAs(targetUser) }
        }

        @Test
        fun `GET by id returns 403 when user doesn't have access`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val authenticatedUser = "user-123"
            val targetUser = "target-user-456"

            coEvery {
                mockProfileAccessControl.hasPublicProfileAccess(authenticatedUser, targetUser)
            } returns false

            val response = client.get("/users/qa/$targetUser") {
                setAuthenticatedUser(authenticatedUser)
            }

            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertTrue(response.bodyAsText().contains("does not have access"))

            coVerify(exactly = 0) { mockQAService.getAllUserQAs(any()) }
        }

        @Test
        fun `GET by id works when accessing own QAs`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val userId = "user-123"
            val qa = createValidQA(userId = userId)

            coEvery {
                mockProfileAccessControl.hasPublicProfileAccess(userId, userId)
            } returns true
            coEvery { mockQAService.getAllUserQAs(userId) } returns listOf(qa)

            val response = client.get("/users/qa/$userId") {
                setAuthenticatedUser(userId)
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        @Test
        fun `GET by id returns 401 when user not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/users/qa/some-user-id")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `GET by id returns empty collection when target user has no QAs`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val authenticatedUser = "user-123"
            val targetUser = "target-user-456"

            coEvery {
                mockProfileAccessControl.hasPublicProfileAccess(authenticatedUser, targetUser)
            } returns true
            coEvery { mockQAService.getAllUserQAs(targetUser) } returns emptyList()

            val response = client.get("/users/qa/$targetUser") {
                setAuthenticatedUser(authenticatedUser)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val collection = response.body<UserQACollectionDTO>()
            assertEquals(0, collection.totalCount)
        }

    }

    @Nested
    inner class `UPDATE entire collection`(){

        @Test
        fun `successful create,update of entire collection`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = UserQACollectionDTO("test-user-id",validQAList(),3)
            val updated = UserQACollection("test-user-id", validQAItemList(),3)

            coEvery { mockQAService.createUserQACollection(request)} returns updated

            val response = client.post("/users/qa/me/collection") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.OK, response.status)


        }

    }


    // Helper functions

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

    private fun ApplicationTestBuilder.setupTestApp() {
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
                            role = "USER"
                        )
                    }
                }
            }

            routing {
                authenticate("firebase-auth") {
                    qaRoutes(mockQAService, mockProfileAccessControl)
                }
            }
        }
    }

    private fun HttpRequestBuilder.setAuthenticatedUser(userId: String) {
        header(HttpHeaders.Authorization, "Bearer user-$userId")
    }

    private val question1 = "What do you see when you close your eyes?"
    private val answer1 = "Darkness..."
    private fun createValidQARequest(userId: String = "test-user-id"): AddUserQARequest {
        return AddUserQARequest(
            userId,
            QuestionDTO(1L, question1),
            answer1,
            1
        )
    }

    private fun createValidQA(userId: String = "test-user-id", questionId:Long=1L): UserQAItem {
        val now = Instant.now()
        return UserQAItem(
            userId = userId,
            question = Question(questionId, question1, now,now),
            answer = answer1,
            displayOrder = 1,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    private fun validQAList(userId: String = "test-user-id", size : Int = 3): List<UserQAItemResponse>{
        val lst = emptyList<UserQAItemResponse>()
        for(i in 1..size){
            lst.plus(UserQAItemResponse(userId,QuestionDTO(i.toLong(),"Do is something you would change about other people?"),"Yes",i ))
        }
        return lst
    }

    private fun validQAItemList(userId: String = "test-user-id", size : Int = 3): List<UserQAItem>{
        val lst = emptyList<UserQAItem>()
        val now = Instant.now()
        for(i in 1..size){
            lst.plus(UserQAItem(userId,
                Question(i.toLong(),"Do is something you would change about other people?",now, now),
                "Yes",
                i,
                now,
                now
                )
            )
        }
        return lst
    }

    private fun validUpdateQARequest(userId: String = "test-user-id", questionId:Long=1L) : UpdateUserQARequest{
        return UpdateUserQARequest(
            userId = userId,
            question = QuestionDTO(questionId,"Do is something you would change about other people?"),
            answer = "Updated answer",
            displayOrder = 2
        )
    }
}
