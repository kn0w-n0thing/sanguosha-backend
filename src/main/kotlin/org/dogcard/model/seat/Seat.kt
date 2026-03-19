package org.dogcard.model.seat

import org.dogcard.model.card.Card
import org.dogcard.model.seat.Allegiance
import org.dogcard.model.hero.Hero
import org.dogcard.model.seat.HpState

/**
 * A player's seat in the game.
 *
 * The seat owns everything that is shared across hero rotations:
 * hand cards, judgment area, current HP, and allegiance.
 * Hero-specific state (skills, equipment, special area) lives on [Hero].
 *
 * @param id            Opaque seat identifier (e.g. session or player ID).
 * @param seatIndex     Zero-based position; used for distance calculation and turn order.
 * @param handCards     Cards currently held in hand.
 * @param judgmentArea  Delayed tricks pending resolution (乐不思蜀, 兵粮寸断, 闪电).
 * @param heroes        The seat's hero lineup (non-empty).
 *                      One hero in standard/identity/1v1 modes; two in Kingdom mode (国战).
 *                      In 1v1 mode the list represents the rotation queue — index 0 is on field.
 * @param hp            Live HP state. Effective max is computed by [GameMode] at seat-assembly
 *                      time (e.g. combining half-fish values in Kingdom mode).
 * @param allegiance    Assigned by [GameMode] at game start; subtype is mode-specific.
 */
data class Seat(
    val id: String,
    val seatIndex: Int,
    val handCards: List<Card>,
    val judgmentArea: List<Card>,
    val heroes: List<Hero>,
    val hp: HpState,
    val allegiance: Allegiance,
) {
    init {
        require(seatIndex >= 0)      { "seatIndex must be non-negative, got $seatIndex" }
        require(heroes.isNotEmpty()) { "heroes must not be empty" }
    }
}