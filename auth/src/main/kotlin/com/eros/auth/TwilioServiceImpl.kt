package com.eros.auth

import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.rest.verify.v2.service.Verification
import com.twilio.rest.verify.v2.service.VerificationCheck
import com.twilio.type.PhoneNumber
import org.slf4j.LoggerFactory

class TwilioServiceImpl(private val settings: TwilioSettings) : TwilioService {
    private val logger = LoggerFactory.getLogger(TwilioServiceImpl::class.java)

    override fun sendOtp(phoneNumber: String, otp: String): Boolean {
        return try {
            if (settings.mockMode) {
                sendMockOtp(phoneNumber, otp)
            } else {
                sendRealOtp(phoneNumber, otp)
            }
        } catch (e: Exception) {
            logger?.error("Failed to send OTP to $phoneNumber", e)
            false
        }
    }

    private fun sendMockOtp(phoneNumber: String, otp: String): Boolean {
        logger?.info("========================================")
        logger?.info("MOCK SMS SENT")
        logger?.info("To: $phoneNumber")
        logger?.info("From: ${settings.phoneNumber}")
        logger?.info("OTP: $otp")
        logger?.info("Message: Your verification code is: $otp. Valid for 10 minutes.")
        logger?.info("========================================")
        return true
    }

    private fun sendRealOtp(phoneNumber: String, otp: String): Boolean {

        // Connect with information.
        Twilio.init(settings.accountSid, settings.authToken)


        // Send OTP to number.
        val verification = Verification.creator(
            settings.verifyServiceId,
            phoneNumber,
            "sms"
        )
            .create()
        println(verification.status)
        return true
    }

    private fun verifyOTP(phoneNumber: String, userInputOtp: String): Boolean{
        Twilio.init(settings.accountSid, settings.authToken)
        val verificationCheck = VerificationCheck.creator(settings.verifyServiceId)
            .setTo(phoneNumber)
            .setCode(userInputOtp)
            .create()

        return verificationCheck.status == "approved"
    }
}


fun main() {
    val twilio = TwilioServiceImpl(TwilioSettings())
    val res = twilio.sendOtp("+1", OtpGenerator.generateOtp())
    //val res = twilio.sendOtp(TwilioSettings().phoneNumber, OtpGenerator.generateOtp())
    println(res)
}
