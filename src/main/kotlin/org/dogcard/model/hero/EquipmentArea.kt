package org.dogcard.model.hero

import org.dogcard.model.card.Card

/**
 * The five equipment slots a hero can occupy simultaneously.
 *
 * A hero may equip at most one card per slot.
 * Playing a new equipment card into an occupied slot discards the old one.
 */
data class EquipmentArea(
    val weapon: Card? = null,
    val armor: Card? = null,
    val offensiveHorse: Card? = null,  // +1 attack range
    val defensiveHorse: Card? = null,  // -1 attack range to opponents
    val treasure: Card? = null,        // e.g. 木牛流马, 玉玺
)