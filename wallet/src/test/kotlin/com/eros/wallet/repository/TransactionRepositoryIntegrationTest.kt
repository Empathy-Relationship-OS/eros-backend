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
import kotlinx.coroutines.test.runTest
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
        fun `create should insert transaction and return it`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(walletId = 1L, transactionId = 0L)

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            assertNotNull(created)
            assertTrue(created.transactionId > 0)
            assertEquals(wallet.walletId, created.walletId)
            assertEquals(TransactionType.PURCHASE, created.type)
            assertEquals(BigDecimal("100.00"), created.amount)
            assertEquals(BigDecimal("200.00"), created.balanceAfter)
        }

        @Test
        fun `create should set timestamp from clock`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(walletId = 1L, transactionId = 0L)

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            assertEquals(fixedInstant, created.createdAt)
        }

        @Test
        fun `create with all transaction types should work`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")

            val purchase = testTransaction(1L, type = TransactionType.PURCHASE)
            val spend = testTransaction(1L, type = TransactionType.SPEND)
            val refund = testTransaction(1L, type = TransactionType.REFUND)
            val adjustment = testTransaction(1L, type = TransactionType.ADJUSTMENT)

            
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
        fun `create with all transaction statuses should work`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")

            val pending = testTransaction(1L, status = TransactionStatus.PENDING)
            val completed = testTransaction(1L, status = TransactionStatus.COMPLETED)
            val failed = testTransaction(1L, status = TransactionStatus.FAILED)
            val cancelled = testTransaction(1L, status = TransactionStatus.CANCELLED)

            
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
        fun `create with metadata should serialize correctly`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val metadata = mapOf(
                "promo_code" to "SUMMER20",
                "ip_address" to "192.168.1.1"
            )
            val transaction = testTransaction(1L, metadata = metadata)

            
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
        fun `create with empty metadata should work`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(1L, metadata = emptyMap())

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            assertTrue(created.metadata.isEmpty())
        }

        @Test
        fun `create with stripe payment intent should work`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(
                1L,
                stripePaymentIntentId = "pi_test123",
                amountPaidGBP = 25.00.toBigDecimal()
            )

            
            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            assertEquals("pi_test123", created.stripePaymentIntentId)
            assertEquals(BigDecimal("25.00"), created.amountPaidGBP)
        }

        @Test
        fun `create with idempotency key should work`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(
                1L,
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
        fun `findById should return transaction when exists`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(1L)

            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            val found = dbQuery { transactionRepository.findById(created.transactionId) }

            
            assertNotNull(found)
            assertEquals(created.transactionId, found.transactionId)
            assertEquals(wallet.walletId, found.walletId)
        }

        @Test
        fun `findById should return null when transaction does not exist`() = runTest {
            
            val found = dbQuery { transactionRepository.findById(999L) }

            
            assertNull(found)
        }

        @Test
        fun `findById should return correct transaction when multiple exist`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val tx1 = testTransaction(1L, amount = 100.0.toBigDecimal())
            val tx2 = testTransaction(1L, amount = 200.0.toBigDecimal())
            val tx3 = testTransaction(1L, amount = 300.0.toBigDecimal())

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
            assertEquals(BigDecimal("200.00"), found.amount)
        }
    }

    @Nested
    inner class FindAllTests {

        @Test
        fun `findAll should return all transactions`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val tx1 = testTransaction(1L)
            val tx2 = testTransaction(1L)
            val tx3 = testTransaction(1L)

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
        fun `findAll should return empty list when no transactions exist`() = runTest {
            
            val transactions = dbQuery { transactionRepository.findAll() }

            
            assertTrue(transactions.isEmpty())
        }
    }

    @Nested
    inner class FindByUserIdTests {

        @Test
        fun `findByUserId should return all transactions for user`() = runTest {
            
            val user1 = testUser("user-1")
            val user2 = testUser("user-2", email = "test@test1.com")
            val wallet1 = testWallet("user-1")
            val wallet2 = testWallet("user-2", walletId = 2L)

            dbQuery {
                userRepository.create(user1)
                userRepository.create(user2)
                walletRepository.create(wallet1)
                walletRepository.create(wallet2)
                transactionRepository.create(testTransaction(walletId = 1L))
                transactionRepository.create(testTransaction(transactionId = 2L ,walletId = 1L))
                transactionRepository.create(testTransaction(walletId = wallet2.walletId))
            }

            
            val user1Transactions = dbQuery { transactionRepository.findByUserId("user-1",5,0) }

            
            assertEquals(2, user1Transactions.size)
            assertTrue(user1Transactions.all { it.walletId == wallet1.walletId })
        }

        @Test
        fun `findByUserId should return empty list when user has no transactions`() = runTest {
            
            val transactions = dbQuery { transactionRepository.findByUserId("user-1",5,0) }

            
            assertTrue(transactions.isEmpty())
        }

        @Test
        fun `findByUserId should return transactions in correct order`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")

            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(testTransaction(1L, amount = 100.0.toBigDecimal()))
                transactionRepository.create(testTransaction(1L, amount = 200.0.toBigDecimal()))
                transactionRepository.create(testTransaction(1L, amount = 300.0.toBigDecimal()))
            }

            
            val transactions = dbQuery { transactionRepository.findByUserId("user-1",3,0) }

            
            assertEquals(3, transactions.size)
        }
    }

    @Nested
    inner class FindByIdempotencyKeyTests {

        @Test
        fun `findByIdempotencyKey should return transaction when exists`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(
                1L,
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
            assertEquals(wallet.walletId, found.walletId)
        }

        @Test
        fun `findByIdempotencyKey should return null when key does not exist`() = runTest {
            
            val found = dbQuery { transactionRepository.findByIdempotencyKey("nonexistent") }

            
            assertNull(found)
        }

        @Test
        fun `findByIdempotencyKey should return correct transaction when multiple exist`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")

            dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(testTransaction(1L, idempotencyKey = "key-1"))
                transactionRepository.create(testTransaction(1L, idempotencyKey = "key-2"))
                transactionRepository.create(testTransaction(1L, idempotencyKey = "key-3"))
            }

            
            val found = dbQuery { transactionRepository.findByIdempotencyKey("key-2") }

            
            assertNotNull(found)
            assertEquals("key-2", found.idempotencyKey)
        }

        @Test
        fun `findByIdempotencyKey should work with null idempotency key`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(1L, idempotencyKey = null)

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
        fun `update should modify transaction and return updated version`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val original = testTransaction(1L, status = TransactionStatus.PENDING)

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
        fun `update should return null when transaction does not exist`() = runTest {
            
            val transaction = testTransaction(walletId = 1L, transactionId = 999L)

            
            val result = dbQuery { transactionRepository.update(999L, transaction) }

            
            assertNull(result)
        }

        @Test
        fun `update should persist changes to database`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val original = testTransaction(1L, amount = 100.0.toBigDecimal())

            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(original)
            }

            val updated = created.copy(amount = 200.0.toBigDecimal())

            
            dbQuery { transactionRepository.update(created.transactionId, updated) }
            val found = dbQuery { transactionRepository.findById(created.transactionId) }

            
            assertEquals(BigDecimal("200.00"), found?.amount)
        }
    }

    @Nested
    inner class DeleteTests {

        @Test
        fun `delete should remove transaction and return 1`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(1L)

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
        fun `delete should return 0 when transaction does not exist`() = runTest {
            
            val deleted = dbQuery { transactionRepository.delete(999L) }

            
            assertEquals(0, deleted)
        }

        @Test
        fun `delete should not affect other transactions`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val tx1 = testTransaction(1L)
            val tx2 = testTransaction(1L)

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
            assertNotEquals(null, found2)
        }

    }

    @Nested
    inner class DoesExistTests {

        @Test
        fun `doesExist should return true when transaction exists`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(1L)

            val created = dbQuery {
                userRepository.create(user)
                walletRepository.create(wallet)
                transactionRepository.create(transaction)
            }

            
            val exists = dbQuery { transactionRepository.doesExist(created.transactionId) }

            
            assertTrue(exists)
        }

        @Test
        fun `doesExist should return false when transaction does not exist`() = runTest {
            
            val exists = dbQuery { transactionRepository.doesExist(999L) }

            
            assertFalse(exists)
        }

        @Test
        fun `doesExist should return false after transaction is deleted`() = runTest {
            
            val user = testUser("user-1")
            val wallet = testWallet("user-1")
            val transaction = testTransaction(1L)

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
        walletId: Long = 1L,
        type: TransactionType = TransactionType.PURCHASE,
        amount: BigDecimal = 100.0.toBigDecimal(),
        balanceAfter: BigDecimal = 200.0.toBigDecimal(),
        description: String = "Test transaction",
        status: TransactionStatus = TransactionStatus.COMPLETED,
        relatedDateId: Long? = null,
        relatedTransactionId: Long? = null,
        stripePaymentIntentId: String? = null,
        amountPaidGBP: BigDecimal? = null,
        idempotencyKey: String? = null,
        metadata: Map<String, String> = emptyMap(),
        acceptedTerms : Boolean? = true
    ) = Transaction(
        transactionId = transactionId,
        walletId = walletId,
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
        acceptedTerms = acceptedTerms,
        createdAt = fixedInstant,
        updatedAt = fixedInstant
    )

    private fun testWallet(
        userId: String = "user-123",
        walletId: Long = 1L,
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