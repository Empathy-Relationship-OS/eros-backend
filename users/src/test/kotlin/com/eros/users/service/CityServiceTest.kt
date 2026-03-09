package com.eros.users.service

import com.eros.users.models.CreateCityRequest
import com.eros.users.repository.CityRepositoryImpl
import com.eros.users.table.Cities
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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
import java.time.ZoneId
import kotlin.test.assertEquals

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CityServiceTest {

    @Nested
    inner class `Get nearest city` {

        @Test
        fun `successfully get nearestCity`() = runTest {
            cityService.createCity(CreateCityRequest("London", 5.0, 5.1))
            cityService.createCity(CreateCityRequest("Brazil", -25.0, 85.1))
            cityService.createCity(CreateCityRequest("Toilet", 19.034, -45.1))
            val nearestCity = cityService.findNearestCity(10.034, 10.034)

            assertEquals("London", nearestCity[0].cityName)
        }

        @Test
        fun `successfully get nearestCity with negative long+lat`() = runBlocking {
            cityService.createCity(CreateCityRequest("London", 5.0, 5.1))
            cityService.createCity(CreateCityRequest("Brazil", -25.0, 85.1))
            cityService.createCity(CreateCityRequest("Toilet", 19.034, -45.1))
            cityService.createCity(CreateCityRequest("ABC", -49.034, -75.1))
            cityService.createCity(CreateCityRequest("DERF", 7.034, 12.1))
            cityService.createCity(CreateCityRequest("HRH", 45.034, -4.1))
            val nearestCity = cityService.findNearestCity(-40.034, -88.034)

            assertEquals("ABC", nearestCity[0].cityName)
        }

        @Test
        fun `successfully get nearestCity matching`() = runBlocking {
            cityService.createCity(CreateCityRequest("London", 5.0, 5.1))
            cityService.createCity(CreateCityRequest("Brazil", -25.0, 85.1))
            cityService.createCity(CreateCityRequest("Toilet", 19.034, -45.1))
            cityService.createCity(CreateCityRequest("ABC", -49.034, -75.1))
            cityService.createCity(CreateCityRequest("DEF", 7.034, 12.1))
            cityService.createCity(CreateCityRequest("HRH", 45.034, -4.1))
            val nearestCity = cityService.findNearestCity(85.1, -25.0)

            assertEquals("Brazil", nearestCity[0].cityName)
        }
    }


    // Helper functions

    companion object {
        @Container
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("test_db")
            withUsername("test_user")
            withPassword("test_password")
        }
    }

    private lateinit var repository: CityRepositoryImpl
    private lateinit var cityService: CityService
    private lateinit var clock: Clock
    private val fixedInstant = Instant.parse("2024-01-15T10:00:00Z")

    @BeforeAll
    fun setup() {
        // Run the actual migrations - This is to ensure the EXTENSIONS in V0 run.
        Flyway.configure()
            .dataSource(
                postgresContainer.jdbcUrl,
                postgresContainer.username,
                postgresContainer.password
            )
            .load()
            .migrate()
        Database.connect(
            url = postgresContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgresContainer.username,
            password = postgresContainer.password
        )
    }

    @BeforeEach
    fun setupEach() {
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
        repository = CityRepositoryImpl(clock)
        cityService = CityService(repository)

        transaction {
            Cities.deleteAll()
        }
    }
}