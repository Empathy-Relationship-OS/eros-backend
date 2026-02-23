package com.eros.users.repository

import com.eros.users.models.City
import com.eros.users.table.Cities
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.*

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CityRepositoryImplTest {

    companion object {
        @Container
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("test_db")
            withUsername("test_user")
            withPassword("test_password")
        }
    }

    private lateinit var repository: CityRepositoryImpl
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
            SchemaUtils.create(Cities)
        }
    }

    @BeforeEach
    fun setupEach() {
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
        repository = CityRepositoryImpl(clock)

        transaction {
            Cities.deleteAll()
        }
    }

    @AfterAll
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Cities)
        }
    }

    @Nested
    inner class `create function` {

        @Test
        fun `should create city with correct fields`() {
            val cityName = "London"

            val city = runBlocking {
                repository.create(City(0L, cityName, fixedInstant, fixedInstant))
            }

            assertNotNull(city.cityId)
            assertTrue(city.cityId > 0)
            assertEquals(cityName, city.cityName)
            assertEquals(fixedInstant, city.createdAt)
            assertEquals(fixedInstant, city.updatedAt)
        }

        @Test
        fun `should auto-increment city IDs`() {
            val city1 = runBlocking {
                repository.create(City(0L, "Paris", fixedInstant, fixedInstant))
            }
            val city2 = runBlocking {
                repository.create(City(0L, "Berlin", fixedInstant, fixedInstant))
            }

            assertTrue(city2.cityId > city1.cityId)
            assertEquals(city1.cityId + 1, city2.cityId)
        }

        @Test
        fun `should use clock for timestamps`() {
            val customInstant = Instant.parse("2025-06-01T12:30:00Z")
            val customClock = Clock.fixed(customInstant, ZoneId.of("UTC"))
            val customRepository = CityRepositoryImpl(customClock)

            val city = runBlocking {
                customRepository.create(City(0L, "Tokyo", customInstant, customInstant))
            }

            assertEquals(customInstant, city.createdAt)
            assertEquals(customInstant, city.updatedAt)
        }

        @Test
        fun `should handle cities with special characters`() {
            val cityName = "São Paulo"

            val city = runBlocking {
                repository.create(City(0L, cityName, fixedInstant, fixedInstant))
            }

            assertEquals(cityName, city.cityName)
        }

        @Test
        fun `should enforce unique city names`() {
            runBlocking {
                repository.create(City(0L, "London", fixedInstant, fixedInstant))
            }

            assertFails {
                runBlocking {
                    repository.create(City(0L, "London", fixedInstant, fixedInstant))
                }
            }
        }
    }

    @Nested
    inner class `update function` {

        @Test
        fun `should update city name successfully`() {
            val originalName = "TestValue"
            val city = runBlocking {
                repository.create(City(0L, originalName, fixedInstant, fixedInstant))
            }

            val newName = "AlteredTestValue"
            val updatedCity = runBlocking {
                repository.update(city.cityId, city.copy(cityName = newName))
            }

            assertNotNull(updatedCity)
            assertEquals(newName, updatedCity.cityName)
            assertEquals(city.cityId, updatedCity.cityId)
            assertNotEquals(originalName, updatedCity.cityName)
        }

        @Test
        fun `should update updatedAt timestamp on update`() {
            val city = runBlocking {
                repository.create(City(0L, "Paris", fixedInstant, fixedInstant))
            }

            val laterInstant = fixedInstant.plusSeconds(3600)
            val laterClock = Clock.fixed(laterInstant, ZoneId.of("UTC"))
            val laterRepository = CityRepositoryImpl(laterClock)

            val updatedCity = runBlocking {
                laterRepository.update(city.cityId, city.copy(cityName = "New Paris"))
            }

            assertNotNull(updatedCity)
            assertEquals(fixedInstant, updatedCity.createdAt) // createdAt unchanged
            assertEquals(laterInstant, updatedCity.updatedAt) // updatedAt changed
        }

        @Test
        fun `should return null when updating non-existent city`() {
            val nonExistentId = 999L

            val result = runBlocking {
                repository.update(nonExistentId, City(nonExistentId, "Ghost City", fixedInstant, fixedInstant))
            }

            assertNull(result)
        }

        @Test
        fun `should not allow updating to duplicate city name`() {
            runBlocking {
                repository.create(City(0L, "London", fixedInstant, fixedInstant))
                val paris = repository.create(City(0L, "Paris", fixedInstant, fixedInstant))

                assertFails {
                    repository.update(paris.cityId, paris.copy(cityName = "London"))
                }
            }
        }
    }

    @Nested
    inner class `doesExist by name function` {

        @Test
        fun `should return true when city name exists`() {
            runBlocking {
                repository.create(City(0L, "London", fixedInstant, fixedInstant))
            }

            val exists = transaction {
                repository.doesExist("London")
            }

            assertTrue(exists)
        }

        @Test
        fun `should return false when city name does not exist`() {
            val exists = transaction { repository.doesExist("NonExistentCity") }

            assertFalse(exists)
        }

        @Test
        fun `should be case sensitive`() {
            runBlocking {
                repository.create(City(0L, "London", fixedInstant, fixedInstant))
            }
            val existsLowercase = transaction {
                repository.doesExist("london")
            }


            assertFalse(existsLowercase)
        }

        @Test
        fun `should not match partial names`() {
            runBlocking {
                repository.create(City(0L, "New York", fixedInstant, fixedInstant))
            }

            val existsPartial = transaction { repository.doesExist("New") }

            assertFalse(existsPartial)
        }

        @Test
        fun `should handle special characters in city names`() {
            runBlocking {
                repository.create(City(0L, "São Paulo", fixedInstant, fixedInstant))
            }

            val exists = transaction { repository.doesExist("São Paulo") }

            assertTrue(exists)
        }
    }

    @Nested
    inner class `doesExist by id function` {

        @Test
        fun `should return true when city id exists`() {
            val city = runBlocking {
                repository.create(City(0L, "London", fixedInstant, fixedInstant))
            }

            val exists = runBlocking {
                repository.doesExist(city.cityId)
            }

            assertTrue(exists)
        }

        @Test
        fun `should return false when city id does not exist`() {
            val exists = runBlocking {
                repository.doesExist(999L)
            }

            assertFalse(exists)
        }

        @Test
        fun `should return false for negative ids`() {
            val exists = runBlocking {
                repository.doesExist(-1L)
            }

            assertFalse(exists)
        }

        @Test
        fun `should return false for zero id`() {
            val exists = runBlocking {
                repository.doesExist(0L)
            }

            assertFalse(exists)
        }
    }

    @Nested
    inner class `findById function` {

        @Test
        fun `should return city when found`() {
            val createdCity = runBlocking {
                repository.create(City(0L, "London", fixedInstant, fixedInstant))
            }

            val foundCity = runBlocking {
                repository.findById(createdCity.cityId)
            }

            assertNotNull(foundCity)
            assertEquals(createdCity.cityId, foundCity.cityId)
            assertEquals(createdCity.cityName, foundCity.cityName)
            assertEquals(createdCity.createdAt, foundCity.createdAt)
            assertEquals(createdCity.updatedAt, foundCity.updatedAt)
        }

        @Test
        fun `should return null when city not found`() {
            val result = runBlocking {
                repository.findById(999L)
            }

            assertNull(result)
        }

        @Test
        fun `should return correct city among multiple cities`() {
            val paris = runBlocking {
                repository.create(City(0L, "Paris", fixedInstant, fixedInstant))
            }
            val london = runBlocking {
                repository.create(City(0L, "London", fixedInstant, fixedInstant))
            }
            val berlin = runBlocking {
                repository.create(City(0L, "Berlin", fixedInstant, fixedInstant))
            }

            val foundCity = runBlocking {
                repository.findById(london.cityId)
            }

            assertNotNull(foundCity)
            assertEquals("London", foundCity.cityName)
            assertEquals(london.cityId, foundCity.cityId)
        }

        @Test
        fun `should handle negative ids gracefully`() {
            val result = runBlocking {
                repository.findById(-1L)
            }

            assertNull(result)
        }
    }

    @Nested
    inner class `delete function` {

        @Test
        fun `should delete city and return 1 when city exists`() {
            val city = runBlocking {
                repository.create(City(0L, "London", fixedInstant, fixedInstant))
            }

            val deleted = runBlocking {
                repository.delete(city.cityId)
            }

            assertEquals(1, deleted)

            // Verify city is actually deleted
            val found = runBlocking {
                repository.findById(city.cityId)
            }
            assertNull(found)
        }

        @Test
        fun `should return 0 when city does not exist`() {
            val deleted = runBlocking {
                repository.delete(999L)
            }

            assertEquals(0, deleted)
        }

        @Test
        fun `should not allow deleting same city twice`() {
            val city = runBlocking {
                repository.create(City(0L, "Paris", fixedInstant, fixedInstant))
            }

            val firstDelete = runBlocking {
                repository.delete(city.cityId)
            }
            val secondDelete = runBlocking {
                repository.delete(city.cityId)
            }

            assertEquals(1, firstDelete)
            assertEquals(0, secondDelete)
        }

        @Test
        fun `should only delete specified city`() {
            val london = runBlocking {
                repository.create(City(0L, "London", fixedInstant, fixedInstant))
            }
            val paris = runBlocking {
                repository.create(City(0L, "Paris", fixedInstant, fixedInstant))
            }

            runBlocking {
                repository.delete(london.cityId)
            }

            val londonFound = runBlocking { repository.findById(london.cityId) }
            val parisFound = runBlocking { repository.findById(paris.cityId) }

            assertNull(londonFound)
            assertNotNull(parisFound)
        }

        @Test
        fun `should handle deleting with negative id`() {
            val deleted = runBlocking {
                repository.delete(-1L)
            }

            assertEquals(0, deleted)
        }
    }

    @Nested
    inner class `getAll function` {

        @Test
        fun `should return empty list when no cities exist`() {
            val cities = runBlocking {
                repository.findAll()
            }

            assertTrue(cities.isEmpty())
        }

        @Test
        fun `should return all cities`() {
            runBlocking {
                repository.create(City(0L, "London", fixedInstant, fixedInstant))
                repository.create(City(0L, "Paris", fixedInstant, fixedInstant))
                repository.create(City(0L, "Berlin", fixedInstant, fixedInstant))
            }

            val cities = runBlocking {
                repository.findAll()
            }

            assertEquals(3, cities.size)
            assertTrue(cities.any { it.cityName == "London" })
            assertTrue(cities.any { it.cityName == "Paris" })
            assertTrue(cities.any { it.cityName == "Berlin" })
        }

        @Test
        fun `should return cities with all fields populated`() {
            val createdCity = runBlocking {
                repository.create(City(0L, "Tokyo", fixedInstant, fixedInstant))
            }

            val cities = runBlocking {
                repository.findAll()
            }

            assertEquals(1, cities.size)
            val city = cities.first()
            assertNotNull(city.cityId)
            assertEquals("Tokyo", city.cityName)
            assertNotNull(city.createdAt)
            assertNotNull(city.updatedAt)
        }
    }

    @Nested
    inner class `integration tests` {

        @Test
        fun `should handle full CRUD lifecycle`() {
            // Create
            val city = runBlocking {
                repository.create(City(0L, "London", fixedInstant, fixedInstant))
            }
            assertNotNull(city.cityId)

            // Read
            val foundCity = runBlocking {
                repository.findById(city.cityId)
            }
            assertNotNull(foundCity)
            assertEquals("London", foundCity.cityName)

            // Update
            val updatedCity = runBlocking {
                repository.update(city.cityId, city.copy(cityName = "Greater London"))
            }
            assertNotNull(updatedCity)
            assertEquals("Greater London", updatedCity.cityName)

            // Delete
            val deleted = runBlocking {
                repository.delete(city.cityId)
            }
            assertEquals(1, deleted)

            // Verify deletion
            val afterDelete = runBlocking {
                repository.findById(city.cityId)
            }
            assertNull(afterDelete)
        }

        @Test
        fun `should handle concurrent city creation`() {
            val cityNames = listOf("London", "Paris", "Berlin", "Madrid", "Rome")

            val cities = runBlocking {
                cityNames.map { name ->
                    repository.create(City(0L, name, fixedInstant, fixedInstant))
                }
            }

            assertEquals(5, cities.size)
            assertEquals(cityNames.toSet(), cities.map { it.cityName }.toSet())

            // All IDs should be unique
            val ids = cities.map { it.cityId }
            assertEquals(ids.size, ids.toSet().size)
        }

        @Test
        fun `should maintain data integrity after multiple operations`() {
            // Create multiple cities
            val london = runBlocking {
                repository.create(City(0L, "London", fixedInstant, fixedInstant))
            }
            val paris = runBlocking {
                repository.create(City(0L, "Paris", fixedInstant, fixedInstant))
            }

            // Update one
            runBlocking {
                repository.update(london.cityId, london.copy(cityName = "Greater London"))
            }

            // Delete another
            runBlocking {
                repository.delete(paris.cityId)
            }

            // Verify final state
            val allCities = runBlocking {
                repository.findAll()
            }

            assertEquals(1, allCities.size)
            assertEquals("Greater London", allCities.first().cityName)
        }
    }
}