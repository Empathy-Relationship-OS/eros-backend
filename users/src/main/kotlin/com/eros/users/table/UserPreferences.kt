package com.eros.users.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

/**
 * Database table definition for user dating preferences.
 *
 * This table stores comprehensive preference criteria that users set for finding potential matches.
 * It includes demographic filters (age, height, ethnicity, gender), dating practicalities
 * (languages, activities, date frequency limits), and references cities through the junction
 * table [UserCitiesPreference].
 *
 * The table enforces several data integrity constraints through database-level checks:
 * - Date limit must be between 1-6 or null (unlimited)
 * - Minimum age must be 18 or older
 * - Maximum age must be greater than minimum age
 *
 * @see com.eros.users.models.UserPreference The domain model representing user preferences
 * @see UserCitiesPreference Junction table linking users to their preferred cities
 */
object UserPreferences : Table("user_preferences") {

    // Primary key
    val id = long("id").autoIncrement()

    // Foreign key to Users table
    val userId = varchar("user_id", 128).references(Users.userId)

    // Who I like section
    val ageRangeMin = integer("age_range_min")
    val ageRangeMax = integer("age_range_max")
    val heightRangeMin = integer("height_range_min") // In cm
    val heightRangeMax = integer("height_range_max") // In cm
    val ethnicities = array<String>("ethnicities")
    val genderIdentities = array<String>("gender_identities")

    // Dating Practicalities
    val dateLanguages = array<String>("date_languages")
    val dateActivities = array<String>("date_activities")
    val dateLimit = integer("date_limit").nullable()

    // Timestamps
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(Users.userId)

    init {
        check("date_limit_range") { dateLimit.isNull() or dateLimit.between(1, 6) }
        check("age_min_valid") { ageRangeMin.greater(17) }
        check("age_max_greater") { ageRangeMax.greater(ageRangeMin) }
    }
}