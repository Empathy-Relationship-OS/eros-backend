package com.eros.users.routes

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.common.plugins.configureExceptionHandling
import com.eros.users.models.*
import com.eros.users.service.PreferenceService
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import software.amazon.awssdk.profiles.Profile
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserPreferenceRoutesTest {

    //todo: Implement tests
    private val mockUserPreferenceService = mockk<PreferenceService>()


    @Nested
    inner class GetUserPreferences {

        @Test
        fun `GET me returns user preferences when they exist`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val testUserId = "test-user-123"
            val testPreference = createTestUserPreference(userId = testUserId)

            coEvery { mockUserPreferenceService.findByUserId(testUserId) } returns testPreference

            val response = client.get("/preference/me") {
                setAuthenticatedUser(testUserId)
            }

            assertEquals(HttpStatusCode.OK, response.status)

            val responseBody = response.body<UserPreferenceDTO>()
            assertEquals(testUserId, responseBody.userId)
            assertEquals(testPreference.genderIdentities, responseBody.genderIdentities)
            assertEquals(testPreference.ageRangeMin, responseBody.ageRangeMin)
            assertEquals(testPreference.ageRangeMax, responseBody.ageRangeMax)
            println(responseBody)
            coVerify(exactly = 1) { mockUserPreferenceService.findByUserId(testUserId) }
        }

        @Test
        fun `GET me returns 404 when preferences don't exist`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val testUserId = "test-user-456"

            coEvery { mockUserPreferenceService.findByUserId(testUserId) } returns null

            val response = client.get("/preference/me") {
                setAuthenticatedUser(testUserId)
            }

            assertEquals(HttpStatusCode.NotFound, response.status)

            coVerify(exactly = 1) { mockUserPreferenceService.findByUserId(testUserId) }
        }

        @Test
        fun `GET me returns 401 when user is not authenticated`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val response = client.get("/preference/me")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
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
                    userPreferenceRoutes(mockUserPreferenceService)
                }
            }
        }
    }

    private fun HttpRequestBuilder.setAuthenticatedUser(userId: String) {
        header(HttpHeaders.Authorization, "Bearer user-$userId")
    }

    private fun createTestUserPreference(
        userId: String = "test-user-id",
        ageRangeMin: Int = 25,
        ageRangeMax: Int = 35
    ): UserPreference {
        return UserPreference(
            id = 1L,
            userId = userId,
            genderIdentities = listOf(Gender.FEMALE),
            ageRangeMin = ageRangeMin,
            ageRangeMax = ageRangeMax,
            heightRangeMin = 160,
            heightRangeMax = 180,
            ethnicity = listOf(Ethnicity.BLACK_AFRICAN_DESCENT),
            dateLanguages = listOf(Language.ENGLISH),
            dateActivities = listOf(Activity.TAKING_A_WALK),
            dateLimit = 5,
            dateCities = listOf(
                City(
                    cityId = 1L,
                    cityName = "London",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            ),
            reachLevel = ReachLevel.OPEN_MINDED,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }


    private fun createValidUserRequest(userId: String = "test-user-id"): CreateUserRequest {
        return CreateUserRequest(
            userId = userId,
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            heightCm = 180,
            dateOfBirth = LocalDate.of(1990, 1, 1),
            city = "London",
            educationLevel = EducationLevel.UNIVERSITY,
            gender = Gender.MALE,
            preferredLanguage = Language.ENGLISH,
            occupation = "Engineer",
            bio = "Test bio",
            interests = List(5) { "Interest$it" },
            traits = List(3) { Trait.entries[it] },
            spokenLanguages = DisplayableField(listOf(Language.ENGLISH), false),
            religion = DisplayableField(null, false),
            politicalView = DisplayableField(null, false),
            alcoholConsumption = DisplayableField(null, false),
            smokingStatus = DisplayableField(SmokingStatus.NEVER, true),
            diet = DisplayableField(null, false),
            dateIntentions = DisplayableField(DateIntentions.SERIOUS_DATING, false),
            relationshipType = DisplayableField(RelationshipType.MONOGAMOUS, false),
            kidsPreference = DisplayableField(KidsPreference.OPEN_TO_KIDS, false),
            sexualOrientation = DisplayableField(SexualOrientation.STRAIGHT, false),
            pronouns = DisplayableField(null, false),
            starSign = DisplayableField(null, false),
            ethnicity = DisplayableField(listOf(Ethnicity.BLACK_AFRICAN_DESCENT), false),
            brainAttributes = DisplayableField(null, false),
            brainDescription = DisplayableField(null, false),
            bodyAttributes = DisplayableField(null, false),
            bodyDescription = DisplayableField(null, false),
            coordinatesLongitude = 45.3246,
            coordinatesLatitude = -314.6,
        )
    }

    private fun createTestUser(
        userId: String = "test-user-id",
        firstName: String = "John"
    ): User {
        return User(
            userId = userId,
            firstName = firstName,
            lastName = "Doe",
            email = "john.doe@example.com",
            heightCm = 180,
            dateOfBirth = LocalDate.of(1990, 1, 1),
            city = "London",
            educationLevel = EducationLevel.UNIVERSITY,
            gender = Gender.MALE,
            occupation = "Engineer",
            bio = "Test bio",
            interests = List(5) { "Interest$it" },
            traits = List(3) { Trait.entries[it] },
            preferredLanguage = Language.ENGLISH,
            spokenLanguages = DisplayableField(listOf(Language.ENGLISH), false),
            religion = DisplayableField(null, false),
            politicalView = DisplayableField(null, false),
            alcoholConsumption = DisplayableField(null, false),
            smokingStatus = DisplayableField(SmokingStatus.NEVER, true),
            diet = DisplayableField(null, false),
            dateIntentions = DisplayableField(DateIntentions.SERIOUS_DATING, false),
            relationshipType = DisplayableField(RelationshipType.MONOGAMOUS, false),
            kidsPreference = DisplayableField(KidsPreference.OPEN_TO_KIDS, false),
            sexualOrientation = DisplayableField(SexualOrientation.STRAIGHT, false),
            pronouns = DisplayableField(null, false),
            starSign = DisplayableField(null, false),
            ethnicity = DisplayableField(listOf(Ethnicity.BLACK_AFRICAN_DESCENT), false),
            brainAttributes = DisplayableField(null, false),
            brainDescription = DisplayableField(null, false),
            bodyAttributes = DisplayableField(null, false),
            bodyDescription = DisplayableField(null, false),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            deletedAt = null,
            profileStatus = ProfileStatus.ACTIVE,
            eloScore = 1000,
            badges = setOf(),
            completeness = 75,
            coordinatesLongitude = 45.3246,
            coordinatesLatitude = -314.6,
            role = Role.USER,
            photoValidationStatus = ValidationStatus.VALIDATED
        )
    }

    private fun createCompleteTestUser(
        userId: String = "test-user-id",
        firstName: String = "John",
        profileStatus: ProfileStatus = ProfileStatus.ACTIVE
    ): User {
        return User(
            userId = userId,
            firstName = firstName,
            lastName = "Doe",
            email = "john.doe@example.com",
            heightCm = 180,
            dateOfBirth = LocalDate.of(1990, 1, 1),
            city = "London",
            educationLevel = EducationLevel.UNIVERSITY,
            gender = Gender.MALE,
            occupation = "Engineer",
            bio = "Test bio",
            interests = List(5) { "Interest$it" },
            traits = List(3) { Trait.entries[it] },
            preferredLanguage = Language.ENGLISH,
            spokenLanguages = DisplayableField(listOf(Language.ENGLISH), true),
            religion = DisplayableField(Religion.CHRISTIANITY, true),
            politicalView = DisplayableField(PoliticalView.MODERATE, true),
            alcoholConsumption = DisplayableField(AlcoholConsumption.SOMETIMES, true),
            smokingStatus = DisplayableField(SmokingStatus.NEVER, true),
            diet = DisplayableField(Diet.HALAL, true),
            dateIntentions = DisplayableField(DateIntentions.SERIOUS_DATING, true),
            relationshipType = DisplayableField(RelationshipType.MONOGAMOUS, true),
            kidsPreference = DisplayableField(KidsPreference.OPEN_TO_KIDS, true),
            sexualOrientation = DisplayableField(SexualOrientation.STRAIGHT, true),
            pronouns = DisplayableField(Pronouns.HE_HIM, true),
            starSign = DisplayableField(StarSign.GEMINI, true),
            ethnicity = DisplayableField(listOf(Ethnicity.BLACK_AFRICAN_DESCENT), true),
            brainAttributes = DisplayableField(
                listOf(
                    BrainAttribute.LEARNING_DISABILITY,
                    BrainAttribute.NEURODIVERGENT
                ), true
            ),
            brainDescription = DisplayableField("Maybe this is string?", true),
            bodyAttributes = DisplayableField(listOf(BodyAttribute.WHEELCHAIR), true),
            bodyDescription = DisplayableField("Is this a string?", true),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            deletedAt = null,
            profileStatus = profileStatus,
            eloScore = 1000,
            badges = setOf(Badge.VERIFIED, Badge.TRUSTED, Badge.GOOD_XP),
            completeness = 75,
            coordinatesLongitude = 45.3246,
            coordinatesLatitude = -314.6,
            role = Role.USER,
            photoValidationStatus = ValidationStatus.VALIDATED
        )
    }

}