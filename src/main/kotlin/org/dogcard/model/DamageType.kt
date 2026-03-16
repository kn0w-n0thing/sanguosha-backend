package org.dogcard.model

enum class DamageType {
    NORMAL,  // standard Attack; blocked by BENEVOLENT_SHIELD (♠/♣), RATTAN_ARMOR
    FIRE,    // Fire Attack or VERMILION_BIRD_FAN conversion; RATTAN_ARMOR takes +1
    THUNDER, // Thunder Attack; bypasses BENEVOLENT_SHIELD
}

/** The damage type an attack card produces, or null for non-attack cards. */
val CardType.damageType: DamageType?
    get() = when (this) {
        CardType.ATTACK        -> DamageType.NORMAL
        CardType.FIRE_ATTACK   -> DamageType.FIRE
        CardType.THUNDER_ATTACK -> DamageType.THUNDER
        else                   -> null
    }