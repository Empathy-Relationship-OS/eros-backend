package com.eros.users.models

import com.eros.common.serializers.InstantSerializer
import kotlinx.serialization.Serializable
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
@Serializable
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

    val reachLevel: ReachLevel,

    // Timestamps
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant
){
    /**
     * Function to determine if another user matches the preferences of a user.
     *
     * @param otherProfile User object of the user that is being checked for compatibility.
     * @param otherPreference UserPreference object of user that is being checked for compatibility to get their
     *                        preferences on cities to ensure a valid location can be used.
     *
     * @return Boolean `true` if the other profile matches the preferences, otherwise `false`.
     */
    fun matchesUser(otherProfile: User, otherPreference: UserPreference): Boolean {
        //todo: Alter to include reach level / other nonnull values.

        // Check gender compatibility
        if (otherProfile.gender !in genderIdentities) return false

        // Check age range
        if (otherProfile.getAge() !in ageRangeMin..ageRangeMax) return false

        // Check height range
        if (otherProfile.heightCm !in heightRangeMin..heightRangeMax) return false

        // Check city overlap
        if (dateCities.none { it in otherPreference.dateCities }) return false

        return true
    }
}

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
@Serializable
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
    val reachLevel: ReachLevel
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
@Serializable
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
    val reachLevel: ReachLevel
)