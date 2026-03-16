package org.dogcard.model

enum class EquipmentSlot {
    WEAPON,
    ARMOR,
    OFFENSIVE_HORSE,  // your attack range +1
    DEFENSIVE_HORSE,  // others' attack range to you −1
}

/** The equipment slot this card occupies, or null if it is not an equipment card. */
val CardType.equipmentSlot: EquipmentSlot?
    get() = when (this) {
        CardType.ZHUGE_CROSSBOW,
        CardType.VERMILION_BIRD_FAN,
        CardType.BOULDER_AXE,
        CardType.WU_SIX_SWORD,
        CardType.THREE_POINTED_BLADE,
        CardType.QILIN_BOW,
        CardType.GENDER_SWORDS,
        CardType.ICE_SWORD,
        CardType.QINGGANG_SWORD,
        CardType.SNAKE_SPEAR,
        CardType.GREEN_DRAGON_CRESCENT_BLADE,
        CardType.HALBERD -> EquipmentSlot.WEAPON

        CardType.RATTAN_ARMOR,
        CardType.BENEVOLENT_SHIELD,
        CardType.SILVER_LION,
        CardType.EIGHT_TRIGRAMS -> EquipmentSlot.ARMOR

        CardType.CHITU,
        CardType.ZIXING,
        CardType.DAYUAN -> EquipmentSlot.DEFENSIVE_HORSE

        CardType.DILU,
        CardType.JUEYING,
        CardType.ZHUHUANGFEIDIAN -> EquipmentSlot.OFFENSIVE_HORSE

        else -> null
    }

/** Attack range granted by this weapon, or null if not a weapon. */
val CardType.weaponRange: Int?
    get() = when (this) {
        CardType.ZHUGE_CROSSBOW              -> 1
        CardType.VERMILION_BIRD_FAN          -> 4
        CardType.BOULDER_AXE                 -> 3
        CardType.WU_SIX_SWORD               -> 2
        CardType.THREE_POINTED_BLADE         -> 3
        CardType.QILIN_BOW                   -> 5
        CardType.GENDER_SWORDS               -> 2
        CardType.ICE_SWORD                   -> 2
        CardType.QINGGANG_SWORD              -> 2
        CardType.SNAKE_SPEAR                 -> 3
        CardType.GREEN_DRAGON_CRESCENT_BLADE -> 3
        CardType.HALBERD                     -> 4
        else                                 -> null
    }