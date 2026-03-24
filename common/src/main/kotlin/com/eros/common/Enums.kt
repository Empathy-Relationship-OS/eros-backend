package com.eros.common

/**
 * Provides the date activities along with their associated cost.
 */
enum class DateActivity(val tokenCost: Double) {
    DRINKS(1.0),          // Includes first drink
    COFFEE(0.5),          // Coffee shop
    WALK_TALK(0.5),       // Free activity, small commitment
    DINNER(1.5),          // Meal included
    MUSEUM(1.0),          // Entry ticket included
    ACTIVITY(1.5),        // Bowling, mini-golf, etc.
    BRUNCH(1.0)           // Weekend brunch
}