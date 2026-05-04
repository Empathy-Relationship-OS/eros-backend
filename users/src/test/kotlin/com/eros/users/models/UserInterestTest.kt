package com.eros.users.models

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * Tests for UserInterest enums to ensure data integrity
 */
class UserInterestTest {

    @Test
    fun `all UserInterest enum values must be unique across all implementing enums`() {
        // Collect all enum names from all UserInterest implementations
        val allInterestNames = mutableListOf<String>()

        allInterestNames.addAll(Activity.entries.map { it.name })
        allInterestNames.addAll(Interest.entries.map { it.name })
        allInterestNames.addAll(Entertainment.entries.map { it.name })
        allInterestNames.addAll(Creative.entries.map { it.name })
        allInterestNames.addAll(MusicGenre.entries.map { it.name })
        allInterestNames.addAll(FoodAndDrink.entries.map { it.name })
        allInterestNames.addAll(Sport.entries.map { it.name })

        // Find duplicates
        val duplicates = allInterestNames
            .groupingBy { it }
            .eachCount()
            .filter { it.value > 1 }
            .keys

        // Assert no duplicates exist
        assertTrue(
            duplicates.isEmpty(),
            "Found duplicate enum values across UserInterest types: ${duplicates.joinToString(", ")}\n" +
            "All UserInterest enum values must be unique to avoid database ambiguity."
        )
    }

    @Test
    fun `displayName should convert enum names to proper case`() {
        // Standard conversions
        assertEquals("City Trips", Activity.CITY_TRIPS.displayName)
        assertEquals("Formula 1", Interest.FORMULA_1.displayName)
        assertEquals("Playing Instruments", Creative.PLAYING_INSTRUMENTS.displayName)
        assertEquals("Kick Boxing", Sport.KICK_BOXING.displayName)

        // Acronyms should preserve proper capitalization
        assertEquals("AI", Interest.AI.displayName)
        assertEquals("DIY", Creative.DIY.displayName)
        assertEquals("Sci-Fi", Entertainment.SCI_FI.displayName)
        assertEquals("EDM", MusicGenre.EDM.displayName)
        assertEquals("Hip-Hop", MusicGenre.HIPHOP.displayName)
        assertEquals("K-Pop", MusicGenre.K_POP.displayName)
        assertEquals("R&B", MusicGenre.R_AND_B.displayName)
        assertEquals("BBQ", FoodAndDrink.BBQ.displayName)
        assertEquals("SUP", Sport.SUP.displayName)
    }

    @Test
    fun `all UserInterest enums should have displayName property`() {
        // Verify that all enum types implementing UserInterest have the displayName property
        // Check every entry in each enum type
        Activity.entries.forEach { entry ->
            assertTrue(entry.displayName.isNotBlank(), "Activity.${entry.name} should have non-blank displayName")
        }
        Interest.entries.forEach { entry ->
            assertTrue(entry.displayName.isNotBlank(), "Interest.${entry.name} should have non-blank displayName")
        }
        Entertainment.entries.forEach { entry ->
            assertTrue(entry.displayName.isNotBlank(), "Entertainment.${entry.name} should have non-blank displayName")
        }
        Creative.entries.forEach { entry ->
            assertTrue(entry.displayName.isNotBlank(), "Creative.${entry.name} should have non-blank displayName")
        }
        MusicGenre.entries.forEach { entry ->
            assertTrue(entry.displayName.isNotBlank(), "MusicGenre.${entry.name} should have non-blank displayName")
        }
        FoodAndDrink.entries.forEach { entry ->
            assertTrue(entry.displayName.isNotBlank(), "FoodAndDrink.${entry.name} should have non-blank displayName")
        }
        Sport.entries.forEach { entry ->
            assertTrue(entry.displayName.isNotBlank(), "Sport.${entry.name} should have non-blank displayName")
        }
    }
}
