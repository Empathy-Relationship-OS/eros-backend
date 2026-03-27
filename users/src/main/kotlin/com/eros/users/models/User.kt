package com.eros.users.models

import com.eros.common.serializers.InstantSerializer
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
    interests = this.interests ,
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
    val educationLevel: EducationLevel,
    val gender: Gender,
    val preferredLanguage: Language,

    val coordinatesLatitude: Double,
    val coordinatesLongitude: Double,

    // Optional fields
    val occupation: String? = null,
    val bio: String = "",
    val interests: List<String>,
    val traits: List<Trait>,

    // Displayable fields — client controls both value and visibility
    val spokenLanguages: DisplayableField<List<Language>>,
    val religion: DisplayableField<Religion?>,
    val politicalView: DisplayableField<PoliticalView?>,
    val alcoholConsumption: DisplayableField<AlcoholConsumption?>,
    val smokingStatus: DisplayableField<SmokingStatus?>,
    val diet: DisplayableField<Diet?>,
    val dateIntentions: DisplayableField<DateIntentions>,
    val relationshipType: DisplayableField<RelationshipType>,
    val kidsPreference: DisplayableField<KidsPreference>,
    val sexualOrientation: DisplayableField<SexualOrientation>,
    val pronouns: DisplayableField<Pronouns?>,
    val starSign: DisplayableField<StarSign?>,
    val ethnicity: DisplayableField<List<Ethnicity>>,
    val brainAttributes: DisplayableField<List<BrainAttribute>?>,
    val brainDescription: DisplayableField<String?>,
    val bodyAttributes: DisplayableField<List<BodyAttribute>?>,
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
    val educationLevel: EducationLevel,
    val gender: Gender,
    val preferredLanguage: Language,

    val coordinatesLatitude: Double,
    val coordinatesLongitude: Double,

    // Optional fields
    val occupation: String? = null,
    val bio: String = "",
    val interests: List<String>,
    val traits: List<Trait>,

    // Displayable fields — client controls both value and visibility
    val spokenLanguages: DisplayableField<List<Language>>,
    val religion: DisplayableField<Religion?>,
    val politicalView: DisplayableField<PoliticalView?>,
    val alcoholConsumption: DisplayableField<AlcoholConsumption?>,
    val smokingStatus: DisplayableField<SmokingStatus?>,
    val diet: DisplayableField<Diet?>,
    val dateIntentions: DisplayableField<DateIntentions>,
    val relationshipType: DisplayableField<RelationshipType>,
    val kidsPreference: DisplayableField<KidsPreference>,
    val sexualOrientation: DisplayableField<SexualOrientation>,
    val pronouns: DisplayableField<Pronouns?>,
    val starSign: DisplayableField<StarSign?>,
    val ethnicity: DisplayableField<List<Ethnicity>>,
    val brainAttributes: DisplayableField<List<BrainAttribute>?>,
    val brainDescription: DisplayableField<String?>,
    val bodyAttributes: DisplayableField<List<BodyAttribute>?>,
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
    val educationLevel: EducationLevel? = null,
    val occupation: String? = null,
    val bio: String? = null,
    val interests: List<String>? = null,
    val traits: List<Trait>? = null,
    val preferredLanguage: Language? = null,
    val coordinatesLatitude: Double? = null,
    val coordinatesLongitude: Double? = null,

    // Displayable fields — null means "do not update", non-null replaces both value and display flag
    val spokenLanguages: DisplayableField<List<Language>>? = null,
    val religion: DisplayableField<Religion?>? = null,
    val politicalView: DisplayableField<PoliticalView?>? = null,
    val alcoholConsumption: DisplayableField<AlcoholConsumption?>? = null,
    val smokingStatus: DisplayableField<SmokingStatus?>? = null,
    val diet: DisplayableField<Diet?>? = null,
    val dateIntentions: DisplayableField<DateIntentions>? = null,
    val relationshipType: DisplayableField<RelationshipType>? = null,
    val kidsPreference: DisplayableField<KidsPreference>? = null,
    val sexualOrientation: DisplayableField<SexualOrientation>? = null,
    val pronouns: DisplayableField<Pronouns?>? = null,
    val starSign: DisplayableField<StarSign?>? = null,
    val ethnicity: DisplayableField<List<Ethnicity>>? = null,
    val brainAttributes: DisplayableField<List<BrainAttribute>?>? = null,
    val brainDescription: DisplayableField<String?>? = null,
    val bodyAttributes: DisplayableField<List<BodyAttribute>?>? = null,
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
 * The following function is used in test classes to get a centralized User object, available to alter as required.
 * Avoids each test class having their own version that will need to be updated in the event of changes to User.
 */
val testClock: Clock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneId.of("UTC"))

