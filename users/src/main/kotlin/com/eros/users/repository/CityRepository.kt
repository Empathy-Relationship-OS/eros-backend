package com.eros.users.repository

import com.eros.database.repository.IBaseDAO
import com.eros.users.models.City

interface CityRepository : IBaseDAO<Long, City> {

    /**
     * Function for finding if a cityName existing in the Cities table.
     */
    suspend fun doesExist(cityName: String) : Boolean

    /**
     * Function to find the nearest [limit] cities to the provided lat and long.
     */
    suspend fun findNearest(limit: Int, latitude: Double, longitude: Double) : List<City>
}
