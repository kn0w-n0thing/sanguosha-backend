package org.dogcard.model.hero

/**
 * A hero card's base HP contribution, stored in half-unit increments to support
 * the Kingdom mode (国战) half-fish (半个阴阳鱼) values (e.g. 1.5, 2.5).
 *
 * Construction:
 * ```
 * HpValue.of(3)     // 3 HP   (whole)
 * HpValue.ofHalf(5) // 2.5 HP (5 half-units)
 * ```
 *
 * Combining two heroes' [HpValue]s to produce the seat's effective integer maxHp:
 * - Sum the halves of both heroes.
 * - [wholes] is the integer maxHp for [org.dogcard.model.seat.HpState].
 * - If [hasHalf] is true on the combined value, the leftover half becomes an
 *   阴阳鱼 marker on the seat (not extra HP).
 */
@JvmInline
value class HpValue(val halves: Int) {

    init {
        require(halves > 0) { "HpValue must be positive, got $halves halves" }
    }

    /** The whole-HP part (floor). */
    val wholes: Int get() = halves / 2

    /** True if there is a leftover half-fish after taking [wholes]. */
    val hasHalf: Boolean get() = halves % 2 != 0

    operator fun plus(other: HpValue) = HpValue(halves + other.halves)

    override fun toString(): String = if (hasHalf) "$wholes.5" else "$wholes"

    companion object {
        /** Create from a whole-number HP value (most heroes). */
        fun of(whole: Int) = HpValue(whole * 2)

        /** Create directly from a half-unit count (e.g. 5 → 2.5 HP). */
        fun ofHalf(n: Int) = HpValue(n)
    }
}