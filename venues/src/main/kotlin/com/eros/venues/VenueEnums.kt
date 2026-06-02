package com.eros.venues

enum class PartnerStatus {
    PARTNER,          // Official venue partner with commission
    LISTED,           // Listed venue, no partnership
    TESTING           // Testing phase
}

enum class PriceRange {
    BUDGET,
    MID_RANGE,
    PREMIUM,
    LUXURY
}

enum class DressCode {
    CASUAL,
    SMART_CASUAL,
    FORMAL
}

enum class IndoorOutdoor {
    INDOOR,
    OUTDOOR,
    BOTH
}