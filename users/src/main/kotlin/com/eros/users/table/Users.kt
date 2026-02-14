package com.eros.users.table


import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

/**
 * Main user profile table
 * 
 * Stores core user information including:
 * - Basic profile (name, email, physical attributes)
 * - Personality (interests, traits via TEXT[] arrays)
 * - Beliefs & values
 * - Habits
 * - Relationship goals
 * - Identity information
 */
object Users : Table("users") {
    // Primary key - Firebase user ID
    val userId = varchar("user_id", 128)
    
    // Required fields
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val email = varchar("email", 255).uniqueIndex()
    val heightCm = integer("height_cm") // stored in centimeters
    val dateOfBirth = date("date_of_birth")
    val city = varchar("city", 100)
    val educationLevel = varchar("education_level", 50) // EducationLevel enum
    val gender = varchar("gender", 50) // Gender enum
    
    // Optional profile fields
    val occupation = varchar("occupation", 100).nullable()
    val bio = varchar("bio", 300).nullable()
    
    // Hobbies & Interests (stored as TEXT[] - PostgreSQL array)
    // Combined: Activity, Interest, Entertainment, Creative, MusicGenre, FoodAndDrink, Sport
    // Min 5, Max 10
    val interests = array<String>("interests")
    
    // Personality Traits (stored as TEXT[] - PostgreSQL array)
    // Includes both personality traits and lifestyle traits
    // Min 3, Max 10
    val traits = array<String>("traits")
    
    // Languages
    val preferredLanguage = varchar("preferred_language", 50) // Language enum
    val spokenLanguages = array<String>("spoken_languages") // Language enum array
    val spokenLanguagesDisplay = bool("spoken_languages_display").default(false)

    // Beliefs & Values
    val religion = varchar("religion", 50).nullable() // Religion enum
    val religionDisplay = bool("religion_display").default(false)
    val politicalView = varchar("political_view", 50).nullable() // PoliticalView enum
    val politicalViewDisplay = bool("political_view_display").default(false)

    // Habits
    val alcoholConsumption = varchar("alcohol_consumption", 50).nullable() // AlcoholConsumption enum
    val alcoholConsumptionDisplay = bool("alcohol_consumption_display").default(false)
    val smokingStatus = varchar("smoking_status", 50).nullable() // SmokingStatus enum
    val smokingStatusDisplay = bool("smoking_status_display").default(false)
    val diet = varchar("diet", 50).nullable() // Diet enum
    val dietDisplay = bool("diet_display").default(false)

    // Relationship goals
    val dateIntentions = varchar("date_intentions", 50).nullable() // DateIntentions enum
    val dateIntentionsDisplay = bool("date_intentions_display").default(false)
    val relationshipType = varchar("relationship_type", 50).nullable() // RelationshipType enum
    val relationshipTypeDisplay = bool("relationship_type_display").default(false)
    val kidsPreference = varchar("kids_preference", 50).nullable() // KidsPreference enum
    val kidsPreferenceDisplay = bool("kids_preference_display").default(false)

    // Identity
    val sexualOrientation = varchar("sexual_orientation", 50).nullable() // SexualOrientation enum
    val sexualOrientationDisplay = bool("sexual_orientation_display").default(false)
    val pronouns = varchar("pronouns", 50).nullable() // Pronouns enum
    val pronounsDisplay = bool("pronouns_display").default(false)
    val starSign = varchar("star_sign", 50).nullable() // StarSign enum
    val starSignDisplay = bool("star_sign_display").default(false)
    val ethnicity = array<String>("ethnicity") // Ethnicity enum array (required)
    val ethnicityDisplay = bool("ethnicity_display").default(false)

    // Brain & Body attributes (stored as TEXT[] - PostgreSQL array)
    // Optional fields for neurodiversity and physical health
    val brainAttributes = array<String>("brain_attributes").nullable() // BrainAttribute enum array
    val brainAttributesDisplay = bool("brain_attributes_display").default(false)
    val brainDescription = varchar("brain_description", 100).nullable() // Free text, max 100 chars
    val brainDescriptionDisplay = bool("brain_description_display").default(false)
    val bodyAttributes = array<String>("body_attributes").nullable() // BodyAttribute enum array
    val bodyAttributesDisplay = bool("body_attributes_display").default(false)
    val bodyDescription = varchar("body_description", 100).nullable() // Free text, max 100 chars
    val bodyDescriptionDisplay = bool("body_description_display").default(false)

    // Timestamps
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
    val deletedAt = timestamp("deleted_at").nullable() // Soft delete
    
    override val primaryKey = PrimaryKey(userId)
}
