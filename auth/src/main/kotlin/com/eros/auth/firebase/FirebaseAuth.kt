package com.eros.auth.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseToken

/**
 * Firebase user principal for authenticated requests
 *
 * This data class represents the authenticated user after successful Firebase ID token verification.
 * It contains the essential user information extracted from the Firebase token.
 *
 * Note: Ktor no longer requires the Principal marker interface. This is a plain data class
 * that can be used directly with call.principal<FirebaseUserPrincipal>().
 *
 * @property uid Firebase user ID (unique identifier)
 * @property email User's email address (verified by Firebase)
 * @property phoneNumber User's phone number (optional, verified by Firebase if present)
 * @property emailVerified Whether the email has been verified
 * @property token The decoded Firebase token containing all claims
 */
data class FirebaseUserPrincipal(
    val uid: String,
    val email: String?,
    val phoneNumber: String?,
    val emailVerified: Boolean,
    val token: FirebaseToken
)

/**
 * Firebase token verification service
 *
 * Handles verification of Firebase ID tokens sent by clients.
 */
object FirebaseAuthService {

    /**
     * Verify a Firebase ID token
     *
     * @param idToken The Firebase ID token from the Authorization header
     * @return FirebaseUserPrincipal if verification succeeds
     * @throws FirebaseAuthException if token is invalid, expired, or revoked
     */
    fun verifyToken(idToken: String): FirebaseUserPrincipal {
        val decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken)

        return FirebaseUserPrincipal(
            uid = decodedToken.uid,
            email = decodedToken.email,
            phoneNumber = decodedToken.claims["phone_number"] as? String,
            emailVerified = decodedToken.isEmailVerified,
            token = decodedToken
        )
    }

    /**
     * Verify a Firebase ID token and return null if invalid
     *
     * This is a safe version that doesn't throw exceptions.
     *
     * @param idToken The Firebase ID token from the Authorization header
     * @return FirebaseUserPrincipal if verification succeeds, null otherwise
     */
    fun verifyTokenOrNull(idToken: String): FirebaseUserPrincipal? {
        return try {
            verifyToken(idToken)
        } catch (e: FirebaseAuthException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
