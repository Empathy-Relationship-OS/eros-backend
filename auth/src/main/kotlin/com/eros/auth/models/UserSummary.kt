package com.eros.auth.models

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


/**
 * Summary model for user information.
 *
 * @property userId Unique identifier for the user
 * @property email User's email address
 * @property name User's full name
 * @property profileStatus User's profile status (PENDING, VERIFIED, SUSPENDED)
 * @property phoneVerified Whether the user's phone number is verified
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class UserSummary(
    val userId: Uuid,
    val email: String,
    val name : String,
    val profileStatus : ProfileStatus,
    val phoneVerified : Boolean
)