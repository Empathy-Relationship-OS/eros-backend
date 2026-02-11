package com.eros.auth

import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.rest.verify.v2.service.Verification
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
        Twilio.init(settings.accountSid, settings.authToken)

        // Message with custom OTP
        Twilio.init(settings.accountSid, settings.authToken)
        val msg = "Your verification code is: $otp. Valid for 10 minutes."
        val message = Message
            .creator(
                PhoneNumber(phoneNumber),
                PhoneNumber(phoneNumber),
                msg
            )
            .create()
        println(message.body)
        println(message.status)


        //Actual - Verification with custom OTP.
        val verification = Verification.creator(
            settings.verifyServiceId,
            phoneNumber,
            "sms"
        )
            .setCustomCode(otp)
            .create()
        println(verification.status)
        return true
    }
}


fun main() {
    val twilio = TwilioServiceImpl(TwilioSettings())
    val res = twilio.sendOtp("+1", OtpGenerator.generateOtp())
    //val res = twilio.sendOtp(TwilioSettings().phoneNumber, OtpGenerator.generateOtp())
    println(res)
}
