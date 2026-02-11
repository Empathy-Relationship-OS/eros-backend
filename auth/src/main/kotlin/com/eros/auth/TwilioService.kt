package com.eros.auth

interface TwilioService {

    fun sendOtp(phoneNumber : String, otp : String) : Boolean

}