package com.eros.users.service

import com.eros.common.config.S3Config
import com.eros.users.models.Activity
import com.eros.users.models.AdminUpdateUserRequest
import com.eros.users.models.CreateUserRequest
import com.eros.users.models.Creative
import com.eros.users.models.DateIntentions
import com.eros.users.models.DisplayableField
import com.eros.users.models.EducationLevel
import com.eros.users.models.Entertainment
import com.eros.users.models.Ethnicity
import com.eros.users.models.Gender
import com.eros.users.models.Interest
import com.eros.users.models.KidsPreference
import com.eros.users.models.Language
import com.eros.users.models.MediaType
import com.eros.users.models.ProfileStatus
import com.eros.users.models.Question
import com.eros.users.models.UserMediaCollection
import com.eros.users.models.UserMediaItem
import com.eros.users.models.RelationshipType
import com.eros.users.models.SexualOrientation
import com.eros.users.models.SmokingStatus
import com.eros.users.models.Sport
import com.eros.users.models.Trait
import com.eros.users.models.UpdateUserRequest
import com.eros.users.models.UserQAItem
import com.eros.users.repository.PhotoRepository
import com.eros.users.repository.UserRepositoryImpl
import com.eros.users.table.Users
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {

    @Nested
    inner class `Update Visibility`{

        @Test
        fun `successfully update visibility to sleep`() {
            val user = createValidUserRequest()

            val (createdUser, updatedUser) = runBlocking {
                val created = service.createUser(user)
                val updated = service.updateUser(user.userId, UpdateUserRequest(setVisible = false))
                created to updated
            }

            assertNotEquals(createdUser.profileStatus, updatedUser?.profileStatus)
            assertEquals(ProfileStatus.SLEEP_MODE ,updatedUser?.profileStatus)
        }

        @Test
        fun `successfully update visibility to active`() {
            val user = createValidUserRequest()

            val (createdChanged, updatedUser) = runBlocking {
                val created = service.createUser(user)
                val createdChanged = service.adminUpdateUser(created.userId, AdminUpdateUserRequest(profileStatus = ProfileStatus.SLEEP_MODE))
                val updated = service.updateUser(user.userId, UpdateUserRequest(setVisible = true))
                createdChanged to updated
            }

            assertNotEquals(createdChanged?.profileStatus, updatedUser?.profileStatus)
            assertEquals(ProfileStatus.ACTIVE ,updatedUser?.profileStatus)
        }

        @Test
        fun `allow update when FROZEN`() {
            val user = createValidUserRequest()

            runBlocking {
                val created = service.createUser(user)
                service.adminUpdateUser(created.userId, AdminUpdateUserRequest(profileStatus = ProfileStatus.FROZEN))

                val updatedUser = runBlocking {
                    service.updateUser(created.userId, UpdateUserRequest(firstName = "Changed"))
                }

                assertEquals("Changed",updatedUser?.firstName)
            }
        }
    }

    @Nested
    inner class `Get Public Profile`{

        @Test
        fun `should return public profile with QAs`() {
            // Create two users in the database
            val requestingUser = createValidUserRequest("requesting-user-id", "requesting@example.com")
            val targetUser = createValidUserRequest("target-user-id", "target@example.com")

            runBlocking {
                service.createUser(requestingUser)
                service.createUser(targetUser)
            }

            // Create test QA data
            val testQAs = listOf(
                UserQAItem(
                    userId = "target-user-id",
                    question = Question(
                        questionId = 1L,
                        question = "What's your favorite hobby?",
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant
                    ),
                    answer = "I love hiking and photography",
                    displayOrder = 1,
                    createdAt = fixedInstant,
                    updatedAt = fixedInstant
                ),
                UserQAItem(
                    userId = "target-user-id",
                    question = Question(
                        questionId = 2L,
                        question = "What's your dream vacation?",
                        createdAt = fixedInstant,
                        updatedAt = fixedInstant
                    ),
                    answer = "Exploring the mountains of Nepal",
                    displayOrder = 2,
                    createdAt = fixedInstant,
                    updatedAt = fixedInstant
                )
            )

            // Stub the mocks
            coEvery { mockQAService.getAllUserQAs("target-user-id") } returns testQAs
            coEvery { mockPhotoRepository.findByUserId("target-user-id") } returns emptyList()

            // Call the method
            val result = runBlocking {
                service.getPublicProfile("requesting-user-id", "target-user-id")
            }

            // Verify the QAs are in the public profile
            assertEquals(2, result.profile.qas.size)
            assertEquals("What's your favorite hobby?", result.profile.qas[0].question)
            assertEquals("I love hiking and photography", result.profile.qas[0].answer)
            assertEquals(1, result.profile.qas[0].displayOrder)
            assertEquals("What's your dream vacation?", result.profile.qas[1].question)
            assertEquals("Exploring the mountains of Nepal", result.profile.qas[1].answer)
            assertEquals(2, result.profile.qas[1].displayOrder)
        }

        @Test
        fun `should return public profile with empty QAs list`() {
            // Create two users in the database
            val requestingUser = createValidUserRequest("requesting-user-id-2", "requesting2@example.com")
            val targetUser = createValidUserRequest("target-user-id-2", "target2@example.com")

            runBlocking {
                service.createUser(requestingUser)
                service.createUser(targetUser)
            }

            // Stub the mocks to return empty lists
            coEvery { mockQAService.getAllUserQAs("target-user-id-2") } returns emptyList()
            coEvery { mockPhotoRepository.findByUserId("target-user-id-2") } returns emptyList()

            // Call the method
            val result = runBlocking {
                service.getPublicProfile("requesting-user-id-2", "target-user-id-2")
            }

            // Verify the QAs list is empty
            assertEquals(0, result.profile.qas.size)
        }
    }

    @Nested
    inner class `Get Public Profile - CloudFront Signed URLs` {

        @Test
        fun `should throw IllegalStateException when CloudFront is not configured`() {
            // Create two users in the database
            val requestingUser = createValidUserRequest("requesting-cf-1", "requesting-cf-1@example.com")
            val targetUser = createValidUserRequest("target-cf-1", "target-cf-1@example.com")

            runBlocking {
                service.createUser(requestingUser)
                service.createUser(targetUser)
            }

            // Mock photo repository to return photos with S3 URLs
            val testPhotos = listOf(
                UserMediaItem(
                    id = 1L,
                    userId = "target-cf-1",
                    mediaUrl = "https://test-bucket.s3.eu-west-2.amazonaws.com/photos/user123/photo1.jpg",
                    thumbnailUrl = null,
                    mediaType = MediaType.PHOTO,
                    displayOrder = 1,
                    isPrimary = true,
                    createdAt = fixedInstant,
                    updatedAt = fixedInstant
                )
            )
            coEvery { mockPhotoRepository.findByUserId("target-cf-1") } returns testPhotos
            coEvery { mockQAService.getAllUserQAs("target-cf-1") } returns emptyList()

            // Call should throw because CloudFront is not configured in test setup
            val exception = runCatching {
                runBlocking {
                    service.getPublicProfile("requesting-cf-1", "target-cf-1")
                }
            }.exceptionOrNull()

            assertNotNull(exception, "Should throw exception when CloudFront not configured")
            assertEquals(
                IllegalStateException::class,
                exception!!::class,
                "Should throw IllegalStateException"
            )
        }
    }

    @Nested
    inner class `Get Public Profile - With CloudFront Enabled` {

        private lateinit var serviceWithCloudFront: UserService
        private lateinit var mockPhotoServiceWithCloudFront: PhotoService

        @BeforeEach
        fun setupCloudFront() {
            // This test verifies the integration would work if CloudFront was properly configured
            // In real scenarios, CloudFront configuration would generate signed URLs
            mockPhotoServiceWithCloudFront = mockk<PhotoService>()
            serviceWithCloudFront = UserService(
                userRepository,
                mockPhotoServiceWithCloudFront,
                mockQAService
            )
        }

        @Test
        fun `should generate CloudFront signed URLs for all photos`() {
            // Create two users in the database
            val requestingUser = createValidUserRequest("requesting-cf-2", "requesting-cf-2@example.com")
            val targetUser = createValidUserRequest("target-cf-2", "target-cf-2@example.com")

            runBlocking {
                serviceWithCloudFront.createUser(requestingUser)
                serviceWithCloudFront.createUser(targetUser)
            }

            // Mock S3 URLs (what's stored in the database)
            val s3Photo1 = "https://test-bucket.s3.eu-west-2.amazonaws.com/photos/user123/photo1.jpg"
            val s3Photo2 = "https://test-bucket.s3.eu-west-2.amazonaws.com/photos/user123/photo2.jpg"
            val s3Thumb1 = "https://test-bucket.s3.eu-west-2.amazonaws.com/photos/user123/thumb1.jpg"

            // Mock CloudFront signed URLs (what should be returned)
            val signedPhoto1 = "https://d111111abcdef8.cloudfront.net/photos/user123/photo1.jpg?Expires=1234&Signature=abc"
            val signedPhoto2 = "https://d111111abcdef8.cloudfront.net/photos/user123/photo2.jpg?Expires=1234&Signature=def"
            val signedThumb1 = "https://d111111abcdef8.cloudfront.net/photos/user123/thumb1.jpg?Expires=1234&Signature=ghi"

            val testPhotos = listOf(
                UserMediaItem(
                    id = 1L,
                    userId = "target-cf-2",
                    mediaUrl = s3Photo1,
                    thumbnailUrl = s3Thumb1,
                    mediaType = MediaType.PHOTO,
                    displayOrder = 1,
                    isPrimary = true,
                    createdAt = fixedInstant,
                    updatedAt = fixedInstant
                ),
                UserMediaItem(
                    id = 2L,
                    userId = "target-cf-2",
                    mediaUrl = s3Photo2,
                    thumbnailUrl = null,
                    mediaType = MediaType.PHOTO,
                    displayOrder = 2,
                    isPrimary = false,
                    createdAt = fixedInstant,
                    updatedAt = fixedInstant
                )
            )

            // Mock getUserMedia to return S3 URLs
            coEvery { mockPhotoServiceWithCloudFront.getUserMedia("target-cf-2") } returns
                UserMediaCollection("target-cf-2", testPhotos, testPhotos.size)

            // Mock generateAccessUrl to convert S3 URLs to CloudFront signed URLs
            coEvery { mockPhotoServiceWithCloudFront.generateAccessUrl(s3Photo1, 48) } returns signedPhoto1
            coEvery { mockPhotoServiceWithCloudFront.generateAccessUrl(s3Photo2, 48) } returns signedPhoto2
            coEvery { mockPhotoServiceWithCloudFront.generateAccessUrl(s3Thumb1, 48) } returns signedThumb1

            coEvery { mockQAService.getAllUserQAs("target-cf-2") } returns emptyList()

            // Call the method
            val result = runBlocking {
                serviceWithCloudFront.getPublicProfile("requesting-cf-2", "target-cf-2")
            }

            // Verify that all photo URLs are CloudFront signed URLs, not S3 URLs
            assertEquals(2, result.profile.photos.size, "Should have 2 photos")
            assertEquals(signedPhoto1, result.profile.photos[0], "First photo should be signed URL")
            assertEquals(signedPhoto2, result.profile.photos[1], "Second photo should be signed URL")
            assertEquals(signedPhoto1, result.profile.coverPhoto, "Cover photo should be signed URL (primary)")
        }

        @Test
        fun `should use custom photoExpiryHours parameter`() {
            // Create two users
            val requestingUser = createValidUserRequest("requesting-cf-3", "requesting-cf-3@example.com")
            val targetUser = createValidUserRequest("target-cf-3", "target-cf-3@example.com")

            runBlocking {
                serviceWithCloudFront.createUser(requestingUser)
                serviceWithCloudFront.createUser(targetUser)
            }

            val s3Photo = "https://test-bucket.s3.eu-west-2.amazonaws.com/photos/user123/photo.jpg"
            val signedPhoto = "https://d111111abcdef8.cloudfront.net/photos/user123/photo.jpg?Expires=5678"

            val testPhotos = listOf(
                UserMediaItem(
                    id = 1L,
                    userId = "target-cf-3",
                    mediaUrl = s3Photo,
                    thumbnailUrl = null,
                    mediaType = MediaType.PHOTO,
                    displayOrder = 1,
                    isPrimary = true,
                    createdAt = fixedInstant,
                    updatedAt = fixedInstant
                )
            )

            coEvery { mockPhotoServiceWithCloudFront.getUserMedia("target-cf-3") } returns
                UserMediaCollection("target-cf-3", testPhotos, testPhotos.size)

            // Verify that custom expiry hours (24) is used instead of default (48)
            coEvery { mockPhotoServiceWithCloudFront.generateAccessUrl(s3Photo, 24) } returns signedPhoto
            coEvery { mockQAService.getAllUserQAs("target-cf-3") } returns emptyList()

            // Call with custom expiry
            val result = runBlocking {
                serviceWithCloudFront.getPublicProfile("requesting-cf-3", "target-cf-3", photoExpiryHours = 24)
            }

            // Verify the signed URL was generated with correct expiry
            assertEquals(signedPhoto, result.profile.photos[0])
        }

        @Test
        fun `should handle photos without thumbnails correctly`() {
            // Create two users
            val requestingUser = createValidUserRequest("requesting-cf-4", "requesting-cf-4@example.com")
            val targetUser = createValidUserRequest("target-cf-4", "target-cf-4@example.com")

            runBlocking {
                serviceWithCloudFront.createUser(requestingUser)
                serviceWithCloudFront.createUser(targetUser)
            }

            val s3Photo = "https://test-bucket.s3.eu-west-2.amazonaws.com/photos/user123/photo.jpg"
            val signedPhoto = "https://d111111abcdef8.cloudfront.net/photos/user123/photo.jpg?Expires=9999"

            // Photo without thumbnail (thumbnailUrl = null)
            val testPhotos = listOf(
                UserMediaItem(
                    id = 1L,
                    userId = "target-cf-4",
                    mediaUrl = s3Photo,
                    thumbnailUrl = null, // No thumbnail
                    mediaType = MediaType.PHOTO,
                    displayOrder = 1,
                    isPrimary = true,
                    createdAt = fixedInstant,
                    updatedAt = fixedInstant
                )
            )

            coEvery { mockPhotoServiceWithCloudFront.getUserMedia("target-cf-4") } returns
                UserMediaCollection("target-cf-4", testPhotos, testPhotos.size)

            coEvery { mockPhotoServiceWithCloudFront.generateAccessUrl(s3Photo, 48) } returns signedPhoto
            coEvery { mockQAService.getAllUserQAs("target-cf-4") } returns emptyList()

            // Should not throw when thumbnail is null
            val result = runBlocking {
                serviceWithCloudFront.getPublicProfile("requesting-cf-4", "target-cf-4")
            }

            assertNotNull(result, "Should successfully return profile even without thumbnails")
            assertEquals(signedPhoto, result.profile.photos[0])
        }
    }

    @Nested
    inner class `Get Public Profile - Parameter Validation` {

        @Test
        fun `should reject negative photoExpiryHours`() {
            // Create two users in the database
            val requestingUser = createValidUserRequest("requesting-param-1", "requesting-param-1@example.com")
            val targetUser = createValidUserRequest("target-param-1", "target-param-1@example.com")

            runBlocking {
                service.createUser(requestingUser)
                service.createUser(targetUser)
            }

            // Mock photo repository to return empty list (doesn't matter for this test)
            coEvery { mockPhotoRepository.findByUserId("target-param-1") } returns emptyList()
            coEvery { mockQAService.getAllUserQAs("target-param-1") } returns emptyList()

            // Call with negative photoExpiryHours should throw
            val exception = runCatching {
                runBlocking {
                    service.getPublicProfile("requesting-param-1", "target-param-1", photoExpiryHours = -1)
                }
            }.exceptionOrNull()

            assertNotNull(exception, "Should throw exception for negative photoExpiryHours")
            assertEquals(IllegalArgumentException::class, exception!!::class, "Should throw IllegalArgumentException")
            assert(exception.message!!.contains("photoExpiryHours must be positive")) {
                "Exception message should mention photoExpiryHours validation"
            }
            assert(exception.message!!.contains("target-param-1")) {
                "Exception message should include targetUserId"
            }
        }

        @Test
        fun `should reject zero photoExpiryHours`() {
            // Create two users in the database
            val requestingUser = createValidUserRequest("requesting-param-2", "requesting-param-2@example.com")
            val targetUser = createValidUserRequest("target-param-2", "target-param-2@example.com")

            runBlocking {
                service.createUser(requestingUser)
                service.createUser(targetUser)
            }

            // Mock photo repository to return empty list (doesn't matter for this test)
            coEvery { mockPhotoRepository.findByUserId("target-param-2") } returns emptyList()
            coEvery { mockQAService.getAllUserQAs("target-param-2") } returns emptyList()

            // Call with zero photoExpiryHours should throw
            val exception = runCatching {
                runBlocking {
                    service.getPublicProfile("requesting-param-2", "target-param-2", photoExpiryHours = 0)
                }
            }.exceptionOrNull()

            assertNotNull(exception, "Should throw exception for zero photoExpiryHours")
            assertEquals(IllegalArgumentException::class, exception!!::class, "Should throw IllegalArgumentException")
            assert(exception.message!!.contains("photoExpiryHours must be positive")) {
                "Exception message should mention photoExpiryHours validation"
            }
        }

        @Test
        fun `should accept positive photoExpiryHours`() {
            // Create two users in the database
            val requestingUser = createValidUserRequest("requesting-param-3", "requesting-param-3@example.com")
            val targetUser = createValidUserRequest("target-param-3", "target-param-3@example.com")

            runBlocking {
                service.createUser(requestingUser)
                service.createUser(targetUser)
            }

            // Mock photo repository to return empty list
            coEvery { mockPhotoRepository.findByUserId("target-param-3") } returns emptyList()
            coEvery { mockQAService.getAllUserQAs("target-param-3") } returns emptyList()

            // Call with positive photoExpiryHours should succeed (even if CloudFront fails later)
            val result = runCatching {
                runBlocking {
                    service.getPublicProfile("requesting-param-3", "target-param-3", photoExpiryHours = 1)
                }
            }

            // We expect it might fail due to CloudFront not being configured, but NOT due to parameter validation
            // If it throws, it should NOT be IllegalArgumentException about photoExpiryHours
            result.exceptionOrNull()?.let { exception ->
                assertNotEquals(
                    IllegalArgumentException::class,
                    exception::class,
                    "Should not throw IllegalArgumentException for valid photoExpiryHours"
                )
            }
        }
    }

    // =============================//
    // ========== Setup ============//
    // =============================//

    companion object {
        @Container
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("test_db")
            withUsername("test_user")
            withPassword("test_password")
        }
    }

    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var photoService: PhotoService
    private lateinit var mockPhotoRepository: PhotoRepository
    private lateinit var mockQAService: QAService
    private lateinit var service: UserService
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
            SchemaUtils.create(Users)
        }
    }

    @BeforeEach
    fun setupEach() {
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
        userRepository = UserRepositoryImpl()
        mockPhotoRepository   = mockk<PhotoRepository>()
        val mockS3Client     = mockk<S3Client>()
        val mockS3Presigner  = mockk<S3Presigner>()
        val s3Config = S3Config(
            region                 = "eu-west-2",
            accessKeyId            = "test-key",
            secretAccessKey        = "test-secret",
            bucketName             = "test-bucket",
            cdnBaseUrl             = null,
            presignedUrlTtlMinutes = 15L,
            cloudFrontKeyPairId    = null,
            cloudFrontPrivateKeyPath = null,
            cloudFrontDistributionDomain = null
        )
        photoService = PhotoService(mockPhotoRepository, s3Config, mockS3Client, mockS3Presigner)
        mockQAService = mockk<QAService>(relaxed = true)
        service = UserService(userRepository, photoService, mockQAService)

        transaction {
            Users.deleteAll()
        }
    }

    @AfterAll
    fun tearDown() {
        transaction {
            SchemaUtils.drop(Users)
        }
    }

    // ===============================//
    // =========== Helpers ===========//
    // ===============================//

    private fun createValidUserRequest(
        userId: String = "test-user-id",
        email: String = "john.doe@example.com"
    ): CreateUserRequest {
        return CreateUserRequest(
            userId = userId,
            firstName = "John",
            lastName = "Doe",
            email = email,
            heightCm = 180,
            dateOfBirth = LocalDate.of(1990, 1, 1),
            city = "London",
            educationLevel = EducationLevel.UNIVERSITY,
            gender = Gender.MALE,
            preferredLanguage = Language.ENGLISH,
            occupation = "Engineer",
            bio = "Test bio",
            interests = listOf(Activity.HIKING, Interest.NATURE, Entertainment.MOVIES, Creative.PHOTOGRAPHY, Sport.YOGA),
            traits = List(3) { Trait.entries[it] },
            spokenLanguages = DisplayableField(listOf(Language.ENGLISH), false),
            religion = DisplayableField(null, false),
            politicalView = DisplayableField(null, false),
            alcoholConsumption = DisplayableField(null, false),
            smokingStatus = DisplayableField(SmokingStatus.NEVER, true),
            diet = DisplayableField(null, false),
            dateIntentions = DisplayableField(DateIntentions.SERIOUS_DATING, false),
            relationshipType = DisplayableField(RelationshipType.MONOGAMOUS, false),
            kidsPreference = DisplayableField(KidsPreference.OPEN_TO_KIDS, false),
            sexualOrientation = DisplayableField(SexualOrientation.STRAIGHT, false),
            pronouns = DisplayableField(null, false),
            starSign = DisplayableField(null, false),
            ethnicity = DisplayableField(listOf(Ethnicity.BLACK_AFRICAN_DESCENT), false),
            brainAttributes = DisplayableField(null, false),
            brainDescription = DisplayableField(null, false),
            bodyAttributes = DisplayableField(null, false),
            bodyDescription = DisplayableField(null, false),
            coordinatesLongitude = 45.3246,
            coordinatesLatitude = -90.0,
        )
    }

    // -------------------------------------------------------------------------
    // getUserMatchProfileData - photoExpiryHours parameter
    // -------------------------------------------------------------------------

    @Nested
    inner class `getUserMatchProfileData() photoExpiryHours` {

        @Test
        fun `should return null when user does not exist`() {
            val result = runBlocking {
                service.getUserMatchProfileData("nonexistent-user-id")
            }

            assertNull(result, "Should return null for non-existent user")
        }

        @Test
        fun `should return null thumbnailUrl when user has no photos`() {
            val user = createValidUserRequest()

            val result = runBlocking {
                service.createUser(user)
                coEvery { mockPhotoRepository.findByUserId(user.userId) } returns emptyList()
                service.getUserMatchProfileData(user.userId)
            }

            assertNotNull(result)
            assertNull(result.thumbnailUrl, "Should have null thumbnailUrl when user has no photos")
        }
    }

}