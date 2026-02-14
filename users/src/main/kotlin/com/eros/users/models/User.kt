package com.eros.users.models

import java.time.Instant
import java.time.LocalDate

/**
 * User domain model representing a complete user profile
 */
data class User(
    val userId: String,
    
    // Required fields
    val firstName: String,
    val lastName: String,
    val email: String,
    val heightCm: Int,
    val dateOfBirth: LocalDate,
    val city: String,
    val educationLevel: EducationLevel,
    val gender: Gender,
    
    // Optional profile fields
    val occupation: String,
    val bio: String = "",
    
    // Hobbies & Interests (5-10 required)
    // These combine all interest categories: Activity, Interest, Entertainment, Creative, MusicGenre, FoodAndDrink, Sport
    val interests: List<String>,
    
    // Personality Traits (3-10 required)
    val traits: List<Trait>,
    
    // Languages
    val preferredLanguage: Language,
    val spokenLanguages: DisplayableField<List<Language>>,
    
    // Beliefs & Values
    val religion: DisplayableField<Religion?>,
    val politicalView: DisplayableField<PoliticalView?>,
    
    // Habits
    val alcoholConsumption: DisplayableField<AlcoholConsumption?>,
    val smokingStatus: DisplayableField<SmokingStatus?>,
    val diet: DisplayableField<Diet?>,
    
    // Relationship goals
    val dateIntentions: DisplayableField<DateIntentions>,
    val relationshipType: DisplayableField<RelationshipType>,
    val kidsPreference: DisplayableField<KidsPreference>,
    
    // Identity
    val sexualOrientation: DisplayableField<SexualOrientation>,
    val pronouns: DisplayableField<Pronouns?>,
    val starSign: DisplayableField<StarSign?>,

    // Brain & Body attributes
    val brainAttributes: DisplayableField<List<BrainAttribute>?>,
    val brainDescription: DisplayableField<String?>, // max 100 chars
    val bodyAttributes: DisplayableField<List<BodyAttribute>?>,
    val bodyDescription: DisplayableField<String?>, // max 100 chars

    // Timestamps
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
) {
    /**
     * Check if the user profile is soft-deleted
     */
    fun isDeleted(): Boolean = deletedAt != null
    
    /**
     * Calculate user's age from date of birth
     */
    fun getAge(): Int {
        val today = LocalDate.from(Instant.now())
        var age = today.year - dateOfBirth.year
        if (today.monthValue < dateOfBirth.monthValue || 
            (today.monthValue == dateOfBirth.monthValue && today.dayOfMonth < dateOfBirth.dayOfMonth)) {
            age--
        }
        return age
    }
    
    /**
     * Get full name
     */
    fun getFullName(): String = "$firstName $lastName"
    
    /**
     * Validate that interests count is within allowed range (5-10)
     */
    fun hasValidInterestsCount(): Boolean = interests.size in 5..10
    
    /**
     * Validate that traits count is within allowed range (3-10)
     */
    fun hasValidTraitsCount(): Boolean = traits.size in 3..10
    
    /**
     * Validate that bio is within character limit (300)
     */
    fun hasValidBio(): Boolean = bio.length <= 300

    /**
     * Validate that brain description is within character limit (200)
     */
    fun hasValidBrainDescription(): Boolean = brainDescription.field?.length!! <= 100
    //TODO might have broken this
    /**
     * Validate that body description is within character limit (200)
     */
    fun hasValidBodyDescription(): Boolean = bodyDescription.field?.length!! <= 100


    /**
     * Check if profile has minimum required fields filled
     */
    fun hasMinimumRequiredFields(): Boolean {
        return firstName.isNotBlank() &&
               lastName.isNotBlank() &&
               email.isNotBlank() &&
               heightCm > 0 &&
               city.isNotBlank() &&
               hasValidInterestsCount() &&
               hasValidTraitsCount()
    }
}

/**
 * Request DTO for creating a new user
 */
