package com.eros.users.serializers

import com.eros.users.models.AlcoholConsumption
import com.eros.users.models.BodyAttribute
import com.eros.users.models.BrainAttribute
import com.eros.users.models.DateIntentions
import com.eros.users.models.Diet
import com.eros.users.models.EducationLevel
import com.eros.users.models.Ethnicity
import com.eros.users.models.Gender
import com.eros.users.models.KidsPreference
import com.eros.users.models.Language
import com.eros.users.models.PoliticalView
import com.eros.users.models.Pronouns
import com.eros.users.models.RelationshipType
import com.eros.users.models.Religion
import com.eros.users.models.SexualOrientation
import com.eros.users.models.SmokingStatus
import com.eros.users.models.StarSign
import com.eros.users.models.Trait
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test suite for EnumSerializers.
 *
 * Tests cover:
 * - Serialization (Enum -> Display Name)
 * - Deserialization (Display Name -> Enum)
 * - Error handling for invalid display names
 */
class EnumSerializersTest {

    private val json = Json { prettyPrint = false }

    // ========================================
    // Gender Serializer Tests
    // ========================================

    @Test
    fun `GenderSerializer should serialize enum to display name`() {
        val result = json.encodeToString(GenderSerializer, Gender.MALE)
        assertEquals("\"Male\"", result)

        assertEquals("\"Female\"", json.encodeToString(GenderSerializer, Gender.FEMALE))
        assertEquals("\"Non-Binary\"", json.encodeToString(GenderSerializer, Gender.NON_BINARY))
        assertEquals("\"Other\"", json.encodeToString(GenderSerializer, Gender.OTHER))
    }

    @Test
    fun `GenderSerializer should deserialize display name to enum`() {
        val result = json.decodeFromString(GenderSerializer, "\"Male\"")
        assertEquals(Gender.MALE, result)

        assertEquals(Gender.FEMALE, json.decodeFromString(GenderSerializer, "\"Female\""))
        assertEquals(Gender.NON_BINARY, json.decodeFromString(GenderSerializer, "\"Non-Binary\""))
        assertEquals(Gender.OTHER, json.decodeFromString(GenderSerializer, "\"Other\""))
    }

    @Test
    fun `GenderSerializer should throw exception for invalid display name`() {
        val exception = assertThrows<IllegalArgumentException> {
            json.decodeFromString(GenderSerializer, "\"INVALID\"")
        }
        assertTrue(exception.message!!.contains("Unknown Gender displayName: 'INVALID'"))
    }

    @Test
    fun `GenderSerializer should accept old enum format for backward compatibility`() {
        // Now accepts enum names for backward compatibility
        assertEquals(Gender.MALE, json.decodeFromString(GenderSerializer, "\"MALE\""))
        assertEquals(Gender.FEMALE, json.decodeFromString(GenderSerializer, "\"FEMALE\""))
        assertEquals(Gender.NON_BINARY, json.decodeFromString(GenderSerializer, "\"NON_BINARY\""))
    }

    // ========================================
    // EducationLevel Serializer Tests
    // ========================================

    @Test
    fun `EducationLevelSerializer should serialize enum to display name`() {
        assertEquals("\"College\"", json.encodeToString(EducationLevelSerializer, EducationLevel.COLLEGE))
        assertEquals("\"University\"", json.encodeToString(EducationLevelSerializer, EducationLevel.UNIVERSITY))
        assertEquals("\"Apprenticeship\"", json.encodeToString(EducationLevelSerializer, EducationLevel.APPRENTICESHIP))
        assertEquals("\"Prefer not to say\"", json.encodeToString(EducationLevelSerializer, EducationLevel.PREFER_NOT_TO_SAY))
    }

