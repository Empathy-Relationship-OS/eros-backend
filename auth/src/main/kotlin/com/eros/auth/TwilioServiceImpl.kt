package com.eros.auth

import com.twilio.Twilio
import com.twilio.rest.verify.v2.service.Verification
import com.twilio.rest.verify.v2.service.VerificationCheck
import org.slf4j.LoggerFactory

/**
 * Implementation of the TwilioService interface for sending and verifying OTP codes via SMS.
 *
 * This service supports both real and mock modes:
 * - **Real mode**: Uses Twilio's Verify API to send actual SMS messages
 * - **Mock mode**: Logs OTP information without sending real messages.
 *
 * @property settings Configuration settings for Twilio including credentials and mock mode flag
 *
 * @see TwilioService
 * @see TwilioSettings
 */
class TwilioServiceImpl(private val settings: TwilioSettings) : TwilioService {
    private val logger = LoggerFactory.getLogger(TwilioServiceImpl::class.java)

    /**
     * Sends a one-time password (OTP) to the specified phone number.
     *
     * Depending on the [TwilioSettings.mockMode] configuration, this method will either:
     * - Send a real SMS via Twilio's Verify API, or
     * - Log mock OTP details to the console (always uses code "123456")
     *
     * @param phoneNumber The recipient's phone number in E.164 format (e.g., "+15551234567")
     * @return The verification status: pending, approved, canceled, max_attempts_reached, deleted, failed or expired
     *
     * @throws Exception Caught internally and logged; returns "failed" on error
     */
    override fun sendOtp(phoneNumber: String): String? {
        return try {
            if (settings.mockMode) {
                sendMockOtp(phoneNumber)
            } else {
                sendRealOtp(phoneNumber)
            }
        } catch (e: Exception) {
            logger.error("Failed to send OTP to $phoneNumber", e)
            return "failed"
        }
    }

    /**
     * Simulates sending an OTP by logging details to the console.
     *
     * This method is used in mock mode for testing without sending real SMS messages.
     * The mock OTP is always "123456" and valid for 10 minutes.
     *
     * @param phoneNumber The recipient's phone number (not actually contacted)
     * @return Always returns "pending" to simulate successful sending
     */
    private fun sendMockOtp(phoneNumber: String): String? {
        logger.info("=================================================================")
        logger.info("MOCK SMS SENT")
        logger.info("To: $phoneNumber")
        logger.info("From: ${settings.phoneNumber}")
        logger.info("OTP: 123456")
        logger.info("Message: Your verification code is: 123456. Valid for 10 minutes.")
        logger.info("=================================================================")
        return "pending"
    }

    /**
     * Sends a real OTP via Twilio's Verify API.
     *
     * Initializes the Twilio client with credentials from settings and creates a verification
     * request to send an SMS with a randomly generated OTP code.
     *
     * @param phoneNumber The recipient's phone number in E.164 format
     * @return The verification status - pending, approved, canceled, max_attempts_reached, deleted, failed or expired
     */
    private fun sendRealOtp(phoneNumber: String): String? {
        // Connect with information.
        Twilio.init(settings.accountSid, settings.authToken)

        // Send OTP to number.
        val verification = Verification.creator(
            settings.verifyServiceId,
            phoneNumber,
            "sms"
        )
            .create()
        return verification.status
    }

    /**
     * Verifies that the user-provided OTP code matches the one sent to their phone number.
     *
     * In mock mode, this method bypasses actual verification and returns "mock".
     * In real mode, it validates the code against Twilio's Verify API.
     *
     * @param phoneNumber The phone number that received the OTP (must match the original request)
     * @param userInputOtp The OTP code entered by the user
     * @return The verification status - pending, approved, canceled, max_attempts_reached, deleted, failed or expired
     *
     * @see <a href="https://www.twilio.com/docs/verify/api">Twilio Verify API</a>
     */
    private fun verifyOTP(phoneNumber: String, userInputOtp: String): String? {
        if (settings.mockMode){
            return "mock"
        }
        Twilio.init(settings.accountSid, settings.authToken)
        val verificationCheck = VerificationCheck.creator(settings.verifyServiceId)
            .setTo(phoneNumber)
            .setCode(userInputOtp)
            .create()
        return verificationCheck.status
    }
}