package com.eros.venues.repository

import com.eros.database.repository.IBaseDAO
import com.eros.venues.models.Venue


interface VenueRepository : IBaseDAO<Long, Venue> {



}