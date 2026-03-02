package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.database.repository.BaseDAOImpl
import com.eros.users.models.City
import com.eros.users.table.Cities
import com.eros.users.table.toCityDTO
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Clock
import java.time.Instant

class CityRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : BaseDAOImpl<Long, City>(Cities, Cities.id), CityRepository {

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    override fun ResultRow.toDomain(): City = toCityDTO()

    override fun toStatement(statement: UpdateBuilder<*>, entity: City) {
        statement.apply {
            this[Cities.cityName] = entity.cityName
            this[Cities.createdAt] = entity.createdAt
            this[Cities.updatedAt] = Instant.now(clock)
        }
    }

    // -------------------------------------------------------------------------
    // IBaseDAO overrides
    // -------------------------------------------------------------------------

    /** Uses insertReturning to avoid a separate re-fetch round-trip after insert. */
    override suspend fun create(entity: City): City = dbQuery {
        val now = Instant.now(clock)
        Cities.insertReturning {
            it[cityName] = entity.cityName
            it[createdAt] = now
            it[updatedAt] = now
        }.single().toDomain()
    }

    /**
     * Function for finding if a city exists in the Cities table with the name cityName.
     */
    override fun doesExist(cityName: String): Boolean {
        return Cities.selectAll()
            .where { Cities.cityName eq cityName }
            .empty().not()
    }
}
