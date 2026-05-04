package com.eros.users.serializers

import com.eros.users.models.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Base serializer class for enums with displayName property.
 * Converts enum values to their displayName for frontend presentation.
 */
abstract class DisplayNameEnumSerializer<T>(
    private val serialName: String,
    private val values: Array<T>,
    private val getDisplayName: (T) -> String,
    private val fromDisplayName: (String) -> T?
) : KSerializer<T> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(getDisplayName(value))
    }

    override fun deserialize(decoder: Decoder): T {
        val displayName = decoder.decodeString()
        return fromDisplayName(displayName)
            ?: throw IllegalArgumentException(
                "Unknown $serialName displayName: '$displayName'. " +
                "Valid values are: ${values.map { getDisplayName(it) }.joinToString(", ")}"
            )
    }
}

// Gender Serializer
object GenderSerializer : DisplayNameEnumSerializer<Gender>(
    serialName = "Gender",
    values = Gender.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = Gender::fromDisplayName
)

// EducationLevel Serializer
object EducationLevelSerializer : DisplayNameEnumSerializer<EducationLevel>(
    serialName = "EducationLevel",
    values = EducationLevel.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = EducationLevel::fromDisplayName
)

// Religion Serializer
object ReligionSerializer : DisplayNameEnumSerializer<Religion>(
    serialName = "Religion",
    values = Religion.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = Religion::fromDisplayName
)

// PoliticalView Serializer
object PoliticalViewSerializer : DisplayNameEnumSerializer<PoliticalView>(
    serialName = "PoliticalView",
    values = PoliticalView.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = PoliticalView::fromDisplayName
)

// AlcoholConsumption Serializer
object AlcoholConsumptionSerializer : DisplayNameEnumSerializer<AlcoholConsumption>(
    serialName = "AlcoholConsumption",
    values = AlcoholConsumption.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = AlcoholConsumption::fromDisplayName
)

// SmokingStatus Serializer
object SmokingStatusSerializer : DisplayNameEnumSerializer<SmokingStatus>(
    serialName = "SmokingStatus",
    values = SmokingStatus.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = SmokingStatus::fromDisplayName
)

// Diet Serializer
object DietSerializer : DisplayNameEnumSerializer<Diet>(
    serialName = "Diet",
    values = Diet.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = Diet::fromDisplayName
)

// DateIntentions Serializer
object DateIntentionsSerializer : DisplayNameEnumSerializer<DateIntentions>(
    serialName = "DateIntentions",
    values = DateIntentions.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = DateIntentions::fromDisplayName
)

// RelationshipType Serializer
object RelationshipTypeSerializer : DisplayNameEnumSerializer<RelationshipType>(
    serialName = "RelationshipType",
    values = RelationshipType.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = RelationshipType::fromDisplayName
)

// KidsPreference Serializer
object KidsPreferenceSerializer : DisplayNameEnumSerializer<KidsPreference>(
    serialName = "KidsPreference",
    values = KidsPreference.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = KidsPreference::fromDisplayName
)

// SexualOrientation Serializer
object SexualOrientationSerializer : DisplayNameEnumSerializer<SexualOrientation>(
    serialName = "SexualOrientation",
    values = SexualOrientation.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = SexualOrientation::fromDisplayName
)

// Pronouns Serializer
object PronounsSerializer : DisplayNameEnumSerializer<Pronouns>(
    serialName = "Pronouns",
    values = Pronouns.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = Pronouns::fromDisplayName
)

// StarSign Serializer
object StarSignSerializer : DisplayNameEnumSerializer<StarSign>(
    serialName = "StarSign",
    values = StarSign.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = StarSign::fromDisplayName
)

// Trait Serializer
object TraitSerializer : DisplayNameEnumSerializer<Trait>(
    serialName = "Trait",
    values = Trait.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = Trait::fromDisplayName
)

// Language Serializer
object LanguageSerializer : DisplayNameEnumSerializer<Language>(
    serialName = "Language",
    values = Language.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = Language::fromDisplayName
)

// BrainAttribute Serializer
object BrainAttributeSerializer : DisplayNameEnumSerializer<BrainAttribute>(
    serialName = "BrainAttribute",
    values = BrainAttribute.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = BrainAttribute::fromDisplayName
)

// BodyAttribute Serializer
object BodyAttributeSerializer : DisplayNameEnumSerializer<BodyAttribute>(
    serialName = "BodyAttribute",
    values = BodyAttribute.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = BodyAttribute::fromDisplayName
)

// Ethnicity Serializer (already had displayName)
object EthnicitySerializer : DisplayNameEnumSerializer<Ethnicity>(
    serialName = "Ethnicity",
    values = Ethnicity.entries.toTypedArray(),
    getDisplayName = { it.displayName },
    fromDisplayName = Ethnicity::fromDisplayName
)
