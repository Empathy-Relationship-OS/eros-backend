package com.eros.services

import com.eros.users.models.CreateUserRequest
import com.eros.users.models.User
import com.eros.users.routes.UserCreationService
import com.eros.users.service.UserService
import com.eros.wallet.services.WalletService
import org.slf4j.LoggerFactory

/**
 * Service for coordinating user onboarding flow.
 *
 * This service orchestrates the creation of a new user account along with
 * all associated resources (wallet, preferences, etc.).
 *
 * ## Architecture Note
 *
 * This is a **Service Composition** pattern that lives in the app module to avoid
 * circular dependencies between feature modules (users ↔ wallet).
 *
 * ### Future Migration Path: Event-Driven Architecture
 *
 * When moving to an event-driven architecture, this service should be refactored to:
 *
 * 1. **Publish Domain Events** instead of direct service calls:
 *    ```kotlin
 *    data class UserCreatedEvent(
 *        val userId: String,
 *        val email: String,
 *        val city: String?,
 *        val occurredAt: Instant = Instant.now()
 *    ) : DomainEvent
 *
 *    suspend fun createUser(request: CreateUserRequest): User {
 *        val user = userService.createUser(request)
 *        eventPublisher.publish(UserCreatedEvent(user.userId, user.email, user.city))
 *        return user
 *    }
 *    ```
 *
 * 2. **Event Handlers** in respective modules:
 *    ```kotlin
 *    // wallet module
 *    class WalletCreationHandler(private val walletService: WalletService) {
 *        suspend fun handle(event: UserCreatedEvent) {
 *            walletService.createWallet(event.userId, deriveCurrency(event.city))
 *        }
 *    }
 *    ```
 *
 * 3. **Benefits of event-driven approach**:
 *    - Loose coupling between modules
 *    - Async processing support
 *    - Easy to add new handlers (notifications, analytics, etc.)
 *    - Better scalability and resilience
 *    - Supports eventual consistency
 *
 * @see com.eros.users.service.UserService
 * @see com.eros.wallet.services.WalletService
 */
class UserOnboardingService(
    private val userService: UserService,
    private val walletService: WalletService
) : UserCreationService {
    private val logger = LoggerFactory.getLogger(UserOnboardingService::class.java)

    /**
     * Creates a new user account with all associated resources.
     *
     * This method:
     * 1. Creates the user profile in the database
     * 2. Creates a wallet for the user (with default GBP currency)
     *
     * Wallet creation failures are logged but do not prevent user creation.
     * Missing wallets can be created later via the admin endpoint:
     * `POST /wallet/admin/ensure/{userId}`
     *
     * @param request The user creation request containing profile data
     * @return The created User entity
     * @throws ConflictException if user already exists
     * @throws IllegalArgumentException if input validation fails
     */
    override suspend fun createUser(request: CreateUserRequest): User {
        // Step 1: Create user profile
        val user = userService.createUser(request)

        logger.info("User created: ${user.userId}")

        // Step 2: Create wallet (non-blocking - failures don't prevent user creation)
        try {
            // TODO: Implement location-based currency determination
            //       - Add country/location field to CreateUserRequest
            //       - Create CurrencyResolver service that maps country/city -> currency code
            //       - Example: deriveCurrencyFromLocation(user.city, request.country)
            //       - Support: GBP (UK), USD (US), EUR (EU), etc.
            val currency = "GBP" // Default currency

            walletService.createWallet(userId = user.userId, currency = currency)
            logger.info("Wallet created for user: ${user.userId} with currency: $currency")
        } catch (e: Exception) {
            // Log the error but don't fail user creation
            // Wallet can be created later via admin endpoint: POST /wallet/admin/ensure/{userId}
            logger.warn("Failed to create wallet for user ${user.userId}: ${e.message}", e)
        }

        return user
    }

    /**
     * TODO: Future enhancement - Add retry mechanism
     *
     * Consider implementing exponential backoff retry for wallet creation:
     * ```kotlin
     * private suspend fun createWalletWithRetry(userId: String, currency: String, maxAttempts: Int = 3) {
     *     repeat(maxAttempts) { attempt ->
     *         try {
     *             walletService.createWallet(userId, currency)
     *             return
     *         } catch (e: Exception) {
     *             if (attempt == maxAttempts - 1) throw e
     *             delay(100L * (attempt + 1)) // Exponential backoff
     *         }
     *     }
     * }
     * ```
     */

    /**
     * TODO: Future enhancement - Background job for failed wallet creation
     *
     * Implement a background job that periodically checks for users without wallets
     * and creates them:
     * ```kotlin
     * suspend fun ensureAllUsersHaveWallets() {
     *     val usersWithoutWallets = userRepository.findUsersWithoutWallets()
     *     usersWithoutWallets.forEach { userId ->
     *         try {
     *             walletService.createWallet(userId, "GBP")
     *             logger.info("Created missing wallet for user $userId")
     *         } catch (e: Exception) {
     *             logger.error("Failed to create wallet for user $userId", e)
     *         }
     *     }
     * }
     * ```
     */
}
