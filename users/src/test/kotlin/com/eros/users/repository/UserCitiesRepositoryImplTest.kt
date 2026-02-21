
package com.eros.users.repository

import com.eros.users.models.*
import com.eros.users.table.Cities
import com.eros.users.table.UserCitiesPreference
import com.eros.users.table.Users
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserCitiesRepositoryImplTest {

    companion object {
        @Container
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("test_db")
            withUsername("test_user")
            withPassword("test_password")
        }
    }

    private lateinit var repository: UserCitiesRepositoryImpl
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

        // Use regular transaction for schema creation
        transaction {
            SchemaUtils.create(Users, Cities, UserCitiesPreference)
        }
    }

    @BeforeEach
    fun setupEach() {
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
        repository = UserCitiesRepositoryImpl(clock)

        val userRepository = UserRepositoryImpl(clock)

        transaction {
            UserCitiesPreference.deleteAll()
            Cities.deleteAll()
            Users.deleteAll()

            // Insert test cities
            Cities.insert {
                it[cityName] = "New York"
                it[createdAt] = fixedInstant
                it[updatedAt] = fixedInstant
            }
            Cities.insert {
                it[cityName] = "Los Angeles"
                it[createdAt] = fixedInstant
                it[updatedAt] = fixedInstant
            }
            Cities.insert {
                it[cityName] = "Chicago"
                it[createdAt] = fixedInstant
                it[updatedAt] = fixedInstant
            }
        }

        // Create test users via repository (service layer handles DTO→domain mapping)
        runBlocking {
            userRepository.create(createTestUser("user123", "John", "Doe", "john@example.com", "New York"))
            userRepository.create(createTestUser("user456", "Jane", "Smith", "jane@example.com", "Los Angeles"))
            userRepository.create(createTestUser("user789", "Alex", "Johnson", "alex@example.com", "Chicago"))
        }
    }

    @AfterAll
    fun tearDown() {
        transaction {
            SchemaUtils.drop(UserCitiesPreference, Cities, Users)
        }
    }

    @Test
    fun `should add single city preference successfully`() = runBlocking {

        val city = CityRepositoryImpl(clock).create(City(0L, "Blah", fixedInstant, fixedInstant))

        val preference = UserCityPreference(
            userId = "user123",
            cityId = city.cityId,
            createdAt = fixedInstant
        )

        val result = repository.addUserCityPreference(preference)

        assertNotEquals(result, null)
        assertEquals("user123", result.userId)
        assertEquals(city.cityId, result.cityId)
        assertEquals(fixedInstant, result.createdAt)
    }

    @Test
    fun `should create preference with correct timestamp`() = runBlocking {
        // Given
        val preference = UserCityPreference(
            userId = "user456",
            cityId = 2L,
            createdAt = fixedInstant
        )

        // When
        val result = repository.addUserCityPreference(preference)

        // Then
        assertEquals(fixedInstant, result.createdAt)
    }

    // Helper to build a minimal User domain object for seeding tests
    private fun createTestUser(
        userId: String,
        firstName: String,
        lastName: String,
        email: String,
        city: String
    ): User = User(
        userId = userId,
        firstName = firstName,
        lastName = lastName,
        email = email,
        heightCm = 175,
        dateOfBirth = LocalDate.of(1990, 1, 1),
        city = city,
        educationLevel = EducationLevel.UNIVERSITY,
        gender = Gender.MALE,
        occupation = "",
        bio = "",
        interests = listOf("Reading", "Hiking", "Movies", "Music", "Travel"),
        traits = listOf(Trait.ADVENTUROUS, Trait.HONEST, Trait.KIND),
        preferredLanguage = Language.ENGLISH,
        spokenLanguages = DisplayableField(listOf(Language.ENGLISH), false),
        religion = DisplayableField(null, false),
        politicalView = DisplayableField(null, false),
        alcoholConsumption = DisplayableField(null, false),
        smokingStatus = DisplayableField(null, false),
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
        createdAt = fixedInstant,
        updatedAt = fixedInstant,
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
