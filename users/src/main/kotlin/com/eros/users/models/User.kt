package com.eros.users.models

import com.eros.common.serializers.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.Clock
import java.time.ZoneId

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

    // Generated fields.
    val profileStatus: ProfileStatus,
    val eloScore: Int,
    val badges: Set<Badge>?,
    val profileCompleteness: Int,
    val coordinatesLongitude: Double,
    val coordinatesLatitude: Double,
    val role: Role,
    val photoValidationStatus: ValidationStatus,

    
    // Hobbies & Interests (5-10 required)
    // These combine all interest categories: Activity, Interest, Entertainment, Creative, MusicGenre, FoodAndDrink, Sport
    val interests: List<UserInterest>,
    
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
    val ethnicity: DisplayableField<List<Ethnicity>>,

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
        val today = LocalDate.now()
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

fun User.toDTO() = UserDTO(
    userId = this.userId,
    firstName = this.firstName,
    lastName = this.lastName,
    email = this.email,
    heightCm = this.heightCm,
    dateOfBirth = this.dateOfBirth,
    city = this.city,
    educationLevel = this.educationLevel,
    gender = this.gender,
    preferredLanguage = this.preferredLanguage,
    coordinatesLatitude = this.coordinatesLatitude,
    coordinatesLongitude = this.coordinatesLongitude,
    occupation = this.occupation,
    bio = this.bio,
    interests = this.interests,
    traits = this.traits ,
    spokenLanguages = this.spokenLanguages ,
    religion = this.religion ,
    politicalView = this.politicalView ,
    alcoholConsumption = this.alcoholConsumption ,
    smokingStatus = this.smokingStatus ,
    diet = this.diet ,
    dateIntentions = this.dateIntentions ,
    kidsPreference = this.kidsPreference ,
    relationshipType = this.relationshipType ,
    sexualOrientation = this.sexualOrientation ,
    pronouns = this.pronouns ,
    starSign = this.starSign ,
    ethnicity = this.ethnicity ,
    brainAttributes = this.brainAttributes ,
    brainDescription = this.brainDescription ,
    bodyAttributes = this.bodyAttributes ,
    bodyDescription = this.bodyDescription
)

/**
 * User DTO.
 */
@Serializable
data class UserDTO(
    val userId: String, // From Firebase JWT
    val firstName: String,
    val lastName: String,
    val email: String,
    val heightCm: Int,
    @Serializable(with = LocalDateSerializer::class)
    val dateOfBirth: LocalDate,
    val city: String,
    @Serializable(with = com.eros.users.serializers.EducationLevelSerializer::class)
    val educationLevel: EducationLevel,
    @Serializable(with = com.eros.users.serializers.GenderSerializer::class)
    val gender: Gender,
    @Serializable(with = com.eros.users.serializers.LanguageSerializer::class)
    val preferredLanguage: Language,

    val coordinatesLatitude: Double,
    val coordinatesLongitude: Double,

    // Optional fields
    val occupation: String? = null,
    val bio: String = "",
    val interests: List<@Serializable(with = com.eros.users.serializers.UserInterestSerializer::class) UserInterest>,
    val traits: List<@Serializable(with = com.eros.users.serializers.TraitSerializer::class) Trait>,

    // Displayable fields — client controls both value and visibility
    val spokenLanguages: DisplayableField<List<@Serializable(with = com.eros.users.serializers.LanguageSerializer::class) Language>>,
    val religion: DisplayableField<@Serializable(with = com.eros.users.serializers.ReligionSerializer::class) Religion?>,
    val politicalView: DisplayableField<@Serializable(with = com.eros.users.serializers.PoliticalViewSerializer::class) PoliticalView?>,
    val alcoholConsumption: DisplayableField<@Serializable(with = com.eros.users.serializers.AlcoholConsumptionSerializer::class) AlcoholConsumption?>,
    val smokingStatus: DisplayableField<@Serializable(with = com.eros.users.serializers.SmokingStatusSerializer::class) SmokingStatus?>,
    val diet: DisplayableField<@Serializable(with = com.eros.users.serializers.DietSerializer::class) Diet?>,
    val dateIntentions: DisplayableField<@Serializable(with = com.eros.users.serializers.DateIntentionsSerializer::class) DateIntentions>,
    val relationshipType: DisplayableField<@Serializable(with = com.eros.users.serializers.RelationshipTypeSerializer::class) RelationshipType>,
    val kidsPreference: DisplayableField<@Serializable(with = com.eros.users.serializers.KidsPreferenceSerializer::class) KidsPreference>,
    val sexualOrientation: DisplayableField<@Serializable(with = com.eros.users.serializers.SexualOrientationSerializer::class) SexualOrientation>,
    val pronouns: DisplayableField<@Serializable(with = com.eros.users.serializers.PronounsSerializer::class) Pronouns?>,
    val starSign: DisplayableField<@Serializable(with = com.eros.users.serializers.StarSignSerializer::class) StarSign?>,
    val ethnicity: DisplayableField<List<@Serializable(with = com.eros.users.serializers.EthnicitySerializer::class) Ethnicity>>,
    val brainAttributes: DisplayableField<List<@Serializable(with = com.eros.users.serializers.BrainAttributeSerializer::class) BrainAttribute>?>,
    val brainDescription: DisplayableField<String?>,
    val bodyAttributes: DisplayableField<List<@Serializable(with = com.eros.users.serializers.BodyAttributeSerializer::class) BodyAttribute>?>,
    val bodyDescription: DisplayableField<String?>
)



