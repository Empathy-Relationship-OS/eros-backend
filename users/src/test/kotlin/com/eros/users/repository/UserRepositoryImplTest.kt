package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.users.models.Activity
import com.eros.users.models.Creative
import com.eros.users.models.DateIntentions
import com.eros.users.models.DisplayableField
import com.eros.users.models.EducationLevel
import com.eros.users.models.Entertainment
import com.eros.users.models.Ethnicity
import com.eros.users.models.Gender
import com.eros.users.models.Interest
import com.eros.users.models.KidsPreference
import com.eros.users.models.Language
import com.eros.users.models.ProfileStatus
import com.eros.users.models.RelationshipType
import com.eros.users.models.Role
import com.eros.users.models.SexualOrientation
import com.eros.users.models.SmokingStatus
import com.eros.users.models.Sport
import com.eros.users.models.Trait
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
            interests = listOf(Activity.HIKING, Interest.NATURE, Entertainment.MOVIES, Creative.PHOTOGRAPHY, Sport.YOGA),
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

}