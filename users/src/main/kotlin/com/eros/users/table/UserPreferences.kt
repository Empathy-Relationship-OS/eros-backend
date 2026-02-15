package com.eros.users.table

import com.eros.users.models.City
import com.eros.users.models.Activity
import com.eros.users.models.Ethnicity
import com.eros.users.models.Gender
import com.eros.users.models.Language
import com.eros.users.models.UserPreference
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object UserPreferences : Table("user_preferences") {

    // Primary key
    val id = long("id").autoIncrement()

    // Foreign key to Users table
    val userId = varchar("user_id", 128).references(Users.userId)

    // Who I like section
    val ageRangeMin = integer("age_range_min")
    val ageRangeMax = integer("age_range_max")
    val heightRangeMin = integer("height_range_max") // In cm
    val heightRangeMax = integer("height_range_max") // In cm
    val ethnicities = array<String>("ethnicities")
    val genderIdentities = array<String>("gender_identities")

    // Dating Practicalities
    val dateLanguages = array<String>("date_languages")
    val dateActivities = array<String>("date_activities")
    val dateLimit = integer("date_limit")

    // Timestamps
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(Users.userId)

    init {
        check("date_limit_range") { dateLimit.between(1, 6) }
        check("age_min_valid") { ageRangeMin.greater(17) }
        check("age_max_greater") { ageRangeMax.greater(ageRangeMin) }
    }
}

fun ResultRow.toUserPreferenceDTO() = UserPreference(
    id = this[UserPreferences.id],
    userId = this[UserPreferences.userId],
    genderIdentities = this[UserPreferences.genderIdentities].map { Gender.valueOf(it) },
    ageRangeMin = this[UserPreferences.ageRangeMin],
    ageRangeMax = this[UserPreferences.ageRangeMax],
    heightRangeMin = this[UserPreferences.heightRangeMin],
    heightRangeMax = this[UserPreferences.heightRangeMax],
    ethnicity = this[UserPreferences.ethnicities].map { Ethnicity.valueOf(it) },
    dateLanguages = this[UserPreferences.dateLanguages].map { Language.valueOf(it) },
    dateActivities = this[UserPreferences.dateActivities].map { Activity.valueOf(it) },
    dateLimit = this[UserPreferences.dateLimit],
    createdAt = this[UserPreferences.createdAt],
    updatedAt = this[UserPreferences.updatedAt]
)
