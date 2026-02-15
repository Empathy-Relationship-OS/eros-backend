package com.eros.users.models.validation

import com.eros.users.models.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnumValidatorTest {

    @Nested
    inner class `isValidInterest` {

        @Test
        fun `should return true for valid Activity enum`() {
            assertTrue(EnumValidator.isValidInterest("CITY_TRIPS"))
            assertTrue(EnumValidator.isValidInterest("HIKING"))
            assertTrue(EnumValidator.isValidInterest("BEACH"))
        }

        @Test
        fun `should return true for valid Interest enum`() {
            assertTrue(EnumValidator.isValidInterest("ENTREPRENEURSHIP"))
            assertTrue(EnumValidator.isValidInterest("AI"))
            assertTrue(EnumValidator.isValidInterest("PHILOSOPHY"))
        }

        @Test
        fun `should return true for valid Entertainment enum`() {
            assertTrue(EnumValidator.isValidInterest("READING"))
            assertTrue(EnumValidator.isValidInterest("GAMING"))
            assertTrue(EnumValidator.isValidInterest("MOVIES"))
        }

        @Test
        fun `should return true for valid Creative enum`() {
            assertTrue(EnumValidator.isValidInterest("PHOTOGRAPHY"))
            assertTrue(EnumValidator.isValidInterest("PAINTING"))
            assertTrue(EnumValidator.isValidInterest("WRITING"))
        }

        @Test
        fun `should return true for valid MusicGenre enum`() {
            assertTrue(EnumValidator.isValidInterest("JAZZ"))
            assertTrue(EnumValidator.isValidInterest("ROCK"))
            assertTrue(EnumValidator.isValidInterest("TECHNO"))
        }

        @Test
        fun `should return true for valid FoodAndDrink enum`() {
            assertTrue(EnumValidator.isValidInterest("PIZZA"))
            assertTrue(EnumValidator.isValidInterest("COFFEE"))
            assertTrue(EnumValidator.isValidInterest("SUSHI"))
        }

        @Test
        fun `should return true for valid Sport enum`() {
            assertTrue(EnumValidator.isValidInterest("FOOTBALL"))
            assertTrue(EnumValidator.isValidInterest("YOGA"))
            assertTrue(EnumValidator.isValidInterest("SWIMMING"))
        }

        @Test
        fun `should return false for invalid interest string`() {
            assertFalse(EnumValidator.isValidInterest("INVALID_INTEREST"))
            assertFalse(EnumValidator.isValidInterest("NOT_A_REAL_THING"))
            assertFalse(EnumValidator.isValidInterest(""))
        }

        @Test
        fun `should be case insensitive`() {
            assertTrue(EnumValidator.isValidInterest("hiking"))
            assertTrue(EnumValidator.isValidInterest("Hiking"))
            assertTrue(EnumValidator.isValidInterest("HIKING"))
            assertTrue(EnumValidator.isValidInterest("HiKiNg"))
        }
    }

    @Nested
    inner class `validateInterests` {

        @Test
        fun `should return true when all interests are valid`() {
            val interests = listOf("HIKING", "PHOTOGRAPHY", "JAZZ", "PIZZA", "YOGA")

            assertTrue(EnumValidator.validateInterests(interests))
        }

        @Test
        fun `should return false when any interest is invalid`() {
            val interests = listOf("HIKING", "INVALID_INTEREST", "JAZZ")

            assertFalse(EnumValidator.validateInterests(interests))
        }

        @Test
        fun `should return true for empty list`() {
            assertTrue(EnumValidator.validateInterests(emptyList()))
        }

        @Test
        fun `should handle mixed case interests`() {
            val interests = listOf("hiking", "Photography", "JAZZ")

            assertTrue(EnumValidator.validateInterests(interests))
        }
    }

    @Nested
    inner class `validateTraits` {

        @Test
        fun `should return true when all traits are valid`() {
            val traits = listOf("HONEST", "KIND", "ADVENTUROUS")

            assertTrue(EnumValidator.validateTraits(traits))
        }

        @Test
        fun `should return false when any trait is invalid`() {
            val traits = listOf("HONEST", "INVALID_TRAIT", "KIND")

            assertFalse(EnumValidator.validateTraits(traits))
        }

        @Test
        fun `should return true for empty list`() {
            assertTrue(EnumValidator.validateTraits(emptyList()))
        }

        @Test
        fun `should be case insensitive`() {
            val traits = listOf("honest", "Kind", "ADVENTUROUS")

            assertTrue(EnumValidator.validateTraits(traits))
        }
    }

    @Nested
    inner class `validateLanguages` {

        @Test
        fun `should return true when all languages are valid`() {
            val languages = listOf("ENGLISH", "SPANISH", "FRENCH")

            assertTrue(EnumValidator.validateLanguages(languages))
        }

        @Test
        fun `should return false when any language is invalid`() {
            val languages = listOf("ENGLISH", "KLINGON", "SPANISH")

            assertFalse(EnumValidator.validateLanguages(languages))
        }

        @Test
        fun `should return true for empty list`() {
            assertTrue(EnumValidator.validateLanguages(emptyList()))
        }

        @Test
        fun `should be case insensitive`() {
            val languages = listOf("english", "Spanish", "FRENCH")

            assertTrue(EnumValidator.validateLanguages(languages))
        }
    }

    @Nested
    inner class `validateBrainAttributes` {

        @Test
        fun `should return true when all brain attributes are valid`() {
            val attributes = listOf("ADHD", "AUTISTIC", "HSP")

            assertTrue(EnumValidator.validateBrainAttributes(attributes))
        }

        @Test
        fun `should return false when any brain attribute is invalid`() {
            val attributes = listOf("ADHD", "INVALID_ATTRIBUTE")

            assertFalse(EnumValidator.validateBrainAttributes(attributes))
        }

        @Test
        fun `should return true for empty list`() {
            assertTrue(EnumValidator.validateBrainAttributes(emptyList()))
        }

        @Test
        fun `should be case insensitive`() {
            val attributes = listOf("adhd", "Autistic", "NEURODIVERGENT")

            assertTrue(EnumValidator.validateBrainAttributes(attributes))
        }
    }

    @Nested
    inner class `validateBodyAttributes` {

        @Test
        fun `should return true when all body attributes are valid`() {
            val attributes = listOf("CHRONIC_ILLNESS", "WHEELCHAIR", "DEAF")

            assertTrue(EnumValidator.validateBodyAttributes(attributes))
        }

        @Test
        fun `should return false when any body attribute is invalid`() {
            val attributes = listOf("WHEELCHAIR", "INVALID_ATTRIBUTE")

            assertFalse(EnumValidator.validateBodyAttributes(attributes))
        }

        @Test
        fun `should return true for empty list`() {
            assertTrue(EnumValidator.validateBodyAttributes(emptyList()))
        }

        @Test
        fun `should be case insensitive`() {
            val attributes = listOf("wheelchair", "Deaf", "MOBILITY_AID")

            assertTrue(EnumValidator.validateBodyAttributes(attributes))
        }
    }

    @Nested
    inner class `validateEthnicities` {

        @Test
        fun `should return true when all ethnicities are valid`() {
            val ethnicities = listOf("BLACK_AFRICAN_DESCENT", "EAST_ASIAN", "SOUTH_ASIAN")

            assertTrue(EnumValidator.validateEthnicities(ethnicities))
        }

        @Test
        fun `should return false when any ethnicity is invalid`() {
            val ethnicities = listOf("EAST_ASIAN", "INVALID_ETHNICITY")

            assertFalse(EnumValidator.validateEthnicities(ethnicities))
        }

        @Test
        fun `should return true for empty list`() {
            assertTrue(EnumValidator.validateEthnicities(emptyList()))
        }

        @Test
        fun `should be case insensitive`() {
            val ethnicities = listOf("east_asian", "South_Asian", "BLACK_AFRICAN_DESCENT")

            assertTrue(EnumValidator.validateEthnicities(ethnicities))
        }
    }

    @Nested
    inner class `enum converter methods` {

        @Test
        fun `toGender should convert valid string to Gender enum`() {
            assertEquals(Gender.MALE, EnumValidator.toGender("MALE"))
            assertEquals(Gender.FEMALE, EnumValidator.toGender("female"))
            assertEquals(Gender.NON_BINARY, EnumValidator.toGender("Non_Binary"))
        }

        @Test
        fun `toGender should return null for invalid string`() {
            assertNull(EnumValidator.toGender("INVALID"))
        }

        @Test
        fun `toEducationLevel should convert valid string to EducationLevel enum`() {
            assertEquals(EducationLevel.UNIVERSITY, EnumValidator.toEducationLevel("UNIVERSITY"))
            assertEquals(EducationLevel.COLLEGE, EnumValidator.toEducationLevel("college"))
        }

        @Test
        fun `toLanguage should convert valid string to Language enum`() {
            assertEquals(Language.ENGLISH, EnumValidator.toLanguage("ENGLISH"))
            assertEquals(Language.SPANISH, EnumValidator.toLanguage("spanish"))
        }

        @Test
        fun `toTrait should convert valid string to Trait enum`() {
            assertEquals(Trait.HONEST, EnumValidator.toTrait("HONEST"))
            assertEquals(Trait.KIND, EnumValidator.toTrait("kind"))
        }

        @Test
        fun `toMediaType should convert valid string to MediaType enum`() {
            assertEquals(MediaType.PHOTO, EnumValidator.toMediaType("PHOTO"))
            assertEquals(MediaType.VIDEO, EnumValidator.toMediaType("video"))
        }

        @Test
        fun `toEthnicity should convert valid string to Ethnicity enum`() {
            assertEquals(Ethnicity.EAST_ASIAN, EnumValidator.toEthnicity("EAST_ASIAN"))
            assertEquals(Ethnicity.SOUTH_ASIAN, EnumValidator.toEthnicity("south_asian"))
        }

        @Test
        fun `toBrainAttribute should convert valid string to BrainAttribute enum`() {
            assertEquals(BrainAttribute.ADHD, EnumValidator.toBrainAttribute("ADHD"))
            assertEquals(BrainAttribute.AUTISTIC, EnumValidator.toBrainAttribute("autistic"))
        }

        @Test
        fun `toBodyAttribute should convert valid string to BodyAttribute enum`() {
            assertEquals(BodyAttribute.WHEELCHAIR, EnumValidator.toBodyAttribute("WHEELCHAIR"))
            assertEquals(BodyAttribute.DEAF, EnumValidator.toBodyAttribute("deaf"))
        }
    }

    @Nested
    inner class `extension functions` {

        @Test
        fun `String toGenderOrNull should work correctly`() {
            assertEquals(Gender.MALE, "MALE".toGenderOrNull())
            assertEquals(Gender.FEMALE, "female".toGenderOrNull())
            assertNull("INVALID".toGenderOrNull())
        }

        @Test
        fun `List areValidInterests should work correctly`() {
            assertTrue(listOf("HIKING", "PHOTOGRAPHY", "JAZZ").areValidInterests())
            assertFalse(listOf("HIKING", "INVALID").areValidInterests())
        }

        @Test
        fun `List areValidTraits should work correctly`() {
            assertTrue(listOf("HONEST", "KIND", "CARING").areValidTraits())
            assertFalse(listOf("HONEST", "INVALID").areValidTraits())
        }

        @Test
        fun `List areValidLanguages should work correctly`() {
            assertTrue(listOf("ENGLISH", "SPANISH").areValidLanguages())
            assertFalse(listOf("ENGLISH", "KLINGON").areValidLanguages())
        }

        @Test
        fun `List areValidBrainAttributes should work correctly`() {
            assertTrue(listOf("ADHD", "AUTISTIC").areValidBrainAttributes())
            assertFalse(listOf("ADHD", "INVALID").areValidBrainAttributes())
        }

        @Test
        fun `List areValidBodyAttributes should work correctly`() {
            assertTrue(listOf("WHEELCHAIR", "DEAF").areValidBodyAttributes())
            assertFalse(listOf("WHEELCHAIR", "INVALID").areValidBodyAttributes())
        }

        @Test
        fun `List areValidEthnicities should work correctly`() {
            assertTrue(listOf("EAST_ASIAN", "SOUTH_ASIAN").areValidEthnicities())
            assertFalse(listOf("EAST_ASIAN", "INVALID").areValidEthnicities())
        }
    }
}
