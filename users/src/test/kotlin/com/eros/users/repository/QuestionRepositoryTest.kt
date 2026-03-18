package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.users.models.Question
import com.eros.users.table.Questions
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuestionRepositoryTest {

    @Nested
    inner class `Create Question` {

        @Test
        fun `create single question`() = runTest {
            val now = Instant.now(clock)
            val question = Question(0L, "Question here", now, now)

            val returnedQuestion = dbQuery { repository.create(question) }

            assertNotNull(returnedQuestion)
            assertEquals(question.question, returnedQuestion.question)

        }

        @Test
        fun `failure to create duplicate question`() = runTest {
            val now = Instant.now(clock)
            val question1 = Question(0L, "Question here", now, now)
            val question2 = Question(0L, "Question here", now, now)

            assertThrows<ExposedSQLException> {
                dbQuery {
                    repository.create(question1)
                    repository.create(question2)
                }
            }
        }
    }

    @Nested
    inner class `findAll Questions` {

        @Test
        fun `successful retrieval of all questions`() = runTest {

            // Populate amd call findAll.
            val questions = dbQuery {
                populateQuestions(5)
                repository.findAll()
            }

            assertNotNull(questions)
            assertTrue(questions.isNotEmpty())

            assertEquals(5, questions.size)

        }

        @Test
        fun `successful retrieval of empty questions`() = runTest {

            // Call findAll with no questions in the database.
            val questions = dbQuery {
                repository.findAll()
            }

            assertNotNull(questions)
            assertEquals(0, questions.size)
        }
    }

    @Nested
    inner class `UPDATE Questions`() {

        @Test
        fun `successful update question`() = runTest {

            val now = Instant.now(clock)
            val question = Question(0L, "BlahBlah", now, now)

            val (created, updated) = dbQuery {
                val created = repository.create(question)
                val res = repository.update(
                    created.questionId,
                    Question(created.questionId, "New Question", question.createdAt, now)
                )
                created to res
            }
            assertEquals(created.questionId, updated?.questionId)
            assertEquals(created.createdAt, updated?.createdAt)
            assertNotEquals(created.question, updated?.question)

        }

        @Test
        fun `update to existing question fails`() = runTest {

            val now = Instant.now(clock)
            val question = Question(0L, "BlahBlah", now, now)
            val question2 = Question(0L, "Already a question", now, now)
            assertThrows<ExposedSQLException> {
                dbQuery {
                    val created = repository.create(question)
                    repository.create(question2)
                    repository.update(
                        created.questionId,
                        Question(created.questionId, "Already a question", question.createdAt, now)
                    )
                }
            }
        }

    }

    @Nested
    inner class `DELETE Question`() {

        @Test
        fun `successful deleted`() = runTest {

            val now = Instant.now(clock)
            val question = Question(0L, "BlahBlah", now, now)

            val deleted = dbQuery {
                val created = repository.create(question)
                repository.delete(created.questionId)
            }

            assertEquals(1, deleted)
        }

        @Test
        fun `error thrown on deleting non existent id`() = runTest {
            val deleted = dbQuery {
                repository.delete(1L)
            }
            assertEquals(0, deleted)
        }

    }


    /**************************************
     *           Helper Functions         *
     **************************************/

    suspend fun populateQuestions(size: Int) {
        val now = Instant.now(clock)
        for (i in 1..size) {
            repository.create(Question(0L, "BlahBlahBlah$i", now, now))
        }
    }


    companion object {
        @Container
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("test_db")
            withUsername("test_user")
            withPassword("test_password")
        }
    }

    private lateinit var repository: QuestionRepositoryImpl
    private lateinit var clock: Clock
    private val fixedInstant = Instant.parse("2024-01-15T10:00:00Z")

    @BeforeAll
    fun setup() {
        Database.connect(
            url = postgresContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgresContainer.username,
            password = postgresContainer.password
        )

        transaction {
            SchemaUtils.create(Questions)
        }
    }

    @BeforeEach
    fun setupEach() {
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
        repository = QuestionRepositoryImpl(clock)

        transaction {
            Questions.deleteAll()
        }
    }

    @AfterAll
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Questions)
        }
    }

}