package com.eros.matching.time

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BatchWindowCalculatorTest {

    private val fixedInstant = Instant.parse("2024-01-15T10:00:00Z") // 10 AM UTC
    private val fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    @Nested
    inner class MidnightUtcBatchWindowCalculator {

        @Test
        fun `should return LocalDate for midnight UTC boundary`() {
            val calculator = MidnightUtcBatchWindowCalculator(fixedClock)

            val instant1 = Instant.parse("2024-01-15T00:00:00Z") // Midnight
            val instant2 = Instant.parse("2024-01-15T23:59:59Z") // Just before next midnight

            assertEquals(LocalDate.of(2024, 1, 15), calculator.getBatchWindow(instant1))
            assertEquals(LocalDate.of(2024, 1, 15), calculator.getBatchWindow(instant2))
        }

        @Test
        fun `should identify same window for timestamps on same day`() {
            val calculator = MidnightUtcBatchWindowCalculator(fixedClock)

            val instant1 = Instant.parse("2024-01-15T02:00:00Z") // 2 AM
            val instant2 = Instant.parse("2024-01-15T22:00:00Z") // 10 PM

            assertTrue(calculator.isSameWindow(instant1, instant2))
        }

        @Test
        fun `should identify different windows for timestamps on different days`() {
            val calculator = MidnightUtcBatchWindowCalculator(fixedClock)

            val instant1 = Instant.parse("2024-01-15T23:59:59Z") // Jan 15, 11:59:59 PM
            val instant2 = Instant.parse("2024-01-16T00:00:01Z") // Jan 16, 12:00:01 AM

            assertFalse(calculator.isSameWindow(instant1, instant2))
        }

        @Test
        fun `should return current window based on clock`() {
            val calculator = MidnightUtcBatchWindowCalculator(fixedClock)

            assertEquals(LocalDate.of(2024, 1, 15), calculator.getCurrentWindow())
        }

        @Test
        fun `should use UTC semantics for getCurrentWindow even with non-UTC clock`() {
            // Regression test: verify getCurrentWindow() uses UTC regardless of clock's zone
            val instant = Instant.parse("2024-01-15T00:30:00Z") // 00:30 UTC on Jan 15

            val utcClock = Clock.fixed(instant, ZoneId.of("UTC"))
            val pstClock = Clock.fixed(instant, ZoneId.of("America/Los_Angeles"))

            val utcCalculator = MidnightUtcBatchWindowCalculator(utcClock)
            val pstCalculator = MidnightUtcBatchWindowCalculator(pstClock)

            // Both should return the same UTC-based date (Jan 15)
            val expectedUtcDate = LocalDate.of(2024, 1, 15)
            assertEquals(expectedUtcDate, utcCalculator.getCurrentWindow())
            assertEquals(expectedUtcDate, pstCalculator.getCurrentWindow())
        }

        @Test
        fun `should calculate next window start as midnight UTC next day`() {
            val calculator = MidnightUtcBatchWindowCalculator(fixedClock)

            val currentWindow = LocalDate.of(2024, 1, 15)
            val nextWindowStart = calculator.getNextWindowStart(currentWindow)

            // Next window should start at midnight UTC on Jan 16
            assertEquals(Instant.parse("2024-01-16T00:00:00Z"), nextWindowStart)
        }
    }

    @Nested
    inner class CustomHourBatchWindowCalculator {

        @Test
        fun `should throw exception for invalid reset hour`() {
            assertThrows<IllegalArgumentException> {
                CustomHourBatchWindowCalculator(resetHour = -1, clock = fixedClock)
            }

            assertThrows<IllegalArgumentException> {
                CustomHourBatchWindowCalculator(resetHour = 24, clock = fixedClock)
            }
        }

        @Test
        fun `should use custom reset hour (7 AM) for batch window boundaries`() {
            val calculator = CustomHourBatchWindowCalculator(resetHour = 7, clock = fixedClock)

            // Before 7 AM on Jan 15 should belong to Jan 14's window
            val instant1 = Instant.parse("2024-01-15T06:59:59Z") // 6:59:59 AM
            assertEquals(LocalDate.of(2024, 1, 14), calculator.getBatchWindow(instant1))

            // At or after 7 AM on Jan 15 should belong to Jan 15's window
            val instant2 = Instant.parse("2024-01-15T07:00:00Z") // 7:00 AM
            assertEquals(LocalDate.of(2024, 1, 15), calculator.getBatchWindow(instant2))

            val instant3 = Instant.parse("2024-01-15T23:59:59Z") // 11:59 PM
            assertEquals(LocalDate.of(2024, 1, 15), calculator.getBatchWindow(instant3))
        }

        @Test
        fun `should identify same window for timestamps within custom hour boundary`() {
            val calculator = CustomHourBatchWindowCalculator(resetHour = 7, clock = fixedClock)

            // Both between 7 AM Jan 15 and 7 AM Jan 16 -> same window (Jan 15)
            val instant1 = Instant.parse("2024-01-15T08:00:00Z") // 8 AM Jan 15
            val instant2 = Instant.parse("2024-01-16T06:59:59Z") // 6:59 AM Jan 16

            assertTrue(calculator.isSameWindow(instant1, instant2))
        }

        @Test
        fun `should identify different windows across custom hour boundary`() {
            val calculator = CustomHourBatchWindowCalculator(resetHour = 7, clock = fixedClock)

            // Just before and after 7 AM reset -> different windows
            val instant1 = Instant.parse("2024-01-15T06:59:59Z") // 6:59 AM Jan 15 (window: Jan 14)
            val instant2 = Instant.parse("2024-01-15T07:00:01Z") // 7:00 AM Jan 15 (window: Jan 15)

            assertFalse(calculator.isSameWindow(instant1, instant2))
        }

        @Test
        fun `should return current window based on custom hour and clock`() {
            // Clock shows 10 AM on Jan 15, reset hour is 7 AM
            // So current window should be Jan 15 (we've passed 7 AM)
            val calculator = CustomHourBatchWindowCalculator(resetHour = 7, clock = fixedClock)

            assertEquals(LocalDate.of(2024, 1, 15), calculator.getCurrentWindow())

            // Test with clock showing 6 AM (before reset hour)
            val earlyMorningClock = Clock.fixed(
                Instant.parse("2024-01-15T06:00:00Z"),
                ZoneId.of("UTC")
            )
            val earlyCalculator = CustomHourBatchWindowCalculator(
                resetHour = 7,
                clock = earlyMorningClock
            )

            // Should return Jan 14 (still in previous day's window)
            assertEquals(LocalDate.of(2024, 1, 14), earlyCalculator.getCurrentWindow())
        }

        @Test
        fun `should handle edge case of midnight reset hour`() {
            val calculator = CustomHourBatchWindowCalculator(resetHour = 0, clock = fixedClock)

            // Reset hour = 0 should behave like MidnightUtcBatchWindowCalculator
            val instant1 = Instant.parse("2024-01-15T00:00:00Z") // Midnight
            val instant2 = Instant.parse("2024-01-15T23:59:59Z") // Just before next midnight

            assertEquals(LocalDate.of(2024, 1, 15), calculator.getBatchWindow(instant1))
            assertEquals(LocalDate.of(2024, 1, 15), calculator.getBatchWindow(instant2))
        }

        @Test
        fun `should calculate next window start at custom reset hour`() {
            val calculator = CustomHourBatchWindowCalculator(resetHour = 7, clock = fixedClock)

            val currentWindow = LocalDate.of(2024, 1, 15)
            val nextWindowStart = calculator.getNextWindowStart(currentWindow)

            // Next window should start at 7 AM UTC on Jan 16
            assertEquals(Instant.parse("2024-01-16T07:00:00Z"), nextWindowStart)
        }

        @Test
        fun `should calculate next window start at midnight when reset hour is 0`() {
            val calculator = CustomHourBatchWindowCalculator(resetHour = 0, clock = fixedClock)

            val currentWindow = LocalDate.of(2024, 1, 15)
            val nextWindowStart = calculator.getNextWindowStart(currentWindow)

            // Next window should start at midnight UTC on Jan 16 (same as MidnightUtcBatchWindowCalculator)
            assertEquals(Instant.parse("2024-01-16T00:00:00Z"), nextWindowStart)
        }
    }
}
