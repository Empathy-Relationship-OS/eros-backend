package com.eros.users.repository

import com.eros.database.repository.IBaseDAO
import com.eros.users.models.City

interface CityRepository : IBaseDAO<Long, City> {

    /**
     * Function for finding if a cityName existing in the Cities table.
     */
    suspend fun doesExist(cityName: String) : Boolean

    /**
     * Function to find the city that is nearest the provided lat and long.
     */
    suspend fun findNearest(latitude: Double, longitude: Double) : City?
}
