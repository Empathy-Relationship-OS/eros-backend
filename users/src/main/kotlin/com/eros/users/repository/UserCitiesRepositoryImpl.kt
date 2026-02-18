package com.eros.users.repository

import com.eros.database.dbQuery
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
}
