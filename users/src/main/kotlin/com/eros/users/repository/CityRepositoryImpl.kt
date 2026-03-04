package com.eros.users.repository

import com.eros.database.repository.BaseDAOImpl
import com.eros.users.models.City
import com.eros.users.table.Cities
import com.eros.users.table.toCityDTO
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.doubleParam
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
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

    override fun ResultRow.toDomain(): City = toCityDTO()

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

    /** Uses insertReturning to avoid a separate re-fetch round-trip after insert. */
    override suspend fun create(entity: City): City {
        val now = Instant.now(clock)
        return Cities.insertReturning {
            it[cityName] = entity.cityName
            it[longitude] = entity.longitude
            it[latitude] = entity.latitude
            it[createdAt] = now
            it[updatedAt] = now
        }.single().toDomain()
    }

    /**
     * Function for finding if a city exists in the Cities table with the name cityName.
     */
    override suspend fun doesExist(cityName: String): Boolean {
        return Cities.selectAll()
            .where { Cities.cityName eq cityName }
            .empty().not()
    }

    /**
     * Function to find the nearest city to a given latitude and longitude
     */
    override suspend fun findNearest(latitude: Double, longitude: Double): City? {
        return Cities
            .selectAll()
            .orderBy(
                CustomFunction(
                    "earth_distance",
                    DoubleColumnType(),
                    Cities.latitude,
                    Cities.longitude,
                    doubleParam(latitude),
                    doubleParam(longitude)
                )
            )
            .limit(1)
            .map { it.toDomain() }
            .firstOrNull()
    }
}
