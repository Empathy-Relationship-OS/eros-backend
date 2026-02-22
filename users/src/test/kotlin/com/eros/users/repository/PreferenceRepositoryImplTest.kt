package com.eros.users.repository


import com.eros.users.models.Activity
import com.eros.users.models.City
import com.eros.users.models.DateIntentions
import com.eros.users.models.DisplayableField
import com.eros.users.models.EducationLevel
import com.eros.users.models.Ethnicity
import com.eros.users.models.Gender
import com.eros.users.models.KidsPreference
import com.eros.users.models.Language
import com.eros.users.models.ProfileStatus
import com.eros.users.models.ReachLevel
import com.eros.users.models.RelationshipType
import com.eros.users.models.Role
import com.eros.users.models.SexualOrientation
import com.eros.users.models.Trait
import com.eros.users.models.User
import com.eros.users.models.UserPreference
import com.eros.users.models.ValidationStatus
import com.eros.users.service.PreferenceService
import com.eros.users.table.Cities
import com.eros.users.table.UserCitiesPreference
import com.eros.users.table.UserPreferences
import com.eros.users.table.Users
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
import org.junit.jupiter.api.assertNotNull
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            SchemaUtils.create(Users, Cities, UserCitiesPreference, UserPreferences)
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
            UserPreferences.deleteAll()
            UserCitiesPreference.deleteAll()
            Cities.deleteAll()
            Users.deleteAll()
        }
    }

    @AfterAll
    fun tearDown() {
        transaction {
            SchemaUtils.drop(UserPreferences, UserCitiesPreference, Cities, Users)
        }
    }

    fun createCity(cityName: String): City {
        val city: City
        runBlocking {
            city = cityRepository.create(City(0L, cityName, fixedInstant, fixedInstant))
        }
        return city
    }

    fun createUser(): User = runBlocking {
        UserRepositoryImpl(clock).create(
            User(
                userId = "user123",
                firstName = "John",
                lastName = "Doe",
                email = "john@example.com",
                heightCm = 175,
                dateOfBirth = LocalDate.of(1990, 1, 1),
                city = "New York",
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
                ethnicity = DisplayableField(listOf(Ethnicity.MIDDLE_EASTERN), false),
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
        )
        UserRepositoryImpl(clock).create(
            User(
                userId = "user456",
                firstName = "Pete",
                lastName = "Georgian",
                email = "p.g@example.com",
                heightCm = 192,
                dateOfBirth = LocalDate.of(1991, 1, 1),
                city = "Liverpool",
                educationLevel = EducationLevel.UNIVERSITY,
                gender = Gender.FEMALE,
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
                ethnicity = DisplayableField(listOf(Ethnicity.MIDDLE_EASTERN), false),
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
        )
    }


    @Test
    fun createUserPreferences() {

        // Create test user
        createUser()

        // Create Test City
        val city = createCity("TestCity")

        val preference = UserPreference(
            id = 0L,
            userId = "user123",
            genderIdentities = listOf(Gender.FEMALE, Gender.NON_BINARY),
            ageRangeMin = 25,
            ageRangeMax = 55,
            heightRangeMin = 160,
            heightRangeMax = 200,
            ethnicity = listOf(Ethnicity.MIDDLE_EASTERN, Ethnicity.PACIFIC_ISLANDER),
            dateLanguages = listOf(Language.ENGLISH, Language.SPANISH),
            dateActivities = listOf(Activity.ESCAPE_ROOMS, Activity.BEACH),
            dateLimit = 5,
            dateCities = listOf(city),
            reachLevel = ReachLevel.OPEN_MINDED,
            createdAt = fixedInstant,
            updatedAt = fixedInstant
        )

        val preference2 = UserPreference(
            id = 0L,
            userId = "user456",
            genderIdentities = listOf(Gender.MALE, Gender.NON_BINARY),
            ageRangeMin = 18,
            ageRangeMax = 80,
            heightRangeMin = 160,
            heightRangeMax = 200,
            ethnicity = listOf(Ethnicity.MIDDLE_EASTERN, Ethnicity.PACIFIC_ISLANDER),
            dateLanguages = listOf(Language.ENGLISH, Language.SPANISH),
            dateActivities = listOf(Activity.ESCAPE_ROOMS, Activity.BEACH),
            dateLimit = 5,
            dateCities = listOf(city),
            reachLevel = ReachLevel.OPEN_MINDED,
            createdAt = fixedInstant,
            updatedAt = fixedInstant
        )

        val createdUser = runBlocking {
            preferenceRepository.create(preference)
        }
        val createdUser2 = runBlocking {preferenceRepository.create(preference2)}

        assertNotNull(createdUser)
        assertEquals(preference.userId, createdUser.userId)
        assertEquals(preference2.userId, createdUser2.userId)
    }

}
