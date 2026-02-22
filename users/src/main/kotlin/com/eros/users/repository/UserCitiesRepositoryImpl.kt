package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.users.models.City
import com.eros.users.models.DeleteAllUserCityPreferenceRequest
import com.eros.users.models.DeleteUserCityPreferenceRequest
import com.eros.users.models.UserCityPreference
import com.eros.users.table.UserCitiesPreference
import com.eros.users.table.toUserCityPreferenceDTO
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Clock
import java.time.Instant

class UserCitiesRepositoryImpl(private val clock: Clock = Clock.systemUTC()) : UserCitiesRepository {

    override suspend fun addUserCityPreference(entity: UserCityPreference): UserCityPreference = dbQuery {
        UserCitiesPreference.insert { row ->
            row[cityId] = entity.cityId
            row[userId] = entity.userId
            row[createdAt] = entity.createdAt
        }

        UserCitiesPreference.selectAll()
            .where {
                (UserCitiesPreference.userId eq entity.userId) and
                        (UserCitiesPreference.cityId eq entity.cityId)
            }
            .single()
            .toUserCityPreferenceDTO()
    }

    /**
     * Batch insert multiple city preferences for a user.
     * More efficient than multiple individual inserts.
     *
     * @param userId  The id of the user.
     * @param cityIds List of city ids to add as preferences.
     */
    override suspend fun addUserCityPreferencesBatch(userId: String, cityIds: List<Long>) = dbQuery {
        if (cityIds.isEmpty()) return@dbQuery

        val now = Instant.now(clock)

        UserCitiesPreference.batchInsert(cityIds) { cityId ->
            this[UserCitiesPreference.userId] = userId
            this[UserCitiesPreference.cityId] = cityId
            this[UserCitiesPreference.createdAt] = now
        }
    }

    /**
     * Batch insert multiple city preferences for a user from within a transaction.
     * More efficient than multiple individual inserts.
     *
     * @param userId  The id of the user.
     * @param cityIds List of city ids to add as preferences.
     */
    override fun addUserCityPreferencesBatchWithinTransaction(userId: String, cityIds: List<Long>) : Boolean{
        if (cityIds.isEmpty()) return false

        val now = Instant.now(clock)

        UserCitiesPreference.batchInsert(cityIds) { cityId ->
            this[UserCitiesPreference.userId] = userId
            this[UserCitiesPreference.cityId] = cityId
            this[UserCitiesPreference.createdAt] = now
        }
        return true
    }

    /**
     * Deletes a city from a user's city preference list.
     *
     * @param request Contains cityId and userId.
     * @return [UserCityPreference] if the record existed and was deleted, null otherwise.
     */
    override suspend fun deleteUserCityPreference(request: DeleteUserCityPreferenceRequest): UserCityPreference? =
        dbQuery {
            val existingRecord = UserCitiesPreference.selectAll()
                .where {
                    (UserCitiesPreference.userId eq request.userId) and
                            (UserCitiesPreference.cityId eq request.cityId)
                }
                .singleOrNull()
                ?.toUserCityPreferenceDTO()

            UserCitiesPreference.deleteWhere {
                (UserCitiesPreference.userId eq request.userId) and
                        (UserCitiesPreference.cityId eq request.cityId)
            }

            existingRecord
        }

    override suspend fun deleteAllUserCityPreference(request: DeleteAllUserCityPreferenceRequest): Int = dbQuery {
        UserCitiesPreference.deleteWhere {
            UserCitiesPreference.userId eq request.userId
        }
    }

    override fun deleteAllUserCityPreferenceWithinTransaction(request: DeleteAllUserCityPreferenceRequest): Int{
        return UserCitiesPreference.deleteWhere {
            UserCitiesPreference.userId eq request.userId
        }
    }

    override fun syncUserCityPreferences(userId: String, newCityIds: List<City>) {
        // Convert City to id
        val newCityIds = newCityIds.map {it.cityId}

        // Get current city IDs
        val currentCityIds = UserCitiesPreference
            .select(UserCitiesPreference.cityId)
            .where { UserCitiesPreference.userId eq userId }
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
                this[UserCitiesPreference.userId] = userId
                this[UserCitiesPreference.cityId] = cityId
            }
        }
    }
}
