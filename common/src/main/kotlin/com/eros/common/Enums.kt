package com.eros.common

import java.math.BigDecimal

/**
 * Provides the date activities along with their associated cost.
 */
enum class DateActivity(val tokenCost: BigDecimal) {
    DRINKS(BigDecimal(1.0)),          // Includes first drink
    COFFEE(BigDecimal(0.5)),          // Coffee shop
    WALK_TALK(BigDecimal(0.5)),       // Free activity, small commitment
    DINNER(BigDecimal(1.5)),          // Meal included
    MUSEUM(BigDecimal(1.0)),          // Entry ticket included
    ACTIVITY(BigDecimal(1.5)),        // Bowling, mini-golf, etc.
    BRUNCH(BigDecimal(1.0))           // Weekend brunch
}