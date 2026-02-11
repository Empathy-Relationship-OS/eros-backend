package com.eros.auth

import java.security.SecureRandom
import kotlin.math.pow

/**
 * Helper function to generate a random OTP for authentication.
 *
 * Functions:
 * [generateOtp] to generate the OTP of length [OTP_LENGTH].
 */
object OtpGenerator {

    val secureRandom: SecureRandom = SecureRandom()
    const val OTP_LENGTH: Double = 6.0

    /**
     * Function to create a random OTP with [OTP_LENGTH] digits.
     *
     * @return String of the OTP number.
     */
    fun generateOtp(): String {
        val otp: Int = secureRandom.nextInt((9*(10.0.pow(OTP_LENGTH - 1))).toInt()) + (10.0.pow(OTP_LENGTH - 1)).toInt()
        return otp.toString()
    }

}