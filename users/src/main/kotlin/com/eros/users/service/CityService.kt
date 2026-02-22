package com.eros.users.service

import com.eros.users.models.City
import com.eros.users.models.CreateCityRequest
import com.eros.users.models.UpdateCityRequest
import com.eros.users.repository.CityRepository
import java.time.Clock
import java.time.Instant

class CityService(
    private val cityRepository: CityRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    suspend fun createCity(request: CreateCityRequest): City {
        val now = Instant.now(clock)
        val city = City(
            cityId = 0L, // DB auto-generates the id on insert
            cityName = request.cityName,
            createdAt = now,
            updatedAt = now
        )
        return cityRepository.create(city)
    }

    suspend fun updateCity(cityId: Long, request: UpdateCityRequest): City? {
        val existing = cityRepository.findById(cityId) ?: return null
        val updated = existing.copy(cityName = request.newCityName)
        return cityRepository.update(cityId, updated)
    }

    fun doesExists(cityName : String) : Boolean {
        return cityRepository.doesExist(cityName)
    }

    suspend fun doesExists(cityId : Long) : Boolean{
        return cityRepository.doesExist(cityId)
    }

    suspend fun deleteCity(cityId : Long) : Int{
        return cityRepository.delete(cityId)
    }

    suspend fun findByCityId(id : Long) : City?{
        return cityRepository.findById(id)
    }

}
