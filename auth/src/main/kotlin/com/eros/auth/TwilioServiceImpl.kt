package com.eros.auth

import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
import org.slf4j.LoggerFactory

/**
 * Implementation of the TwilioService interface for sending SMS.
 *
 *
 * @property settings Configuration settings for Twilio including credentials and mock mode flag
 *
 * @see TwilioService
 * @see TwilioSettings
 */
class TwilioServiceImpl(private val settings: TwilioSettings) : TwilioService {
    private val logger = LoggerFactory.getLogger(TwilioServiceImpl::class.java)

    /**
     * Sends an SMS to the specified phone number.
     *
     * @param phoneNumber The recipient's phone number in E.164 format (e.g., "+15551234567")
     * @return The message status: pending, approved, failed
     *
     * @throws Exception Caught internally and logged; returns "failed" on error
     */
    override fun sendSms(phoneNumber: String): String {
        try {
            Twilio.init(settings.accountSid, settings.authToken)
            val msg = "Blah Blah Blah"
            val message = Message
                .creator(
                    PhoneNumber(phoneNumber),
                    PhoneNumber(settings.phoneNumber),
                    msg
                )
                .create()
            return message.status.toString()
        } catch (e: Exception) {
            logger.error("Failed to send OTP to $phoneNumber", e)
            return "failed"
        }
    }
}