package com.eros.auth


interface TwilioService {
    fun sendSms(phoneNumber : String) : String?
}