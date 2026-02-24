package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.database.repository.BaseDAOImpl
import com.eros.users.models.*
import com.eros.users.table.Cities
import com.eros.users.table.UserCitiesPreference
import com.eros.users.table.UserPreferences
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Clock
import java.time.Instant

class PreferenceRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
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
        reachLevel = ReachLevel.valueOf(this[UserPreferences.reachLevel]),
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
            this[UserPreferences.reachLevel] = entity.reachLevel.name
            this[UserPreferences.createdAt] = entity.createdAt
            this[UserPreferences.updatedAt] = Instant.now(clock)
        }
    }

    // -------------------------------------------------------------------------
    // IBaseDAO overrides — also manages city preferences
    // -------------------------------------------------------------------------

    override suspend fun create(entity: UserPreference): UserPreference = dbQuery {
        // Insert preference
        UserPreferences.insert { toStatement(it, entity) }

        // Insert cities
        val now = Instant.now(clock)
        UserCitiesPreference.batchInsert(entity.dateCities) { cityId ->
            this[UserCitiesPreference.userId] = entity.userId
            this[UserCitiesPreference.cityId] = cityId.cityId
            this[UserCitiesPreference.createdAt] = now
        }

        // Return the result (still inside transaction)
        getUserPreferenceWithCitiesWithinTransaction(entity.userId)
    }

    override suspend fun update(id: Long, entity: UserPreference): UserPreference?  = dbQuery {

        //val rowsUpdated = super.update(id, entity)
        val rowsUpdated = UserPreferences.update({ UserPreferences.id eq id }) { toStatement(it, entity) }
        if (rowsUpdated == 0) throw NotFoundException("User preferences not found.")
        val newCityIds = entity.dateCities.map {it.cityId}

        // Get current city IDs
        val currentCityIds = UserCitiesPreference
            .select(UserCitiesPreference.cityId)
            .where { UserCitiesPreference.userId eq entity.userId }
            .map { it[UserCitiesPreference.cityId] }
            .toSet()

        val newCityIdSet = newCityIds.toSet()

        // Delete removed cities
        val toDelete = currentCityIds - newCityIdSet
        if (toDelete.isNotEmpty()) {
            UserCitiesPreference.deleteWhere {
                (UserCitiesPreference.userId eq userId) and (cityId inList toDelete)
            }
        }

        // Insert new cities only
        val toInsert = newCityIdSet - currentCityIds
        if (toInsert.isNotEmpty()) {
            UserCitiesPreference.batchInsert(toInsert) { cityId ->
                this[UserCitiesPreference.userId] = entity.userId
                this[UserCitiesPreference.cityId] = cityId
                this[UserCitiesPreference.createdAt] = Instant.now(clock)
            }
        }
        getUserPreferenceWithCitiesWithinTransaction(entity.userId)
    }

    // -------------------------------------------------------------------------
    // PreferenceRepository extras
    // -------------------------------------------------------------------------

    override suspend fun getUserPreferenceWithCities(userId: String): UserPreference = dbQuery {
        val preferences = UserPreferences.selectAll()
            .where { UserPreferences.userId eq userId }
            .single()

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

        preferences.toDomain().copy(dateCities = cities)
    }

    fun getUserPreferenceWithCitiesWithinTransaction(userId: String): UserPreference{
        val preferences = UserPreferences.selectAll().where { UserPreferences.userId eq userId }.single()

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
        return preferences.toDomain().copy(dateCities = cities)
    }

    override suspend fun userPreferencesDoesExist(userId: String): Boolean {
        return UserPreferences.selectAll()
            .where { UserPreferences.userId eq userId }
            .count() > 0
    }


    /**
     * Function to delete all the UserCitiesPreference records and the users preferences.
     *
     * @param userId String of the user's id that is having their preferences deleted.
     *
     * @return `1` if UserPreference was remove, otherwise `0`.
     */
    override fun delete(userId: String): Int {
        // Delete the UserCitiesPreference records.
        UserCitiesPreference.deleteWhere { UserPreferences.userId eq userId }
        // Delete the UserPreference record.
        return UserPreferences.deleteWhere { UserPreferences.userId eq userId}
    }
}
