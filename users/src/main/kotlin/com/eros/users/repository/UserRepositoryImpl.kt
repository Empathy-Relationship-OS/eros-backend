package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.database.repository.BaseDAOImpl
import com.eros.users.models.Badge
import com.eros.users.models.User
import com.eros.users.table.Users
import com.eros.users.table.toDTO
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Clock
import java.time.Instant

/**
 * Implementation of [UserRepository] using Exposed ORM.
 *
 * Extends [BaseDAOImpl] for standard CRUD and overrides methods where
 * domain-specific behaviour is required (soft-delete, Firebase UID as PK).
 *
 * The service layer is responsible for mapping request DTOs → [User] before
 * calling this repository.
 *
 * @param clock Clock instance for time-based operations (defaults to system UTC).
 */
class UserRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : BaseDAOImpl<String, User>(Users, Users.userId), UserRepository {

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    override fun ResultRow.toDomain(): User = toDTO()

    override fun toStatement(statement: UpdateBuilder<*>, entity: User) {
        statement.apply {
            this[Users.userId] = entity.userId
            this[Users.firstName] = entity.firstName
            this[Users.lastName] = entity.lastName
            this[Users.email] = entity.email
            this[Users.heightCm] = entity.heightCm
            this[Users.dateOfBirth] = entity.dateOfBirth
            this[Users.city] = entity.city
            this[Users.educationLevel] = entity.educationLevel.name
            this[Users.gender] = entity.gender.name
            this[Users.occupation] = entity.occupation
            this[Users.bio] = entity.bio
            this[Users.interests] = entity.interests
            this[Users.traits] = entity.traits.map { it.name }
            this[Users.preferredLanguage] = entity.preferredLanguage.name
            this[Users.spokenLanguages] = entity.spokenLanguages.field.map { it.name }
            this[Users.spokenLanguagesDisplay] = entity.spokenLanguages.display
            this[Users.religion] = entity.religion.field?.name
            this[Users.religionDisplay] = entity.religion.display
            this[Users.politicalView] = entity.politicalView.field?.name
            this[Users.politicalViewDisplay] = entity.politicalView.display
            this[Users.alcoholConsumption] = entity.alcoholConsumption.field?.name
            this[Users.alcoholConsumptionDisplay] = entity.alcoholConsumption.display
            this[Users.smokingStatus] = entity.smokingStatus.field?.name
            this[Users.smokingStatusDisplay] = entity.smokingStatus.display
            this[Users.diet] = entity.diet.field?.name
            this[Users.dietDisplay] = entity.diet.display
            this[Users.dateIntentions] = entity.dateIntentions.field?.name
            this[Users.dateIntentionsDisplay] = entity.dateIntentions.display
            this[Users.relationshipType] = entity.relationshipType.field?.name
            this[Users.relationshipTypeDisplay] = entity.relationshipType.display
            this[Users.kidsPreference] = entity.kidsPreference.field?.name
            this[Users.kidsPreferenceDisplay] = entity.kidsPreference.display
            this[Users.sexualOrientation] = entity.sexualOrientation.field?.name
            this[Users.sexualOrientationDisplay] = entity.sexualOrientation.display
            this[Users.pronouns] = entity.pronouns.field?.name
            this[Users.pronounsDisplay] = entity.pronouns.display
            this[Users.starSign] = entity.starSign.field?.name
            this[Users.starSignDisplay] = entity.starSign.display
            this[Users.ethnicity] = entity.ethnicity.field.map { it.name }
            this[Users.ethnicityDisplay] = entity.ethnicity.display
            this[Users.brainAttributes] = entity.brainAttributes.field?.map { it.name }
            this[Users.brainAttributesDisplay] = entity.brainAttributes.display
            this[Users.brainDescription] = entity.brainDescription.field
            this[Users.brainDescriptionDisplay] = entity.brainDescription.display
            this[Users.bodyAttributes] = entity.bodyAttributes.field?.map { it.name }
            this[Users.bodyAttributesDisplay] = entity.bodyAttributes.display
            this[Users.bodyDescription] = entity.bodyDescription.field
            this[Users.bodyDescriptionDisplay] = entity.bodyDescription.display
            this[Users.createdAt] = entity.createdAt
            this[Users.updatedAt] = Instant.now(clock)
            this[Users.profileStatus] = entity.profileStatus.name
            this[Users.eloScore] = entity.eloScore
            this[Users.trustedBadge] = entity.badges?.contains(Badge.TRUSTED) ?: false
            this[Users.verifiedPhotoBadge] = entity.badges?.contains(Badge.VERIFIED) ?: false
            this[Users.goodExperienceBadge] = entity.badges?.contains(Badge.GOOD_XP) ?: false
            this[Users.completeness] = entity.completeness
            this[Users.coordinates_latitude] = entity.coordinatesLatitude
            this[Users.coordinates_longitude] = entity.coordinatesLongitude
            this[Users.role] = entity.role.name
            this[Users.photoValidationStatus] = entity.photoValidationStatus.name
        }
    }

    // -------------------------------------------------------------------------
    // IBaseDAO overrides — domain-specific behaviour
    // -------------------------------------------------------------------------

    /**
     * Creates a new user and re-fetches by Firebase UID.
     *
     * Checks for existing user (respecting soft-delete) before insertion.
     *
     * @throws IllegalStateException if a user with the given ID already exists
     */
    override suspend fun create(entity: User): User = dbQuery {
        val hasExisted = Users.selectAll()
            .where { (Users.userId eq entity.userId) }
            .count() > 0

        if (hasExisted) {
            throw IllegalStateException("User with ID ${entity.userId} already exists")
        }

        Users.insert { toStatement(it, entity) }
        Users.selectAll()
            .where { Users.userId eq entity.userId }
            .single()
            .toDomain()
    }

    /** Respects soft-delete (excludes rows where deletedAt is not null). */
    override suspend fun findById(id: String): User? = dbQuery {
        Users.selectAll()
            .where { (Users.userId eq id) and Users.deletedAt.isNull() }
            .singleOrNull()
            ?.toDomain()
    }

    /** Soft-delete: sets deletedAt timestamp instead of removing the row. */
    override suspend fun delete(id: String): Int = dbQuery {
        Users.update({ Users.userId eq id }) {
            it[deletedAt] = Instant.now(clock)
        }
    }

    /** Respects soft-delete. */
    override suspend fun doesExist(id: String): Boolean = dbQuery {
        Users.selectAll()
            .where { (Users.userId eq id) and Users.deletedAt.isNull() }
            .count() > 0
    }

    // -------------------------------------------------------------------------
    // UserRepository extras
    // -------------------------------------------------------------------------

    override suspend fun findByEmail(email: String): User? = dbQuery {
        Users.selectAll()
            .where { (Users.email.lowerCase() eq email.lowercase()) and Users.deletedAt.isNull() }
            .singleOrNull()
            ?.toDomain()
    }
}