    @Test
    fun `EducationLevelSerializer should deserialize display name to enum`() {
        assertEquals(EducationLevel.COLLEGE, json.decodeFromString(EducationLevelSerializer, "\"College\""))
        assertEquals(EducationLevel.UNIVERSITY, json.decodeFromString(EducationLevelSerializer, "\"University\""))
        assertEquals(EducationLevel.APPRENTICESHIP, json.decodeFromString(EducationLevelSerializer, "\"Apprenticeship\""))
        assertEquals(EducationLevel.PREFER_NOT_TO_SAY, json.decodeFromString(EducationLevelSerializer, "\"Prefer not to say\""))
    }

    @Test
    fun `EducationLevelSerializer should accept old enum format for backward compatibility`() {
        // Now accepts enum names for backward compatibility
        assertEquals(EducationLevel.UNIVERSITY, json.decodeFromString(EducationLevelSerializer, "\"UNIVERSITY\""))
        assertEquals(EducationLevel.COLLEGE, json.decodeFromString(EducationLevelSerializer, "\"COLLEGE\""))
    }

    // ========================================
    // Language Serializer Tests
    // ========================================

    @Test
    fun `LanguageSerializer should serialize enum to display name`() {
        assertEquals("\"English\"", json.encodeToString(LanguageSerializer, Language.ENGLISH))
        assertEquals("\"Spanish\"", json.encodeToString(LanguageSerializer, Language.SPANISH))
        assertEquals("\"Sign Language\"", json.encodeToString(LanguageSerializer, Language.SIGN_LANGUAGE))
    }

    @Test
    fun `LanguageSerializer should deserialize display name to enum`() {
        assertEquals(Language.ENGLISH, json.decodeFromString(LanguageSerializer, "\"English\""))
        assertEquals(Language.SPANISH, json.decodeFromString(LanguageSerializer, "\"Spanish\""))
        assertEquals(Language.SIGN_LANGUAGE, json.decodeFromString(LanguageSerializer, "\"Sign Language\""))
    }

    @Test
    fun `LanguageSerializer should accept old enum format for backward compatibility`() {
        // Now accepts enum names for backward compatibility
        assertEquals(Language.ENGLISH, json.decodeFromString(LanguageSerializer, "\"ENGLISH\""))
        assertEquals(Language.SPANISH, json.decodeFromString(LanguageSerializer, "\"SPANISH\""))
    }

    // ========================================
    // Trait Serializer Tests
    // ========================================

    @Test
    fun `TraitSerializer should serialize enum to display name`() {
        assertEquals("\"Adventurous\"", json.encodeToString(TraitSerializer, Trait.ADVENTUROUS))
        assertEquals("\"Deep Thinker\"", json.encodeToString(TraitSerializer, Trait.DEEP_THINKER))
        assertEquals("\"Family Orientated\"", json.encodeToString(TraitSerializer, Trait.FAMILY_ORIENTATED))
    }

    @Test
    fun `TraitSerializer should deserialize display name to enum`() {
        assertEquals(Trait.ADVENTUROUS, json.decodeFromString(TraitSerializer, "\"Adventurous\""))
        assertEquals(Trait.DEEP_THINKER, json.decodeFromString(TraitSerializer, "\"Deep Thinker\""))
        assertEquals(Trait.FAMILY_ORIENTATED, json.decodeFromString(TraitSerializer, "\"Family Orientated\""))
    }

    @Test
    fun `TraitSerializer should accept old enum format for backward compatibility`() {
        // Now accepts enum names for backward compatibility
        assertEquals(Trait.ADVENTUROUS, json.decodeFromString(TraitSerializer, "\"ADVENTUROUS\""))
    }

    // ========================================
    // Religion Serializer Tests
    // ========================================

    @Test
    fun `ReligionSerializer should serialize enum to display name`() {
        assertEquals("\"Christianity\"", json.encodeToString(ReligionSerializer, Religion.CHRISTIANITY))
        assertEquals("\"Islam\"", json.encodeToString(ReligionSerializer, Religion.ISLAM))
        assertEquals("\"Prefer not to say\"", json.encodeToString(ReligionSerializer, Religion.PREFER_NOT_TO_SAY))
    }

