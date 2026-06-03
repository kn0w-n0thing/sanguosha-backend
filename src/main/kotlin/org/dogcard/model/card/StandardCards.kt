package org.dogcard.model.card

/**
 * The standard 108-card deck (标准版+EX) shared by Identity mode and 1v1 mode.
 *
 * Layout source: doc/cards.md — Identity Mode deck table.
 * Structure: 13 ranks × 4 suits × 2 cards = 104, plus 4 EX cards = 108.
 * EX cards: 仁王盾(♣️2), 寒冰剑(♠️2), 闪电(♥️Q), 无懈可击(♦️Q).
 */
val StandardCards: List<Card> = listOf(

    // ── Rank A ──────────────────────────────────────────────────────────
    Card(CardType.HAIL_OF_ARROWS,     Suit.HEART,   1, id =  0),
    Card(CardType.PEACH_GARDEN_OATH,  Suit.HEART,   1, id =  1),
    Card(CardType.DUEL,               Suit.DIAMOND, 1, id =  2),
    Card(CardType.ZHUGE_CROSSBOW,     Suit.DIAMOND, 1, id =  3),
    Card(CardType.ZHUGE_CROSSBOW,     Suit.CLUB,    1, id =  4),
    Card(CardType.DUEL,               Suit.CLUB,    1, id =  5),
    Card(CardType.LIGHTNING,          Suit.SPADE,   1, id =  6),
    Card(CardType.DUEL,               Suit.SPADE,   1, id =  7),

    // ── Rank 2 ──────────────────────────────────────────────────────────
    Card(CardType.DODGE,              Suit.HEART,   2, id =  8),
    Card(CardType.DODGE,              Suit.HEART,   2, id =  9),
    Card(CardType.DODGE,              Suit.DIAMOND, 2, id = 10),
    Card(CardType.DODGE,              Suit.DIAMOND, 2, id = 11),
    Card(CardType.EIGHT_TRIGRAMS,     Suit.CLUB,    2, id = 12),
    Card(CardType.ATTACK,             Suit.CLUB,    2, id = 13),
    Card(CardType.BENEVOLENT_SHIELD,  Suit.CLUB,    2, id = 14),   // EX
    Card(CardType.GENDER_SWORDS,      Suit.SPADE,   2, id = 15),
    Card(CardType.EIGHT_TRIGRAMS,     Suit.SPADE,   2, id = 16),
    Card(CardType.ICE_SWORD,          Suit.SPADE,   2, id = 17),   // EX

    // ── Rank 3 ──────────────────────────────────────────────────────────
    Card(CardType.BOUNTIFUL_HARVEST,  Suit.HEART,   3, id = 18),
    Card(CardType.PEACH,              Suit.HEART,   3, id = 19),
    Card(CardType.DODGE,              Suit.DIAMOND, 3, id = 20),
    Card(CardType.STEAL,              Suit.DIAMOND, 3, id = 21),
    Card(CardType.ATTACK,             Suit.CLUB,    3, id = 22),
    Card(CardType.DISMANTLE,          Suit.CLUB,    3, id = 23),
    Card(CardType.DISMANTLE,          Suit.SPADE,   3, id = 24),
    Card(CardType.STEAL,              Suit.SPADE,   3, id = 25),

    // ── Rank 4 ──────────────────────────────────────────────────────────
    Card(CardType.PEACH,              Suit.HEART,   4, id = 26),
    Card(CardType.BOUNTIFUL_HARVEST,  Suit.HEART,   4, id = 27),
    Card(CardType.STEAL,              Suit.DIAMOND, 4, id = 28),
    Card(CardType.DODGE,              Suit.DIAMOND, 4, id = 29),
    Card(CardType.ATTACK,             Suit.CLUB,    4, id = 30),
    Card(CardType.DISMANTLE,          Suit.CLUB,    4, id = 31),
    Card(CardType.DISMANTLE,          Suit.SPADE,   4, id = 32),
    Card(CardType.STEAL,              Suit.SPADE,   4, id = 33),

    // ── Rank 5 ──────────────────────────────────────────────────────────
    Card(CardType.CHITU,              Suit.HEART,   5, id = 34),
    Card(CardType.QILIN_BOW,          Suit.HEART,   5, id = 35),
    Card(CardType.BOULDER_AXE,        Suit.DIAMOND, 5, id = 36),
    Card(CardType.DODGE,              Suit.DIAMOND, 5, id = 37),
    Card(CardType.DILU,               Suit.CLUB,    5, id = 38),
    Card(CardType.ATTACK,             Suit.CLUB,    5, id = 39),
    Card(CardType.GREEN_DRAGON_CRESCENT_BLADE, Suit.SPADE, 5, id = 40),
    Card(CardType.JUEYING,            Suit.SPADE,   5, id = 41),

    // ── Rank 6 ──────────────────────────────────────────────────────────
    Card(CardType.ECSTASY,            Suit.HEART,   6, id = 42),
    Card(CardType.PEACH,              Suit.HEART,   6, id = 43),
    Card(CardType.ATTACK,             Suit.DIAMOND, 6, id = 44),
    Card(CardType.DODGE,              Suit.DIAMOND, 6, id = 45),
    Card(CardType.ATTACK,             Suit.CLUB,    6, id = 46),
    Card(CardType.ECSTASY,            Suit.CLUB,    6, id = 47),
    Card(CardType.ECSTASY,            Suit.SPADE,   6, id = 48),
    Card(CardType.QINGGANG_SWORD,     Suit.SPADE,   6, id = 49),

    // ── Rank 7 ──────────────────────────────────────────────────────────
    Card(CardType.PEACH,              Suit.HEART,   7, id = 50),
    Card(CardType.SOMETHING_FROM_NOTHING, Suit.HEART, 7, id = 51),
    Card(CardType.DODGE,              Suit.DIAMOND, 7, id = 52),
    Card(CardType.ATTACK,             Suit.DIAMOND, 7, id = 53),
    Card(CardType.BARBARIAN_INVASION, Suit.CLUB,    7, id = 54),
    Card(CardType.ATTACK,             Suit.CLUB,    7, id = 55),
    Card(CardType.BARBARIAN_INVASION, Suit.SPADE,   7, id = 56),
    Card(CardType.ATTACK,             Suit.SPADE,   7, id = 57),

    // ── Rank 8 ──────────────────────────────────────────────────────────
    Card(CardType.SOMETHING_FROM_NOTHING, Suit.HEART, 8, id = 58),
    Card(CardType.PEACH,              Suit.HEART,   8, id = 59),
    Card(CardType.DODGE,              Suit.DIAMOND, 8, id = 60),
    Card(CardType.ATTACK,             Suit.DIAMOND, 8, id = 61),
    Card(CardType.ATTACK,             Suit.CLUB,    8, id = 62),
    Card(CardType.ATTACK,             Suit.CLUB,    8, id = 63),
    Card(CardType.ATTACK,             Suit.SPADE,   8, id = 64),
    Card(CardType.ATTACK,             Suit.SPADE,   8, id = 65),

    // ── Rank 9 ──────────────────────────────────────────────────────────
    Card(CardType.PEACH,              Suit.HEART,   9, id = 66),
    Card(CardType.SOMETHING_FROM_NOTHING, Suit.HEART, 9, id = 67),
    Card(CardType.DODGE,              Suit.DIAMOND, 9, id = 68),
    Card(CardType.ATTACK,             Suit.DIAMOND, 9, id = 69),
    Card(CardType.ATTACK,             Suit.CLUB,    9, id = 70),
    Card(CardType.ATTACK,             Suit.CLUB,    9, id = 71),
    Card(CardType.ATTACK,             Suit.SPADE,   9, id = 72),
    Card(CardType.ATTACK,             Suit.SPADE,   9, id = 73),

    // ── Rank 10 ─────────────────────────────────────────────────────────
    Card(CardType.ATTACK,             Suit.HEART,  10, id = 74),
    Card(CardType.ATTACK,             Suit.HEART,  10, id = 75),
    Card(CardType.ATTACK,             Suit.DIAMOND,10, id = 76),
    Card(CardType.DODGE,              Suit.DIAMOND,10, id = 77),
    Card(CardType.ATTACK,             Suit.CLUB,   10, id = 78),
    Card(CardType.ATTACK,             Suit.CLUB,   10, id = 79),
    Card(CardType.ATTACK,             Suit.SPADE,  10, id = 80),
    Card(CardType.ATTACK,             Suit.SPADE,  10, id = 81),

    // ── Rank J (11) ─────────────────────────────────────────────────────
    Card(CardType.SOMETHING_FROM_NOTHING, Suit.HEART, 11, id = 82),
    Card(CardType.ATTACK,             Suit.HEART,  11, id = 83),
    Card(CardType.DODGE,              Suit.DIAMOND,11, id = 84),
    Card(CardType.DODGE,              Suit.DIAMOND,11, id = 85),
    Card(CardType.ATTACK,             Suit.CLUB,   11, id = 86),
    Card(CardType.ATTACK,             Suit.CLUB,   11, id = 87),
    Card(CardType.STEAL,              Suit.SPADE,  11, id = 88),
    Card(CardType.NEGATE,             Suit.SPADE,  11, id = 89),

    // ── Rank Q (12) ─────────────────────────────────────────────────────
    Card(CardType.DISMANTLE,          Suit.HEART,  12, id = 90),
    Card(CardType.PEACH,              Suit.HEART,  12, id = 91),
    Card(CardType.LIGHTNING,          Suit.HEART,  12, id = 92),   // EX
    Card(CardType.PEACH,              Suit.DIAMOND,12, id = 93),
    Card(CardType.NEGATE,             Suit.DIAMOND,12, id = 94),   // EX
    Card(CardType.HALBERD,            Suit.DIAMOND,12, id = 95),
    Card(CardType.BORROW_SWORD,       Suit.CLUB,   12, id = 96),
    Card(CardType.NEGATE,             Suit.CLUB,   12, id = 97),
    Card(CardType.SNAKE_SPEAR,        Suit.SPADE,  12, id = 98),
    Card(CardType.DISMANTLE,          Suit.SPADE,  12, id = 99),

    // ── Rank K (13) ─────────────────────────────────────────────────────
    Card(CardType.ZHUHUANGFEIDIAN,    Suit.HEART,  13, id = 100),
    Card(CardType.DODGE,              Suit.HEART,  13, id = 101),
    Card(CardType.ATTACK,             Suit.DIAMOND,13, id = 102),
    Card(CardType.ZIXING,             Suit.DIAMOND,13, id = 103),
    Card(CardType.BORROW_SWORD,       Suit.CLUB,   13, id = 104),
    Card(CardType.NEGATE,             Suit.CLUB,   13, id = 105),
    Card(CardType.DAYUAN,             Suit.SPADE,  13, id = 106),
    Card(CardType.BARBARIAN_INVASION, Suit.SPADE,  13, id = 107),

    ).also { require(it.size == 108) { "StandardCards must contain exactly 108 cards, got ${it.size}" } }