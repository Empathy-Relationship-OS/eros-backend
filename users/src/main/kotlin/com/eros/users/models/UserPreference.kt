package com.eros.users.models

import kotlinx.serialization.Serializable
import java.time.Instant


data class UserPreference(

    val id: Long,

    val userId: String,

    val genderIdentities: List<Gender>,

    val ageRangeMin: Int,
    val ageRangeMax: Int,
    val heightRangeMin: Int,
    val heightRangeMax: Int,

    val ethnicity: List<Ethnicity>,

    val dateLanguages: List<Language>,
    val dateActivities: List<Activity>,
    val dateLimit: Int,

    // For collecting user city preferences from UserCitiesPreferences table
    val dateCities: List<City>,

    // Timestamps
    val createdAt: Instant,
    val updatedAt: Instant

) {
}

data class CreatePreferenceRequest(
    val id: Long,

    val userId: String,

    val genderIdentities: List<Gender>,

    val ageRangeMin: Int,
    val ageRangeMax: Int,
    val heightRangeMin: Int,
    val heightRangeMax: Int,

    val ethnicity: List<Ethnicity>,

    val dateLanguages: List<Language>,
    val dateActivities: List<Activity>,
    val dateLimit: Int,
    val dateCities: List<Long>, // Just city IDs in the request

    // Timestamps
    val createdAt: Instant,
    val updatedAt: Instant,
) {}

data class UpdatePreferenceRequest(
    val id: Long,

    val userId: String,

    val genderIdentities: List<Gender>,

    val ageRangeMin: Int,
    val ageRangeMax: Int,
    val heightRangeMin: Int,
    val heightRangeMax: Int,

    val ethnicity: List<Ethnicity>,

    val dateLanguages: List<Language>,
    val dateActivities: List<Activity>,
    val dateLimit: Int,
    val dateCities: List<Long>, // Just city IDs in the request

    // Timestamps
    val createdAt: Instant,
    val updatedAt: Instant,
) {}




