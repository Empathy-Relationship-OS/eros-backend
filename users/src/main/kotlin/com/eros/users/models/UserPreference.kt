package com.eros.users.models

import java.time.Instant

/**
 * Represents a user's dating preferences.
 *
 * This data class stores all preference criteria that a user has set for potential matches,
 * including demographic filters, physical attributes, and activity preferences.
 *
 * @property id Unique identifier for the preference record
 * @property userId The ID of the user who owns these preferences
 * @property genderIdentities List of gender identities the user is interested in matching with
 * @property ageRangeMin Minimum age (in years) for potential matches
 * @property ageRangeMax Maximum age (in years) for potential matches
 * @property heightRangeMin Minimum height for potential matches
 * @property heightRangeMax Maximum height for potential matches
 * @property ethnicity List of ethnicities the user is interested in matching with
 * @property dateLanguages List of languages the user prefers for dates
 * @property dateActivities List of activities the user enjoys on dates
 * @property dateLimit Optional limit on the number of dates per period (null means unlimited)
 * @property dateCities List of cities where the user is willing to date (populated from UserCitiesPreferences table)
 * @property createdAt Timestamp when the preference record was created
 * @property updatedAt Timestamp when the preference record was last modified
 */
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
    val dateLimit: Int?,

    // For collecting user city preferences from UserCitiesPreferences table
    val dateCities: List<City>,

    // Timestamps
    val createdAt: Instant,
    val updatedAt: Instant

)

/**
 * Request payload for creating a new user preference record.
 *
 * This class is used when a user initially sets up their dating preferences.
 * Unlike [UserPreference], this does not include system-generated fields like id and timestamps.
 *
 * @property userId The ID of the user creating preferences
 * @property genderIdentities List of gender identities the user is interested in matching with
 * @property ageRangeMin Minimum age (in years) for potential matches
 * @property ageRangeMax Maximum age (in years) for potential matches
 * @property heightRangeMin Minimum height for potential matches
 * @property heightRangeMax Maximum height for potential matches
 * @property ethnicity List of ethnicities the user is interested in matching with
 * @property dateLanguages List of languages the user prefers for dates
 * @property dateActivities List of activities the user enjoys on dates
 * @property dateLimit Optional limit on the number of dates per period (null means unlimited)
 * @property dateCities List of city IDs where the user is willing to date
 */
data class CreatePreferenceRequest(

    val userId: String,

    val genderIdentities: List<Gender>,

    val ageRangeMin: Int,
    val ageRangeMax: Int,
    val heightRangeMin: Int,
    val heightRangeMax: Int,

    val ethnicity: List<Ethnicity>,

    val dateLanguages: List<Language>,
    val dateActivities: List<Activity>,
    val dateLimit: Int?,
    val dateCities: List<Long>,  // Array of CityId's
)

/**
 * Request payload for updating an existing user preference record.
 *
 * This class is used when a user modifies their existing dating preferences.
 * All preference fields can be updated except system-managed timestamps.
 *
 * @property id The unique identifier of the preference record to update
 * @property userId The ID of the user who owns these preferences
 * @property genderIdentities List of gender identities the user is interested in matching with
 * @property ageRangeMin Minimum age (in years) for potential matches
 * @property ageRangeMax Maximum age (in years) for potential matches
 * @property heightRangeMin Minimum height for potential matches
 * @property heightRangeMax Maximum height for potential matches
 * @property ethnicity List of ethnicities the user is interested in matching with
 * @property dateLanguages List of languages the user prefers for dates
 * @property dateActivities List of activities the user enjoys on dates
 * @property dateLimit Optional limit on the number of dates per period (null means unlimited)
 * @property dateCities List of city IDs where the user is willing to date
 */
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
    val dateLimit: Int?,
    val dateCities: List<Long>, // Array of CityId's
)