package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.users.models.CreateUserCityPreferenceRequest
import com.eros.users.models.DeleteAllUserCityPreferenceRequest
import com.eros.users.models.DeleteUserCityPreferenceRequest
import com.eros.users.models.UserCityPreference
import com.eros.users.table.UserCitiesPreference
import com.eros.users.table.toUserCityPreferenceDTO
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Clock
import java.time.Instant

class UserCitiesRepositoryImpl(private val clock: Clock = Clock.systemUTC()) : UserCitiesRepository {

    override suspend fun addUserCityPreference(request: CreateUserCityPreferenceRequest): UserCityPreference = dbQuery {
        val now = Instant.now(clock)

        UserCitiesPreference.insert { row ->
            row[cityId] = request.cityId
            row[userId] = request.userId
            row[createdAt] = now
        }

        // FIXED: Use UserCitiesPreference.userId instead of UserPreferences.userId
        UserCitiesPreference.selectAll()
            .where {
                (UserCitiesPreference.userId eq request.userId) and
                        (UserCitiesPreference.cityId eq request.cityId)
            }
            .single()
            .toUserCityPreferenceDTO()
    }

    /**
     * Batch insert multiple city preferences for a user.
     * More efficient than multiple individual inserts.
     *
     * @param userId - The id of the user
     * @param cityIds - List of city ids to add as preferences
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
     * Function to delete a City from a Users city preference list.
     *
     * @param request - Contains cityId and userId
     *
     * @return [UserCityPreference] if the record was deleted otherwise `null`
     */
    override suspend fun deleteUserCityPreference(request: DeleteUserCityPreferenceRequest): UserCityPreference? =
        dbQuery {
            // Get the record before deleting - To ensure it is a valid record.
            val existingRecord = UserCitiesPreference.selectAll()
                .where {
                    (UserCitiesPreference.userId eq request.userId) and
                            (UserCitiesPreference.cityId eq request.cityId)
                }
                .singleOrNull()
                ?.toUserCityPreferenceDTO()

            // Delete the record
            UserCitiesPreference.deleteWhere {
                (UserCitiesPreference.userId eq request.userId) and
                        (UserCitiesPreference.cityId eq request.cityId)
            }

            // Return the deleted record (or null if it didn't exist)
            existingRecord
        }

    override suspend fun deleteAllUserCityPreference(request: DeleteAllUserCityPreferenceRequest): Int = dbQuery {
        // Delete the record
        UserCitiesPreference.deleteWhere {
            UserCitiesPreference.userId eq request.userId
        }
    }
}