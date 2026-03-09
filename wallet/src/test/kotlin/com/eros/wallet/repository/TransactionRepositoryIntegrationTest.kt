package com.eros.wallet.repository

import com.eros.database.dbQuery
import com.eros.dates.tables.DateCommitments
import com.eros.users.models.*
import com.eros.users.repository.UserRepositoryImpl
import com.eros.users.table.Users
import com.eros.wallet.models.Transaction
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.models.Wallet
import com.eros.wallet.table.Transactions
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionRepositoryImplTest {

    private lateinit var transactionRepository: TransactionRepositoryImpl
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
            SchemaUtils.create(Users, Wallets, Transactions, DateCommitments)
        }
    }

    @BeforeEach
    fun setupEach() {
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
        transactionRepository = TransactionRepositoryImpl(clock)
        walletRepository = WalletRepositoryImpl(clock)
        userRepository = UserRepositoryImpl(clock)
        transaction {
            DateCommitments.deleteAll()
            Transactions.deleteAll()
            Wallets.deleteAll()
            Users.deleteAll()
        }
    }

    @Nested
    inner class CreateTests {

        @Test
        fun `create should insert transaction and return it`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(userId = "user-1", transactionId = 0L)

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            assertNotNull(created)
            assertTrue(created.transactionId > 0)
            assertEquals("user-1", created.userId)
            assertEquals(TransactionType.PURCHASE, created.type)
            assertEquals(100.0, created.amount)
            assertEquals(200.0, created.balanceAfter)
        }

        @Test
        fun `create should set timestamp from clock`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(userId = "user-1", transactionId = 0L)

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            assertEquals(fixedInstant, created.createdAt)
        }

        @Test
        fun `create with all transaction types should work`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")

            val purchase = testTransaction(userId = "user-1", type = TransactionType.PURCHASE)
            val spend = testTransaction(userId = "user-1", type = TransactionType.SPEND)
            val refund = testTransaction(userId = "user-1", type = TransactionType.REFUND)
            val adjustment = testTransaction(userId = "user-1", type = TransactionType.ADJUSTMENT)

            
            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(purchase)
                transactionRepository.create(spend)
                transactionRepository.create(refund)
                transactionRepository.create(adjustment)
            }

            val all = dbQuery { transactionRepository.findAll() }

            
            assertEquals(4, all.size)
            assertTrue(all.any { it.type == TransactionType.PURCHASE })
            assertTrue(all.any { it.type == TransactionType.SPEND })
            assertTrue(all.any { it.type == TransactionType.REFUND })
            assertTrue(all.any { it.type == TransactionType.ADJUSTMENT })
        }

        @Test
        fun `create with all transaction statuses should work`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")

            val pending = testTransaction(userId = "user-1", status = TransactionStatus.PENDING)
            val completed = testTransaction(userId = "user-1", status = TransactionStatus.COMPLETED)
            val failed = testTransaction(userId = "user-1", status = TransactionStatus.FAILED)
            val cancelled = testTransaction(userId = "user-1", status = TransactionStatus.CANCELLED)

            
            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(pending)
                transactionRepository.create(completed)
                transactionRepository.create(failed)
                transactionRepository.create(cancelled)
            }

            val all = dbQuery { transactionRepository.findAll() }

            
            assertEquals(4, all.size)
            assertTrue(all.any { it.status == TransactionStatus.PENDING })
            assertTrue(all.any { it.status == TransactionStatus.COMPLETED })
            assertTrue(all.any { it.status == TransactionStatus.FAILED })
            assertTrue(all.any { it.status == TransactionStatus.CANCELLED })
        }

        @Test
        fun `create with metadata should serialize correctly`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val metadata = mapOf(
                "promo_code" to "SUMMER20",
                "ip_address" to "192.168.1.1"
            )
            val transaction = testTransaction(userId = "user-1", metadata = metadata)

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            assertEquals(2, created.metadata.size)
            assertEquals("SUMMER20", created.metadata["promo_code"])
            assertEquals("192.168.1.1", created.metadata["ip_address"])
        }

        @Test
        fun `create with empty metadata should work`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(userId = "user-1", metadata = emptyMap())

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            assertTrue(created.metadata.isEmpty())
        }

        @Test
        fun `create with stripe payment intent should work`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(
                userId = "user-1",
                stripePaymentIntentId = "pi_test123",
                amountPaidGBP = 25.00
            )

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            assertEquals("pi_test123", created.stripePaymentIntentId)
            assertEquals(25.00, created.amountPaidGBP)
        }

        @Test
        fun `create with idempotency key should work`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(
                userId = "user-1",
                idempotencyKey = "idem-key-123"
            )

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            assertEquals("idem-key-123", created.idempotencyKey)
        }
    }

    @Nested
    inner class FindByIdTests {

        @Test
        fun `findById should return transaction when exists`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(userId = "user-1")

            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            val found = dbQuery { transactionRepository.findById(created.transactionId) }

            
            assertNotNull(found)
            assertEquals(created.transactionId, found.transactionId)
            assertEquals("user-1", found.userId)
        }

        @Test
        fun `findById should return null when transaction does not exist`() = runBlocking {
            
            val found = dbQuery { transactionRepository.findById(999L) }

            
            assertNull(found)
        }

        @Test
        fun `findById should return correct transaction when multiple exist`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val tx1 = testTransaction(userId = "user-1", amount = 100.0)
            val tx2 = testTransaction(userId = "user-1", amount = 200.0)
            val tx3 = testTransaction(userId = "user-1", amount = 300.0)

            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                listOf(
                    transactionRepository.create(tx1),
                    transactionRepository.create(tx2),
                    transactionRepository.create(tx3)
                )
            }

            
            val found = dbQuery { transactionRepository.findById(created[1].transactionId) }

            
            assertNotNull(found)
            assertEquals(200.0, found.amount)
        }
    }

    @Nested
    inner class FindAllTests {

        @Test
        fun `findAll should return all transactions`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val tx1 = testTransaction(userId = "user-1")
            val tx2 = testTransaction(userId = "user-1")
            val tx3 = testTransaction(userId = "user-1")

            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(tx1)
                transactionRepository.create(tx2)
                transactionRepository.create(tx3)
            }

            
            val transactions = dbQuery { transactionRepository.findAll() }

            
            assertEquals(3, transactions.size)
        }

        @Test
        fun `findAll should return empty list when no transactions exist`() = runBlocking {
            
            val transactions = dbQuery { transactionRepository.findAll() }

            
            assertTrue(transactions.isEmpty())
        }
    }

    @Nested
    inner class FindByUserIdTests {

        @Test
        fun `findByUserId should return all transactions for user`() = runBlocking {
            
            val user1 = testUser("user-1")
            val user2 = testUser("user-2", email = "test@test1.com")
            val wallet1 = testWallet("user-1")
            val wallet2 = testWallet("user-2")

            dbQuery {
                userRepository.create(user1)
                userRepository.create(user2)
                walletRepository.create(wallet1)
                walletRepository.create(wallet2)
                transactionRepository.create(testTransaction(userId = "user-1"))
                transactionRepository.create(testTransaction(userId = "user-1"))
                transactionRepository.create(testTransaction(userId = "user-2"))
            }

            
            val user1Transactions = dbQuery { transactionRepository.findByUserId("user-1") }

            
            assertEquals(2, user1Transactions.size)
            assertTrue(user1Transactions.all { it?.userId == "user-1" })
        }

        @Test
        fun `findByUserId should return empty list when user has no transactions`() = runBlocking {
            
            val transactions = dbQuery { transactionRepository.findByUserId("user-1") }

            
            assertTrue(transactions.isEmpty())
        }

        @Test
        fun `findByUserId should return transactions in correct order`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")

            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(testTransaction(userId = "user-1", amount = 100.0))
                transactionRepository.create(testTransaction(userId = "user-1", amount = 200.0))
                transactionRepository.create(testTransaction(userId = "user-1", amount = 300.0))
            }

            
            val transactions = dbQuery { transactionRepository.findByUserId("user-1") }

            
            assertEquals(3, transactions.size)
        }
    }

    @Nested
    inner class FindByIdempotencyKeyTests {

        @Test
        fun `findByIdempotencyKey should return transaction when exists`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(
                userId = "user-1",
                idempotencyKey = "idem-key-123"
            )

            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            val found = dbQuery { transactionRepository.findByIdempotencyKey("idem-key-123") }

            
            assertNotNull(found)
            assertEquals("idem-key-123", found.idempotencyKey)
            assertEquals("user-1", found.userId)
        }

        @Test
        fun `findByIdempotencyKey should return null when key does not exist`() = runBlocking {
            
            val found = dbQuery { transactionRepository.findByIdempotencyKey("nonexistent") }

            
            assertNull(found)
        }

        @Test
        fun `findByIdempotencyKey should return correct transaction when multiple exist`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")

            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(testTransaction(userId = "user-1", idempotencyKey = "key-1"))
                transactionRepository.create(testTransaction(userId = "user-1", idempotencyKey = "key-2"))
                transactionRepository.create(testTransaction(userId = "user-1", idempotencyKey = "key-3"))
            }

            
            val found = dbQuery { transactionRepository.findByIdempotencyKey("key-2") }

            
            assertNotNull(found)
            assertEquals("key-2", found.idempotencyKey)
        }

        @Test
        fun `findByIdempotencyKey should work with null idempotency key`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(userId = "user-1", idempotencyKey = null)

            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            val found = dbQuery { transactionRepository.findByIdempotencyKey("some-key") }

            
            assertNull(found)
        }
    }

    @Nested
    inner class UpdateTests {

        @Test
        fun `update should modify transaction and return updated version`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val original = testTransaction(userId = "user-1", status = TransactionStatus.PENDING)

            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(original)
            }

            val updated = created.copy(status = TransactionStatus.COMPLETED)

            
            val result = dbQuery { transactionRepository.update(created.transactionId, updated) }

            
            assertNotNull(result)
            assertEquals(TransactionStatus.COMPLETED, result.status)
        }

        @Test
        fun `update should return null when transaction does not exist`() = runBlocking {
            
            val transaction = testTransaction(userId = "user-1", transactionId = 999L)

            
            val result = dbQuery { transactionRepository.update(999L, transaction) }

            
            assertNull(result)
        }

        @Test
        fun `update should persist changes to database`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val original = testTransaction(userId = "user-1", amount = 100.0)

            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(original)
            }

            val updated = created.copy(amount = 200.0)

            
            dbQuery { transactionRepository.update(created.transactionId, updated) }
            val found = dbQuery { transactionRepository.findById(created.transactionId) }

            
            assertEquals(200.0, found?.amount)
        }
    }

    @Nested
    inner class DeleteTests {

        @Test
        fun `delete should remove transaction and return 1`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(userId = "user-1")

            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            val deleted = dbQuery { transactionRepository.delete(created.transactionId) }

            
            assertEquals(1, deleted)
            val found = dbQuery { transactionRepository.findById(created.transactionId) }
            assertNull(found)
        }

        @Test
        fun `delete should return 0 when transaction does not exist`() = runBlocking {
            
            val deleted = dbQuery { transactionRepository.delete(999L) }

            
            assertEquals(0, deleted)
        }

        @Test
        fun `delete should not affect other transactions`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val tx1 = testTransaction(userId = "user-1")
            val tx2 = testTransaction(userId = "user-1")

            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                listOf(
                    transactionRepository.create(tx1),
                    transactionRepository.create(tx2)
                )
            }

            
            dbQuery { transactionRepository.delete(created[0].transactionId) }

            
            val found1 = dbQuery { transactionRepository.findById(created[0].transactionId) }
            val found2 = dbQuery { transactionRepository.findById(created[1].transactionId) }
            assertNull(found1)
            assertNotNull(found2)
            Unit
        }
    }

    @Nested
    inner class DoesExistTests {

        @Test
        fun `doesExist should return true when transaction exists`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(userId = "user-1")

            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            val exists = dbQuery { transactionRepository.doesExist(created.transactionId) }

            
            assertTrue(exists)
        }

        @Test
        fun `doesExist should return false when transaction does not exist`() = runBlocking {
            
            val exists = dbQuery { transactionRepository.doesExist(999L) }

            
            assertFalse(exists)
        }

        @Test
        fun `doesExist should return false after transaction is deleted`() = runBlocking {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(userId = "user-1")

            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            dbQuery { transactionRepository.delete(created.transactionId) }
            val exists = dbQuery { transactionRepository.doesExist(created.transactionId) }

            
            assertFalse(exists)
        }
    }

    // Helper functions
    private fun testTransaction(
        transactionId: Long = 0L,
        userId: String = "user-123",
        type: TransactionType = TransactionType.PURCHASE,
        amount: Double = 100.0,
        balanceAfter: Double = 200.0,
        description: String = "Test transaction",
        status: TransactionStatus = TransactionStatus.COMPLETED,
        relatedDateId: Long? = null,
        relatedTransactionId: Long? = null,
        stripePaymentIntentId: String? = null,
        amountPaidGBP: Double? = null,
        idempotencyKey: String? = null,
        metadata: Map<String, String> = emptyMap()
    ) = Transaction(
        transactionId = transactionId,
        userId = userId,
        type = type,
        amount = amount,
        balanceAfter = balanceAfter,
        description = description,
        status = status,
        relatedDateId = relatedDateId,
        relatedTransactionId = relatedTransactionId,
        stripePaymentIntentId = stripePaymentIntentId,
        amountPaidGBP = amountPaidGBP,
        idempotencyKey = idempotencyKey,
        metadata = metadata,
        createdAt = fixedInstant
    )

    private fun testWallet(
        userId: String = "user-123",
        tokenBalance: Double = 100.0,
        lifetimeSpent: Double = 50.0,
        lifetimePurchased: Double = 150.0,
        currency: String = "GBP"
    ) = Wallet(
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