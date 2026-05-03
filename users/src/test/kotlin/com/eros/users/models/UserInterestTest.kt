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
        assertEquals("City Trips", Activity.CITY_TRIPS.displayName)
        assertEquals("Formula 1", Interest.FORMULA_1.displayName)
        assertEquals("Sci Fi", Entertainment.SCI_FI.displayName)
        assertEquals("Playing Instruments", Creative.PLAYING_INSTRUMENTS.displayName)
        assertEquals("R And B", MusicGenre.R_AND_B.displayName)
        assertEquals("Bbq", FoodAndDrink.BBQ.displayName)
        assertEquals("Kick Boxing", Sport.KICK_BOXING.displayName)
    }

    @Test
    fun `all UserInterest enums should have displayName property`() {
        // Verify that all enum types implementing UserInterest have the displayName property
        val activityExample: UserInterest = Activity.HIKING
        val interestExample: UserInterest = Interest.NATURE
        val entertainmentExample: UserInterest = Entertainment.MOVIES
        val creativeExample: UserInterest = Creative.PHOTOGRAPHY
        val musicGenreExample: UserInterest = MusicGenre.JAZZ
        val foodAndDrinkExample: UserInterest = FoodAndDrink.PIZZA
        val sportExample: UserInterest = Sport.YOGA

        // Should not throw - just verify they have displayName
        assertTrue(activityExample.displayName.isNotBlank())
        assertTrue(interestExample.displayName.isNotBlank())
        assertTrue(entertainmentExample.displayName.isNotBlank())
        assertTrue(creativeExample.displayName.isNotBlank())
        assertTrue(musicGenreExample.displayName.isNotBlank())
        assertTrue(foodAndDrinkExample.displayName.isNotBlank())
        assertTrue(sportExample.displayName.isNotBlank())
    }
}
