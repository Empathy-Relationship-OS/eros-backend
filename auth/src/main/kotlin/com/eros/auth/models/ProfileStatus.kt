package com.eros.auth.models

import kotlinx.serialization.Serializable

@Serializable
enum class ProfileStatus(s : String){
    PENDING("PENDING"),
    VERIFIED("VERIFIED"),
    SUSPENDED("SUSPENDED")
}