package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.users.models.*
import com.eros.users.table.Users
import com.eros.users.table.toDTO
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import java.time.Clock
import java.time.Instant

/**
 * Implementation of UserRepository using Exposed ORM.
 *
 * This repository manages user profile data in the database.
 * All database operations are wrapped in dbQuery for proper transaction management
 * and IO dispatcher execution.
 *
 * @param clock Clock instance for time-based operations (defaults to system UTC)
 */
class UserRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : UserRepository {

    override suspend fun createUser(request: CreateUserRequest): User = dbQuery {
        val now = Instant.now(clock)

        Users.insert { row ->
            row[Users.userId] = request.userId
            row[Users.firstName] = request.firstName
            row[Users.lastName] = request.lastName
            row[Users.email] = request.email
            row[Users.heightCm] = request.heightCm
            row[Users.dateOfBirth] = request.dateOfBirth
            row[Users.city] = request.city
            row[Users.educationLevel] = request.educationLevel.name
            row[Users.gender] = request.gender.name
            row[Users.occupation] = request.occupation
            row[Users.bio] = request.bio
            row[Users.interests] = request.interests
            row[Users.traits] = request.traits.map { it.name }
            row[Users.preferredLanguage] = request.preferredLanguage.name
            row[Users.spokenLanguages] = (request.spokenLanguages ?: emptyList()).map { it.name }
            row[Users.spokenLanguagesDisplay] = false
            row[Users.religion] = request.religion?.name
            row[Users.religionDisplay] = false
            row[Users.politicalView] = request.politicalView?.name
            row[Users.politicalViewDisplay] = false
            row[Users.alcoholConsumption] = request.alcoholConsumption?.name
            row[Users.alcoholConsumptionDisplay] = false
            row[Users.smokingStatus] = request.smokingStatus?.name
            row[Users.smokingStatusDisplay] = false
            row[Users.diet] = request.diet?.name
            row[Users.dietDisplay] = false
            row[Users.dateIntentions] = request.dateIntentions?.name
            row[Users.dateIntentionsDisplay] = false
            row[Users.relationshipType] = request.relationshipType?.name
            row[Users.relationshipTypeDisplay] = false
            row[Users.kidsPreference] = request.kidsPreference?.name
            row[Users.kidsPreferenceDisplay] = false
            row[Users.sexualOrientation] = request.sexualOrientation?.name
            row[Users.sexualOrientationDisplay] = false
            row[Users.pronouns] = request.pronouns?.name
            row[Users.pronounsDisplay] = false
            row[Users.starSign] = request.starSign?.name
            row[Users.starSignDisplay] = false
            row[Users.ethnicity] = request.ethnicity.map { it.name }
            row[Users.ethnicityDisplay] = false
            row[Users.brainAttributes] = request.brainAttributes?.map { it.name }
            row[Users.brainAttributesDisplay] = false
            row[Users.brainDescription] = request.brainDescription
            row[Users.brainDescriptionDisplay] = false
            row[Users.bodyAttributes] = request.bodyAttributes?.map { it.name }
            row[Users.bodyAttributesDisplay] = false
            row[Users.bodyDescription] = request.bodyDescription
            row[Users.bodyDescriptionDisplay] = false
            row[Users.createdAt] = now
            row[Users.updatedAt] = now
        }

        Users.selectAll()
            .where { Users.userId eq request.userId }
            .single()
            .toDTO()
    }

    override suspend fun updateUser(userId: String, request: UpdateUserRequest): User? = dbQuery {
        val now = Instant.now(clock)

        val rowsUpdated = Users.update({ Users.userId eq userId }) { row ->
            request.firstName?.let { row[Users.firstName] = it }
            request.lastName?.let { row[Users.lastName] = it }
            request.email?.let { row[Users.email] = it }
            request.heightCm?.let { row[Users.heightCm] = it }
            request.city?.let { row[Users.city] = it }
            request.educationLevel?.let { row[Users.educationLevel] = it.name }
            request.occupation?.let { row[Users.occupation] = it }
            request.bio?.let { row[Users.bio] = it }
            request.interests?.let { row[Users.interests] = it }
            request.traits?.let { row[Users.traits] = it.map { trait -> trait.name } }
            request.preferredLanguage?.let { row[Users.preferredLanguage] = it.name }
            request.spokenLanguages?.let { row[Users.spokenLanguages] = it.map { lang -> lang.name } }
            request.religion?.let { row[Users.religion] = it.name }
            request.politicalView?.let { row[Users.politicalView] = it.name }
            request.alcoholConsumption?.let { row[Users.alcoholConsumption] = it.name }
            request.smokingStatus?.let { row[Users.smokingStatus] = it.name }
            request.diet?.let { row[Users.diet] = it.name }
            request.dateIntentions?.let { row[Users.dateIntentions] = it.name }
            request.relationshipType?.let { row[Users.relationshipType] = it.name }
            request.kidsPreference?.let { row[Users.kidsPreference] = it.name }
            request.sexualOrientation?.let { row[Users.sexualOrientation] = it.name }
            request.pronouns?.let { row[Users.pronouns] = it.name }
            request.starSign?.let { row[Users.starSign] = it.name }
            request.ethnicity?.let { row[Users.ethnicity] = it.map { eth -> eth.name } }
            request.brainAttributes?.let { row[Users.brainAttributes] = it.map { attr -> attr.name } }
            request.brainDescription?.let { row[Users.brainDescription] = it }
            request.bodyAttributes?.let { row[Users.bodyAttributes] = it.map { attr -> attr.name } }
            request.bodyDescription?.let { row[Users.bodyDescription] = it }
            row[Users.updatedAt] = now
        }

        if (rowsUpdated == 0) {
            null
        } else {
            Users.selectAll()
                .where { Users.userId eq userId }
                .singleOrNull()
                ?.toDTO()
        }
    }

    override suspend fun findByUserId(userId: String): User? = dbQuery {
        Users.selectAll()
            .where { (Users.userId eq userId) and Users.deletedAt.isNull() }
            .singleOrNull()
            ?.toDTO()
    }

    override suspend fun findByEmail(email: String): User? = dbQuery {
        Users.selectAll()
            .where { (Users.email.lowerCase() eq email.lowercase()) and Users.deletedAt.isNull() }
            .singleOrNull()
            ?.toDTO()
    }

    override suspend fun deleteUser(userId: String): Int = dbQuery {
        val now = Instant.now(clock)
        Users.update({ Users.userId eq userId }) {
            it[deletedAt] = now
        }
    }

    override suspend fun userExists(userId: String): Boolean = dbQuery {
        Users.selectAll()
            .where { (Users.userId eq userId) and Users.deletedAt.isNull() }
            .count() > 0
    }
}
