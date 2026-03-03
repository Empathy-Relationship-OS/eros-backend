package com.eros.users.service

import com.eros.common.errors.NotFoundException
import com.eros.database.dbQuery
import com.eros.users.models.City
import com.eros.users.models.CreateCityRequest
import com.eros.users.models.UpdateCityRequest
import com.eros.users.repository.CityRepository
import com.eros.users.table.Cities
import com.eros.users.table.toCityDTO
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Clock
import java.time.Instant

class CityService(
    private val cityRepository: CityRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    /**
     * This function creates a City in the database with the provided name.
     * Function is wrapped in a dbQuery to perform database queries within a coroutine-safe transaction context.
     *
     * @param [CreateCityRequest] Name of the city to be added to the database.
     * @return [City] Object of the city that has been created and added to the database.
     */
    suspend fun createCity(request: CreateCityRequest): City = dbQuery {
        val now = Instant.now(clock)
        val city = City(
            cityId = 0L, // DB auto-generates the id on insert
            cityName = request.cityName,
            longitude = request.longitude,
            latitude = request.latitude,
            createdAt = now,
            updatedAt = now
        )
        cityRepository.create(city)
    }


    /**
     * This function is used to update the name of a City that is already in the database.
     * Function is wrapped in a dbQuery to perform database queries within a coroutine-safe transaction context.
     *
     * @param [UpdateCityRequest] id of the city to be updated and the new city name.
     * @return [City] City object of the updated city in the database.
     */
    suspend fun updateCity(cityId: Long, request: UpdateCityRequest): City? = dbQuery {
        val existing = cityRepository.findById(cityId) ?: throw NotFoundException("No city with the provided id: $cityId")
        val updated = existing.copy(cityName = request.newCityName ?: existing.cityName,
            longitude = request.newCityLongitude ?: existing.longitude,
            latitude = request.newCityLatitude ?: existing.latitude)
        cityRepository.update(cityId, updated)
    }


    /**
     * Function to check if a city already exists with a given name.
     * Function is wrapped in a dbQuery to perform database queries within a coroutine-safe transaction context.
     *
     * @param cityName String of the city name to check if it exists.
     * @return `true` if the city name is already in the database, otherwise `false`
     */
    suspend fun doesExists(cityName : String) : Boolean = dbQuery {
        cityRepository.doesExist(cityName)
    }


    /**
     * Function to check if a city already exists with a given id.
     * Function is wrapped in a dbQuery to perform database queries within a coroutine-safe transaction context.
     *
     * @param cityId id to check if a city is in the database.
     * @return `true` if the city name is already in the database, otherwise `false`
     */
    suspend fun doesExists(cityId : Long) : Boolean = dbQuery {
        cityRepository.doesExist(cityId)
    }


    /**
     * Function to delete a city from the database.
     * Function is wrapped in a dbQuery to perform database queries within a coroutine-safe transaction context.
     *
     * @param cityId id of the city to be deleted.
     * @return `1` if the city was deleted, otherwise `false`
     */
    suspend fun deleteCity(cityId : Long) : Int = dbQuery {
        cityRepository.delete(cityId)
    }


    /**
     * Function to return a city with a given id.
     * Function is wrapped in a dbQuery to perform database queries within a coroutine-safe transaction context.
     *
     * @param id id of the city to be retrieved.
     * @return [City] City object of the retrieved city.
     */
    suspend fun findByCityId(id : Long) : City? = dbQuery {
        cityRepository.findById(id)
    }

    /**
     * Function to retrieve a list of all the cities in the database.
     * Function is wrapped in a dbQuery to perform database queries within a coroutine-safe transaction context.
     *
     * @return List of [City] objects for every city in the database.
     */
    suspend fun getAllCities() : List<City> = dbQuery {
        Cities.selectAll().map { it.toCityDTO()}
    }

}
