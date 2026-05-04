package com.eros.users.models

/**
 * Gender options for user profile
 */
enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    NON_BINARY("Non-Binary"),
    OTHER("Other");

    companion object {
        fun fromDisplayName(displayName: String): Gender? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Education level completed
 */
enum class EducationLevel(val displayName: String) {
    COLLEGE("College"),
    UNIVERSITY("University"),
    APPRENTICESHIP("Apprenticeship"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    companion object {
        fun fromDisplayName(displayName: String): EducationLevel? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Religious affiliation
 */
enum class Religion(val displayName: String) {
    CHRISTIANITY("Christianity"),
    ISLAM("Islam"),
    HINDUISM("Hinduism"),
    BUDDHISM("Buddhism"),
    JUDAISM("Judaism"),
    SIKHISM("Sikhism"),
    ATHEIST("Atheist"),
    AGNOSTIC("Agnostic"),
    SPIRITUAL("Spiritual"),
    OTHER("Other"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    companion object {
        fun fromDisplayName(displayName: String): Religion? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Political orientation
 */
enum class PoliticalView(val displayName: String) {
    LIBERAL("Liberal"),
    MODERATE("Moderate"),
    CONSERVATIVE("Conservative"),
    APOLITICAL("Apolitical"),
    OTHER("Other"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    companion object {
        fun fromDisplayName(displayName: String): PoliticalView? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Alcohol consumption habits
 */
enum class AlcoholConsumption(val displayName: String) {
    NEVER("Never"),
    SOMETIMES("Sometimes"),
    REGULARLY("Regularly"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    companion object {
        fun fromDisplayName(displayName: String): AlcoholConsumption? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Smoking status
 */
enum class SmokingStatus(val displayName: String) {
    NEVER("Never"),
    SOMETIMES("Sometimes"),
    REGULARLY("Regularly"),
    QUITTING("Quitting"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    companion object {
        fun fromDisplayName(displayName: String): SmokingStatus? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Dietary preferences
 */
enum class Diet(val displayName: String) {
    OMNIVORE("Omnivore"),
    FLEXITARIAN("Flexitarian"),
    VEGETARIAN("Vegetarian"),
    VEGAN("Vegan"),
    PESCATARIAN("Pescatarian"),
    HALAL("Halal"),
    KOSHER("Kosher"),
    OTHER("Other"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    companion object {
        fun fromDisplayName(displayName: String): Diet? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Food allergies - Used for Venue choice
 */
enum class Allergies {
    GLUTEN_FREE,
    LACTOSE_FREE,
}

/**
 * Dating intentions
 */
enum class DateIntentions(val displayName: String) {
    CASUAL_DATING("Casual Dating"),
    SERIOUS_DATING("Serious Dating"),
    FRIENDSHIP("Friendship"),
    NETWORKING("Networking"),
    NOT_SURE("Not Sure");

    companion object {
        fun fromDisplayName(displayName: String): DateIntentions? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Preferred relationship type
 */
enum class RelationshipType(val displayName: String) {
    MONOGAMOUS("Monogamous"),
    NON_MONOGAMOUS("Non-Monogamous"),
    OPEN("Open");

    companion object {
        fun fromDisplayName(displayName: String): RelationshipType? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Stance on having children
 */
enum class KidsPreference(val displayName: String) {
    WANT_KIDS("Want Kids"),
    DONT_WANT_KIDS("Don't Want Kids"),
    HAVE_KIDS("Have Kids"),
    OPEN_TO_KIDS("Open to Kids"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    companion object {
        fun fromDisplayName(displayName: String): KidsPreference? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Sexual orientation
 */
enum class SexualOrientation(val displayName: String) {
    STRAIGHT("Straight"),
    GAY("Gay"),
    LESBIAN("Lesbian"),
    BISEXUAL("Bisexual"),
    PANSEXUAL("Pansexual"),
    ASEXUAL("Asexual"),
    QUESTIONING("Questioning"),
    OTHER("Other"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    companion object {
        fun fromDisplayName(displayName: String): SexualOrientation? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Gender pronouns - User class uses String because always changing
 */
enum class Pronouns(val displayName: String) {
    HE_HIM("He/Him"),
    SHE_HER("She/Her"),
    THEY_THEM("They/Them"),
    OTHER("Other"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    companion object {
        fun fromDisplayName(displayName: String): Pronouns? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Zodiac star signs
 */
enum class StarSign(val displayName: String) {
    ARIES("Aries"),
    TAURUS("Taurus"),
    GEMINI("Gemini"),
    CANCER("Cancer"),
    LEO("Leo"),
    VIRGO("Virgo"),
    LIBRA("Libra"),
    SCORPIO("Scorpio"),
    SAGITTARIUS("Sagittarius"),
    CAPRICORN("Capricorn"),
    AQUARIUS("Aquarius"),
    PISCES("Pisces");

    companion object {
        fun fromDisplayName(displayName: String): StarSign? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Helper function to convert enum names to display format
 * CITY_TRIPS -> "City Trips"
 * FORMULA_1 -> "Formula 1"
 */
private fun String.toDisplayName(): String =
    this.split('_').joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }

/**
 * Base interface for all user interest enums.
 *
 * IMPORTANT: All enum values across Activity, Interest, Entertainment, Creative,
 * MusicGenre, FoodAndDrink, and Sport MUST be unique to avoid database ambiguity.
 *
 * A test enforces this uniqueness constraint.
 */
interface UserInterest {
    /**
     * Display name for frontend presentation.
     * Defaults to automatic conversion (CITY_TRIPS -> "City Trips")
     *
     * Can be overridden for custom formatting:
     * ```
     * FORMULA_1 {
     *     override val displayName = "Formula 1"
     * }
     * ```
     */
    val displayName: String
        get() = (this as Enum<*>).name.toDisplayName()
}

/**
 * Hobbies and interests - Activities section
 */
enum class Activity : UserInterest {
    CITY_TRIPS,
    OUTDOORS,
    PUB_QUIZ,
    WELLNESS,
    BACKPACKING,
    BAKING,
    BEACH,
    CAMPING,
    CONCERTS,
    COOKING,
    COSPLAY,
    DINING_OUT,
    DINNER_PARTIES,
    ESCAPE_ROOMS,
    FESTIVALS,
    HAVING_DRINKS,
    HIKING,
    KARAOKE,
    MEDITATION,
    MOUNTAINS,
    MUSEUM,
    PARTY,
    RESORT_VACATIONS,
    ROAD_TRIPS,
    SAUNA,
    SHOPPING,
    TAKING_A_WALK,
    THRIFTING;

    override val displayName: String
        get() = name.toDisplayName()
}

/**
 * Hobbies and interests - General interests
 */
enum class Interest : UserInterest{
    ENTREPRENEURSHIP,
    FORMULA_1,
    LANGUAGES,
    AI {
        override val displayName = "AI"
    },
    ANIMALS,
    ARCHITECTURE,
    ART,
    BIOLOGY,
    CARS,
    CATS,
    CINEMA,
    DOGS,
    FINANCE,
    HISTORY,
    HORSES,
    NATURE,
    PERSONAL_DEVELOPMENT,
    PHILOSOPHY,
    PLANTS,
    POLITICS,
    PROGRAMMING,
    PSYCHOLOGY,
    SCIENCE,
    SNEAKERS,
    SUSTAINABILITY,
    TATTOOS,
    TECH,
    THEATRE
}

/**
 * Entertainment preferences
 */
enum class Entertainment : UserInterest {
    READING,
    ANIME,
    BOARD_GAMES,
    CARTOONS,
    CHESS,
    COMEDY,
    COMICS,
    DISNEY,
    DOCUMENTARIES,
    FANTASY,
    GAMING,
    HORROR,
    MEMES,
    MOVIES,
    MUSICALS,
    NETFLIX,
    PODCASTS,
    PUZZLES,
    SCI_FI {
        override val displayName = "Sci-Fi"
    },
    SITCOMS,
    TRUE_CRIME,
    VINYL_RECORDS,
    YOUTUBE
}

/**
 * Creative pursuits
 */
enum class Creative : UserInterest {
    ACTING,
    COMPOSING_MUSIC,
    CRAFTS,
    DIY {
        override val displayName = "DIY"
    },
    DESIGN,
    DRAWING,
    FASHION,
    KNITTING,
    PAINTING,
    PHOTOGRAPHY,
    PLAYING_INSTRUMENTS,
    POETRY,
    POTTERY,
    SEWING,
    SINGING,
    WRITING
}

/**
 * Music genres
 */
enum class MusicGenre : UserInterest {
    AFRO_BEATS,
    BLUES,
    CLASSICAL_MUSIC,
    COUNTRY_MUSIC,
    DJING,
    DANCEHALL,
    DISCO,
    DRUM_AND_BASS,
    DRUMS,
    DUBSTEP,
    EDM {
        override val displayName = "EDM"
    },
    FUNK,
    GUITAR,
    HARDSTYLE,
    HIPHOP {
        override val displayName = "Hip-Hop"
    },
    HOUSE,
    INDIE_MUSIC,
    JAZZ,
    K_POP {
        override val displayName = "K-Pop"
    },
    LATIN_MUSIC,
    METAL,
    PIANO,
    POP_MUSIC,
    PUNK,
    R_AND_B {
        override val displayName = "R&B"
    },
    RAP,
    REGGAE,
    REGGAETON,
    ROCK,
    SALSA,
    SOUL,
    TECHNO
}

/**
 * Food and drink preferences
 */
enum class FoodAndDrink : UserInterest{
    BBQ {
        override val displayName = "BBQ"
    },
    BARBECUE,
    BEER,
    BIRYANI,
    BOBA,
    CHAMPAGNE,
    CHARCUTERIE,
    CHEESE,
    CHOCOLATE,
    COCKTAILS,
    COFFEE,
    CRAFT_BEERS,
    CURRY,
    EMPANADA,
    FALAFEL,
    HOT_POT,
    JOLLOF,
    MATCHA,
    PASTA,
    PIZZA,
    PUBS,
    RAMEN,
    ROTI,
    SUSHI,
    TAPAS,
    TEA,
    WHISKEY,
    WINE
}

/**
 * Sports and physical activities
 */
enum class Sport : UserInterest {
    KICK_BOXING,
    GOLF,
    KITE_SURFING,
    ATHLETICS,
    BADMINTON,
    BALLET,
    BASEBALL,
    BASKETBALL,
    BOULDERING,
    BOWLING,
    CLIMBING,
    CRICKET,
    CROSS_TRAINING,
    CYCLING,
    DANCING,
    EXTREME_SPORTS,
    FENCING,
    FITNESS,
    FOOTBALL,
    HANDBALL,
    HOCKEY,
    HORSE_RIDING,
    ICE_SKATING,
    KAYAKING,
    KITESURFING,
    MARTIAL_ARTS,
    MOTORCYCLING,
    MOUNTAINBIKING,
    PADEL,
    PICKLEBALL,
    PILATES,
    ROWING,
    RUGBY,
    RUNNING,
    SUP {
        override val displayName = "SUP"
    },
    SAILING,
    SCUBA_DIVING,
    SKATEBOARDING,
    SKIING,
    SNOWBOARDING,
    SPIKEBALL,
    SQUASH,
    SURFING,
    SWIMMING,
    TENNIS,
    VOLLEYBALL,
    YOGA
}

/**
 * Personality traits
 */
enum class Trait(val displayName: String) {
    ADVENTUROUS("Adventurous"),
    AMBITIOUS("Ambitious"),
    SPONTANEOUS("Spontaneous"),
    ENERGETIC("Energetic"),
    HONEST("Honest"),
    WITTY("Witty"),
    FAMILY_ORIENTATED("Family Orientated"),
    MINIMALIST("Minimalist"),
    HEALTH_CONSCIOUS("Health Conscious"),
    COMPETITION_SEEKER("Competition Seeker"),
    AMBIVERT("Ambivert"),
    CALM("Calm"),
    CARING("Caring"),
    CHAOTIC("Chaotic"),
    CREATIVE("Creative"),
    CURIOUS("Curious"),
    DEEP_THINKER("Deep Thinker"),
    DREAMER("Dreamer"),
    EMPATHETIC("Empathetic"),
    EXTRAVERT("Extravert"),
    FLEXIBLE("Flexible"),
    GENEROUS("Generous"),
    GO_GETTER("Go Getter"),
    INTROVERT("Introvert"),
    KIND("Kind"),
    LISTENER("Listener"),
    OPEN_MINDED("Open Minded"),
    OPTIMISTIC("Optimistic"),
    ORGANIZED("Organized"),
    OUTGOING("Outgoing"),
    PASSIONATE("Passionate"),
    PATIENT("Patient"),
    PLAYFUL("Playful"),
    PRACTICAL("Practical"),
    QUIET("Quiet"),
    RELIABLE("Reliable"),
    RESERVED("Reserved"),
    ROMANTIC("Romantic"),
    SARCASTIC("Sarcastic"),
    SENSITIVE("Sensitive"),
    SERIOUS("Serious"),
    SHY("Shy"),
    THOUGHTFUL("Thoughtful"),
    TRADITIONAL("Traditional");

    companion object {
        fun fromDisplayName(displayName: String): Trait? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Languages spoken
 */
enum class Language(val displayName: String) {
    ENGLISH("English"),
    SPANISH("Spanish"),
    FRENCH("French"),
    GERMAN("German"),
    ITALIAN("Italian"),
    PORTUGUESE("Portuguese"),
    RUSSIAN("Russian"),
    CHINESE("Chinese"),
    JAPANESE("Japanese"),
    KOREAN("Korean"),
    ARABIC("Arabic"),
    HINDI("Hindi"),
    BENGALI("Bengali"),
    PUNJABI("Punjabi"),
    URDU("Urdu"),
    TURKISH("Turkish"),
    VIETNAMESE("Vietnamese"),
    POLISH("Polish"),
    DUTCH("Dutch"),
    GREEK("Greek"),
    SWEDISH("Swedish"),
    NORWEGIAN("Norwegian"),
    DANISH("Danish"),
    FINNISH("Finnish"),
    CZECH("Czech"),
    ROMANIAN("Romanian"),
    HUNGARIAN("Hungarian"),
    HEBREW("Hebrew"),
    THAI("Thai"),
    INDONESIAN("Indonesian"),
    MALAY("Malay"),
    TAGALOG("Tagalog"),
    SWAHILI("Swahili"),
    SIGN_LANGUAGE("Sign Language");

    companion object {
        fun fromDisplayName(displayName: String): Language? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Predefined Q&A questions
 */
enum class PredefinedQuestion {
    // Fun category
    LAST_MEAL_EVER,
    SECOND_DATE_IDEA,
    PERFECT_HOLIDAY,
    RAINY_SUNDAY_ACTIVITY,
    MYTHICAL_CREATURE_RELATE,
    WEIRDEST_FOOD_COMBO,
    NEVER_FAILS_TO_LAUGH,
    LIFE_THEME_SONG,
    MOST_BEAUTIFUL_VIEW,
    SUPERPOWER_FOR_FUN,
    DINNER_WITH_3_FAMOUS,
    
    // Ambitions category
    WANT_TO_LEARN,
    DREAM_JOB_NO_MONEY,
    LIFE_GOAL,
    PROUD_OF,
    WISH_REALLY_GOOD_AT,
    CHALLENGE_SURPRISED_BEATING,
    RANDOM_BUCKET_LIST,
    IDEAL_PLACE_TO_LIVE,
    LOOKING_FORWARD_TO,
    POINTLESS_SKILL_PROUD_OF,
    
    // Interests category
    TALK_ABOUT_FOR_HOURS,
    READING_AT_MOMENT,
    FAVOURITE_MUSIC_ARTIST,
    FAVOURITE_BOOK_MOVIE_TV,
    THINGS_GIVE_JOY,
    EVERYONE_SHOULD_TRY,
    CURRENTLY_OBSESSED_SONG,
    OBSCURE_FACT_KNOW,
    ACTIVITY_LOSE_MYSELF_IN,
    FICTIONAL_CHARACTER_RELATE,
    NICHE_RABBIT_HOLE,
    
    // Personal category
    UNUSUAL_FIND_ATTRACTIVE,
    CORE_VALUES,
    RANDOM_FACTS,
    MOST_AWKWARD_MOMENT,
    PERSONAL_MOTTO,
    FAVOURITE_TATTOO_STORY,
    WHAT_HOME_MEANS,
    BRINGS_OUT_INNER_CHILD,
    COMPLIMENT_NEVER_FORGOTTEN,
    FRIENDS_COME_TO_ME_FOR,
    EMOJI_CAPTURES_ENERGY,
    LOOKING_FOR,
    RELATIONSHIP_GOALS;
    
    fun getDisplayText(): String = when (this) {
        LAST_MEAL_EVER -> "My last meal ever would be"
        SECOND_DATE_IDEA -> "If I could organize our second date, we would"
        PERFECT_HOLIDAY -> "My perfect holiday"
        RAINY_SUNDAY_ACTIVITY -> "What I like to do on a rainy Sunday"
        MYTHICAL_CREATURE_RELATE -> "The mythical creature I relate to most"
        WEIRDEST_FOOD_COMBO -> "The weirdest food combination I enjoy"
        NEVER_FAILS_TO_LAUGH -> "This never fails to make me laugh"
        LIFE_THEME_SONG -> "If my life had a theme song, it would be"
        MOST_BEAUTIFUL_VIEW -> "The most beautiful view I have ever seen"
        SUPERPOWER_FOR_FUN -> "A superpower I'd like to have just for fun"
        DINNER_WITH_3_FAMOUS -> "I'd host dinner for these 3 famous people"
        WANT_TO_LEARN -> "Something I still want to learn"
        DREAM_JOB_NO_MONEY -> "My dream job if money didn't matter"
        LIFE_GOAL -> "A life goal of mine"
        PROUD_OF -> "What I'm proud of"
        WISH_REALLY_GOOD_AT -> "Something I wish I was really good at"
        CHALLENGE_SURPRISED_BEATING -> "A challenge I surprised myself by beating"
        RANDOM_BUCKET_LIST -> "The most random thing on my bucket list"
        IDEAL_PLACE_TO_LIVE -> "My ideal place to live"
        LOOKING_FORWARD_TO -> "Something I'm currently looking forward to"
        POINTLESS_SKILL_PROUD_OF -> "A pointless skill I'm oddly proud of"
        TALK_ABOUT_FOR_HOURS -> "Something I could talk about for hours"
        READING_AT_MOMENT -> "What I'm reading at the moment"
        FAVOURITE_MUSIC_ARTIST -> "My favourite music artist or band"
        FAVOURITE_BOOK_MOVIE_TV -> "My favourite book/movie/tv series"
        THINGS_GIVE_JOY -> "Things that give me joy"
        EVERYONE_SHOULD_TRY -> "What everyone should try at least once"
        CURRENTLY_OBSESSED_SONG -> "The song I'm currently obsessed with"
        OBSCURE_FACT_KNOW -> "The most obscure fact I know"
        ACTIVITY_LOSE_MYSELF_IN -> "An activity I lose myself in"
        FICTIONAL_CHARACTER_RELATE -> "The fictional character I relate to most"
        NICHE_RABBIT_HOLE -> "A niche rabbit hole that fascinates me"
        UNUSUAL_FIND_ATTRACTIVE -> "Something unusual I find attractive in a person"
        CORE_VALUES -> "My core values"
        RANDOM_FACTS -> "Random facts about me"
        MOST_AWKWARD_MOMENT -> "Most awkward moment of my life"
        PERSONAL_MOTTO -> "My personal motto"
        FAVOURITE_TATTOO_STORY -> "The story behind my favourite tattoo"
        WHAT_HOME_MEANS -> "What 'home' means to me"
        BRINGS_OUT_INNER_CHILD -> "What brings out my inner child"
        COMPLIMENT_NEVER_FORGOTTEN -> "A compliment I've never forgotten"
        FRIENDS_COME_TO_ME_FOR -> "Friends always come to me for"
        EMOJI_CAPTURES_ENERGY -> "The emoji(s) that captures my energy best"
        LOOKING_FOR -> "I'm looking for"
        RELATIONSHIP_GOALS -> "My relationship goals"
    }
}

/**
 * Brain attributes - neurodiversity and mental health
 * Optional multi-select field with description support
 */
enum class BrainAttribute(val displayName: String) {
    ADHD("I have AD(H)D"),
    LEARNING_DISABILITY("I have a learning disability"),
    MENTAL_HEALTH_CHALLENGES("I have mental health challenges"),
    HSP("I'm an HSP"), // Highly Sensitive Person
    AUTISTIC("I'm autistic"),
    NEURODIVERGENT("I'm neurodivergent");

    companion object {
        fun fromDisplayName(displayName: String): BrainAttribute? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Body attributes - physical health and accessibility
 * Optional multi-select field with description support
 */
enum class BodyAttribute(val displayName: String) {
    CHRONIC_ILLNESS("I have a chronic illness"),
    VISUAL_IMPAIRMENT("I have a visual impairment"),
    DEAF("I'm deaf"),
    IMMUNOCOMPROMISED("I'm immunocompromised"),
    MOBILITY_AID("I use a mobility aid"),
    WHEELCHAIR("I use a wheelchair");

    companion object {
        fun fromDisplayName(displayName: String): BodyAttribute? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Ethnicity used for preferences + required
 */
enum class Ethnicity(val displayName: String) {
    BLACK_AFRICAN_DESCENT("Black/African Descent"),
    EAST_ASIAN("East Asian"),
    HISPANIC_LATINO("Hispanic/Latino"),
    MIDDLE_EASTERN("Middle Eastern"),
    NATIVE_AMERICAN("Native American"),
    PACIFIC_ISLANDER("Pacific Islander"),
    SOUTH_ASIAN("South Asian"),
    SOUTHEAST_ASIAN("Southeast Asian"),
    WHITE_CAUCASIAN("White/Caucasian"),
    OTHER("Other");

    companion object {
        fun fromDisplayName(displayName: String): Ethnicity? {
            return entries.find { it.displayName == displayName }
        }
    }
}

/**
 * Media type for user uploads
 */
enum class MediaType {
    PHOTO,
    VIDEO
}

/**
 * Reach Level - for how flexible a user's preferences are used for matching.
 */
enum class ReachLevel() {
    OPEN_MINDED,
    BALANCED,
    SELECTIVE
}

/**
 * Verification Status
 */
enum class ValidationStatus {
    UNVALIDATED,
    PENDING,
    VALIDATED
}

/**
 * Badges for user profiles.
 */
enum class Badge {
    TRUSTED,     // 10+ completed dates
    GOOD_XP,     // Good date experience
    VERIFIED     // ID verification
}

/**
 * Roles for users.
 */
enum class Role{
    USER,
    EMPLOYEE,
    ADMIN,
    BUSINESS
}

/**
 * ProfileStatus
 */
enum class ProfileStatus {
    SLEEP_MODE,
    ACTIVE,
    FROZEN,
    BANNED
}