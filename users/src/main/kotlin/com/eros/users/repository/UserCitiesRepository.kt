package com.eros.users.repository

import com.eros.users.models.CreateUserCityPreferenceRequest
import com.eros.users.models.DeleteAllUserCityPreferenceRequest
import com.eros.users.models.DeleteUserCityPreferenceRequest
import com.eros.users.models.UserCityPreference

interface UserCitiesRepository {

    suspend fun addUserCityPreference(request : CreateUserCityPreferenceRequest) : UserCityPreference

    suspend fun deleteUserCityPreference(request : DeleteUserCityPreferenceRequest) : UserCityPreference?
    suspend fun deleteAllUserCityPreference(request : DeleteAllUserCityPreferenceRequest) : Int

}