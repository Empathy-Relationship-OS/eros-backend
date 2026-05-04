package com.eros.users.serializers

import com.eros.users.models.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for UserInterest enums.
 *
 * Converts enum values to their displayName for frontend presentation
 * and back from displayName to enum when deserializing.
 *
 * Examples:
 * - Serialization: Activity.CITY_TRIPS -> "City Trips"
 * - Deserialization: "City Trips" -> Activity.CITY_TRIPS
 * - Deserialization: "CITY_TRIPS" -> Activity.CITY_TRIPS (backward compatibility)
 */
object UserInterestSerializer : KSerializer<UserInterest> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UserInterest", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UserInterest) {
        encoder.encodeString(value.displayName)
    }

    override fun deserialize(decoder: Decoder): UserInterest {
        val displayName = decoder.decodeString()

        // Use shared helper function to find UserInterest by name or displayName
        return findUserInterest(displayName)
            ?: throw IllegalArgumentException(
                "Unknown UserInterest displayName: '$displayName'. " +
                "Valid values must match a displayName from Activity, Interest, Entertainment, " +
                "Creative, MusicGenre, FoodAndDrink, or Sport enums."
            )
    }
}
