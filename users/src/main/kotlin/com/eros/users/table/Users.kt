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
    
    // Beliefs & Values
    val religion = varchar("religion", 50).nullable() // Religion enum
    val politicalView = varchar("political_view", 50).nullable() // PoliticalView enum
    
    // Habits
    val alcoholConsumption = varchar("alcohol_consumption", 50).nullable() // AlcoholConsumption enum
    val smokingStatus = varchar("smoking_status", 50).nullable() // SmokingStatus enum
    val diet = varchar("diet", 50).nullable() // Diet enum
    
    // Relationship goals
    val dateIntentions = varchar("date_intentions", 50).nullable() // DateIntentions enum
    val relationshipType = varchar("relationship_type", 50).nullable() // RelationshipType enum
    val kidsPreference = varchar("kids_preference", 50).nullable() // KidsPreference enum
    
    // Identity
    val sexualOrientation = varchar("sexual_orientation", 50).nullable() // SexualOrientation enum
    val pronouns = varchar("pronouns", 50).nullable() // Pronouns enum
    val starSign = varchar("star_sign", 50).nullable() // StarSign enum

    // Brain & Body attributes (stored as TEXT[] - PostgreSQL array)
    // Optional fields for neurodiversity and physical health
    val brainAttributes = array<String>("brain_attributes").nullable() // BrainAttribute enum array
    val brainDescription = varchar("brain_description", 100).nullable() // Free text, max 100 chars
    val bodyAttributes = array<String>("body_attributes").nullable() // BodyAttribute enum array
    val bodyDescription = varchar("body_description", 100).nullable() // Free text, max 100 chars

    // Timestamps
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
    val deletedAt = timestamp("deleted_at").nullable() // Soft delete
    
    override val primaryKey = PrimaryKey(userId)
}
