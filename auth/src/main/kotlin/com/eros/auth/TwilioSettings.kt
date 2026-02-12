package com.eros.auth

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.ApplicationConfigurationException
import io.github.cdimascio.dotenv.dotenv

/**
 * Configuration settings for Twilio SMS and OTP verification services.
 *
 * This class encapsulates all necessary credentials and configuration options required
 * to integrate with Twilio's API for sending SMS messages and verifying one-time passwords.
 *
 * **Mock Mode:**
 * When [mockMode] is enabled, the service will simulate SMS sending without making actual
 * API calls to Twilio, which is useful for development and testing environments.
 *
 * **Creating Instances:**
 * Instead of using the constructor directly, consider using the companion object factory methods:
 * - [TwilioSettings.fromApplicationConfig] to load settings from application configuration
 * - [TwilioSettings.fromEnvironment] to load settings from environment variables
 *
 * @property accountSid The Twilio Account SID - unique identifier for your Twilio account.
 *                      Defaults to "a" (placeholder value).
 * @property authToken The Twilio Auth Token - authentication token for API access.
 *                     Keep this secret and never commit to version control.
 *                     Defaults to "a" (placeholder value).
 * @property phoneNumber The Twilio phone number to send SMS from, in E.164 format
 *                       (e.g., "+15551234567"). Must be a verified Twilio number.
 *                       Defaults to "a" (placeholder value).
 * @property mockMode Flag to enable/disable mock mode. When `true`, OTP operations are
 *                    simulated without real SMS sending. When `false`, real Twilio API
 *                    calls are made. Defaults to `true` for safety.
 * @property verifyServiceId The Twilio Verify Service SID - identifier for your Verify
 *                           service configuration. Create this in the Twilio Console
 *                           under Verify > Services. Defaults to "a" (placeholder value).
 *
 * @see TwilioService
 * @see TwilioServiceImpl
 */
class TwilioSettings(
    var accountSid: String = "a",
    var authToken: String = "a",
    var phoneNumber: String = "a",
    var mockMode: Boolean = true,
    var verifyServiceId : String = "a"
) {

    companion object {
        /**
         * Factory method to create TwilioSettings from Ktor's ApplicationConfig.
         *
         * Extracts Twilio configuration from application.yaml and constructs a TwilioSettings instance.
         *
         * @param config Ktor ApplicationConfig instance (typically from Application.environment.config)
         * @return TwilioSettings instance populated from configuration
         * @throws ApplicationConfigurationException if required properties are missing
         */
        fun fromApplicationConfig(config: ApplicationConfig): TwilioSettings {
            return TwilioSettings(
                accountSid = config.property("twilio.accountSid").getString(),
                authToken = config.property("twilio.authToken").getString(),
                phoneNumber = config.property("twilio.phoneNumber").getString(),
                mockMode = config.property("twilio.mockMode").getString().toBoolean(),
                verifyServiceId = config.property("twilio.verifyServiceId").getString()
            )
        }

        /**
         * Factory method to create TwilioSettings for local testing using .env variables.
         *
         * Extracts Twilio configuration from .env and constructs a TwilioSettings instance.
         *
         * @return TwilioSettings instance populated from .env config
         */
        fun fromEnvironment(): TwilioSettings {
            val dotenv = dotenv { ignoreIfMissing = true }

            return TwilioSettings(
                accountSid = dotenv["TWILIO_ACCOUNT_SID"] ?: System.getenv("TWILIO_ACCOUNT_SID") ?: "a",
                authToken = dotenv["TWILIO_AUTH_TOKEN"] ?: System.getenv("TWILIO_AUTH_TOKEN") ?: "a",
                phoneNumber = dotenv["TWILIO_PHONE_NUMBER"] ?: System.getenv("TWILIO_PHONE_NUMBER") ?: "a",
                mockMode = (dotenv["TWILIO_MOCK_MODE"] ?: System.getenv("TWILIO_MOCK_MODE"))?.toBoolean() ?: true,
                verifyServiceId = dotenv["TWILIO_VERIFY_SERVICE_ID"] ?: System.getenv("TWILIO_VERIFY_SERVICE_ID") ?: "a"
            )
        }
    }
}