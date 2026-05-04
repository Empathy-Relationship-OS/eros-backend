package com.eros.users.serializers

import com.eros.users.models.Activity
import com.eros.users.models.Creative
import com.eros.users.models.Entertainment
import com.eros.users.models.FoodAndDrink
import com.eros.users.models.Interest
import com.eros.users.models.MusicGenre
import com.eros.users.models.Sport
import com.eros.users.models.UserInterest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test suite for UserInterestSerializer.
 *
 * Tests cover:
 * - Serialization (Enum -> Display Name)
 * - Deserialization (Display Name -> Enum)
 * - Acronym handling (DIY, AI, BBQ, etc.)
 * - Error handling for invalid display names
 */
class UserInterestSerializerTest {

    private val json = Json { prettyPrint = false }

    // ========================================
    // Standard Display Name Tests
    // ========================================

    @Test
    fun `UserInterestSerializer should serialize standard enum values to display names`() {
        assertEquals("\"City Trips\"", json.encodeToString(UserInterestSerializer, Activity.CITY_TRIPS))
        assertEquals("\"Hiking\"", json.encodeToString(UserInterestSerializer, Activity.HIKING))
        assertEquals("\"Formula 1\"", json.encodeToString(UserInterestSerializer, Interest.FORMULA_1))
        assertEquals("\"Photography\"", json.encodeToString(UserInterestSerializer, Creative.PHOTOGRAPHY))
    }

    @Test
    fun `UserInterestSerializer should deserialize standard display names to enums`() {
        assertEquals(Activity.CITY_TRIPS, json.decodeFromString(UserInterestSerializer, "\"City Trips\""))
        assertEquals(Activity.HIKING, json.decodeFromString(UserInterestSerializer, "\"Hiking\""))
        assertEquals(Interest.FORMULA_1, json.decodeFromString(UserInterestSerializer, "\"Formula 1\""))
        assertEquals(Creative.PHOTOGRAPHY, json.decodeFromString(UserInterestSerializer, "\"Photography\""))
    }

    // ========================================
    // Acronym Handling Tests
    // ========================================

    @Test
    fun `UserInterestSerializer should serialize AI with proper capitalization`() {
        val result = json.encodeToString(UserInterestSerializer, Interest.AI)
        assertEquals("\"AI\"", result)
    }

    @Test
    fun `UserInterestSerializer should deserialize AI from proper capitalization`() {
        val result = json.decodeFromString(UserInterestSerializer, "\"AI\"")
        assertEquals(Interest.AI, result)
    }

    @Test
    fun `UserInterestSerializer should serialize DIY with proper capitalization`() {
        val result = json.encodeToString(UserInterestSerializer, Creative.DIY)
        assertEquals("\"DIY\"", result)
    }

    @Test
    fun `UserInterestSerializer should deserialize DIY from proper capitalization`() {
        val result = json.decodeFromString(UserInterestSerializer, "\"DIY\"")
        assertEquals(Creative.DIY, result)
    }

    @Test
    fun `UserInterestSerializer should serialize Sci-Fi with proper format`() {
        val result = json.encodeToString(UserInterestSerializer, Entertainment.SCI_FI)
        assertEquals("\"Sci-Fi\"", result)
    }

    @Test
    fun `UserInterestSerializer should deserialize Sci-Fi from proper format`() {
        val result = json.decodeFromString(UserInterestSerializer, "\"Sci-Fi\"")
        assertEquals(Entertainment.SCI_FI, result)
    }

    @Test
    fun `UserInterestSerializer should serialize EDM with proper capitalization`() {
        val result = json.encodeToString(UserInterestSerializer, MusicGenre.EDM)
        assertEquals("\"EDM\"", result)
    }

    @Test
    fun `UserInterestSerializer should deserialize EDM from proper capitalization`() {
        val result = json.decodeFromString(UserInterestSerializer, "\"EDM\"")
        assertEquals(MusicGenre.EDM, result)
    }

    @Test
    fun `UserInterestSerializer should serialize Hip-Hop with proper format`() {
        val result = json.encodeToString(UserInterestSerializer, MusicGenre.HIPHOP)
        assertEquals("\"Hip-Hop\"", result)
    }

    @Test
    fun `UserInterestSerializer should deserialize Hip-Hop from proper format`() {
        val result = json.decodeFromString(UserInterestSerializer, "\"Hip-Hop\"")
        assertEquals(MusicGenre.HIPHOP, result)
    }

    @Test
    fun `UserInterestSerializer should serialize K-Pop with proper format`() {
        val result = json.encodeToString(UserInterestSerializer, MusicGenre.K_POP)
        assertEquals("\"K-Pop\"", result)
    }

