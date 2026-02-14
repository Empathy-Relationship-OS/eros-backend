package com.eros.users.models.validation

import com.eros.users.models.*


/**
 * Helper object for validating enum values from string inputs
 * Useful when deserializing from JSON or validating API requests
 */
object EnumValidator {
    
    /**
     * Validate and convert string to Gender enum
     */
    fun toGender(value: String): Gender? = 
        Gender.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to EducationLevel enum
     */
    fun toEducationLevel(value: String): EducationLevel? = 
        EducationLevel.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to Religion enum
     */
    fun toReligion(value: String): Religion? = 
        Religion.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to PoliticalView enum
     */
    fun toPoliticalView(value: String): PoliticalView? = 
        PoliticalView.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to AlcoholConsumption enum
     */
    fun toAlcoholConsumption(value: String): AlcoholConsumption? = 
        AlcoholConsumption.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to SmokingStatus enum
     */
    fun toSmokingStatus(value: String): SmokingStatus? = 
        SmokingStatus.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to Diet enum
     */
    fun toDiet(value: String): Diet? = 
        Diet.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to DateIntentions enum
     */
    fun toDateIntentions(value: String): DateIntentions? = 
        DateIntentions.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to RelationshipType enum
     */
    fun toRelationshipType(value: String): RelationshipType? = 
        RelationshipType.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to KidsPreference enum
     */
    fun toKidsPreference(value: String): KidsPreference? = 
        KidsPreference.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to SexualOrientation enum
     */
    fun toSexualOrientation(value: String): SexualOrientation? = 
        SexualOrientation.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to Pronouns enum
     */
    fun toPronouns(value: String): Pronouns? = 
        Pronouns.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to StarSign enum
     */
    fun toStarSign(value: String): StarSign? = 
        StarSign.entries.find { it.name.equals(value, ignoreCase = true) }

    /**
     * Validate and convert string to Language enum
     */
    fun toLanguage(value: String): Language? = 
        Language.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to Trait enum
     */
    fun toTrait(value: String): Trait? = 
        Trait.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to PredefinedQuestion enum
     */
    fun toPredefinedQuestion(value: String): PredefinedQuestion? = 
        PredefinedQuestion.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate and convert string to MediaType enum
     */
    fun toMediaType(value: String): MediaType? = 
        MediaType.entries.find { it.name.equals(value, ignoreCase = true) }
    
    /**
     * Validate interest string against all interest-related enums
     * Returns true if the string matches any of: Activity, Interest, Entertainment, 
     * Creative, MusicGenre, FoodAndDrink, Sport
     */
    fun isValidInterest(value: String): Boolean {
        return Activity.entries.any { it.name.equals(value, ignoreCase = true) } ||
               Interest.entries.any { it.name.equals(value, ignoreCase = true) } ||
               Entertainment.entries.any { it.name.equals(value, ignoreCase = true) } ||
               Creative.entries.any { it.name.equals(value, ignoreCase = true) } ||
               MusicGenre.entries.any { it.name.equals(value, ignoreCase = true) } ||
               FoodAndDrink.entries.any { it.name.equals(value, ignoreCase = true) } ||
               Sport.entries.any { it.name.equals(value, ignoreCase = true) }
    }
    
    /**
     * Validate a list of interest strings
     */
    fun validateInterests(interests: List<String>): Boolean {
        return interests.all { isValidInterest(it) }
    }
    
    /**
     * Validate a list of trait strings
     */
    fun validateTraits(traits: List<String>): Boolean {
        return traits.all { trait -> 
            Trait.entries.any { it.name.equals(trait, ignoreCase = true) }
        }
    }
    
    /**
     * Validate a list of language strings
     */
    fun validateLanguages(languages: List<String>): Boolean {
        return languages.all { lang -> 
            Language.entries.any { it.name.equals(lang, ignoreCase = true) }
        }
    }

    /**
     * Validate a list of brain attribute strings
     */
    fun validateBrainAttributes(attributes: List<String>): Boolean {
        return attributes.all { attr ->
            BrainAttribute.entries.any { it.name.equals(attr, ignoreCase = true) }
        }
    }

    /**
     * Validate a list of body attribute strings
     */
    fun validateBodyAttributes(attributes: List<String>): Boolean {
        return attributes.all { attr ->
            BodyAttribute.entries.any { it.name.equals(attr, ignoreCase = true) }
        }
    }
}

/**
 * Extension functions for easier enum validation
 */
fun String.toGenderOrNull(): Gender? = EnumValidator.toGender(this)
fun String.toEducationLevelOrNull(): EducationLevel? = EnumValidator.toEducationLevel(this)
fun String.toLanguageOrNull(): Language? = EnumValidator.toLanguage(this)
fun String.toTraitOrNull(): Trait? = EnumValidator.toTrait(this)
fun String.toPredefinedQuestionOrNull(): PredefinedQuestion? = EnumValidator.toPredefinedQuestion(this)
fun String.toMediaTypeOrNull(): MediaType? = EnumValidator.toMediaType(this)

fun List<String>.areValidInterests(): Boolean = EnumValidator.validateInterests(this)
fun List<String>.areValidTraits(): Boolean = EnumValidator.validateTraits(this)
fun List<String>.areValidLanguages(): Boolean = EnumValidator.validateLanguages(this)
fun List<String>.areValidBrainAttributes(): Boolean = EnumValidator.validateBrainAttributes(this)
fun List<String>.areValidBodyAttributes(): Boolean = EnumValidator.validateBodyAttributes(this)