fun createTestUser(
    userId: String = "test-user-id",
    firstName: String = "John",
    lastName: String = "Doe",
    email: String = "john.doe@example.com",
    heightCm: Int = 180,
    dateOfBirth: LocalDate = LocalDate.of(1990, 1, 1),
    city: String = "London",
    educationLevel: EducationLevel = EducationLevel.UNIVERSITY,
    gender: Gender = Gender.MALE,
    occupation: String = "Engineer",
    bio: String = "Test bio",
    interests: List<String> = List(5) { "Interest$it" },
    traits: List<Trait> = List(3) { Trait.entries[it] },
    preferredLanguage: Language = Language.ENGLISH,
    spokenLanguages: DisplayableField<List<Language>> = DisplayableField(listOf(Language.ENGLISH), true),
    religion: DisplayableField<Religion?> = DisplayableField(Religion.CHRISTIANITY, true),
    politicalView: DisplayableField<PoliticalView?> = DisplayableField(PoliticalView.MODERATE, true),
    alcoholConsumption: DisplayableField<AlcoholConsumption?> = DisplayableField(AlcoholConsumption.SOMETIMES, true),
    smokingStatus: DisplayableField<SmokingStatus?> = DisplayableField(SmokingStatus.NEVER, true),
    diet: DisplayableField<Diet?> = DisplayableField(Diet.HALAL, true),
    dateIntentions: DisplayableField<DateIntentions> = DisplayableField(DateIntentions.SERIOUS_DATING, true),
    relationshipType: DisplayableField<RelationshipType> = DisplayableField(RelationshipType.MONOGAMOUS, true),
    kidsPreference: DisplayableField<KidsPreference> = DisplayableField(KidsPreference.OPEN_TO_KIDS, true),
    sexualOrientation: DisplayableField<SexualOrientation> = DisplayableField(SexualOrientation.STRAIGHT, true),
    pronouns: DisplayableField<Pronouns?> = DisplayableField(Pronouns.HE_HIM, true),
    starSign: DisplayableField<StarSign?> = DisplayableField(StarSign.GEMINI, true),
    ethnicity: DisplayableField<List<Ethnicity>> = DisplayableField(listOf(Ethnicity.BLACK_AFRICAN_DESCENT), true),
    brainAttributes: DisplayableField<List<BrainAttribute>?> = DisplayableField(
        listOf(BrainAttribute.LEARNING_DISABILITY, BrainAttribute.NEURODIVERGENT),
        true
    ),
    brainDescription: DisplayableField<String?> = DisplayableField("Maybe this is string?", true),
    bodyAttributes: DisplayableField<List<BodyAttribute>?> = DisplayableField(listOf(BodyAttribute.WHEELCHAIR), true),
    bodyDescription: DisplayableField<String?> = DisplayableField("Is this a string?", true),
    createdAt: Instant = Instant.now(testClock),
    updatedAt: Instant = Instant.now(testClock),
    deletedAt: Instant? = null,
    profileStatus: ProfileStatus = ProfileStatus.ACTIVE,
    eloScore: Int = 1000,
    badges: Set<Badge> = setOf(Badge.VERIFIED, Badge.TRUSTED, Badge.GOOD_XP),
    profileCompleteness: Int = 75,
    coordinatesLongitude: Double = 45.3246,
    coordinatesLatitude: Double = -90.0,
    role: Role = Role.USER,
    photoValidationStatus: ValidationStatus = ValidationStatus.VALIDATED
) = User(
    userId                  = userId,
    firstName               = firstName,
    lastName                = lastName,
    email                   = email,
    heightCm                = heightCm,
    dateOfBirth             = dateOfBirth,
    city                    = city,
    educationLevel          = educationLevel,
    gender                  = gender,
    occupation              = occupation,
    bio                     = bio,
    interests               = interests,
    traits                  = traits,
    preferredLanguage       = preferredLanguage,
    spokenLanguages         = spokenLanguages,
    religion                = religion,
    politicalView           = politicalView,
    alcoholConsumption      = alcoholConsumption,
    smokingStatus           = smokingStatus,
    diet                    = diet,
    dateIntentions          = dateIntentions,
    relationshipType        = relationshipType,
    kidsPreference          = kidsPreference,
    sexualOrientation       = sexualOrientation,
    pronouns                = pronouns,
    starSign                = starSign,
    ethnicity               = ethnicity,
    brainAttributes         = brainAttributes,
    brainDescription        = brainDescription,
    bodyAttributes          = bodyAttributes,
    bodyDescription         = bodyDescription,
    createdAt               = createdAt,
    updatedAt               = updatedAt,
    deletedAt               = deletedAt,
    profileStatus           = profileStatus,
    eloScore                = eloScore,
    badges                  = badges,
    profileCompleteness     = profileCompleteness,
    coordinatesLongitude    = coordinatesLongitude,
    coordinatesLatitude     = coordinatesLatitude,
    role                    = role,
    photoValidationStatus   = photoValidationStatus
)