
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
            SchemaUtils.create(Cities, UserCitiesPreference)
        }
    }

    @BeforeEach
    fun setupEach() {
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
        repository = UserCitiesRepositoryImpl(clock)

        // Create UserRepository to insert test users
        val userRepository = UserRepositoryImpl(clock)

        transaction {
            UserCitiesPreference.deleteAll()
            Cities.deleteAll()
            Users.deleteAll()

            // Insert test cities
            Cities.insert {
                it[id] = 1L
                it[cityName] = "New York"
                it[createdAt] = fixedInstant
                it[updatedAt] = fixedInstant
            }
            Cities.insert {
                it[id] = 2L
                it[cityName] = "Los Angeles"
                it[createdAt] = fixedInstant
                it[updatedAt] = fixedInstant
            }
            Cities.insert {
                it[id] = 3L
                it[cityName] = "Chicago"
                it[createdAt] = fixedInstant
                it[updatedAt] = fixedInstant
            }
        }

        // Create test users
        runBlocking {
            // Create user123
            userRepository.createUser(
                CreateUserRequest(
                    userId = "user123",
                    firstName = "John",
                    lastName = "Doe",
                    email = "john@example.com",
                    heightCm = 175,
                    dateOfBirth = LocalDate.of(1990, 1, 1),
                    city = "New York",
                    educationLevel = EducationLevel.UNIVERSITY,
                    gender = Gender.MALE,
                    preferredLanguage = Language.ENGLISH,
                    interests = listOf("Reading", "Hiking", "Movies", "Music", "Travel"),
                    traits = listOf(Trait.ADVENTUROUS, Trait.HONEST, Trait.KIND),
                    ethnicity = listOf(Ethnicity.MIDDLE_EASTERN)
                )
            )

            // Create user456
            userRepository.createUser(
                CreateUserRequest(
                    userId = "user456",
                    firstName = "Jane",
                    lastName = "Smith",
                    email = "jane@example.com",
                    heightCm = 165,
                    dateOfBirth = LocalDate.of(1992, 5, 15),
                    city = "Los Angeles",
                    educationLevel = EducationLevel.UNIVERSITY,
                    gender = Gender.FEMALE,
                    preferredLanguage = Language.ENGLISH,
                    interests = listOf("Yoga", "Cooking", "Art", "Photography", "Fashion"),
                    traits = listOf(Trait.CREATIVE, Trait.EMPATHETIC, Trait.OUTGOING),
                    ethnicity = listOf(Ethnicity.PACIFIC_ISLANDER)
                )
            )

            // Create user789
            userRepository.createUser(
                CreateUserRequest(
                    userId = "user789",
                    firstName = "Alex",
                    lastName = "Johnson",
                    email = "alex@example.com",
                    heightCm = 180,
                    dateOfBirth = LocalDate.of(1988, 10, 20),
                    city = "Chicago",
                    educationLevel = EducationLevel.APPRENTICESHIP,
                    gender = Gender.NON_BINARY,
                    preferredLanguage = Language.ENGLISH,
                    interests = listOf("Gaming", "Tech", "Running", "Coffee", "Coding"),
                    traits = listOf(Trait.CREATIVE, Trait.AMBITIOUS, Trait.OUTGOING),
                    ethnicity = listOf(Ethnicity.HISPANIC_LATINO, Ethnicity.SOUTHEAST_ASIAN)
                )
            )
        }
    }

    @AfterAll
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Cities, UserCitiesPreference)
        }
    }

    @Test
    fun `should add single city preference successfully`() = runBlocking {
        // Given
        val request = CreateUserCityPreferenceRequest(
            userId = "user123",
            cityId = 1L
        )

        // When
        val result = repository.addUserCityPreference(request)

        // Then
        assertNotEquals(result,null)
        assertEquals("user123", result.userId)
        assertEquals(1L, result.cityId)
        assertEquals(fixedInstant, result.createdAt)
    }

    @Test
    fun `should create preference with correct timestamp`() = runBlocking {
        // Given
        val request = CreateUserCityPreferenceRequest(
            userId = "user456",
            cityId = 2L
        )

        // When
        val result = repository.addUserCityPreference(request)

        // Then
        assertEquals(fixedInstant, result.createdAt)
    }
}