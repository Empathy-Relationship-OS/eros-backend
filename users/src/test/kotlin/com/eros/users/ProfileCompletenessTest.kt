package com.eros.users

import com.eros.users.models.*
import com.eros.users.models.MediaType
import org.junit.jupiter.api.*
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProfileCompletenessTest {

    @Test
    fun `completeness calculation`() {
        val user = createTestUser()
        val userMedia = createMediaList(3)
        val userMediaCollection = UserMediaCollection(user.userId, userMedia, userMedia.size)
        val userQA = createQAList(2)
        val userQACollection = UserQACollection(user.userId, userQA, userQA.size)
        val completeness = ProfileCompleteness().calculateCompleteness(user, userMediaCollection, userQACollection)
        assertEquals(completeness, 65)
    }

    // Helper function to create test users with defaults
    private fun createTestUser(
        userId: String = "test-user-id",
        firstName: String = "John",
        lastName: String = "Doe",
        email: String = "john.doe@example.com",
        heightCm: Int = 180,
        dateOfBirth: LocalDate = LocalDate.of(1990, 1, 1),
        city: String = "London",
        educationLevel: EducationLevel = EducationLevel.UNIVERSITY,
        gender: Gender = Gender.MALE,
        preferredLanguage: Language = Language.ENGLISH,
        coordinatesLatitude: Double = 51.5074,
        coordinatesLongitude: Double = -0.1278,
        occupation: String = "toilet seat inspector",
        bio: String = "",
        interests: List<String> = List(5) { "Interest$it" },
        traits: List<Trait> = List(3) { Trait.entries[it] },
        spokenLanguages: DisplayableField<List<Language>> = DisplayableField(listOf(Language.ENGLISH), false),
        religion: DisplayableField<Religion?> = DisplayableField(null, false),
        politicalView: DisplayableField<PoliticalView?> = DisplayableField(null, false),
        alcoholConsumption: DisplayableField<AlcoholConsumption?> = DisplayableField(null, false),
        smokingStatus: DisplayableField<SmokingStatus?> = DisplayableField(null, false),
        diet: DisplayableField<Diet?> = DisplayableField(null, false),
        dateIntentions: DisplayableField<DateIntentions> = DisplayableField(DateIntentions.SERIOUS_DATING, false),
        relationshipType: DisplayableField<RelationshipType> = DisplayableField(RelationshipType.MONOGAMOUS, false),
        kidsPreference: DisplayableField<KidsPreference> = DisplayableField(KidsPreference.OPEN_TO_KIDS, false),
        sexualOrientation: DisplayableField<SexualOrientation> = DisplayableField(SexualOrientation.STRAIGHT, false),
        pronouns: DisplayableField<Pronouns?> = DisplayableField(null, false),
        starSign: DisplayableField<StarSign?> = DisplayableField(null, false),
        ethnicity: DisplayableField<List<Ethnicity>> = DisplayableField(listOf(Ethnicity.BLACK_AFRICAN_DESCENT), false),
        brainAttributes: DisplayableField<List<BrainAttribute>?> = DisplayableField(null, false),
        brainDescription: DisplayableField<String?> = DisplayableField(null, false),
        bodyAttributes: DisplayableField<List<BodyAttribute>?> = DisplayableField(null, false),
        bodyDescription: DisplayableField<String?> = DisplayableField(null, false),
        profileStatus: ProfileStatus = ProfileStatus.ACTIVE,
        eloScore: Int = 1000,
        badges: Set<Badge>? = null,
        completeness: Int = 58,
        role: Role = Role.USER,
        photoValidationStatus: ValidationStatus = ValidationStatus.UNVALIDATED,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now(),
    ): User {
        return User(
            userId = userId,
            firstName = firstName,
            lastName = lastName,
            email = email,
            heightCm = heightCm,
            dateOfBirth = dateOfBirth,
            city = city,
            educationLevel = educationLevel,
            gender = gender,
            preferredLanguage = preferredLanguage,
            coordinatesLatitude = coordinatesLatitude,
            coordinatesLongitude = coordinatesLongitude,
            occupation = occupation,
            bio = bio,
            interests = interests,
            traits = traits,
            spokenLanguages = spokenLanguages,
            religion = religion,
            politicalView = politicalView,
            alcoholConsumption = alcoholConsumption,
            smokingStatus = smokingStatus,
            diet = diet,
            dateIntentions = dateIntentions,
            relationshipType = relationshipType,
            kidsPreference = kidsPreference,
            sexualOrientation = sexualOrientation,
            pronouns = pronouns,
            starSign = starSign,
            ethnicity = ethnicity,
            brainAttributes = brainAttributes,
            brainDescription = brainDescription,
            bodyAttributes = bodyAttributes,
            bodyDescription = bodyDescription,
            profileStatus = profileStatus,
            eloScore = eloScore,
            badges = badges,
            profileCompleteness = completeness,
            role = role,
            photoValidationStatus = photoValidationStatus,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    // Helper functions
    private fun createMediaItem(
        id: Long = 1L,
        mediaUrl: String = "https://example.com/photo.jpg",
        mediaType: MediaType = MediaType.PHOTO,
        displayOrder: Int = 1,
        isPrimary: Boolean = false
    ): UserMediaItemDTO {
        return UserMediaItemDTO(
            id = id,
            mediaUrl = mediaUrl,
            thumbnailUrl = null,
            mediaType = mediaType,
            displayOrder = displayOrder,
            isPrimary = isPrimary
        )
    }

    private fun createMediaList(count: Int): List<UserMediaItemDTO> {
        return (1..count).map { index ->
            createMediaItem(id = index.toLong(), displayOrder = index)
        }
    }

    // Helper functions
    private fun createQAItem(
        id: Long = 1L,
        userId: String = "user-123",
        question: PredefinedQuestion = PredefinedQuestion.LAST_MEAL_EVER,
        answer: String = "Pizza and ice cream",
        displayOrder: Int = 1
    ): UserQAItem {
        return UserQAItem(
            id = id,
            userId = userId,
            question = question,
            answer = answer,
            displayOrder = displayOrder,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createQAList(count: Int): List<UserQAItem> {
        val questions = listOf(
            PredefinedQuestion.LAST_MEAL_EVER,
            PredefinedQuestion.FAVOURITE_BOOK_MOVIE_TV,
            PredefinedQuestion.DREAM_JOB_NO_MONEY,
            PredefinedQuestion.LIFE_GOAL
        )

        return (1..count).map { index ->
            createQAItem(
                id = index.toLong(),
                question = questions[(index - 1) % questions.size],
                answer = "Answer $index",
                displayOrder = index.coerceAtMost(3)
            )
        }
    }

}