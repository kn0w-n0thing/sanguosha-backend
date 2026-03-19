package org.dogcard.model.card

enum class CardCategory {
    BASIC,
    INSTANT_TRICK,
    DELAYED_TRICK,
    EQUIPMENT,
}

val CardType.category: CardCategory
    get() = when (this) {
        CardType.ATTACK,
        CardType.FIRE_ATTACK,
        CardType.THUNDER_ATTACK,
        CardType.DODGE,
        CardType.PEACH,
        CardType.WINE -> CardCategory.BASIC

        CardType.ECSTASY,
        CardType.SUPPRESSION,
        CardType.LIGHTNING -> CardCategory.DELAYED_TRICK

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
        CardType.HALBERD,
        CardType.RATTAN_ARMOR,
        CardType.BENEVOLENT_SHIELD,
        CardType.SILVER_LION,
        CardType.EIGHT_TRIGRAMS,
        CardType.CHITU,
        CardType.ZIXING,
        CardType.DAYUAN,
        CardType.DILU,
        CardType.JUEYING,
        CardType.ZHUHUANGFEIDIAN -> CardCategory.EQUIPMENT

        else -> CardCategory.INSTANT_TRICK
    }