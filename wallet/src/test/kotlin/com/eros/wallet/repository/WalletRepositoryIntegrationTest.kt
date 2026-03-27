package com.eros.wallet.repository

import com.eros.database.dbQuery
import com.eros.users.models.AlcoholConsumption
import com.eros.users.models.Badge
import com.eros.users.models.BodyAttribute
import com.eros.users.models.BrainAttribute
import com.eros.users.models.DateIntentions
import com.eros.users.models.Diet
import com.eros.users.models.DisplayableField
import com.eros.users.models.EducationLevel
import com.eros.users.models.Ethnicity
import com.eros.users.models.Gender
import com.eros.users.models.KidsPreference
import com.eros.users.models.Language
import com.eros.users.models.PoliticalView
import com.eros.users.models.ProfileStatus
import com.eros.users.models.Pronouns
import com.eros.users.models.RelationshipType
import com.eros.users.models.Religion
import com.eros.users.models.Role
import com.eros.users.models.SexualOrientation
import com.eros.users.models.SmokingStatus
import com.eros.users.models.StarSign
import com.eros.users.models.Trait
import com.eros.users.models.User
import com.eros.users.models.ValidationStatus
import com.eros.users.repository.UserRepositoryImpl
import com.eros.users.table.Users
import com.eros.wallet.models.Wallet
import com.eros.wallet.table.Wallets
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WalletRepositoryImplTest {

    private lateinit var walletRepository: WalletRepositoryImpl
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var clock: Clock
    private val fixedInstant = Instant.parse("2024-01-15T10:00:00Z")

    companion object {
        @Container
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("test_db")
            withUsername("test_user")
            withPassword("test_password")
        }
    }

    @BeforeAll
    fun setup() {
        Database.connect(
            url = postgresContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgresContainer.username,
            password = postgresContainer.password
        )

        transaction {
            SchemaUtils.create(Users, Wallets)
        }
    }

    @BeforeEach
    fun setupEach() {
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
        walletRepository = WalletRepositoryImpl(clock)
        userRepository = UserRepositoryImpl(clock)
        transaction {
            Wallets.deleteAll()
            Users.deleteAll()
        }
    }

    @Nested
    inner class CreateTests {

        @Test
        fun `create should insert wallet and return it`() = runBlocking {
            
            val wallet = testWallet(userId = "user-1")
            val user = testUser(wallet.userId)

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
            }

            
            assertNotNull(created)
            assertEquals("user-1", created.userId)
            assertEquals(BigDecimal("100.00"), created.tokenBalance)
            assertEquals(BigDecimal("50.00"), created.lifetimeSpent)
            assertEquals(BigDecimal("150.00"), created.lifetimePurchased)
            assertEquals("GBP", created.currency)
        }

        @Test
        fun `create should set timestamps from entity`() = runBlocking {
            
            val wallet = testWallet()
            val user = testUser(wallet.userId)

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
            }

            
            assertEquals(fixedInstant, created.createdAt)
            assertEquals(fixedInstant, created.updatedAt)
        }

        @Test
        fun `create with default currency should use GBP`() = runBlocking {
            
            val wallet = testWallet(currency = "GBP")
            val user = testUser(wallet.userId)

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
            }

            
            assertEquals("GBP", created.currency)
        }

        @Test
        fun `create with different currencies should work`() = runBlocking {
            
            val usdWallet = testWallet(userId = "user-usd", currency = "USD")
            val eurWallet = testWallet(userId = "user-eur", currency = "EUR" , walletId = 2L)
            val userUsd = testUser(usdWallet.userId, email = "awd@awddwa.com")
            val userEur = testUser(eurWallet.userId)

            
            dbQuery {
                userRepository.create(userUsd)
                userRepository.create(userEur)
                walletRepository.create(usdWallet)
                walletRepository.create(eurWallet)
            }

            val foundUsd = dbQuery { walletRepository.findById("user-usd") }
            val foundEur = dbQuery { walletRepository.findById("user-eur") }

            
            assertEquals("USD", foundUsd?.currency)
            assertEquals("EUR", foundEur?.currency)
        }

        @Test
        fun `create with zero balance should work`() = runBlocking {
            
            val wallet = testWallet(tokenBalance = 0.0.toBigDecimal(), lifetimeSpent = 0.0.toBigDecimal(), lifetimePurchased = 0.0.toBigDecimal())
            val user = testUser(wallet.userId)

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
            }

            
            assertEquals(BigDecimal("0.00"), created.tokenBalance)
            assertEquals(BigDecimal("0.00"), created.lifetimeSpent)
            assertEquals(BigDecimal("0.00"), created.lifetimePurchased)
        }

        @Test
        fun `create with decimal values should preserve precision`() = runBlocking {
            
            val wallet = testWallet(
                tokenBalance = 123.45.toBigDecimal(),
                lifetimeSpent = 67.89.toBigDecimal(),
                lifetimePurchased = 191.34.toBigDecimal()
            )
            val user = testUser(wallet.userId)

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
            }

            
            assertEquals(123.45.toBigDecimal(), created.tokenBalance)
            assertEquals(67.89.toBigDecimal(), created.lifetimeSpent)
            assertEquals(191.34.toBigDecimal(), created.lifetimePurchased)
        }
    }

    @Nested
    inner class FindByIdTests {

        @Test
        fun `findById should return wallet when exists`() = runBlocking {
            
            val wallet = testWallet(userId = "user-1")
            val user = testUser(wallet.userId)
            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
            }

            
            val found = dbQuery { walletRepository.findById("user-1") }

            
            assertNotNull(found)
            assertEquals("user-1", found.userId)
            assertEquals(BigDecimal("100.00"), found.tokenBalance)
        }

        @Test
        fun `findById should return null when wallet does not exist`() = runBlocking {
            
            val found = dbQuery { walletRepository.findById("nonexistent-user") }

            
            assertNull(found)
        }

        @Test
        fun `findById should return correct wallet when multiple exist`() = runBlocking {
            
            val wallet1 = testWallet(userId = "user-1", tokenBalance = 100.0.toBigDecimal())
            val wallet2 = testWallet(userId = "user-2", tokenBalance = 200.0.toBigDecimal(), walletId = 2L)
            val wallet3 = testWallet(userId = "user-3", tokenBalance = 300.0.toBigDecimal(), walletId = 3L)
            val user1 = testUser("user-1")
            val user2 = testUser("user-2", email = "test@test1.com")
            val user3 = testUser("user-3", email = "test@test2.com")

            dbQuery {
                userRepository.create(user1)
                userRepository.create(user2)
                userRepository.create(user3)
                walletRepository.create(wallet1)
                walletRepository.create(wallet2)
                walletRepository.create(wallet3)
            }

            
            val found = dbQuery { walletRepository.findById("user-2") }

            
            assertNotNull(found)
            assertEquals("user-2", found.userId)
            assertEquals(BigDecimal("200.00"), found.tokenBalance)
        }
    }

    @Nested
    inner class FindAllTests {

        @Test
        fun `findAll should return all wallets`() = runBlocking {
            
            val wallet1 = testWallet(userId = "user-1")
            val wallet2 = testWallet(userId = "user-2", walletId = 2L)
            val wallet3 = testWallet(userId = "user-3", walletId = 3L)
            val user1 = testUser("user-1")
            val user2 = testUser("user-2", email = "test@test1.com")
            val user3 = testUser("user-3", email = "test@test2.com")

            dbQuery {
                userRepository.create(user1)
                userRepository.create(user2)
                userRepository.create(user3)
                walletRepository.create(wallet1)
                walletRepository.create(wallet2)
                walletRepository.create(wallet3)
            }

            
            val wallets = dbQuery { walletRepository.findAll() }

            
            assertEquals(3, wallets.size)
            assertTrue(wallets.any { it.userId == "user-1" })
            assertTrue(wallets.any { it.userId == "user-2" })
            assertTrue(wallets.any { it.userId == "user-3" })
        }

        @Test
        fun `findAll should return empty list when no wallets exist`() = runBlocking {
            
            val wallets = dbQuery { walletRepository.findAll() }

            
            assertTrue(wallets.isEmpty())
        }
    }

    @Nested
    inner class UpdateTests {

        @Test
        fun `update should modify wallet and return updated version`() = runBlocking {
            
            val original = testWallet(userId = "user-1", tokenBalance = 100.0.toBigDecimal())
            val user = testUser(original.userId)
            dbQuery {
                userRepository.create(user)
                walletRepository.create(original)
            }

            val updated = original.copy(
                tokenBalance = 150.0.toBigDecimal(),
                lifetimeSpent = 75.0.toBigDecimal()
            )

            
            val result = dbQuery { walletRepository.update("user-1", updated) }

            
            assertNotNull(result)
            assertEquals(BigDecimal("150.00"), result.tokenBalance)
            assertEquals(BigDecimal("75.00"), result.lifetimeSpent)
        }

        @Test
        fun `update should return null when wallet does not exist`() = runBlocking {
            
            val wallet = testWallet(userId = "nonexistent")

            
            val result = dbQuery { walletRepository.update("nonexistent", wallet) }

            
            assertNull(result)
        }

        @Test
        fun `update should persist changes to database`() = runBlocking {
            
            val original = testWallet(userId = "user-1", tokenBalance = 100.0.toBigDecimal())
            val user = testUser(original.userId)
            dbQuery {
                userRepository.create(user)
                walletRepository.create(original)
            }

            val updated = original.copy(tokenBalance = 200.0.toBigDecimal())

            
            dbQuery { walletRepository.update("user-1", updated) }
            val found = dbQuery { walletRepository.findById("user-1") }

            
            assertEquals(BigDecimal("200.00"), found?.tokenBalance)
        }

        @Test
        fun `update should update all fields correctly`() = runBlocking {
            
            val original = testWallet(userId = "user-1")
            val user = testUser(original.userId)
            dbQuery {
                userRepository.create(user)
                walletRepository.create(original)
            }

            val updated = original.copy(
                tokenBalance = 250.0.toBigDecimal(),
                lifetimeSpent = 125.0.toBigDecimal(),
                lifetimePurchased = 375.0.toBigDecimal(),
                currency = "USD"
            )

            
            val result = dbQuery { walletRepository.update("user-1", updated) }

            
            assertNotNull(result)
            assertEquals(BigDecimal("250.00"), result.tokenBalance)
            assertEquals(BigDecimal("125.00"), result.lifetimeSpent)
            assertEquals(BigDecimal("375.00"), result.lifetimePurchased)
            assertEquals("USD", result.currency)
        }
    }

    @Nested
    inner class UpdateBalanceTests {

        @Test
        fun `updateBalance should update only balance and timestamp`() = runBlocking {
            
            val wallet = testWallet(
                userId = "user-1",
                tokenBalance = 100.0.toBigDecimal(),
                lifetimeSpent = 50.0.toBigDecimal()
            )
            val user = testUser(wallet.userId)
            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
            }

            
            val updated = dbQuery { walletRepository.updateBalance("user-1", 150.0.toBigDecimal(), wallet.lifetimeSpent) }

            
            assertNotNull(updated)
            assertEquals(BigDecimal("150.00"), updated.tokenBalance)
            assertEquals(BigDecimal("50.00"), updated.lifetimeSpent)
        }

        @Test
        fun `updateBalance should update timestamp`() = runBlocking {
            
            val wallet = testWallet(userId = "user-1")
            val user = testUser(wallet.userId)
            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
            }

            
            val updated = dbQuery { walletRepository.updateBalance("user-1", 75.0.toBigDecimal(), 50.0.toBigDecimal()) }

            
            assertNotNull(updated)
            assertEquals(fixedInstant, updated.updatedAt)
        }

        @Test
        fun `updateBalance should return null when wallet does not exist`() = runBlocking {
            
            val result = dbQuery { walletRepository.updateBalance("nonexistent", BigDecimal("100.00"), BigDecimal("200.00")) }

            
            assertNull(result)
        }

        @Test
        fun `updateBalance should handle zero balance`() = runBlocking {
            val wallet = testWallet(userId = "user-1", tokenBalance = BigDecimal("100.00"))
            val user = testUser(wallet.userId)
            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
            }
            
            val updated = dbQuery { walletRepository.updateBalance("user-1", 0.0.toBigDecimal(),50.0.toBigDecimal()) }

            assertNotNull(updated)
            assertEquals(BigDecimal("0.00"), updated.tokenBalance)
        }

        @Test
        fun `updateBalance should handle decimal values`() = runBlocking {
            
            val wallet = testWallet(userId = "user-1")
            val user = testUser(wallet.userId)
            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
            }

            
            val updated = dbQuery { walletRepository.updateBalance("user-1", 123.45.toBigDecimal(), 50.0.toBigDecimal()) }

            
            assertNotNull(updated)
            assertEquals(123.45.toBigDecimal(), updated.tokenBalance)
        }

        @Test
        fun `updateBalance should persist changes to database`() = runBlocking {
            
            val wallet = testWallet(userId = "user-1", tokenBalance = 100.0.toBigDecimal())
            val user = testUser(wallet.userId)
            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
            }

            
            dbQuery { walletRepository.updateBalance("user-1", 200.0.toBigDecimal(), 150.0.toBigDecimal()) }
            val found = dbQuery { walletRepository.findById("user-1") }

            
            assertEquals(BigDecimal("200.00"), found?.tokenBalance)
        }
    }

    @Nested
    inner class DeleteTests {

        @Test
        fun `delete should remove wallet and return 1`() = runBlocking {
            
            val wallet = testWallet(userId = "user-1")
            val user = testUser(wallet.userId)
            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
            }

            
            val deleted = dbQuery { walletRepository.delete("user-1") }

            
            assertEquals(1, deleted)
            assertNull(dbQuery { walletRepository.findById("user-1") })
        }

        @Test
        fun `delete should return 0 when wallet does not exist`() = runBlocking {
            
            val deleted = dbQuery { walletRepository.delete("nonexistent") }

            
            assertEquals(0, deleted)
        }

        @Test
        fun `delete should not affect other wallets`() : Unit = runBlocking {
            val wallet1 = testWallet(userId = "user-1")
            val wallet2 = testWallet(userId = "user-2", walletId = 2L)
            val user1 = testUser("user-1")
            val user2 = testUser("user-2", email = "test@test1.com")
            dbQuery {
                userRepository.create(user1)
                userRepository.create(user2)
                walletRepository.create(wallet1)
                walletRepository.create(wallet2)
            }

            val deleted = dbQuery { walletRepository.delete("user-1") }

            assertEquals(1, deleted)
            assertNull(dbQuery { walletRepository.findById("user-1") })
            assertNotNull(dbQuery { walletRepository.findById("user-2") })
        }
    }

    @Nested
    inner class DoesExistTests {

        @Test
        fun `doesExist should return true when wallet exists`() = runBlocking {
            
            val wallet = testWallet(userId = "user-1")
            val user = testUser(wallet.userId)
            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
            }

            
            val exists = dbQuery { walletRepository.doesExist("user-1") }

            
            assertTrue(exists)
        }

        @Test
        fun `doesExist should return false when wallet does not exist`() = runBlocking {
            
            val exists = dbQuery { walletRepository.doesExist("nonexistent") }

            
            assertFalse(exists)
        }

        @Test
        fun `doesExist should return false after wallet is deleted`() = runBlocking {
            
            val wallet = testWallet(userId = "user-1")
            val user = testUser(wallet.userId)
            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
            }

            
            dbQuery { walletRepository.delete("user-1") }
            val exists = dbQuery { walletRepository.doesExist("user-1") }

            
            assertFalse(exists)
        }
    }

    // Helper function to create test wallet
    private fun testWallet(
        userId: String = "user-123",
        walletId : Long = 1L,
        tokenBalance: BigDecimal = 100.0.toBigDecimal(),
        lifetimeSpent: BigDecimal = 50.0.toBigDecimal(),
        lifetimePurchased: BigDecimal = 150.0.toBigDecimal(),
        currency: String = "GBP"
    ) = Wallet(
        walletId = walletId,
        userId = userId,
        tokenBalance = tokenBalance,
        lifetimeSpent = lifetimeSpent,
        lifetimePurchased = lifetimePurchased,
        currency = currency,
        createdAt = fixedInstant,
        updatedAt = fixedInstant
    )

    private fun testUser(
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
        deletedAt: Instant? = null,
        profileStatus: ProfileStatus = ProfileStatus.ACTIVE,
        eloScore: Int = 1000,
        completeness: Int = 50,
        coordinatesLongitude: Double = -0.102,
        coordinatesLatitude: Double = 51.503,
        badges: Set<Badge> = setOf(),
        role: Role = Role.USER,
        photoValidationStatus: ValidationStatus = ValidationStatus.VALIDATED
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
            deletedAt = deletedAt,
            profileStatus = profileStatus,
            eloScore = eloScore,
            badges = badges,
            profileCompleteness = completeness,
            coordinatesLongitude = coordinatesLongitude,
            coordinatesLatitude = coordinatesLatitude,
            role = role,
            photoValidationStatus = photoValidationStatus
        )
    }
}