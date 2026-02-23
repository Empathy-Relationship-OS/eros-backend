package com.eros.users.repository

import com.eros.users.models.City
import com.eros.users.models.DeleteAllUserCityPreferenceRequest
import com.eros.users.models.DeleteUserCityPreferenceRequest
import com.eros.users.models.UserCityPreference

/**
 * Repository for the user-city junction table.
 *
 * This table has a composite primary key (userId, cityId) and does not map cleanly to
 * [com.eros.database.repository.IBaseDAO]. All operations are keyed on the composite
 * pair rather than a single ID, so domain-specific methods are defined here directly.
 */
interface UserCitiesRepository {

    suspend fun addUserCityPreference(entity: UserCityPreference): UserCityPreference

    suspend fun addUserCityPreferencesBatch(userId: String, cityIds: List<Long>)

    fun addUserCityPreferencesBatchWithinTransaction(userId: String, cityIds: List<Long>) : Boolean

    suspend fun deleteUserCityPreference(request: DeleteUserCityPreferenceRequest): UserCityPreference?

    suspend fun deleteAllUserCityPreference(request: DeleteAllUserCityPreferenceRequest): Int

    fun deleteAllUserCityPreferenceWithinTransaction(request: DeleteAllUserCityPreferenceRequest): Int

}
