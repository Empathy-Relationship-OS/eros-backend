package com.eros.users.repository

import com.eros.common.errors.BadRequestException
import com.eros.common.errors.ConflictException
import com.eros.common.errors.NotFoundException
import com.eros.database.repository.BaseDAOImpl
import com.eros.users.models.*
import com.eros.users.table.Cities
import com.eros.users.table.UserCitiesPreference
import com.eros.users.table.UserPreferences
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant


class PreferenceRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : BaseDAOImpl<String, UserPreference>(UserPreferences, UserPreferences.userId), PreferenceRepository {

    companion object{
        private val logger = LoggerFactory.getLogger(PreferenceRepositoryImpl::class.java)
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    override fun ResultRow.toDomain(): UserPreference = UserPreference(
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
        reachLevel = runCatching {
            ReachLevel.valueOf(this[UserPreferences.reachLevel])
        }.getOrElse { error ->
            val message =
                "Invalid reachLevel value '${this[UserPreferences.reachLevel]}' for userId '${this[UserPreferences.userId]}'"
            logger.error(message, error)
            throw BadRequestException(message)
        }, // TODO log + rethrow as a domain exception,
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

    override suspend fun create(entity: UserPreference): UserPreference {
        try {
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
            return getUserPreferenceWithCities(entity.userId)
        } catch (e: ExposedSQLException) {
            // PostgreSQL unique constraint violation error code is 23505
            // Primary key constraint on user_id prevents duplicate preferences
            if (e.sqlState == "23505" && e.message?.contains("user_preferences_pkey") == true) {
                throw ConflictException("User preferences already exist")
            }
            throw e
        }
    }

    override suspend fun update(id: String, entity: UserPreference): UserPreference {
        if (entity.userId != id) {
            throw BadRequestException("Path userId and payload userId must match.")
        }
        //val rowsUpdated = super.update(id, entity)
        val rowsUpdated = UserPreferences.update({ UserPreferences.userId eq id }) { toStatement(it, entity) }
        if (rowsUpdated == 0) throw NotFoundException("User preferences not found.")
        val newCityIds = entity.dateCities.map { it.cityId }

        // Get current city IDs
        val currentCityIds = UserCitiesPreference
            .select(UserCitiesPreference.cityId)
            .where { UserCitiesPreference.userId eq id }
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
                this[UserCitiesPreference.userId] = id
                this[UserCitiesPreference.cityId] = cityId
                this[UserCitiesPreference.createdAt] = Instant.now(clock)
            }
        }
        return getUserPreferenceWithCities(id)
    }

    // -------------------------------------------------------------------------
    // PreferenceRepository extras
    // -------------------------------------------------------------------------

    override suspend fun getUserPreferenceWithCities(userId: String): UserPreference {
        val preferences = UserPreferences.selectAll()
            .where { UserPreferences.userId eq userId }
            .single()

        val cities = (Cities innerJoin UserCitiesPreference)
            .selectAll()
            .where { UserCitiesPreference.userId eq userId }
            .map { row ->
                City(
                    cityId = row[Cities.cityId],
                    cityName = row[Cities.cityName],
                    longitude = row[Cities.longitude],
                    latitude = row[Cities.latitude],
                    createdAt = row[Cities.createdAt],
                    updatedAt = row[Cities.updatedAt]
                )
            }

        return preferences.toDomain().copy(dateCities = cities)
    }


    /**
     * Function to delete all the UserCitiesPreference records and the users preferences.
     *
     * @param id String of the user's id that is having their preferences deleted.
     *
     * @return `1` if UserPreference was remove, otherwise `0`.
     */
    override suspend fun delete(id: String): Int {
        // Delete the UserCitiesPreference records.
        UserCitiesPreference.deleteWhere { UserCitiesPreference.userId eq id }
        // Delete the UserPreference record.
        return UserPreferences.deleteWhere { UserPreferences.userId eq id }
    }
}