    @Test
    fun `ReligionSerializer should deserialize display name to enum`() {
        assertEquals(Religion.CHRISTIANITY, json.decodeFromString(ReligionSerializer, "\"Christianity\""))
        assertEquals(Religion.ISLAM, json.decodeFromString(ReligionSerializer, "\"Islam\""))
        assertEquals(Religion.PREFER_NOT_TO_SAY, json.decodeFromString(ReligionSerializer, "\"Prefer not to say\""))
    }

    // ========================================
    // PoliticalView Serializer Tests
    // ========================================

    @Test
    fun `PoliticalViewSerializer should serialize enum to display name`() {
        assertEquals("\"Liberal\"", json.encodeToString(PoliticalViewSerializer, PoliticalView.LIBERAL))
        assertEquals("\"Moderate\"", json.encodeToString(PoliticalViewSerializer, PoliticalView.MODERATE))
        assertEquals("\"Conservative\"", json.encodeToString(PoliticalViewSerializer, PoliticalView.CONSERVATIVE))
    }

    @Test
    fun `PoliticalViewSerializer should deserialize display name to enum`() {
        assertEquals(PoliticalView.LIBERAL, json.decodeFromString(PoliticalViewSerializer, "\"Liberal\""))
        assertEquals(PoliticalView.MODERATE, json.decodeFromString(PoliticalViewSerializer, "\"Moderate\""))
        assertEquals(PoliticalView.CONSERVATIVE, json.decodeFromString(PoliticalViewSerializer, "\"Conservative\""))
    }

    // ========================================
    // AlcoholConsumption Serializer Tests
    // ========================================

    @Test
    fun `AlcoholConsumptionSerializer should serialize enum to display name`() {
        assertEquals("\"Never\"", json.encodeToString(AlcoholConsumptionSerializer, AlcoholConsumption.NEVER))
        assertEquals("\"Sometimes\"", json.encodeToString(AlcoholConsumptionSerializer, AlcoholConsumption.SOMETIMES))
        assertEquals("\"Regularly\"", json.encodeToString(AlcoholConsumptionSerializer, AlcoholConsumption.REGULARLY))
    }

    @Test
    fun `AlcoholConsumptionSerializer should deserialize display name to enum`() {
        assertEquals(AlcoholConsumption.NEVER, json.decodeFromString(AlcoholConsumptionSerializer, "\"Never\""))
        assertEquals(AlcoholConsumption.SOMETIMES, json.decodeFromString(AlcoholConsumptionSerializer, "\"Sometimes\""))
        assertEquals(AlcoholConsumption.REGULARLY, json.decodeFromString(AlcoholConsumptionSerializer, "\"Regularly\""))
    }

    // ========================================
    // SmokingStatus Serializer Tests
    // ========================================

    @Test
    fun `SmokingStatusSerializer should serialize enum to display name`() {
        assertEquals("\"Never\"", json.encodeToString(SmokingStatusSerializer, SmokingStatus.NEVER))
        assertEquals("\"Sometimes\"", json.encodeToString(SmokingStatusSerializer, SmokingStatus.SOMETIMES))
        assertEquals("\"Quitting\"", json.encodeToString(SmokingStatusSerializer, SmokingStatus.QUITTING))
    }

    @Test
    fun `SmokingStatusSerializer should deserialize display name to enum`() {
        assertEquals(SmokingStatus.NEVER, json.decodeFromString(SmokingStatusSerializer, "\"Never\""))
        assertEquals(SmokingStatus.SOMETIMES, json.decodeFromString(SmokingStatusSerializer, "\"Sometimes\""))
        assertEquals(SmokingStatus.QUITTING, json.decodeFromString(SmokingStatusSerializer, "\"Quitting\""))
    }

    // ========================================
    // Diet Serializer Tests
    // ========================================

    @Test
    fun `DietSerializer should serialize enum to display name`() {
        assertEquals("\"Omnivore\"", json.encodeToString(DietSerializer, Diet.OMNIVORE))
        assertEquals("\"Vegetarian\"", json.encodeToString(DietSerializer, Diet.VEGETARIAN))
        assertEquals("\"Vegan\"", json.encodeToString(DietSerializer, Diet.VEGAN))
    }