    @Test
    fun `UserInterestSerializer should deserialize K-Pop from proper format`() {
        val result = json.decodeFromString(UserInterestSerializer, "\"K-Pop\"")
        assertEquals(MusicGenre.K_POP, result)
    }

    @Test
    fun `UserInterestSerializer should serialize R&B with proper format`() {
        val result = json.encodeToString(UserInterestSerializer, MusicGenre.R_AND_B)
        assertEquals("\"R&B\"", result)
    }

    @Test
    fun `UserInterestSerializer should deserialize R&B from proper format`() {
        val result = json.decodeFromString(UserInterestSerializer, "\"R&B\"")
        assertEquals(MusicGenre.R_AND_B, result)
    }

    @Test
    fun `UserInterestSerializer should serialize BBQ with proper capitalization`() {
        val result = json.encodeToString(UserInterestSerializer, FoodAndDrink.BBQ)
        assertEquals("\"BBQ\"", result)
    }

    @Test
    fun `UserInterestSerializer should deserialize BBQ from proper capitalization`() {
        val result = json.decodeFromString(UserInterestSerializer, "\"BBQ\"")
        assertEquals(FoodAndDrink.BBQ, result)
    }

    @Test
    fun `UserInterestSerializer should serialize SUP with proper capitalization`() {
        val result = json.encodeToString(UserInterestSerializer, Sport.SUP)
        assertEquals("\"SUP\"", result)
    }

    @Test
    fun `UserInterestSerializer should deserialize SUP from proper capitalization`() {
        val result = json.decodeFromString(UserInterestSerializer, "\"SUP\"")
        assertEquals(Sport.SUP, result)
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    fun `UserInterestSerializer should throw exception for invalid display name`() {
        val exception = assertThrows<IllegalArgumentException> {
            json.decodeFromString(UserInterestSerializer, "\"INVALID_INTEREST\"")
        }
        assertTrue(exception.message!!.contains("Unknown UserInterest displayName: 'INVALID_INTEREST'"))
    }

    @Test
    fun `UserInterestSerializer should accept old enum format for backward compatibility`() {
        // Now accepts enum names for backward compatibility
        assertEquals(Activity.CITY_TRIPS, json.decodeFromString(UserInterestSerializer, "\"CITY_TRIPS\""))
        assertEquals(Interest.NATURE, json.decodeFromString(UserInterestSerializer, "\"NATURE\""))
        assertEquals(Entertainment.MOVIES, json.decodeFromString(UserInterestSerializer, "\"MOVIES\""))
    }

    @Test
    fun `UserInterestSerializer should throw exception for incorrect acronym capitalization`() {
        // "Diy" instead of "DIY"
        val exception = assertThrows<IllegalArgumentException> {
            json.decodeFromString(UserInterestSerializer, "\"Diy\"")
        }
        assertTrue(exception.message!!.contains("Unknown UserInterest displayName: 'Diy'"))
    }

    @Test
    fun `UserInterestSerializer should throw exception for incorrect Sci-Fi format`() {
        // "Sci Fi" (space) instead of "Sci-Fi" (hyphen)
        val exception = assertThrows<IllegalArgumentException> {
            json.decodeFromString(UserInterestSerializer, "\"Sci Fi\"")
        }
        assertTrue(exception.message!!.contains("Unknown UserInterest displayName: 'Sci Fi'"))
    }

    // ========================================
    // Round-Trip Tests
    // ========================================

    @Test
    fun `UserInterestSerializer should handle round-trip serialization for all acronyms`() {
        val acronymInterests = listOf(
            Interest.AI,
            Creative.DIY,
            Entertainment.SCI_FI,
            MusicGenre.EDM,
            MusicGenre.HIPHOP,
            MusicGenre.K_POP,
            MusicGenre.R_AND_B,
            FoodAndDrink.BBQ,
            Sport.SUP
        )

        acronymInterests.forEach { interest ->
            val serialized = json.encodeToString(UserInterestSerializer, interest)
            val deserialized = json.decodeFromString<UserInterest>(UserInterestSerializer, serialized)
            assertEquals(interest, deserialized, "Round-trip failed for ${interest.displayName}")
        }
    }

    @Test
    fun `UserInterestSerializer should handle round-trip serialization for standard interests`() {
        val standardInterests = listOf(
            Activity.CITY_TRIPS,
            Activity.HIKING,
            Interest.NATURE,
            Entertainment.MOVIES,
            Creative.PHOTOGRAPHY,
            MusicGenre.JAZZ,
            FoodAndDrink.PIZZA,
            Sport.YOGA
        )

        standardInterests.forEach { interest ->
            val serialized = json.encodeToString(UserInterestSerializer, interest)
            val deserialized = json.decodeFromString<UserInterest>(UserInterestSerializer, serialized)
            assertEquals(interest, deserialized, "Round-trip failed for ${interest.displayName}")
        }
    }
}
