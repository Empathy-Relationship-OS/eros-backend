package com.eros.users.models

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserTest {

    @Nested
    inner class `getAge` {

        @Test
        fun `should calculate correct age when birthday has passed this year`() {
            // Born on Jan 1, 2000 - birthday has passed
            val user = createTestUser(dateOfBirth = LocalDate.of(2000, 1, 1))
            val today = LocalDate.now()
            val expectedAge = today.year - 2000

            assertEquals(expectedAge, user.getAge())
        }

        @Test
        fun `should calculate correct age when birthday has not passed this year`() {
            // Born on Dec 31 - birthday likely hasn't passed yet (unless running on Dec 31)
            val user = createTestUser(dateOfBirth = LocalDate.of(2000, 12, 31))
            val today = LocalDate.now()
            val expectedAge = if (today.monthValue == 12 && today.dayOfMonth == 31) {
                today.year - 2000
            } else {
                today.year - 2000 - 1
            }

            assertEquals(expectedAge, user.getAge())
        }

        @Test
        fun `should calculate correct age on exact birthday`() {
            val today = LocalDate.now()
            val user = createTestUser(dateOfBirth = today.minusYears(25))

            assertEquals(25, user.getAge())
        }

        @Test
        fun `should handle leap year birthdays`() {
            // Born on Feb 29, 2000 (leap year)
            val user = createTestUser(dateOfBirth = LocalDate.of(2000, 2, 29))
            val today = LocalDate.now()

            // Just verify it doesn't crash and produces a reasonable age
            val age = user.getAge()
            assertTrue(age > 0)
            assertTrue(age < 150) // Sanity check
        }
    }

    @Nested
    inner class `getFullName` {

        @Test
        fun `should concatenate first and last name`() {
            val user = createTestUser(firstName = "John", lastName = "Doe")

            assertEquals("John Doe", user.getFullName())
        }
    }

    @Nested
    inner class `isDeleted` {

        @Test
        fun `should return false when deletedAt is null`() {
            val user = createTestUser(deletedAt = null)

            assertFalse(user.isDeleted())
        }

        @Test
        fun `should return true when deletedAt is set`() {
            val user = createTestUser(deletedAt = Instant.now())

            assertTrue(user.isDeleted())
        }
    }

    @Nested
    inner class `hasValidInterestsCount` {

        @Test
        fun `should return true when interests count is 5`() {
            val user = createTestUser(interests = listOf("A", "B", "C", "D", "E"))

            assertTrue(user.hasValidInterestsCount())
        }

        @Test
        fun `should return true when interests count is 10`() {
            val user = createTestUser(interests = List(10) { "Interest$it" })

            assertTrue(user.hasValidInterestsCount())
        }

        @Test
        fun `should return true when interests count is between 5 and 10`() {
            val user = createTestUser(interests = List(7) { "Interest$it" })

            assertTrue(user.hasValidInterestsCount())
        }

        @Test
        fun `should return false when interests count is less than 5`() {
            val user = createTestUser(interests = listOf("A", "B", "C", "D"))

            assertFalse(user.hasValidInterestsCount())
        }

        @Test
        fun `should return false when interests count is more than 10`() {
            val user = createTestUser(interests = List(11) { "Interest$it" })

            assertFalse(user.hasValidInterestsCount())
        }
    }

    @Nested
    inner class `hasValidTraitsCount` {

        @Test
        fun `should return true when traits count is 3`() {
            val user = createTestUser(traits = listOf(Trait.HONEST, Trait.KIND, Trait.CARING))

            assertTrue(user.hasValidTraitsCount())
        }

        @Test
        fun `should return true when traits count is 10`() {
            val user = createTestUser(traits = Trait.entries.take(10))

            assertTrue(user.hasValidTraitsCount())
        }

        @Test
        fun `should return true when traits count is between 3 and 10`() {
            val user = createTestUser(traits = Trait.entries.take(5))

            assertTrue(user.hasValidTraitsCount())
        }

        @Test
        fun `should return false when traits count is less than 3`() {
            val user = createTestUser(traits = listOf(Trait.HONEST, Trait.KIND))

            assertFalse(user.hasValidTraitsCount())
        }

        @Test
        fun `should return false when traits count is more than 10`() {
            val user = createTestUser(traits = Trait.entries.take(11))

            assertFalse(user.hasValidTraitsCount())
        }
    }

    @Nested
    inner class `hasValidBio` {

        @Test
        fun `should return true when bio is empty`() {
            val user = createTestUser(bio = "")

            assertTrue(user.hasValidBio())
        }

        @Test
        fun `should return true when bio is exactly 300 characters`() {
            val user = createTestUser(bio = "a".repeat(300))

            assertTrue(user.hasValidBio())
        }

        @Test
        fun `should return true when bio is less than 300 characters`() {
            val user = createTestUser(bio = "Short bio")

            assertTrue(user.hasValidBio())
        }

        @Test
        fun `should return false when bio exceeds 300 characters`() {
            val user = createTestUser(bio = "a".repeat(301))

            assertFalse(user.hasValidBio())
        }
    }

    @Nested
    inner class `hasMinimumRequiredFields` {

        @Test
        fun `should return true when all required fields are valid`() {
            val user = createTestUser(
                firstName = "John",
                lastName = "Doe",
                email = "john@example.com",
                heightCm = 180,
                city = "London",
                interests = List(5) { "Interest$it" },
                traits = List(3) { Trait.entries[it] }
            )

            assertTrue(user.hasMinimumRequiredFields())
        }

        @Test
        fun `should return false when firstName is blank`() {
            val user = createTestUser(firstName = "  ")

            assertFalse(user.hasMinimumRequiredFields())
        }

        @Test
        fun `should return false when lastName is blank`() {
            val user = createTestUser(lastName = "")

            assertFalse(user.hasMinimumRequiredFields())
        }

        @Test
        fun `should return false when email is blank`() {
            val user = createTestUser(email = "   ")

            assertFalse(user.hasMinimumRequiredFields())
        }

        @Test
        fun `should return false when heightCm is zero`() {
            val user = createTestUser(heightCm = 0)

            assertFalse(user.hasMinimumRequiredFields())
        }

        @Test
        fun `should return false when heightCm is negative`() {
            val user = createTestUser(heightCm = -1)

            assertFalse(user.hasMinimumRequiredFields())
        }

        @Test
        fun `should return false when city is blank`() {
            val user = createTestUser(city = "")

            assertFalse(user.hasMinimumRequiredFields())
        }

        @Test
        fun `should return false when interests count is invalid`() {
            val user = createTestUser(interests = listOf("A", "B", "C"))

            assertFalse(user.hasMinimumRequiredFields())
        }

        @Test
        fun `should return false when traits count is invalid`() {
            val user = createTestUser(traits = listOf(Trait.HONEST))

            assertFalse(user.hasMinimumRequiredFields())
        }
    }

    @Nested
    inner class `CreateUserRequest validation` {

        @Test
        fun `should throw exception when interests count is less than 5`() {
            val exception = assertThrows<IllegalArgumentException> {
                CreateUserRequest(
                    userId = "test-id",
                    firstName = "John",
                    lastName = "Doe",
                    email = "test@example.com",
                    heightCm = 180,
                    dateOfBirth = LocalDate.of(1990, 1, 1),
                    city = "London",
                    educationLevel = EducationLevel.UNIVERSITY,
                    gender = Gender.MALE,
                    preferredLanguage = Language.ENGLISH,
                    interests = listOf("A", "B", "C", "D"),
                    traits = List(3) { Trait.entries[it] },
                    ethnicity = listOf(Ethnicity.BLACK_AFRICAN_DESCENT)
                )
            }
            assertEquals("Interests must be between 5 and 10 items", exception.message)
        }

        @Test
        fun `should throw exception when interests count is more than 10`() {
            val exception = assertThrows<IllegalArgumentException> {
                CreateUserRequest(
                    userId = "test-id",
                    firstName = "John",
                    lastName = "Doe",
                    email = "test@example.com",
                    heightCm = 180,
                    dateOfBirth = LocalDate.of(1990, 1, 1),
                    city = "London",
                    educationLevel = EducationLevel.UNIVERSITY,
                    gender = Gender.MALE,
                    preferredLanguage = Language.ENGLISH,
                    interests = List(11) { "Interest$it" },
                    traits = List(3) { Trait.entries[it] },
                    ethnicity = listOf(Ethnicity.BLACK_AFRICAN_DESCENT)
                )
            }
            assertEquals("Interests must be between 5 and 10 items", exception.message)
        }

        @Test
        fun `should throw exception when traits count is less than 3`() {
            val exception = assertThrows<IllegalArgumentException> {
                CreateUserRequest(
                    userId = "test-id",
                    firstName = "John",
                    lastName = "Doe",
                    email = "test@example.com",
                    heightCm = 180,
                    dateOfBirth = LocalDate.of(1990, 1, 1),
                    city = "London",
                    educationLevel = EducationLevel.UNIVERSITY,
                    gender = Gender.MALE,
                    preferredLanguage = Language.ENGLISH,
                    interests = List(5) { "Interest$it" },
                    traits = listOf(Trait.HONEST, Trait.KIND),
                    ethnicity = listOf(Ethnicity.BLACK_AFRICAN_DESCENT)
                )
            }
            assertEquals("Traits must be between 3 and 10 items", exception.message)
        }

        @Test
        fun `should throw exception when bio exceeds 300 characters`() {
            val exception = assertThrows<IllegalArgumentException> {
                CreateUserRequest(
                    userId = "test-id",
                    firstName = "John",
                    lastName = "Doe",
                    email = "test@example.com",
                    heightCm = 180,
                    dateOfBirth = LocalDate.of(1990, 1, 1),
                    city = "London",
                    educationLevel = EducationLevel.UNIVERSITY,
                    gender = Gender.MALE,
                    preferredLanguage = Language.ENGLISH,
                    bio = "a".repeat(301),
                    interests = List(5) { "Interest$it" },
                    traits = List(3) { Trait.entries[it] },
                    ethnicity = listOf(Ethnicity.BLACK_AFRICAN_DESCENT)
                )
            }
            assertEquals("Bio must not exceed 300 characters", exception.message)
        }

        @Test
        fun `should throw exception when heightCm is zero or negative`() {
            val exception = assertThrows<IllegalArgumentException> {
                CreateUserRequest(
                    userId = "test-id",
                    firstName = "John",
                    lastName = "Doe",
                    email = "test@example.com",
                    heightCm = 0,
                    dateOfBirth = LocalDate.of(1990, 1, 1),
                    city = "London",
                    educationLevel = EducationLevel.UNIVERSITY,
                    gender = Gender.MALE,
                    preferredLanguage = Language.ENGLISH,
                    interests = List(5) { "Interest$it" },
                    traits = List(3) { Trait.entries[it] },
                    ethnicity = listOf(Ethnicity.BLACK_AFRICAN_DESCENT)
                )
            }
            assertEquals("Height must be positive", exception.message)
        }

        @Test
        fun `should throw exception when firstName is blank`() {
            val exception = assertThrows<IllegalArgumentException> {
                CreateUserRequest(
                    userId = "test-id",
                    firstName = "  ",
                    lastName = "Doe",
                    email = "test@example.com",
                    heightCm = 180,
                    dateOfBirth = LocalDate.of(1990, 1, 1),
                    city = "London",
                    educationLevel = EducationLevel.UNIVERSITY,
                    gender = Gender.MALE,
                    preferredLanguage = Language.ENGLISH,
                    interests = List(5) { "Interest$it" },
                    traits = List(3) { Trait.entries[it] },
                    ethnicity = listOf(Ethnicity.BLACK_AFRICAN_DESCENT)
                )
            }
            assertEquals("First name is required", exception.message)
        }

        @Test
        fun `should throw exception when brainDescription exceeds 200 characters`() {
            val exception = assertThrows<IllegalArgumentException> {
                CreateUserRequest(
                    userId = "test-id",
                    firstName = "John",
                    lastName = "Doe",
                    email = "test@example.com",
                    heightCm = 180,
                    dateOfBirth = LocalDate.of(1990, 1, 1),
                    city = "London",
                    educationLevel = EducationLevel.UNIVERSITY,
                    gender = Gender.MALE,
                    preferredLanguage = Language.ENGLISH,
                    interests = List(5) { "Interest$it" },
                    traits = List(3) { Trait.entries[it] },
                    ethnicity = listOf(Ethnicity.BLACK_AFRICAN_DESCENT),
                    brainDescription = "a".repeat(201)
                )
            }
            assertEquals("Brain description must not exceed 200 characters", exception.message)
        }

        @Test
        fun `should create request successfully with valid data`() {
            val request = CreateUserRequest(
                userId = "test-id",
                firstName = "John",
                lastName = "Doe",
                email = "test@example.com",
                heightCm = 180,
                dateOfBirth = LocalDate.of(1990, 1, 1),
                city = "London",
                educationLevel = EducationLevel.UNIVERSITY,
                gender = Gender.MALE,
                preferredLanguage = Language.ENGLISH,
                interests = List(5) { "Interest$it" },
                traits = List(3) { Trait.entries[it] },
                ethnicity = listOf(Ethnicity.BLACK_AFRICAN_DESCENT)
            )

            assertEquals("test-id", request.userId)
            assertEquals("John", request.firstName)
        }
    }

    @Nested
    inner class `UpdateUserRequest validation` {

        @Test
        fun `should throw exception when interests count is invalid`() {
            val exception = assertThrows<IllegalArgumentException> {
                UpdateUserRequest(interests = listOf("A", "B"))
            }
            assertEquals("Interests must be between 5 and 10 items", exception.message)
        }

        @Test
        fun `should throw exception when traits count is invalid`() {
            val exception = assertThrows<IllegalArgumentException> {
                UpdateUserRequest(traits = listOf(Trait.HONEST))
            }
            assertEquals("Traits must be between 3 and 10 items", exception.message)
        }

        @Test
        fun `should throw exception when bio exceeds 300 characters`() {
            val exception = assertThrows<IllegalArgumentException> {
                UpdateUserRequest(bio = "a".repeat(301))
            }
            assertEquals("Bio must not exceed 300 characters", exception.message)
        }

        @Test
        fun `should throw exception when heightCm is not positive`() {
            val exception = assertThrows<IllegalArgumentException> {
                UpdateUserRequest(heightCm = -5)
            }
            assertEquals("Height must be positive", exception.message)
        }

        @Test
        fun `should throw exception when brainDescription exceeds 100 characters`() {
            val exception = assertThrows<IllegalArgumentException> {
                UpdateUserRequest(brainDescription = "a".repeat(101))
            }
            assertEquals("Brain description must not exceed 100 characters", exception.message)
        }

        @Test
        fun `should throw exception when bodyDescription exceeds 100 characters`() {
            val exception = assertThrows<IllegalArgumentException> {
                UpdateUserRequest(bodyDescription = "a".repeat(101))
            }
            assertEquals("Body description must not exceed 100 characters", exception.message)
        }

        @Test
        fun `should throw exception when ethnicity list is empty`() {
            val exception = assertThrows<IllegalArgumentException> {
                UpdateUserRequest(ethnicity = emptyList())
            }
            assertEquals("Ethnicity must not be empty", exception.message)
        }

        @Test
        fun `should create update request successfully with valid data`() {
            val request = UpdateUserRequest(
                firstName = "Jane",
                bio = "Updated bio",
                interests = List(7) { "Interest$it" }
            )

            assertEquals("Jane", request.firstName)
            assertEquals("Updated bio", request.bio)
        }

        @Test
        fun `should create update request successfully with all null fields`() {
            val request = UpdateUserRequest()

            assertEquals(null, request.firstName)
            assertEquals(null, request.bio)
        }
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
        occupation: String = "Engineer",
        bio: String = "Test bio",
        interests: List<String> = List(5) { "Interest$it" },
        traits: List<Trait> = List(3) { Trait.entries[it] },
        preferredLanguage: Language = Language.ENGLISH,
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
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now(),
        deletedAt: Instant? = null
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
            occupation = occupation,
            bio = bio,
            interests = interests,
            traits = traits,
            preferredLanguage = preferredLanguage,
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
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt
        )
    }
}