    @Test
    fun `DietSerializer should deserialize display name to enum`() {
        assertEquals(Diet.OMNIVORE, json.decodeFromString(DietSerializer, "\"Omnivore\""))
        assertEquals(Diet.VEGETARIAN, json.decodeFromString(DietSerializer, "\"Vegetarian\""))
        assertEquals(Diet.VEGAN, json.decodeFromString(DietSerializer, "\"Vegan\""))
    }

    // ========================================
    // DateIntentions Serializer Tests
    // ========================================

    @Test
    fun `DateIntentionsSerializer should serialize enum to display name`() {
        assertEquals("\"Casual Dating\"", json.encodeToString(DateIntentionsSerializer, DateIntentions.CASUAL_DATING))
        assertEquals("\"Serious Dating\"", json.encodeToString(DateIntentionsSerializer, DateIntentions.SERIOUS_DATING))
        assertEquals("\"Not Sure\"", json.encodeToString(DateIntentionsSerializer, DateIntentions.NOT_SURE))
    }

    @Test
    fun `DateIntentionsSerializer should deserialize display name to enum`() {
        assertEquals(DateIntentions.CASUAL_DATING, json.decodeFromString(DateIntentionsSerializer, "\"Casual Dating\""))
        assertEquals(DateIntentions.SERIOUS_DATING, json.decodeFromString(DateIntentionsSerializer, "\"Serious Dating\""))
        assertEquals(DateIntentions.NOT_SURE, json.decodeFromString(DateIntentionsSerializer, "\"Not Sure\""))
    }

    @Test
    fun `DateIntentionsSerializer should accept old enum format for backward compatibility`() {
        // Now accepts enum names for backward compatibility
        assertEquals(DateIntentions.SERIOUS_DATING, json.decodeFromString(DateIntentionsSerializer, "\"SERIOUS_DATING\""))
    }

    // ========================================
    // RelationshipType Serializer Tests
    // ========================================

    @Test
    fun `RelationshipTypeSerializer should serialize enum to display name`() {
        assertEquals("\"Monogamous\"", json.encodeToString(RelationshipTypeSerializer, RelationshipType.MONOGAMOUS))
        assertEquals("\"Non-Monogamous\"", json.encodeToString(RelationshipTypeSerializer, RelationshipType.NON_MONOGAMOUS))
        assertEquals("\"Open\"", json.encodeToString(RelationshipTypeSerializer, RelationshipType.OPEN))
    }

    @Test
    fun `RelationshipTypeSerializer should deserialize display name to enum`() {
        assertEquals(RelationshipType.MONOGAMOUS, json.decodeFromString(RelationshipTypeSerializer, "\"Monogamous\""))
        assertEquals(RelationshipType.NON_MONOGAMOUS, json.decodeFromString(RelationshipTypeSerializer, "\"Non-Monogamous\""))
        assertEquals(RelationshipType.OPEN, json.decodeFromString(RelationshipTypeSerializer, "\"Open\""))
    }

    // ========================================
    // KidsPreference Serializer Tests
    // ========================================

    @Test
    fun `KidsPreferenceSerializer should serialize enum to display name`() {
        assertEquals("\"Want Kids\"", json.encodeToString(KidsPreferenceSerializer, KidsPreference.WANT_KIDS))
        assertEquals("\"Don't Want Kids\"", json.encodeToString(KidsPreferenceSerializer, KidsPreference.DONT_WANT_KIDS))
        assertEquals("\"Open to Kids\"", json.encodeToString(KidsPreferenceSerializer, KidsPreference.OPEN_TO_KIDS))
    }

