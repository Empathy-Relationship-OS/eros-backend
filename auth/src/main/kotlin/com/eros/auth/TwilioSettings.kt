package com.eros.auth

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.ApplicationConfigurationException


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
    }
}