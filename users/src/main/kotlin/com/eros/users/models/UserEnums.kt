package com.eros.users.models

/**
 * Gender options for user profile
 */
enum class Gender {
    MALE,
    FEMALE,
    NON_BINARY,
    OTHER
}

/**
 * Education level completed
 */
enum class EducationLevel {
    COLLEGE,
    UNIVERSITY,
    APPRENTICESHIP,
    PREFER_NOT_TO_SAY
}

/**
 * Religious affiliation
 */
enum class Religion {
    CHRISTIANITY,
    ISLAM,
    HINDUISM,
    BUDDHISM,
    JUDAISM,
    SIKHISM,
    ATHEIST,
    AGNOSTIC,
    SPIRITUAL,
    OTHER,
    PREFER_NOT_TO_SAY
}

/**
 * Political orientation
 */
enum class PoliticalView {
    LIBERAL,
    MODERATE,
    CONSERVATIVE,
    APOLITICAL,
    OTHER,
    PREFER_NOT_TO_SAY
}

/**
 * Alcohol consumption habits
 */
enum class AlcoholConsumption {
    NEVER,
    SOMETIMES,
    REGULARLY,
    PREFER_NOT_TO_SAY
}

/**
 * Smoking status
 */
enum class SmokingStatus {
    NEVER,
    SOMETIMES,
    REGULARLY,
    QUITTING,
    PREFER_NOT_TO_SAY
}

/**
 * Dietary preferences
 */
enum class Diet {
    OMNIVORE,
    FLEXITARIAN,
    VEGETARIAN,
    VEGAN,
    PESCATARIAN,
    HALAL,
    KOSHER,
    OTHER,
    PREFER_NOT_TO_SAY
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
enum class DateIntentions {
    CASUAL_DATING,
    SERIOUS_DATING,
    FRIENDSHIP,
    NETWORKING,
    NOT_SURE
}

/**
 * Preferred relationship type
 */
enum class RelationshipType {
    MONOGAMOUS,
    NON_MONOGAMOUS,
    OPEN
}

/**
 * Stance on having children
 */
enum class KidsPreference {
    WANT_KIDS,
    DONT_WANT_KIDS,
    HAVE_KIDS,
    OPEN_TO_KIDS,
    PREFER_NOT_TO_SAY
}

/**
 * Sexual orientation
 */
enum class SexualOrientation {
    STRAIGHT,
    GAY,
    LESBIAN,
    BISEXUAL,
    PANSEXUAL,
    ASEXUAL,
    QUESTIONING,
    OTHER,
    PREFER_NOT_TO_SAY
}

/**
 * Gender pronouns - User class uses String because always changing
 */
enum class Pronouns {
    HE_HIM,
    SHE_HER,
    THEY_THEM,
    OTHER,
    PREFER_NOT_TO_SAY;
}

/**
 * Zodiac star signs
 */
enum class StarSign {
    ARIES,
    TAURUS,
    GEMINI,
    CANCER,
    LEO,
    VIRGO,
    LIBRA,
    SCORPIO,
    SAGITTARIUS,
    CAPRICORN,
    AQUARIUS,
    PISCES
}

/**
 * Hobbies and interests - Activities section
 */
enum class Activity {
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
    THRIFTING
}

/**
 * Hobbies and interests - General interests
 */
enum class Interest {
    ENTREPRENEURSHIP,
    FORMULA_1,
    LANGUAGES,
    AI,
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
enum class Entertainment {
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
    SCI_FI,
    SITCOMS,
    TRUE_CRIME,
    VINYL_RECORDS,
    YOUTUBE
}

/**
 * Creative pursuits
 */
enum class Creative {
    ACTING,
    COMPOSING_MUSIC,
    CRAFTS,
    DIY,
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
enum class MusicGenre {
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
    EDM,
    FUNK,
    GUITAR,
    HARDSTYLE,
    HIPHOP,
    HOUSE,
    INDIE_MUSIC,
    JAZZ,
    K_POP,
    LATIN_MUSIC,
    METAL,
    PIANO,
    POP_MUSIC,
    PUNK,
    R_AND_B,
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
enum class FoodAndDrink {
    BBQ,
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
enum class Sport {
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
    SUP,
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
enum class Trait {
    ADVENTUROUS,
    AMBITIOUS,
    SPONTANEOUS,
    ENERGETIC,
    HONEST,
    WITTY,
    FAMILY_ORIENTATED,
    MINIMALIST,
    HEALTH_CONSCIOUS,
    COMPETITION_SEEKER,
    AMBIVERT,
    CALM,
    CARING,
    CHAOTIC,
    CREATIVE,
    CURIOUS,
    DEEP_THINKER,
    DREAMER,
    EMPATHETIC,
    EXTRAVERT,
    FLEXIBLE,
    GENEROUS,
    GO_GETTER,
    INTROVERT,
    KIND,
    LISTENER,
    OPEN_MINDED,
    OPTIMISTIC,
    ORGANIZED,
    OUTGOING,
    PASSIONATE,
    PATIENT,
    PLAYFUL,
    PRACTICAL,
    QUIET,
    RELIABLE,
    RESERVED,
    ROMANTIC,
    SARCASTIC,
    SENSITIVE,
    SERIOUS,
    SHY,
    THOUGHTFUL,
    TRADITIONAL
}

/**
 * Languages spoken
 */
enum class Language {
    ENGLISH,
    SPANISH,
    FRENCH,
    GERMAN,
    ITALIAN,
    PORTUGUESE,
    RUSSIAN,
    CHINESE,
    JAPANESE,
    KOREAN,
    ARABIC,
    HINDI,
    BENGALI,
    PUNJABI,
    URDU,
    TURKISH,
    VIETNAMESE,
    POLISH,
    DUTCH,
    GREEK,
    SWEDISH,
    NORWEGIAN,
    DANISH,
    FINNISH,
    CZECH,
    ROMANIAN,
    HUNGARIAN,
    HEBREW,
    THAI,
    INDONESIAN,
    MALAY,
    TAGALOG,
    SWAHILI,
    SIGN_LANGUAGE
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
enum class BrainAttribute {
    ADHD,
    LEARNING_DISABILITY,
    MENTAL_HEALTH_CHALLENGES,
    HSP, // Highly Sensitive Person
    AUTISTIC,
    NEURODIVERGENT;

    fun getDisplayText(): String = when (this) {
        ADHD -> "I have AD(H)D"
        LEARNING_DISABILITY -> "I have a learning disability"
        MENTAL_HEALTH_CHALLENGES -> "I have mental health challenges"
        HSP -> "I'm an HSP"
        AUTISTIC -> "I'm autistic"
        NEURODIVERGENT -> "I'm neurodivergent"
    }
}

/**
 * Body attributes - physical health and accessibility
 * Optional multi-select field with description support
 */
enum class BodyAttribute {
    CHRONIC_ILLNESS,
    VISUAL_IMPAIRMENT,
    DEAF,
    IMMUNOCOMPROMISED,
    MOBILITY_AID,
    WHEELCHAIR;

    fun getDisplayText(): String = when (this) {
        CHRONIC_ILLNESS -> "I have a chronic illness"
        VISUAL_IMPAIRMENT -> "I have a visual impairment"
        DEAF -> "I'm deaf"
        IMMUNOCOMPROMISED -> "I'm immunocompromised"
        MOBILITY_AID -> "I use a mobility aid"
        WHEELCHAIR -> "I use a wheelchair"
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
    SOUTHEAST_ASIAN("Southeast Asian");

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
