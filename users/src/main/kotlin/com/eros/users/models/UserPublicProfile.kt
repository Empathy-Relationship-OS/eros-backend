package com.eros.users.models

import kotlinx.serialization.Serializable
import kotlin.String

data class PublicProfile(
    val userId: String,
    val name: String,
    val age: Int,
    val height: Int,
    val city: String,
    val language : String,
    val education: String,
    val occupation: String?,
    val badges: Set<String>?,
    val profile: PublicProfileDetails
) {
    companion object {
        fun from(
            user: User,
            userMediaCollection: UserMediaCollection,
            sharedInterests: List<String>
        ): PublicProfile {
            return PublicProfile(
                userId = user.userId,
                name = user.firstName,
                age = user.getAge(),
                height = user.heightCm,
                city = user.city,
                education = user.educationLevel.name,
                occupation = user.occupation,
                badges = user.badges?.map { it.name }?.toSet(),
                language = user.preferredLanguage.name,
                profile = PublicProfileDetails.from(user, sharedInterests,userMediaCollection)
            )
        }
    }
}


@Serializable
data class PublicProfileDTO(
    val userId: String,
    val name: String,
    val age: Int,
    val height: Int,
    val city: String,
    val language : String,
    val education: String,
    val occupation: String?,
    val badges: Set<String>?,
    val profile: PublicProfileDetailsDTO
)


fun PublicProfile.toDTO() = PublicProfileDTO(
    userId = this.userId,
    name = this.name,
    age = this.age,
    height = this.height,
    city = this.city,
    language = this.language,
    education = this.education,
    occupation = this.occupation,
    badges = this.badges,
    profile = this.profile.toDTO()
)


@Serializable
data class HabitsResponse(
    val alcoholConsumption: String?,
    val smokingStatus: String?,
    val diet: String?,
){
    companion object {
        fun from(user:User): HabitsResponse{
            return HabitsResponse(
            smokingStatus = user.smokingStatus.let { if (it.display) it.field?.name else null },
            alcoholConsumption = user.alcoholConsumption.let { if (it.display) it.field?.name else null },
            diet = user.diet.let { if (it.display) it.field?.name else null },
            )
        }
    }
}

@Serializable
data class RelationshipResponse(
    val intention: String?,
    val kidsPreference: String?,
    val relationshipType: String?
){
    companion object {
        fun from(user:User): RelationshipResponse{
            return RelationshipResponse(
                intention = user.dateIntentions.let { if (it.display) it.field.name else null },
                kidsPreference = user.kidsPreference.let { if (it.display) it.field.name else null },
                relationshipType = user.relationshipType.let { if (it.display) it.field.name else null },
            )
        }
    }
}

data class PublicProfileDetails(
    val coverPhoto: String?,
    val photos: List<String>,
    val bio: String?,
    val hobbies: List<String>,
    val traits: List<String>,
    val habits: HabitsResponse,
    val relationshipGoals: RelationshipResponse,
    val sharedInterests: List<String>,

    val spokenLanguages: List<String>?,
    val religion: String?,
    val politicalView : String?,
    val sexualOrientation: String?,
    val pronouns : String?,
    val starSign : String?,
    val ethnicity : List<String>?,

    val brainAttribute: List<String>?,
    val brainDescription: String?,
    val bodyAttribute: List<String>?,
    val bodyDescription: String?,
) {
    companion object {
        fun from(user: User, sharedInterests: List<String>, userMediaCollection: UserMediaCollection): PublicProfileDetails {
            return PublicProfileDetails(
                coverPhoto = userMediaCollection.getPrimaryMedia()?.mediaUrl,
                photos = userMediaCollection.media.map { it.mediaUrl },
                bio = user.bio,
                hobbies = user.interests,
                traits = user.traits.map {it.name},
                habits = HabitsResponse.from(user),
                relationshipGoals = RelationshipResponse.from(user),

                spokenLanguages = user.spokenLanguages.let { if (it.display) it.field.map { it.name } else null },
                religion = user.religion.let { if (it.display) it.field?.name else null },
                politicalView = user.politicalView.let { if (it.display) it.field?.name else null },
                sexualOrientation = user.sexualOrientation.let { if (it.display) it.field.name else null },
                pronouns = user.pronouns.let { if (it.display) it.field?.name else null },
                starSign = user.starSign.let { if (it.display) it.field?.name else null },
                ethnicity = user.ethnicity.let { if (it.display) it.field.map { it.name } else null },
                brainAttribute = user.brainAttributes.let { if (it.display) it.field?.map { it.name } else null },
                brainDescription = user.brainDescription.let { if (it.display) it.field else null },
                bodyAttribute = user.bodyAttributes.let { if (it.display) it.field?.map { it.name } else null },
                bodyDescription = user.bodyDescription.let { if (it.display) it.field else null },
                sharedInterests = sharedInterests
            )
        }
    }
}

@Serializable
data class PublicProfileDetailsDTO(
    val coverPhoto: String?,
    val photos: List<String>,
    val bio: String?,
    val hobbies: List<String>,
    val traits: List<String>,
    val habits: HabitsResponse,
    val relationshipGoals: RelationshipResponse,
    val sharedInterests: List<String>,

    val spokenLanguages: List<String>?,
    val religion: String?,
    val politicalView : String?,
    val sexualOrientation: String?,
    val pronouns : String?,
    val starSign : String?,
    val ethnicity : List<String>?,

    val brainAttribute: List<String>?,
    val brainDescription: String?,
    val bodyAttribute: List<String>?,
    val bodyDescription: String?,
)

fun PublicProfileDetails.toDTO() = PublicProfileDetailsDTO(
    coverPhoto = this.coverPhoto,
    photos = this.photos,
    bio = this.bio,
    hobbies = this.hobbies,
    traits = this.traits,
    habits = this.habits,
    relationshipGoals = this.relationshipGoals,
    sharedInterests = this.sharedInterests,
    
    spokenLanguages = this.spokenLanguages,
    religion = this.religion,
    politicalView  = this.politicalView,
    sexualOrientation = this.sexualOrientation,
    pronouns  = this.pronouns,
    starSign  = this.starSign,
    ethnicity  = this.ethnicity,
    
    brainAttribute = this.brainAttribute,
    brainDescription = this.brainDescription,
    bodyAttribute = this.bodyAttribute,
    bodyDescription = this.bodyDescription,
)


/**
 * Lightweight DTO for user profile data used in matching.
 *
 * This DTO provides minimal user information needed for match presentation,
 * avoiding exposure of sensitive profile data across module boundaries.
 */
data class UserMatchProfileData(
    val userId: String,
    val name: String,
    val age: Int,
    val thumbnailUrl: String?,
    val badges: Set<String>?
)