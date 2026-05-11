package com.eros.matching.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Calculates which "batch window" a given instant belongs to.
 *
 * A batch window defines when daily batch counts reset. Currently, uses midnight UTC
 * as the boundary, but can be extended to support custom reset times (e.g., 7 AM).
 *
 * This abstraction ensures consistency between:
 * - DailyBatch record lookups (which use LocalDate)
 * - Match servedAt timestamp comparisons
 * - Batch reset time calculations
 */
interface BatchWindowCalculator {
    /**
     * Determines which batch window the given instant belongs to.
     *
     * @param instant The timestamp to evaluate
     * @return The batch window identifier (currently a LocalDate)
     */
    fun getBatchWindow(instant: Instant): LocalDate

    /**
     * Checks if two instants belong to the same batch window.
     *
     * @param instant1 First timestamp
     * @param instant2 Second timestamp
     * @return True if both belong to the same batch window
     */
    fun isSameWindow(instant1: Instant, instant2: Instant): Boolean {
        return getBatchWindow(instant1) == getBatchWindow(instant2)
    }

    /**
     * Gets the current batch window based on the clock.
     *
     * @return The current batch window identifier
     */
    fun getCurrentWindow(): LocalDate

    /**
     * Calculates when the next batch window starts (when the current window expires).
     *
     * This is used for the resetAt timestamp in rate limit errors.
     *
     * @param currentWindow The current batch window
     * @return The instant when the next batch window begins
     */
    fun getNextWindowStart(currentWindow: LocalDate): Instant
}

/**
 * Default implementation using midnight UTC as the batch window boundary.
 *
 * This means batch counts reset at midnight UTC each day.
 */
class MidnightUtcBatchWindowCalculator(
    private val clock: Clock = Clock.systemUTC()
) : BatchWindowCalculator {

    private val zoneId = ZoneId.of("UTC")

    override fun getBatchWindow(instant: Instant): LocalDate {
        return instant.atZone(zoneId).toLocalDate()
    }

    override fun getCurrentWindow(): LocalDate {
        return LocalDate.now(clock)
    }

    override fun getNextWindowStart(currentWindow: LocalDate): Instant {
        // Next window starts at midnight UTC on the next day
        return currentWindow.plusDays(1).atStartOfDay(zoneId).toInstant()
    }
}

/**
 * Future implementation example: Custom hour-based batch window.
 *
 * For example, if resetHour = 7, batches reset at 7 AM UTC each day.
 * Times between 7 AM today and 7 AM tomorrow belong to the same window.
 */
class CustomHourBatchWindowCalculator(
    private val resetHour: Int,
    private val clock: Clock = Clock.systemUTC()
) : BatchWindowCalculator {

    private val zoneId = ZoneId.of("UTC")

    init {
        require(resetHour in 0..23) { "resetHour must be between 0 and 23, got: $resetHour" }
    }

    override fun getBatchWindow(instant: Instant): LocalDate {
        val zonedDateTime = instant.atZone(zoneId)

        // If current hour is before reset hour, we're still in "yesterday's" window
        return if (zonedDateTime.hour < resetHour) {
            zonedDateTime.toLocalDate().minusDays(1)
        } else {
            zonedDateTime.toLocalDate()
        }
    }

    override fun getCurrentWindow(): LocalDate {
        return getBatchWindow(Instant.now(clock))
    }

    override fun getNextWindowStart(currentWindow: LocalDate): Instant {
        // Next window starts at the reset hour on the next day
        return currentWindow.plusDays(1).atTime(resetHour, 0).atZone(zoneId).toInstant()
    }
}
