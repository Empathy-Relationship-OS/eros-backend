package com.eros.users.repository

import com.eros.database.repository.IBaseDAO
import com.eros.users.models.City

interface CityRepository : IBaseDAO<Long, City>