    @Test
    fun `KidsPreferenceSerializer should deserialize display name to enum`() {
        assertEquals(KidsPreference.WANT_KIDS, json.decodeFromString(KidsPreferenceSerializer, "\"Want Kids\""))
        assertEquals(KidsPreference.DONT_WANT_KIDS, json.decodeFromString(KidsPreferenceSerializer, "\"Don't Want Kids\""))
        assertEquals(KidsPreference.OPEN_TO_KIDS, json.decodeFromString(KidsPreferenceSerializer, "\"Open to Kids\""))
    }

    // ========================================
    // SexualOrientation Serializer Tests
    // ========================================

    @Test
    fun `SexualOrientationSerializer should serialize enum to display name`() {
        assertEquals("\"Straight\"", json.encodeToString(SexualOrientationSerializer, SexualOrientation.STRAIGHT))
        assertEquals("\"Gay\"", json.encodeToString(SexualOrientationSerializer, SexualOrientation.GAY))
        assertEquals("\"Bisexual\"", json.encodeToString(SexualOrientationSerializer, SexualOrientation.BISEXUAL))
    }

    @Test
    fun `SexualOrientationSerializer should deserialize display name to enum`() {
        assertEquals(SexualOrientation.STRAIGHT, json.decodeFromString(SexualOrientationSerializer, "\"Straight\""))
        assertEquals(SexualOrientation.GAY, json.decodeFromString(SexualOrientationSerializer, "\"Gay\""))
        assertEquals(SexualOrientation.BISEXUAL, json.decodeFromString(SexualOrientationSerializer, "\"Bisexual\""))
    }

    // ========================================
    // Pronouns Serializer Tests
    // ========================================

    @Test
    fun `PronounsSerializer should serialize enum to display name`() {
        assertEquals("\"He/Him\"", json.encodeToString(PronounsSerializer, Pronouns.HE_HIM))
        assertEquals("\"She/Her\"", json.encodeToString(PronounsSerializer, Pronouns.SHE_HER))
        assertEquals("\"They/Them\"", json.encodeToString(PronounsSerializer, Pronouns.THEY_THEM))
    }

    @Test
    fun `PronounsSerializer should deserialize display name to enum`() {
        assertEquals(Pronouns.HE_HIM, json.decodeFromString(PronounsSerializer, "\"He/Him\""))
        assertEquals(Pronouns.SHE_HER, json.decodeFromString(PronounsSerializer, "\"She/Her\""))
        assertEquals(Pronouns.THEY_THEM, json.decodeFromString(PronounsSerializer, "\"They/Them\""))
    }

    @Test
    fun `PronounsSerializer should accept old enum format for backward compatibility`() {
        // Now accepts enum names for backward compatibility
        assertEquals(Pronouns.HE_HIM, json.decodeFromString(PronounsSerializer, "\"HE_HIM\""))
    }

    // ========================================
    // StarSign Serializer Tests
    // ========================================

    @Test
    fun `StarSignSerializer should serialize enum to display name`() {
        assertEquals("\"Aries\"", json.encodeToString(StarSignSerializer, StarSign.ARIES))
        assertEquals("\"Taurus\"", json.encodeToString(StarSignSerializer, StarSign.TAURUS))
        assertEquals("\"Sagittarius\"", json.encodeToString(StarSignSerializer, StarSign.SAGITTARIUS))
    }

    @Test
    fun `StarSignSerializer should deserialize display name to enum`() {
        assertEquals(StarSign.ARIES, json.decodeFromString(StarSignSerializer, "\"Aries\""))
        assertEquals(StarSign.TAURUS, json.decodeFromString(StarSignSerializer, "\"Taurus\""))
        assertEquals(StarSign.SAGITTARIUS, json.decodeFromString(StarSignSerializer, "\"Sagittarius\""))
    }

    // ========================================
    // BrainAttribute Serializer Tests
    // ========================================

    @Test
    fun `BrainAttributeSerializer should serialize enum to display name`() {
        assertEquals("\"I have AD(H)D\"", json.encodeToString(BrainAttributeSerializer, BrainAttribute.ADHD))
        assertEquals("\"I'm neurodivergent\"", json.encodeToString(BrainAttributeSerializer, BrainAttribute.NEURODIVERGENT))
        assertEquals("\"I'm an HSP\"", json.encodeToString(BrainAttributeSerializer, BrainAttribute.HSP))
    }

