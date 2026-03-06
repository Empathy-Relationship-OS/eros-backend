package com.eros.users.repository

import com.eros.common.errors.ConflictException
import com.eros.database.repository.BaseDAOImpl
import com.eros.users.models.City
import com.eros.users.table.Cities
import com.eros.users.table.toCity
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Clock
import java.time.Instant

class CityRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : BaseDAOImpl<Long, City>(Cities, Cities.cityId), CityRepository {

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    override fun ResultRow.toDomain(): City = toCity()

    override fun toStatement(statement: UpdateBuilder<*>, entity: City) {
        statement.apply {
            this[Cities.cityName] = entity.cityName
            this[Cities.createdAt] = entity.createdAt
            this[Cities.longitude] = entity.longitude
            this[Cities.latitude] = entity.latitude
            this[Cities.updatedAt] = Instant.now(clock)
        }
    }

    // -------------------------------------------------------------------------
    // IBaseDAO overrides
    // -------------------------------------------------------------------------

    /**
     * Uses insertReturning to avoid a separate re-fetch round-trip after insert.
     * Catches duplicate key violations and throws ConflictException.
     */
    override suspend fun create(entity: City): City {
        val now = Instant.now(clock)
        try {
            return Cities.insertReturning {
                it[cityName] = entity.cityName
                it[longitude] = entity.longitude
                it[latitude] = entity.latitude
                it[createdAt] = now
                it[updatedAt] = now
            }.single().toDomain()
        } catch (e: ExposedSQLException) {
            // PostgreSQL unique constraint violation error code is 23505
            if (e.sqlState == "23505" && e.message?.contains("city_name") == true) {
                throw ConflictException("City already exists")
            }
            throw e
        }
    }

    /**
     * Function for finding if a city exists in the Cities table with the name cityName.
     */
    override suspend fun doesExist(cityName: String): Boolean {
        return Cities.selectAll()
            .where { Cities.cityName eq cityName }
            .empty().not()
    }
}
