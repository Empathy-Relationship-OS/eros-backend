package com.eros.users.service

import com.eros.users.models.City
import com.eros.users.models.CreateCityRequest
import com.eros.users.repository.CityRepository

class CityService(private val cityRepository: CityRepository) {

    suspend fun createCity(request: CreateCityRequest): City {
        return cityRepository.createCity(request)
    }


}