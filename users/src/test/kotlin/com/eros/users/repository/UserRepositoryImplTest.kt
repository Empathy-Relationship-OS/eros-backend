package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.users.models.AlcoholConsumption
import com.eros.users.models.Badge
import com.eros.users.models.BodyAttribute
import com.eros.users.models.BrainAttribute
import com.eros.users.models.CreateUserRequest
import com.eros.users.models.DateIntentions
import com.eros.users.models.Diet
import com.eros.users.models.DisplayableField
import com.eros.users.models.EducationLevel
import com.eros.users.models.Ethnicity
import com.eros.users.models.Gender
import com.eros.users.models.KidsPreference
import com.eros.users.models.Language
import com.eros.users.models.PoliticalView
import com.eros.users.models.ProfileStatus
import com.eros.users.models.Pronouns
import com.eros.users.models.RelationshipType
import com.eros.users.models.Religion
import com.eros.users.models.Role
import com.eros.users.models.SexualOrientation
import com.eros.users.models.SmokingStatus
import com.eros.users.models.StarSign
import com.eros.users.models.Trait
import com.eros.users.models.UpdateUserRequest
import com.eros.users.models.User
import com.eros.users.models.ValidationStatus
import com.eros.users.table.Users
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryImplTest {

    @Nested
    inner class `Update Visibility`{

        @Test
        fun `successfully update visibility to sleep`() = runBlocking {

            val user = createTestUser()
            val request = createTestUser(visibility = ProfileStatus.SLEEP_MODE)

            val updatedUser = dbQuery{
                repository.create(user)
                repository.update(user.userId, request)
            }
            assertEquals(ProfileStatus.SLEEP_MODE, updatedUser?.profileStatus)
        }


    }

    // =============================//
    // ========== Setup ============//
    // =============================//

    companion object {
        @Container
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("test_db")
            withUsername("test_user")
            withPassword("test_password")
        }
    }

    private lateinit var repository: UserRepositoryImpl
    private lateinit var clock: Clock
    private val fixedInstant = Instant.parse("2024-01-15T10:00:00Z")

    @BeforeAll
    fun setup() {
        Database.connect(
            url = postgresContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgresContainer.username,
            password = postgresContainer.password
        )

        transaction {
            SchemaUtils.create(Users)
        }
    }

    @BeforeEach
    fun setupEach() {
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
        repository = UserRepositoryImpl(clock)

        transaction {
            Users.deleteAll()
        }
    }

    @AfterAll
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Users)
        }
    }

    // ===============================//
    // =========== Helpers ===========//
    // ===============================//

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
            coordinatesLatitude = -90.0,
        )
    }

    private fun createTestUser(
        userId: String = "test-user-id",
        firstName: String = "John",
        visibility: ProfileStatus = ProfileStatus.ACTIVE
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
            profileStatus = visibility,
            eloScore = 1000,
            badges = setOf(),
            profileCompleteness = 75,
            coordinatesLongitude = 45.3246,
            coordinatesLatitude = -90.0,
            role = Role.USER,
            photoValidationStatus = ValidationStatus.VALIDATED
        )
    }

    private fun createCompleteTestUser(
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
            spokenLanguages = DisplayableField(listOf(Language.ENGLISH), true),
            religion = DisplayableField(Religion.CHRISTIANITY,true),
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
            brainAttributes = DisplayableField(listOf(BrainAttribute.LEARNING_DISABILITY, BrainAttribute.NEURODIVERGENT), true),
            brainDescription = DisplayableField("Maybe this is string?", true),
            bodyAttributes = DisplayableField(listOf(BodyAttribute.WHEELCHAIR), true),
            bodyDescription = DisplayableField("Is this a string?", true),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            deletedAt = null,
            profileStatus = ProfileStatus.ACTIVE,
            eloScore = 1000,
            badges = setOf(Badge.VERIFIED, Badge.TRUSTED, Badge.GOOD_XP),
            profileCompleteness = 75,
            coordinatesLongitude = 45.3246,
            coordinatesLatitude = -90.0,
            role = Role.USER,
            photoValidationStatus = ValidationStatus.VALIDATED
        )
    }

}