/**
 * Request DTO for creating a new user
 */
@Serializable
data class CreateUserRequest(
    val userId: String, // From Firebase JWT
    val firstName: String,
    val lastName: String,
    val email: String,
    val heightCm: Int,
    @Serializable(with = LocalDateSerializer::class)
    val dateOfBirth: LocalDate,
    val city: String,
    @Serializable(with = com.eros.users.serializers.EducationLevelSerializer::class)
    val educationLevel: EducationLevel,
    @Serializable(with = com.eros.users.serializers.GenderSerializer::class)
    val gender: Gender,
    @Serializable(with = com.eros.users.serializers.LanguageSerializer::class)
    val preferredLanguage: Language,

    val coordinatesLatitude: Double,
    val coordinatesLongitude: Double,

    // Optional fields
    val occupation: String? = null,
    val bio: String = "",
    val interests: List<@Serializable(with = com.eros.users.serializers.UserInterestSerializer::class) UserInterest>,
    val traits: List<@Serializable(with = com.eros.users.serializers.TraitSerializer::class) Trait>,

    // Displayable fields — client controls both value and visibility
    val spokenLanguages: DisplayableField<List<@Serializable(with = com.eros.users.serializers.LanguageSerializer::class) Language>>,
    val religion: DisplayableField<@Serializable(with = com.eros.users.serializers.ReligionSerializer::class) Religion?>,
    val politicalView: DisplayableField<@Serializable(with = com.eros.users.serializers.PoliticalViewSerializer::class) PoliticalView?>,
    val alcoholConsumption: DisplayableField<@Serializable(with = com.eros.users.serializers.AlcoholConsumptionSerializer::class) AlcoholConsumption?>,
    val smokingStatus: DisplayableField<@Serializable(with = com.eros.users.serializers.SmokingStatusSerializer::class) SmokingStatus?>,
    val diet: DisplayableField<@Serializable(with = com.eros.users.serializers.DietSerializer::class) Diet?>,
    val dateIntentions: DisplayableField<@Serializable(with = com.eros.users.serializers.DateIntentionsSerializer::class) DateIntentions>,
    val relationshipType: DisplayableField<@Serializable(with = com.eros.users.serializers.RelationshipTypeSerializer::class) RelationshipType>,
    val kidsPreference: DisplayableField<@Serializable(with = com.eros.users.serializers.KidsPreferenceSerializer::class) KidsPreference>,
    val sexualOrientation: DisplayableField<@Serializable(with = com.eros.users.serializers.SexualOrientationSerializer::class) SexualOrientation>,
    val pronouns: DisplayableField<@Serializable(with = com.eros.users.serializers.PronounsSerializer::class) Pronouns?>,
    val starSign: DisplayableField<@Serializable(with = com.eros.users.serializers.StarSignSerializer::class) StarSign?>,
    val ethnicity: DisplayableField<List<@Serializable(with = com.eros.users.serializers.EthnicitySerializer::class) Ethnicity>>,
    val brainAttributes: DisplayableField<List<@Serializable(with = com.eros.users.serializers.BrainAttributeSerializer::class) BrainAttribute>?>,
    val brainDescription: DisplayableField<String?>,
    val bodyAttributes: DisplayableField<List<@Serializable(with = com.eros.users.serializers.BodyAttributeSerializer::class) BodyAttribute>?>,
    val bodyDescription: DisplayableField<String?>
) {
    init {
        require(coordinatesLatitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(coordinatesLongitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
        require(interests.size in 5..10) { "Interests must be between 5 and 10 items" }
        require(traits.size in 3..10) { "Traits must be between 3 and 10 items" }
        require(bio.length <= 300) { "Bio must not exceed 300 characters" }
        require(heightCm > 0) { "Height must be positive" }
        require(firstName.isNotBlank()) { "First name is required" }
        require(lastName.isNotBlank()) { "Last name is required" }
        require(email.isNotBlank()) { "Email is required" }
        require(city.isNotBlank()) { "City is required" }
        require(brainDescription.field == null || brainDescription.field.length <= 200) { "Brain description must not exceed 200 characters" }
        require(bodyDescription.field == null || bodyDescription.field.length <= 200) { "Body description must not exceed 200 characters" }
    }
}

/**
 * Request DTO for updating user profile.
 *
 * All fields are optional — only non-null fields are applied to the existing profile.
 * Displayable fields use [DisplayableField] so the client can update a value, its
 * visibility, or both in a single request. A null displayable field means "leave this
 * field unchanged".
 *
 * Note: Server-managed fields (eloScore, role, profileStatus, badges, etc.) are not
 * included here and can only be modified via admin endpoints.
 */
@Serializable
data class UpdateUserRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val heightCm: Int? = null,
    val city: String? = null,
    @Serializable(with = com.eros.users.serializers.EducationLevelSerializer::class)
    val educationLevel: EducationLevel? = null,
    val occupation: String? = null,
    val bio: String? = null,
    val interests: List<@Serializable(with = com.eros.users.serializers.UserInterestSerializer::class) UserInterest>? = null,
    val traits: List<@Serializable(with = com.eros.users.serializers.TraitSerializer::class) Trait>? = null,
    @Serializable(with = com.eros.users.serializers.LanguageSerializer::class)
    val preferredLanguage: Language? = null,
    val coordinatesLatitude: Double? = null,
    val coordinatesLongitude: Double? = null,

    // Displayable fields — null means "do not update", non-null replaces both value and display flag
    val spokenLanguages: DisplayableField<List<@Serializable(with = com.eros.users.serializers.LanguageSerializer::class) Language>>? = null,
    val religion: DisplayableField<@Serializable(with = com.eros.users.serializers.ReligionSerializer::class) Religion?>? = null,
    val politicalView: DisplayableField<@Serializable(with = com.eros.users.serializers.PoliticalViewSerializer::class) PoliticalView?>? = null,
    val alcoholConsumption: DisplayableField<@Serializable(with = com.eros.users.serializers.AlcoholConsumptionSerializer::class) AlcoholConsumption?>? = null,
    val smokingStatus: DisplayableField<@Serializable(with = com.eros.users.serializers.SmokingStatusSerializer::class) SmokingStatus?>? = null,
    val diet: DisplayableField<@Serializable(with = com.eros.users.serializers.DietSerializer::class) Diet?>? = null,
    val dateIntentions: DisplayableField<@Serializable(with = com.eros.users.serializers.DateIntentionsSerializer::class) DateIntentions>? = null,
    val relationshipType: DisplayableField<@Serializable(with = com.eros.users.serializers.RelationshipTypeSerializer::class) RelationshipType>? = null,
    val kidsPreference: DisplayableField<@Serializable(with = com.eros.users.serializers.KidsPreferenceSerializer::class) KidsPreference>? = null,
    val sexualOrientation: DisplayableField<@Serializable(with = com.eros.users.serializers.SexualOrientationSerializer::class) SexualOrientation>? = null,
    val pronouns: DisplayableField<@Serializable(with = com.eros.users.serializers.PronounsSerializer::class) Pronouns?>? = null,
    val starSign: DisplayableField<@Serializable(with = com.eros.users.serializers.StarSignSerializer::class) StarSign?>? = null,
    val ethnicity: DisplayableField<List<@Serializable(with = com.eros.users.serializers.EthnicitySerializer::class) Ethnicity>>? = null,
    val brainAttributes: DisplayableField<List<@Serializable(with = com.eros.users.serializers.BrainAttributeSerializer::class) BrainAttribute>?>? = null,
    val brainDescription: DisplayableField<String?>? = null,
    val bodyAttributes: DisplayableField<List<@Serializable(with = com.eros.users.serializers.BodyAttributeSerializer::class) BodyAttribute>?>? = null,
    val bodyDescription: DisplayableField<String?>? = null,

    // Profile Visibility
    val setVisible : Boolean? = null
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
        if (brainDescription?.field != null) {
            require(brainDescription.field.length <= 100) { "Brain description must not exceed 100 characters" }
        }
        if (bodyDescription?.field != null) {
            require(bodyDescription.field.length <= 100) { "Body description must not exceed 100 characters" }
        }
        if (ethnicity != null) {
            require(ethnicity.field.isNotEmpty()) { "Ethnicity must not be empty" }
        }
        if (coordinatesLatitude != null) {
            require(coordinatesLatitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        }
        if (coordinatesLongitude != null) {
            require(coordinatesLongitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
        }
    }
}

/**
 * Request DTO for admin-level user updates.
 *
 * This DTO includes server-managed fields that can only be modified by users with
 * ADMIN or EMPLOYEE roles. It must be used with role-based authorization checks.
 */
@Serializable
data class AdminUpdateUserRequest(
    // Server-managed fields that require admin privileges
    val profileStatus: ProfileStatus? = null,
    val eloScore: Int? = null,
    val goodExperienceBadge: Boolean? = null,
    val trustedBadge: Boolean? = null,
    val verifiedPhotoBadge: Boolean? = null,
    val profileCompleteness: Int? = null,
    val role: Role? = null,
    val photoValidationStatus: ValidationStatus? = null
) {
    init {
        if (eloScore != null) {
            require(eloScore >= 0) { "ELO score must be non-negative" }
        }
        if (profileCompleteness != null) {
            require(profileCompleteness in 0..100) { "Profile completeness must be between 0 and 100" }
        }
    }
}


/**
 * Request DTO for change the visibility/status of a User
 */
@Serializable
data class ProfileStatusUpdateRequest(
    val isVisible: Boolean
)

@Serializable
data class ProfileStatusDTO(
    val isVisible: Boolean
)

fun User.toVisibilityDTO() = ProfileStatusDTO(
    isVisible = this.profileStatus == ProfileStatus.ACTIVE
)

/**
 * This is an encapsulating class for DTOs to reflect if a field should be viewable
 * on users profile
 */
@Serializable
data class DisplayableField<T>(val field: T, val display: Boolean)

/**
 * Helper function to convert UserInterest enum to String for database storage
 */
fun List<UserInterest>.toStorageFormat(): List<String> =
    this.map { (it as Enum<*>).name }

/**
 * Finds a UserInterest enum by either its enum name or display name.
 * Supports both new enum.name format and legacy display name format.
 *
 * @param value The string value to look up (can be enum name or display name)
 * @return The matching UserInterest enum, or null if not found
 */
internal fun findUserInterest(value: String): UserInterest? {
    // Try matching by enum name first (new format - preferred)
    return Activity.entries.find { it.name == value }
        ?: Interest.entries.find { it.name == value }
        ?: Entertainment.entries.find { it.name == value }
        ?: Creative.entries.find { it.name == value }
        ?: MusicGenre.entries.find { it.name == value }
        ?: FoodAndDrink.entries.find { it.name == value }
        ?: Sport.entries.find { it.name == value }
        // Fallback to display name for backward compatibility (legacy format)
        ?: Activity.entries.find { it.displayName == value }
        ?: Interest.entries.find { it.displayName == value }
        ?: Entertainment.entries.find { it.displayName == value }
        ?: Creative.entries.find { it.displayName == value }
        ?: MusicGenre.entries.find { it.displayName == value }
        ?: FoodAndDrink.entries.find { it.displayName == value }
        ?: Sport.entries.find { it.displayName == value }
}

/**
 * Helper function to convert String from database to UserInterest enum
 * Supports both new enum.name format and legacy display name format
 * @throws IllegalArgumentException if an enumName cannot be mapped
 */
fun List<String>.toUserInterests(): List<UserInterest> =
    this.map { enumName ->
        findUserInterest(enumName)
            ?: throw IllegalArgumentException(
                "Unknown UserInterest value in database: '$enumName'. " +
                "This indicates corrupt or legacy data that needs migration."
            )
    }