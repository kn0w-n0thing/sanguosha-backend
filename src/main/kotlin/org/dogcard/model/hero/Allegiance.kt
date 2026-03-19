package org.dogcard.model.hero

/**
 * The allegiance a seat holds within a game session.
 *
 * The correct subtype is assigned by [GameMode][org.dogcard.model.game.GameMode]
 * at game start; the type system prevents cross-mode field leakage.
 */
sealed class Allegiance {

    /** Identity mode (Lord/Loyalist/Rebel/Spy), 1v1 mode (Lord/Spy), and Doudizhu mode. */
    data class RoleBased(val role: Role) : Allegiance()

    /** Kingdom mode (国战): allegiance is determined by the hero's faction. */
    data class KingdomBased(val kingdom: Kingdom) : Allegiance()

    /**
     * 3v3 mode: allegiance is an opaque team identifier.
     * Will be replaced by a typed [Team] class when the mode is implemented.
     */
    data class TeamBased(val teamId: String) : Allegiance()

    /**
     * Allegiance not yet determined — used in Kingdom mode (国战) while a seat's
     * generals are still face-down (暗置). Transitions to [KingdomBased] when
     * the first general is revealed (明置).
     */
    data object Unrevealed : Allegiance()
}