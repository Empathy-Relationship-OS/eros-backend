package com.eros.users.repository

import com.eros.database.repository.IBaseDAO
import com.eros.users.models.City

interface CityRepository : IBaseDAO<Long, City> {

    /**
     * Function for finding if a cityName existing in the Cities table.
     */
    suspend fun doesExist(cityName: String) : Boolean
}
