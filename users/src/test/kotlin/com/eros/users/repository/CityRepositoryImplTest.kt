package com.eros.users.repository

import com.eros.users.models.City
import com.eros.users.models.CreateCityRequest
import com.eros.users.models.UpdateCityRequest
import com.eros.users.table.Cities
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
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
    }

    @AfterAll
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Cities)
        }
    }

    @Test
    fun createCity(){

        val id = 1L
        val cityName = "TestValue"
        val cityDefault = City(id,cityName,clock.instant(),clock.instant())
        var city = City(id,cityName,clock.instant(),clock.instant())
        runBlocking {
            city = repository.createCity(CreateCityRequest(id, cityName))
        }
        assertEquals(city, cityDefault)
    }

    @Test
    fun updateCity(){

        val id = 1L
        val cityName = "TestValue"
        val cityDefault = City(id,cityName,clock.instant(),clock.instant())
        var city: City? = City(id,cityName,clock.instant(),clock.instant())
        runBlocking {
            city = repository.createCity(CreateCityRequest(id, cityName))
        }
        val newCityName = "AlteredTestValue"
        runBlocking {
            city = repository.updateCity(cityDefault.id, UpdateCityRequest(newCityName))
        }
        assertNotEquals(city?.cityName ?: cityDefault.cityName, cityDefault.cityName)
    }


}