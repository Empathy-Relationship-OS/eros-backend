package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.database.repository.BaseDAOImpl
import com.eros.users.models.*
import com.eros.users.table.Cities
import com.eros.users.table.UserCitiesPreference
import com.eros.users.table.UserPreferences
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Clock
import java.time.Instant

class PreferenceRepositoryImpl(
    private val clock: Clock = Clock.systemUTC(),
    private val userCitiesRepository: UserCitiesRepository
) : BaseDAOImpl<Long, UserPreference>(UserPreferences, UserPreferences.id), PreferenceRepository {

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    override fun ResultRow.toDomain(): UserPreference = UserPreference(
        id = this[UserPreferences.id],
        userId = this[UserPreferences.userId],
        genderIdentities = this[UserPreferences.genderIdentities].map { Gender.valueOf(it) },
        ageRangeMin = this[UserPreferences.ageRangeMin],
        ageRangeMax = this[UserPreferences.ageRangeMax],
        heightRangeMin = this[UserPreferences.heightRangeMin],
        heightRangeMax = this[UserPreferences.heightRangeMax],
        ethnicity = this[UserPreferences.ethnicities].map { Ethnicity.valueOf(it) },
        dateCities = emptyList(), // populated separately via getUserPreferenceWithCities
        dateLanguages = this[UserPreferences.dateLanguages].map { Language.valueOf(it) },
        dateActivities = this[UserPreferences.dateActivities].map { Activity.valueOf(it) },
        dateLimit = this[UserPreferences.dateLimit],
        createdAt = this[UserPreferences.createdAt],
        updatedAt = this[UserPreferences.updatedAt]
    )

    override fun toStatement(statement: UpdateBuilder<*>, entity: UserPreference) {
        statement.apply {
            this[UserPreferences.userId] = entity.userId
            this[UserPreferences.genderIdentities] = entity.genderIdentities.map { it.name }
            this[UserPreferences.ageRangeMin] = entity.ageRangeMin
            this[UserPreferences.ageRangeMax] = entity.ageRangeMax
            this[UserPreferences.heightRangeMin] = entity.heightRangeMin
            this[UserPreferences.heightRangeMax] = entity.heightRangeMax
            this[UserPreferences.ethnicities] = entity.ethnicity.map { it.name }
            this[UserPreferences.dateLanguages] = entity.dateLanguages.map { it.name }
            this[UserPreferences.dateActivities] = entity.dateActivities.map { it.name }
            this[UserPreferences.dateLimit] = entity.dateLimit
            this[UserPreferences.createdAt] = entity.createdAt
            this[UserPreferences.updatedAt] = Instant.now(clock)
        }
    }

    // -------------------------------------------------------------------------
    // IBaseDAO overrides — also manages city preferences
    // -------------------------------------------------------------------------

    override suspend fun create(entity: UserPreference): UserPreference {
        dbQuery {
            UserPreferences.insert { toStatement(it, entity) }
        }
        userCitiesRepository.addUserCityPreferencesBatch(
            userId = entity.userId,
            cityIds = entity.dateCities.map { it.cityId }
        )
        return getUserPreferenceWithCities(entity.userId)
    }

    override suspend fun update(id: Long, entity: UserPreference): UserPreference? {
        val rowsUpdated = dbQuery {
            UserPreferences.update({ UserPreferences.id eq id }) { toStatement(it, entity) }
        }
        if (rowsUpdated == 0) return null

        userCitiesRepository.deleteAllUserCityPreference(DeleteAllUserCityPreferenceRequest(entity.userId))
        userCitiesRepository.addUserCityPreferencesBatch(
            userId = entity.userId,
            cityIds = entity.dateCities.map { it.cityId }
        )
        return getUserPreferenceWithCities(entity.userId)
    }

    // -------------------------------------------------------------------------
    // PreferenceRepository extras
    // -------------------------------------------------------------------------

    override suspend fun getUserPreferenceWithCities(userId: String): UserPreference = dbQuery {
        val prefs = UserPreferences.selectAll().where { UserPreferences.userId eq userId }.single()

        val cities = (Cities innerJoin UserCitiesPreference)
            .selectAll()
            .where { UserCitiesPreference.userId eq userId }
            .map { row ->
                City(
                    cityId = row[Cities.id],
                    cityName = row[Cities.cityName],
                    createdAt = row[Cities.createdAt],
                    updatedAt = row[Cities.updatedAt]
                )
            }

        prefs.toDomain().copy(dateCities = cities)
    }
}
