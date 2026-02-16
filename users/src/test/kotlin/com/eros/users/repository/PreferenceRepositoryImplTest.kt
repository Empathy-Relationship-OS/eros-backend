package com.eros.users.repository


import com.eros.users.models.Activity
import com.eros.users.models.City
import com.eros.users.models.CreateCityRequest
import com.eros.users.models.CreatePreferenceRequest
import com.eros.users.models.CreateUserRequest
import com.eros.users.models.EducationLevel
import com.eros.users.models.Ethnicity
import com.eros.users.models.Gender
import com.eros.users.models.Language
import com.eros.users.models.Trait
import com.eros.users.models.User
import com.eros.users.table.Cities
import com.eros.users.table.UserCitiesPreference
import com.eros.users.table.UserPreferences
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PreferenceRepositoryImplTest {

    companion object {
        @Container
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("test_db")
            withUsername("test_user")
            withPassword("test_password")
        }
    }

    private lateinit var preferenceRepository: PreferenceRepositoryImpl
    private lateinit var userCitiesRepository: UserCitiesRepositoryImpl
    private lateinit var cityRepository: CityRepositoryImpl
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

        // Use regular transaction for schema creation (doesn't conflict)
        transaction {
            SchemaUtils.create(Cities, UserCitiesPreference,UserPreferences)
        }
    }

    @BeforeEach
    fun setupEach() {
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
        userCitiesRepository = UserCitiesRepositoryImpl(clock)
        preferenceRepository = PreferenceRepositoryImpl(clock, userCitiesRepository)
        cityRepository = CityRepositoryImpl(clock)

        // Clear the tables before each test.
        transaction {
            Cities.deleteAll()
            UserCitiesPreference.deleteAll()
            UserPreferences.deleteAll()
        }
    }

    @AfterAll
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Cities, UserCitiesPreference,UserPreferences)
        }
    }

    fun createCity(cityName : String) : City{
        val city : City
        runBlocking {
            city = cityRepository.createCity(CreateCityRequest(cityName))
        }
        return city
    }

    fun createUser() : User{
        val user : User
        runBlocking {
            // Create user123
            user = UserRepositoryImpl().createUser(
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
        }
        return user
    }


    @Test
    fun createUserPreferences(){

        // Create test user
        val user = createUser()

        // Create Test City
        val city = createCity( "TestCity")

        val request = CreatePreferenceRequest(
            "user123",
            listOf(Gender.FEMALE, Gender.NON_BINARY),
            25,
            35,
            160,
            180,
            listOf(Ethnicity.MIDDLE_EASTERN, Ethnicity.PACIFIC_ISLANDER),
            listOf(Language.ENGLISH, Language.SPANISH),
            listOf(Activity.ESCAPE_ROOMS, Activity.BEACH),
            5,
            listOf(1L),
        )

        runBlocking {
            preferenceRepository.createPreferences(request)
        }
    }

}