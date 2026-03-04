package com.eros.users.repository

import com.eros.database.dbQuery
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
        fun `should create city with correct fields`() = runBlocking {
            val cityName = "London"

            val city = dbQuery {
                repository.create(City(0L, cityName, 5.0 ,5.0,fixedInstant, fixedInstant))
            }

            assertNotNull(city.cityId)
            assertTrue(city.cityId > 0)
            assertEquals(cityName, city.cityName)
            assertEquals(fixedInstant, city.createdAt)
            assertEquals(fixedInstant, city.updatedAt)
        }

        @Test
        fun `should auto-increment city IDs`() = runBlocking {
            val city1 = dbQuery {
                repository.create(City(0L, "Paris",5.0 ,5.0, fixedInstant, fixedInstant))
            }
            val city2 = dbQuery {
                repository.create(City(0L, "Berlin", 5.0 ,5.0,fixedInstant, fixedInstant))
            }

            assertTrue(city2.cityId > city1.cityId)
            assertEquals(city1.cityId + 1, city2.cityId)
        }

        @Test
        fun `should use clock for timestamps`() = runBlocking {
            val customInstant = Instant.parse("2025-06-01T12:30:00Z")
            val customClock = Clock.fixed(customInstant, ZoneId.of("UTC"))
            val customRepository = CityRepositoryImpl(customClock)
            val inputCreatedAt = Instant.parse("2000-01-01T00:00:00Z")
            val inputUpdatedAt = Instant.parse("2000-01-02T00:00:00Z")


            val city = dbQuery {
                customRepository.create(City(0L, "Tokyo", 5.0, 5.0, inputCreatedAt, inputUpdatedAt))
            }

            assertEquals(customInstant, city.createdAt)
            assertEquals(customInstant, city.updatedAt)
            assertNotEquals(inputCreatedAt, city.createdAt)
            assertNotEquals(inputUpdatedAt, city.updatedAt)
        }

        @Test
        fun `should handle cities with special characters`() = runBlocking {
            val cityName = "São Paulo"

            val city = dbQuery {
                repository.create(City(0L, cityName, 5.0 ,5.0,fixedInstant, fixedInstant))
            }

            assertEquals(cityName, city.cityName)
        }

        @Test
        fun `should enforce unique city names`() {
            runBlocking {
                dbQuery {
                    repository.create(City(0L, "London", 5.0 ,5.0,fixedInstant, fixedInstant))
                    assertFails {
                        repository.create(City(0L, "London", 5.0 ,5.0,fixedInstant, fixedInstant))
                    }
                }
            }
        }
    }

    @Nested
    inner class `update function` {

        @Test
        fun `should update city name successfully`() = runBlocking {
            val originalName = "TestValue"
            val city = dbQuery {
                repository.create(City(0L, originalName, 5.0 ,5.0,fixedInstant, fixedInstant))
            }

            val newName = "AlteredTestValue"
            val updatedCity = dbQuery {
                repository.update(city.cityId, city.copy(cityName = newName))
            }
            assertNotNull(updatedCity)
            assertEquals(newName, updatedCity.cityName)
            assertEquals(city.cityId, updatedCity.cityId)
            assertNotEquals(originalName, updatedCity.cityName)
        }

        @Test
        fun `should update updatedAt timestamp on update`() = runBlocking {
            val city = dbQuery {
                repository.create(City(0L, "Paris", 5.0 ,5.0,fixedInstant, fixedInstant))
            }

            val laterInstant = fixedInstant.plusSeconds(3600)
            val laterClock = Clock.fixed(laterInstant, ZoneId.of("UTC"))
            val laterRepository = CityRepositoryImpl(laterClock)

            val updatedCity = dbQuery {
                laterRepository.update(city.cityId, city.copy(cityName = "New Paris"))
            }

            assertNotNull(updatedCity)
            assertEquals(fixedInstant, updatedCity.createdAt) // createdAt unchanged
            assertEquals(laterInstant, updatedCity.updatedAt) // updatedAt changed
        }

        @Test
        fun `should return null when updating non-existent city`() = runBlocking {
            val nonExistentId = 999L

            val result = dbQuery {
                repository.update(nonExistentId, City(nonExistentId, "Ghost City", 5.0 ,5.0,fixedInstant, fixedInstant))
            }

            assertNull(result)
        }

        @Test
        fun `should not allow updating to duplicate city name`(){
            runBlocking {
                dbQuery {
                    repository.create(City(0L, "London", 5.0 ,5.0,fixedInstant, fixedInstant))
                    val paris = repository.create(City(0L, "Paris", 5.0 ,5.0,fixedInstant, fixedInstant))

                    assertFails {
                        repository.update(paris.cityId, paris.copy(cityName = "London"))
                    }
                }
            }
        }
    }

    @Nested
    inner class `doesExist by name function` {

        @Test
        fun `should return true when city name exists`() = runBlocking {
            val exists = dbQuery {
                repository.create(City(0L, "London", 5.0 ,5.0,fixedInstant, fixedInstant))
                repository.doesExist("London")
            }

            assertTrue(exists)
        }

        @Test
        fun `should return false when city name does not exist`() = runBlocking {
            val exists = dbQuery { repository.doesExist("NonExistentCity") }

            assertFalse(exists)
        }

        @Test
        fun `should be case sensitive`() = runBlocking {
            val existsLowercase = dbQuery {
                repository.create(City(0L, "London", 5.0 ,5.0,fixedInstant, fixedInstant))
                repository.doesExist("london")
            }

            assertFalse(existsLowercase)
        }

        @Test
        fun `should not match partial names`() = runBlocking {
            val existsPartial = dbQuery {
                repository.create(City(0L, "New York", 5.0 ,5.0,fixedInstant, fixedInstant))
                repository.doesExist("New")
            }

            assertFalse(existsPartial)
        }

        @Test
        fun `should handle special characters in city names`() = runBlocking {
            val exists =  dbQuery {
                repository.create(City(0L, "São Paulo", 5.0 ,5.0,fixedInstant, fixedInstant))
                repository.doesExist("São Paulo")
            }

            assertTrue(exists)
        }
    }

    @Nested
    inner class `doesExist by id function` {

        @Test
        fun `should return true when city id exists`() = runBlocking {
            val city = dbQuery {
                repository.create(City(0L, "London", 5.0 ,5.0,fixedInstant, fixedInstant))
            }

            val exists = dbQuery {
                repository.doesExist(city.cityId)
            }

            assertTrue(exists)
        }

        @Test
        fun `should return false when city id does not exist`() = runBlocking {
            val exists = dbQuery {
                repository.doesExist(999L)
            }

            assertFalse(exists)
        }

        @Test
        fun `should return false for negative ids`() = runBlocking {
            val exists = dbQuery {
                repository.doesExist(-1L)
            }

            assertFalse(exists)
        }

        @Test
        fun `should return false for zero id`() = runBlocking {
            val exists = dbQuery {
                repository.doesExist(0L)
            }

            assertFalse(exists)
        }
    }

    @Nested
    inner class `findById function` {

        @Test
        fun `should return city when found`() = runBlocking {
            val createdCity = dbQuery {
                repository.create(City(0L, "London",5.0 ,5.0, fixedInstant, fixedInstant))
            }

            val foundCity = dbQuery {
                repository.findById(createdCity.cityId)
            }

            assertNotNull(foundCity)
            assertEquals(createdCity.cityId, foundCity.cityId)
            assertEquals(createdCity.cityName, foundCity.cityName)
            assertEquals(createdCity.createdAt, foundCity.createdAt)
            assertEquals(createdCity.updatedAt, foundCity.updatedAt)
        }

        @Test
        fun `should return null when city not found`() = runBlocking {
            val result = dbQuery {
                repository.findById(999L)
            }

            assertNull(result)
        }

        @Test
        fun `should return correct city among multiple cities`() = runBlocking {
            val paris = dbQuery {
                repository.create(City(0L, "Paris", 5.0 ,5.0,fixedInstant, fixedInstant))
            }
            val london = dbQuery {
                repository.create(City(0L, "London", 5.0 ,5.0,fixedInstant, fixedInstant))
            }
            val berlin = dbQuery {
                repository.create(City(0L, "Berlin", 5.0 ,5.0,fixedInstant, fixedInstant))
            }

            val foundCity = dbQuery {
                repository.findById(london.cityId)
            }

            assertNotNull(foundCity)
            assertEquals("London", foundCity.cityName)
            assertEquals(london.cityId, foundCity.cityId)
        }

        @Test
        fun `should handle negative ids gracefully`() = runBlocking {
            val result = dbQuery {
                repository.findById(-1L)
            }

            assertNull(result)
        }
    }

    @Nested
    inner class `delete function` {

        @Test
        fun `should delete city and return 1 when city exists`() = runBlocking {
            val city = dbQuery {
                repository.create(City(0L, "London", 5.0 ,5.0,fixedInstant, fixedInstant))
            }

            val deleted = dbQuery {
                repository.delete(city.cityId)
            }

            assertEquals(1, deleted)

            // Verify city is actually deleted
            val found = dbQuery {
                repository.findById(city.cityId)
            }
            assertNull(found)
        }

        @Test
        fun `should return 0 when city does not exist`() = runBlocking {
            val deleted = dbQuery {
                repository.delete(999L)
            }

            assertEquals(0, deleted)
        }

        @Test
        fun `should not allow deleting same city twice`() = runBlocking {
            val city = dbQuery {
                repository.create(City(0L, "Paris",5.0 ,5.0, fixedInstant, fixedInstant))
            }

            val firstDelete = dbQuery {
                repository.delete(city.cityId)
            }
            val secondDelete = dbQuery {
                repository.delete(city.cityId)
            }

            assertEquals(1, firstDelete)
            assertEquals(0, secondDelete)
        }

        @Test
        fun `should only delete specified city`() = runBlocking {
            val london = dbQuery {
                repository.create(City(0L, "London", 5.0 ,5.0,fixedInstant, fixedInstant))
            }
            val paris = dbQuery {
                repository.create(City(0L, "Paris", 5.0 ,5.0,fixedInstant, fixedInstant))
            }

            dbQuery {
                repository.delete(london.cityId)
            }

            val londonFound = dbQuery { repository.findById(london.cityId) }
            val parisFound = dbQuery { repository.findById(paris.cityId) }

            assertNull(londonFound)
            assertNotNull(parisFound)
        }

        @Test
        fun `should handle deleting with negative id`()= runBlocking  {
            val deleted = dbQuery {
                repository.delete(-1L)
            }

            assertEquals(0, deleted)
        }
    }

    @Nested
    inner class `getAll function` {

        @Test
        fun `should return empty list when no cities exist`()= runBlocking  {
            val cities = dbQuery {
                repository.findAll()
            }

            assertTrue(cities.isEmpty())
        }

        @Test
        fun `should return all cities`() = runBlocking {
            dbQuery {
                repository.create(City(0L, "London",5.0 ,5.0, fixedInstant, fixedInstant))
                repository.create(City(0L, "Paris", 5.0 ,5.0,fixedInstant, fixedInstant))
                repository.create(City(0L, "Berlin",5.0 ,5.0, fixedInstant, fixedInstant))
            }

            val cities = dbQuery {
                repository.findAll()
            }

            assertEquals(3, cities.size)
            assertTrue(cities.any { it.cityName == "London" })
            assertTrue(cities.any { it.cityName == "Paris" })
            assertTrue(cities.any { it.cityName == "Berlin" })
        }

        @Test
        fun `should return cities with all fields populated`() = runBlocking {
            val createdCity = dbQuery {
                repository.create(City(0L, "Tokyo", 5.0 ,5.0,fixedInstant, fixedInstant))
            }

            val cities = dbQuery {
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
        fun `should handle full CRUD lifecycle`() = runBlocking {
            // Create
            val city = dbQuery {
                repository.create(City(0L, "London", 5.0 ,5.0,fixedInstant, fixedInstant))
            }
            assertNotNull(city.cityId)

            // Read
            val foundCity = dbQuery {
                repository.findById(city.cityId)
            }
            assertNotNull(foundCity)
            assertEquals("London", foundCity.cityName)

            // Update
            val updatedCity = dbQuery {
                repository.update(city.cityId, city.copy(cityName = "Greater London"))
            }
            assertNotNull(updatedCity)
            assertEquals("Greater London", updatedCity.cityName)

            // Delete
            val deleted = dbQuery {
                repository.delete(city.cityId)
            }
            assertEquals(1, deleted)

            // Verify deletion
            val afterDelete = dbQuery {
                repository.findById(city.cityId)
            }
            assertNull(afterDelete)
        }

        @Test
        fun `should handle concurrent city creation`() = runBlocking {
            val cityNames = listOf("London", "Paris", "Berlin", "Madrid", "Rome")

            val cities = dbQuery  {
                cityNames.map { name ->
                    repository.create(City(0L, name, 5.0 ,5.0,fixedInstant, fixedInstant))
                }
            }

            assertEquals(5, cities.size)
            assertEquals(cityNames.toSet(), cities.map { it.cityName }.toSet())

            // All IDs should be unique
            val ids = cities.map { it.cityId }
            assertEquals(ids.size, ids.toSet().size)
        }

        @Test
        fun `should maintain data integrity after multiple operations`() = runBlocking {
            // Create multiple cities
            val london = dbQuery {
                repository.create(City(0L, "London", 5.0 ,5.0,fixedInstant, fixedInstant))
            }
            val paris = dbQuery {
                repository.create(City(0L, "Paris", 5.0 ,5.0,fixedInstant, fixedInstant))
            }

            // Update one
            dbQuery {
                repository.update(london.cityId, london.copy(cityName = "Greater London"))
            }

            // Delete another
            dbQuery {
                repository.delete(paris.cityId)
            }

            // Verify final state
            val allCities = dbQuery {
                repository.findAll()
            }

            assertEquals(1, allCities.size)
            assertEquals("Greater London", allCities.first().cityName)
        }
    }
}