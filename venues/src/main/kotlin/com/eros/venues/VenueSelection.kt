package com.eros.venues

import com.eros.users.models.Activity
import com.eros.users.models.City
import com.eros.venues.models.Venue
import java.time.LocalTime


object VenueSelection{


    suspend fun randomSelect(city: City, time: LocalTime, activity: Activity) : Venue? {

        // BusinessService - Get all business for city of type activity

        // Reduce to only business with time

        // Randomly select and return

        return null;
    }

    suspend fun weightedSelect(){

    }


}
