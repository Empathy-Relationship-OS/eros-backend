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
 */
object UserInterestSerializer : KSerializer<UserInterest> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UserInterest", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UserInterest) {
        encoder.encodeString(value.displayName)
    }

    override fun deserialize(decoder: Decoder): UserInterest {
        val displayName = decoder.decodeString()

        // Try each enum type that implements UserInterest
        // This works because we enforce uniqueness of enum values across all types
        return Activity.entries.find { it.displayName == displayName }
            ?: Interest.entries.find { it.displayName == displayName }
            ?: Entertainment.entries.find { it.displayName == displayName }
            ?: Creative.entries.find { it.displayName == displayName }
            ?: MusicGenre.entries.find { it.displayName == displayName }
            ?: FoodAndDrink.entries.find { it.displayName == displayName }
            ?: Sport.entries.find { it.displayName == displayName }
            ?: throw IllegalArgumentException(
                "Unknown UserInterest displayName: '$displayName'. " +
                "Valid values must match a displayName from Activity, Interest, Entertainment, " +
                "Creative, MusicGenre, FoodAndDrink, or Sport enums."
            )
    }
}
