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
    Card(CardType.HAIL_OF_ARROWS, Suit.HEART, 1),
    Card(CardType.PEACH_GARDEN_OATH, Suit.HEART, 1),
    Card(CardType.DUEL, Suit.DIAMOND, 1),
    Card(CardType.ZHUGE_CROSSBOW, Suit.DIAMOND, 1),
    Card(CardType.ZHUGE_CROSSBOW, Suit.CLUB, 1),
    Card(CardType.DUEL, Suit.CLUB, 1),
    Card(CardType.LIGHTNING, Suit.SPADE, 1),
    Card(CardType.DUEL, Suit.SPADE, 1),

    // ── Rank 2 ──────────────────────────────────────────────────────────
    Card(CardType.DODGE, Suit.HEART, 2),
    Card(CardType.DODGE, Suit.HEART, 2),
    Card(CardType.DODGE, Suit.DIAMOND, 2),
    Card(CardType.DODGE, Suit.DIAMOND, 2),
    Card(CardType.EIGHT_TRIGRAMS, Suit.CLUB, 2),
    Card(CardType.ATTACK, Suit.CLUB, 2),
    Card(CardType.BENEVOLENT_SHIELD, Suit.CLUB, 2),   // EX
    Card(CardType.GENDER_SWORDS, Suit.SPADE, 2),
    Card(CardType.EIGHT_TRIGRAMS, Suit.SPADE, 2),
    Card(CardType.ICE_SWORD, Suit.SPADE, 2),   // EX

    // ── Rank 3 ──────────────────────────────────────────────────────────
    Card(CardType.BOUNTIFUL_HARVEST, Suit.HEART, 3),
    Card(CardType.PEACH, Suit.HEART, 3),
    Card(CardType.DODGE, Suit.DIAMOND, 3),
    Card(CardType.STEAL, Suit.DIAMOND, 3),
    Card(CardType.ATTACK, Suit.CLUB, 3),
    Card(CardType.DISMANTLE, Suit.CLUB, 3),
    Card(CardType.DISMANTLE, Suit.SPADE, 3),
    Card(CardType.STEAL, Suit.SPADE, 3),

    // ── Rank 4 ──────────────────────────────────────────────────────────
    Card(CardType.PEACH, Suit.HEART, 4),
    Card(CardType.BOUNTIFUL_HARVEST, Suit.HEART, 4),
    Card(CardType.STEAL, Suit.DIAMOND, 4),
    Card(CardType.DODGE, Suit.DIAMOND, 4),
    Card(CardType.ATTACK, Suit.CLUB, 4),
    Card(CardType.DISMANTLE, Suit.CLUB, 4),
    Card(CardType.DISMANTLE, Suit.SPADE, 4),
    Card(CardType.STEAL, Suit.SPADE, 4),

    // ── Rank 5 ──────────────────────────────────────────────────────────
    Card(CardType.CHITU, Suit.HEART, 5),
    Card(CardType.QILIN_BOW, Suit.HEART, 5),
    Card(CardType.BOULDER_AXE, Suit.DIAMOND, 5),
    Card(CardType.DODGE, Suit.DIAMOND, 5),
    Card(CardType.DILU, Suit.CLUB, 5),
    Card(CardType.ATTACK, Suit.CLUB, 5),
    Card(CardType.GREEN_DRAGON_CRESCENT_BLADE, Suit.SPADE, 5),
    Card(CardType.JUEYING, Suit.SPADE, 5),

    // ── Rank 6 ──────────────────────────────────────────────────────────
    Card(CardType.ECSTASY, Suit.HEART, 6),
    Card(CardType.PEACH, Suit.HEART, 6),
    Card(CardType.ATTACK, Suit.DIAMOND, 6),
    Card(CardType.DODGE, Suit.DIAMOND, 6),
    Card(CardType.ATTACK, Suit.CLUB, 6),
    Card(CardType.ECSTASY, Suit.CLUB, 6),
    Card(CardType.ECSTASY, Suit.SPADE, 6),
    Card(CardType.QINGGANG_SWORD, Suit.SPADE, 6),

    // ── Rank 7 ──────────────────────────────────────────────────────────
    Card(CardType.PEACH, Suit.HEART, 7),
    Card(CardType.SOMETHING_FROM_NOTHING, Suit.HEART, 7),
    Card(CardType.DODGE, Suit.DIAMOND, 7),
    Card(CardType.ATTACK, Suit.DIAMOND, 7),
    Card(CardType.BARBARIAN_INVASION, Suit.CLUB, 7),
    Card(CardType.ATTACK, Suit.CLUB, 7),
    Card(CardType.BARBARIAN_INVASION, Suit.SPADE, 7),
    Card(CardType.ATTACK, Suit.SPADE, 7),

    // ── Rank 8 ──────────────────────────────────────────────────────────
    Card(CardType.SOMETHING_FROM_NOTHING, Suit.HEART, 8),
    Card(CardType.PEACH, Suit.HEART, 8),
    Card(CardType.DODGE, Suit.DIAMOND, 8),
    Card(CardType.ATTACK, Suit.DIAMOND, 8),
    Card(CardType.ATTACK, Suit.CLUB, 8),
    Card(CardType.ATTACK, Suit.CLUB, 8),
    Card(CardType.ATTACK, Suit.SPADE, 8),
    Card(CardType.ATTACK, Suit.SPADE, 8),

    // ── Rank 9 ──────────────────────────────────────────────────────────
    Card(CardType.PEACH, Suit.HEART, 9),
    Card(CardType.SOMETHING_FROM_NOTHING, Suit.HEART, 9),
    Card(CardType.DODGE, Suit.DIAMOND, 9),
    Card(CardType.ATTACK, Suit.DIAMOND, 9),
    Card(CardType.ATTACK, Suit.CLUB, 9),
    Card(CardType.ATTACK, Suit.CLUB, 9),
    Card(CardType.ATTACK, Suit.SPADE, 9),
    Card(CardType.ATTACK, Suit.SPADE, 9),

    // ── Rank 10 ─────────────────────────────────────────────────────────
    Card(CardType.ATTACK, Suit.HEART, 10),
    Card(CardType.ATTACK, Suit.HEART, 10),
    Card(CardType.ATTACK, Suit.DIAMOND, 10),
    Card(CardType.DODGE, Suit.DIAMOND, 10),
    Card(CardType.ATTACK, Suit.CLUB, 10),
    Card(CardType.ATTACK, Suit.CLUB, 10),
    Card(CardType.ATTACK, Suit.SPADE, 10),
    Card(CardType.ATTACK, Suit.SPADE, 10),

    // ── Rank J (11) ─────────────────────────────────────────────────────
    Card(CardType.SOMETHING_FROM_NOTHING, Suit.HEART, 11),
    Card(CardType.ATTACK, Suit.HEART, 11),
    Card(CardType.DODGE, Suit.DIAMOND, 11),
    Card(CardType.DODGE, Suit.DIAMOND, 11),
    Card(CardType.ATTACK, Suit.CLUB, 11),
    Card(CardType.ATTACK, Suit.CLUB, 11),
    Card(CardType.STEAL, Suit.SPADE, 11),
    Card(CardType.NEGATE, Suit.SPADE, 11),

    // ── Rank Q (12) ─────────────────────────────────────────────────────
    Card(CardType.DISMANTLE, Suit.HEART, 12),
    Card(CardType.PEACH, Suit.HEART, 12),
    Card(CardType.LIGHTNING, Suit.HEART, 12),   // EX
    Card(CardType.PEACH, Suit.DIAMOND, 12),
    Card(CardType.NEGATE, Suit.DIAMOND, 12),  // EX
    Card(CardType.HALBERD, Suit.DIAMOND, 12),
    Card(CardType.BORROW_SWORD, Suit.CLUB, 12),
    Card(CardType.NEGATE, Suit.CLUB, 12),
    Card(CardType.SNAKE_SPEAR, Suit.SPADE, 12),
    Card(CardType.DISMANTLE, Suit.SPADE, 12),

    // ── Rank K (13) ─────────────────────────────────────────────────────
    Card(CardType.ZHUHUANGFEIDIAN, Suit.HEART, 13),
    Card(CardType.DODGE, Suit.HEART, 13),
    Card(CardType.ATTACK, Suit.DIAMOND, 13),
    Card(CardType.ZIXING, Suit.DIAMOND, 13),
    Card(CardType.BORROW_SWORD, Suit.CLUB, 13),
    Card(CardType.NEGATE, Suit.CLUB, 13),
    Card(CardType.DAYUAN, Suit.SPADE, 13),
    Card(CardType.BARBARIAN_INVASION, Suit.SPADE, 13),

    ).also { require(it.size == 108) { "StandardCards must contain exactly 108 cards, got ${it.size}" } }