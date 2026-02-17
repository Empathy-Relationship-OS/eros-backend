package com.eros.users.repository

import com.eros.users.models.City
import com.eros.users.models.CreateCityRequest
import com.eros.users.models.UpdateCityRequest

interface CityRepository {

    suspend fun createCity(request : CreateCityRequest) : City

    suspend fun updateCity(cityId: Long, request : UpdateCityRequest) : City?

}