data class CreateUserRequest(
    val userId: String, // From Firebase JWT
    val firstName: String,
    val lastName: String,
    val email: String,
    val heightCm: Int,
    val dateOfBirth: LocalDate,
    val city: String,
    val educationLevel: EducationLevel,
    val gender: Gender,
    val preferredLanguage: Language,
    
    // Optional fields
    val occupation: String? = null,
    val bio: String = "",
    val interests: List<String>,
    val traits: List<Trait>,
    val spokenLanguages: List<Language>? = null,
    val religion: Religion? = null,
    val politicalView: PoliticalView? = null,
    val alcoholConsumption: AlcoholConsumption? = null,
    val smokingStatus: SmokingStatus? = null,
    val diet: Diet? = null,
    val dateIntentions: DateIntentions? = null,
    val relationshipType: RelationshipType? = null,
    val kidsPreference: KidsPreference? = null,
    val sexualOrientation: SexualOrientation? = null,
    val pronouns: Pronouns? = null,
    val starSign: StarSign? = null,
    val brainAttributes: List<BrainAttribute>? = null,
    val brainDescription: String? = null,
    val bodyAttributes: List<BodyAttribute>? = null,
    val bodyDescription: String? = null
) {
    init {
        require(interests.size in 5..10) { "Interests must be between 5 and 10 items" }
        require(traits.size in 3..10) { "Traits must be between 3 and 10 items" }
        require( bio.length <= 300) { "Bio must not exceed 300 characters" }
        require(heightCm > 0) { "Height must be positive" }
        require(firstName.isNotBlank()) { "First name is required" }
        require(lastName.isNotBlank()) { "Last name is required" }
        require(email.isNotBlank()) { "Email is required" }
        require(city.isNotBlank()) { "City is required" }
        require(brainDescription == null || brainDescription.length <= 200) { "Brain description must not exceed 200 characters" }
        require(bodyDescription == null || bodyDescription.length <= 200) { "Body description must not exceed 200 characters" }

    }
}

/**
 * Request DTO for updating user profile
 */
data class UpdateUserRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val heightCm: Int? = null,
    val city: String? = null,
    val educationLevel: EducationLevel? = null,
    val occupation: String? = null,
    val bio: String? = null,
    val interests: List<String>? = null,
    val traits: List<Trait>? = null,
    val preferredLanguage: Language? = null,
    val spokenLanguages: List<Language>? = null,
    val religion: Religion? = null,
    val politicalView: PoliticalView? = null,
    val alcoholConsumption: AlcoholConsumption? = null,
    val smokingStatus: SmokingStatus? = null,
    val diet: Diet? = null,
    val dateIntentions: DateIntentions? = null,
    val relationshipType: RelationshipType? = null,
    val kidsPreference: KidsPreference? = null,
    val sexualOrientation: SexualOrientation? = null,
    val pronouns: Pronouns? = null,
    val starSign: StarSign? = null,
    val brainAttributes: List<BrainAttribute>? = null,
    val brainDescription: String? = null,
    val bodyAttributes: List<BodyAttribute>? = null,
    val bodyDescription: String? = null
) {
    init {
        if (interests != null) {
            require(interests.size in 5..10) { "Interests must be between 5 and 10 items" }
        }
        if (traits != null) {
            require(traits.size in 3..10) { "Traits must be between 3 and 10 items" }
        }
        if (bio != null) {
            require(bio.length <= 300) { "Bio must not exceed 300 characters" }
        }
        if (heightCm != null) {
            require(heightCm > 0) { "Height must be positive" }
        }
        if (brainDescription != null) {
            require(brainDescription.length <= 100) { "Brain description must not exceed 100 characters" }
        }
        if (bodyDescription != null) {
            require(bodyDescription.length <= 100) { "Body description must not exceed 100 characters" }
        }
    }
}

/**
 * This is an encapsulating class for DTOs to reflect if a field should be viewable
 * on users profile
 */
data class DisplayableField<T>(val field: T, val display: Boolean)
