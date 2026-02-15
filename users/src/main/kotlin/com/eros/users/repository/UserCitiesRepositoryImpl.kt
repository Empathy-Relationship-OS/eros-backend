package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.users.models.DeleteUserCityPreferenceRequest
import com.eros.users.models.UserCityPreference
import com.eros.users.table.UserCitiesPreference
import com.eros.users.table.UserPreferences
import com.eros.users.table.toUserCityPreferenceDTO
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Clock
import java.time.Instant

class UserCitiesRepositoryImpl(private val clock: Clock = Clock.systemUTC()) : UserCitiesRepository {

    override suspend fun addUserCityPreference(request: DeleteUserCityPreferenceRequest): UserCityPreference = dbQuery{
        val now = Instant.now(clock)

        UserCitiesPreference.insert { row ->
            row[cityId] = request.cityId
            row[userId] = request.userId
            row[createdAt] = now
        }

        UserCitiesPreference.selectAll()
            .where { UserPreferences.userId eq request.userId }
            .single()
            .toUserCityPreferenceDTO()

    }

    /**
     * Function to delete a City from a Users city preference list.
     *
     * @param cityId - The id of the city to be removed from the users preference list.
     * @param userId - The id of the user for the city to be removed from.
     *
     * @return [UserCityPreference] if the record was deleted otherwise `null`
     */
    override suspend fun deleteUserCityPreference(cityId: Long, userId: String): UserCityPreference? = dbQuery {
        // Get the record before deleting - To ensure it is a valid record.
        val existingRecord = UserCitiesPreference.selectAll()
            .where {
                (UserCitiesPreference.userId eq userId) and
                        (UserCitiesPreference.cityId eq cityId)
            }
            .singleOrNull()
            ?.toUserCityPreferenceDTO()

        // Delete the record
        UserCitiesPreference.deleteWhere {
            (UserCitiesPreference.userId eq userId) and
                    (UserCitiesPreference.cityId eq cityId)
        }

        // Return the deleted record (or null if it didn't exist)
        existingRecord
    }
}