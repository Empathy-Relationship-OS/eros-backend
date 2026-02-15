package com.eros.users.models

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserQATest {

    @Nested
    inner class `UserQAItem validation` {

        @Test
        fun `should throw exception when displayOrder is less than 1`() {
            val exception = assertThrows<IllegalArgumentException> {
                UserQAItem(
                    id = 1L,
                    userId = "user-123",
                    question = PredefinedQuestion.LAST_MEAL_EVER,
                    answer = "Pizza",
                    displayOrder = 0,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            }
            assertEquals("Display order must be between 1 and 3", exception.message)
        }

        @Test
        fun `should throw exception when displayOrder is greater than 3`() {
            val exception = assertThrows<IllegalArgumentException> {
                UserQAItem(
                    id = 1L,
                    userId = "user-123",
                    question = PredefinedQuestion.LAST_MEAL_EVER,
                    answer = "Pizza",
                    displayOrder = 4,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            }
            assertEquals("Display order must be between 1 and 3", exception.message)
        }

        @Test
        fun `should throw exception when answer is blank`() {
            val exception = assertThrows<IllegalArgumentException> {
                UserQAItem(
                    id = 1L,
                    userId = "user-123",
                    question = PredefinedQuestion.LAST_MEAL_EVER,
                    answer = "   ",
                    displayOrder = 1,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            }
            assertEquals("Answer is required", exception.message)
        }

        @Test
        fun `should throw exception when answer exceeds 200 characters`() {
            val exception = assertThrows<IllegalArgumentException> {
                UserQAItem(
                    id = 1L,
                    userId = "user-123",
                    question = PredefinedQuestion.LAST_MEAL_EVER,
                    answer = "a".repeat(201),
                    displayOrder = 1,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            }
            assertEquals("Answer must not exceed 200 characters", exception.message)
        }

        @Test
        fun `should create QA item successfully with valid displayOrder range`() {
            for (order in 1..3) {
                val qaItem = UserQAItem(
                    id = 1L,
                    userId = "user-123",
                    question = PredefinedQuestion.LAST_MEAL_EVER,
                    answer = "Pizza and ice cream",
                    displayOrder = order,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
                assertEquals(order, qaItem.displayOrder)
            }
        }

        @Test
        fun `getQuestionText should return display text from question enum`() {
            val qaItem = createQAItem(question = PredefinedQuestion.LAST_MEAL_EVER)

            assertEquals("My last meal ever would be", qaItem.getQuestionText())
        }
    }

    @Nested
    inner class `AddUserQARequest validation` {

        @Test
        fun `should throw exception when displayOrder is out of range`() {
            val exception = assertThrows<IllegalArgumentException> {
                AddUserQARequest(
                    userId = 1L,
                    question = PredefinedQuestion.LAST_MEAL_EVER,
                    answer = "Pizza",
                    displayOrder = 5
                )
            }
            assertEquals("Display order must be between 1 and 3", exception.message)
        }

        @Test
        fun `should throw exception when answer is blank`() {
            val exception = assertThrows<IllegalArgumentException> {
                AddUserQARequest(
                    userId = 1L,
                    question = PredefinedQuestion.LAST_MEAL_EVER,
                    answer = "",
                    displayOrder = 1
                )
            }
            assertEquals("Answer is required", exception.message)
        }

        @Test
        fun `should throw exception when answer exceeds 200 characters`() {
            val exception = assertThrows<IllegalArgumentException> {
                AddUserQARequest(
                    userId = 1L,
                    question = PredefinedQuestion.LAST_MEAL_EVER,
                    answer = "a".repeat(201),
                    displayOrder = 1
                )
            }
            assertEquals("Answer must not exceed 200 characters", exception.message)
        }

        @Test
        fun `should create request successfully with exactly 200 characters`() {
            val request = AddUserQARequest(
                userId = 1L,
                question = PredefinedQuestion.LAST_MEAL_EVER,
                answer = "a".repeat(200),
                displayOrder = 1
            )

            assertEquals(200, request.answer.length)
        }

        @Test
        fun `should create request successfully with valid data`() {
            val request = AddUserQARequest(
                userId = 1L,
                question = PredefinedQuestion.FAVOURITE_BOOK_MOVIE_TV,
                answer = "The Lord of the Rings",
                displayOrder = 2
            )

            assertEquals(1L, request.userId)
            assertEquals(PredefinedQuestion.FAVOURITE_BOOK_MOVIE_TV, request.question)
            assertEquals("The Lord of the Rings", request.answer)
            assertEquals(2, request.displayOrder)
        }
    }

    @Nested
    inner class `UpdateUserQARequest validation` {

        @Test
        fun `should throw exception when displayOrder is out of range`() {
            val exception = assertThrows<IllegalArgumentException> {
                UpdateUserQARequest(displayOrder = 0)
            }
            assertEquals("Display order must be between 1 and 3", exception.message)
        }

        @Test
        fun `should throw exception when answer is blank`() {
            val exception = assertThrows<IllegalArgumentException> {
                UpdateUserQARequest(answer = "  ")
            }
            assertEquals("Answer is required", exception.message)
        }

        @Test
        fun `should throw exception when answer exceeds 200 characters`() {
            val exception = assertThrows<IllegalArgumentException> {
                UpdateUserQARequest(answer = "a".repeat(201))
            }
            assertEquals("Answer must not exceed 200 characters", exception.message)
        }

        @Test
        fun `should create update request successfully with null fields`() {
            val request = UpdateUserQARequest()

            assertNull(request.question)
            assertNull(request.answer)
            assertNull(request.displayOrder)
        }

        @Test
        fun `should create update request successfully with valid data`() {
            val request = UpdateUserQARequest(
                question = PredefinedQuestion.DREAM_JOB_NO_MONEY,
                answer = "Professional beach volleyball player",
                displayOrder = 3
            )

            assertEquals(PredefinedQuestion.DREAM_JOB_NO_MONEY, request.question)
            assertEquals("Professional beach volleyball player", request.answer)
            assertEquals(3, request.displayOrder)
        }
    }

    @Nested
    inner class `UserQACollection` {

        @Test
        fun `hasMinimumQAs should return true when count is 1 or more`() {
            val collection = UserQACollection(
                userId = "user-123",
                qas = createQAList(1),
                totalCount = 1
            )

            assertTrue(collection.hasMinimumQAs())
        }

        @Test
        fun `hasMinimumQAs should return false when count is 0`() {
            val collection = UserQACollection(
                userId = "user-123",
                qas = emptyList(),
                totalCount = 0
            )

            assertFalse(collection.hasMinimumQAs())
        }

        @Test
        fun `hasReachedMaximum should return true when count is 3 or more`() {
            val collection = UserQACollection(
                userId = "user-123",
                qas = createQAList(3),
                totalCount = 3
            )

            assertTrue(collection.hasReachedMaximum())
        }

        @Test
        fun `hasReachedMaximum should return false when count is less than 3`() {
            val collection = UserQACollection(
                userId = "user-123",
                qas = createQAList(2),
                totalCount = 2
            )

            assertFalse(collection.hasReachedMaximum())
        }

        @Test
        fun `isValidCollection should return true when count is between 1 and 3`() {
            for (count in 1..3) {
                val collection = UserQACollection(
                    userId = "user-123",
                    qas = createQAList(count),
                    totalCount = count
                )
                assertTrue(collection.isValidCollection(), "Failed for count: $count")
            }
        }

        @Test
        fun `isValidCollection should return false when count is 0`() {
            val collection = UserQACollection(
                userId = "user-123",
                qas = emptyList(),
                totalCount = 0
            )

            assertFalse(collection.isValidCollection())
        }

        @Test
        fun `isValidCollection should return false when count is more than 3`() {
            val collection = UserQACollection(
                userId = "user-123",
                qas = createQAList(4),
                totalCount = 4
            )

            assertFalse(collection.isValidCollection())
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
