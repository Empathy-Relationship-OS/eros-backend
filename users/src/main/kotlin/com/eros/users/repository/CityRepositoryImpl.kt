package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.users.models.City
import com.eros.users.models.CreateCityRequest
import com.eros.users.models.UpdateCityRequest
import com.eros.users.table.Cities
import com.eros.users.table.Users
import com.eros.users.table.toCityDTO
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Clock
import java.time.Instant

class CityRepositoryImpl(private val clock: Clock = Clock.systemUTC()) : CityRepository {

    override suspend fun createCity(request: CreateCityRequest): City = dbQuery {
        val now = Instant.now(clock)

        val city = Cities.insertReturning{
            it[cityName] = request.cityName
            it[createdAt] = now
            it[updatedAt] = now
        }.single()
        city.toCityDTO()
    }

    override suspend fun updateCity(cityId : Long, request: UpdateCityRequest): City? = dbQuery {
        val now = Instant.now(clock)

        val rowsUpdated = Cities.update({ Cities.id eq cityId }) { row ->
            request.newCityName.let { row[Cities.cityName] = it }
            row[Users.updatedAt] = now
        }

        if (rowsUpdated == 0) {
            null
        } else {
            Cities.selectAll().where { Cities.id eq cityId }.single().toCityDTO()
        }
    }
}