    @Test
    fun `BrainAttributeSerializer should deserialize display name to enum`() {
        assertEquals(BrainAttribute.ADHD, json.decodeFromString(BrainAttributeSerializer, "\"I have AD(H)D\""))
        assertEquals(BrainAttribute.NEURODIVERGENT, json.decodeFromString(BrainAttributeSerializer, "\"I'm neurodivergent\""))
        assertEquals(BrainAttribute.HSP, json.decodeFromString(BrainAttributeSerializer, "\"I'm an HSP\""))
    }

    @Test
    fun `BrainAttributeSerializer should accept old enum format for backward compatibility`() {
        // Now accepts enum names for backward compatibility
        assertEquals(BrainAttribute.ADHD, json.decodeFromString(BrainAttributeSerializer, "\"ADHD\""))
    }

    // ========================================
    // BodyAttribute Serializer Tests
    // ========================================

    @Test
    fun `BodyAttributeSerializer should serialize enum to display name`() {
        assertEquals("\"I have a chronic illness\"", json.encodeToString(BodyAttributeSerializer, BodyAttribute.CHRONIC_ILLNESS))
        assertEquals("\"I use a wheelchair\"", json.encodeToString(BodyAttributeSerializer, BodyAttribute.WHEELCHAIR))
        assertEquals("\"I'm deaf\"", json.encodeToString(BodyAttributeSerializer, BodyAttribute.DEAF))
    }

    @Test
    fun `BodyAttributeSerializer should deserialize display name to enum`() {
        assertEquals(BodyAttribute.CHRONIC_ILLNESS, json.decodeFromString(BodyAttributeSerializer, "\"I have a chronic illness\""))
        assertEquals(BodyAttribute.WHEELCHAIR, json.decodeFromString(BodyAttributeSerializer, "\"I use a wheelchair\""))
        assertEquals(BodyAttribute.DEAF, json.decodeFromString(BodyAttributeSerializer, "\"I'm deaf\""))
    }

    @Test
    fun `BodyAttributeSerializer should accept old enum format for backward compatibility`() {
        // Now accepts enum names for backward compatibility
        assertEquals(BodyAttribute.WHEELCHAIR, json.decodeFromString(BodyAttributeSerializer, "\"WHEELCHAIR\""))
    }

    // ========================================
    // Ethnicity Serializer Tests
    // ========================================

    @Test
    fun `EthnicitySerializer should serialize enum to display name`() {
        assertEquals("\"White/Caucasian\"", json.encodeToString(EthnicitySerializer, Ethnicity.WHITE_CAUCASIAN))
        assertEquals("\"Black/African Descent\"", json.encodeToString(EthnicitySerializer, Ethnicity.BLACK_AFRICAN_DESCENT))
        assertEquals("\"South Asian\"", json.encodeToString(EthnicitySerializer, Ethnicity.SOUTH_ASIAN))
    }

    @Test
    fun `EthnicitySerializer should deserialize display name to enum`() {
        assertEquals(Ethnicity.WHITE_CAUCASIAN, json.decodeFromString(EthnicitySerializer, "\"White/Caucasian\""))
        assertEquals(Ethnicity.BLACK_AFRICAN_DESCENT, json.decodeFromString(EthnicitySerializer, "\"Black/African Descent\""))
        assertEquals(Ethnicity.SOUTH_ASIAN, json.decodeFromString(EthnicitySerializer, "\"South Asian\""))
    }

    @Test
    fun `EthnicitySerializer should accept old enum format for backward compatibility`() {
        // Now accepts enum names for backward compatibility
        assertEquals(Ethnicity.WHITE_CAUCASIAN, json.decodeFromString(EthnicitySerializer, "\"WHITE_CAUCASIAN\""))
    }
}
