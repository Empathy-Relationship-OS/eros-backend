package com.eros.users.repository

import com.eros.users.models.City
import com.eros.users.table.Cities
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
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

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

        // Use regular transaction for schema creation (doesn't conflict)
        transaction {
            SchemaUtils.create(Cities)
        }
    }

    @BeforeEach
    fun setupEach() {
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
        repository = CityRepositoryImpl(clock)

        // Clear Cities before each test.
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

    @Test
    fun createCity() {
        val cityName = "TestValue"

        val city = runBlocking {
            repository.create(City(0L, cityName, fixedInstant, fixedInstant))
        }

        assertNotNull(city.cityId)
        assertEquals(cityName, city.cityName)
        assertEquals(fixedInstant, city.createdAt)
        assertEquals(fixedInstant, city.updatedAt)
    }

    @Test
    fun updateCity() {
        val cityName = "TestValue"
        val city: City = runBlocking {
            repository.create(City(0L, cityName, fixedInstant, fixedInstant))
        }
        val newCityName = "AlteredTestValue"
        val updatedCity: City? = runBlocking {
             repository.update(city.cityId, city.copy(cityName = newCityName))
        }
        assertNotEquals(updatedCity?.cityName ?: city.cityName, city.cityName)
    }


}