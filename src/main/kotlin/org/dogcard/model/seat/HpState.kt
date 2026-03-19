package org.dogcard.model.seat

import org.dogcard.model.hero.HpValue

/**
 * The live HP state of a seat during a game.
 *
 * Both [current] and [max] are whole integers — fractions are resolved at the
 * seat-assembly step (see [org.dogcard.model.hero.HpValue]) before this is constructed.
 *
 * @param current Current HP; 0 means the seat is dying (濒死).
 * @param max     Effective HP cap for this seat. In Kingdom mode this is derived
 *                from combining the two heroes' [org.dogcard.model.hero.HpValue]s; in other modes it
 *                equals the single hero's [org.dogcard.model.hero.HpValue.wholes].
 */
data class HpState(val current: Int, val max: Int) {

    init {
        require(max > 0)           { "max must be positive, got $max" }
        require(current in 0..max) { "current must be in 0..$max, got $current" }
    }

    /** True when the seat is in the dying state (濒死) and must receive a 桃 or die. */
    val isDying: Boolean get() = current == 0

    /** True when the seat is at full HP. */
    val isFull: Boolean get() = current == max

    /** HP already lost (已损失体力值); used in skill conditions (e.g. 司马懿·鬼才). */
    val lost: Int get() = max - current

    companion object {
        /** Construct a full-health [HpState] from a hero's base [org.dogcard.model.hero.HpValue]. */
        fun full(value: HpValue) = HpState(current = value.wholes, max = value.wholes)
    }
}