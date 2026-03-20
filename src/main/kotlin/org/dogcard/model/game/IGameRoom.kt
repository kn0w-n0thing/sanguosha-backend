package org.dogcard.model.game

import org.dogcard.model.deck.IDeck
import org.dogcard.model.seat.Seat

/**
 * The subset of GameRoom that GameMode strategies are allowed to see.
 * Keeps model.game free of game-layer dependencies (TurnEngine, etc.).
 */
interface IGameRoom {
    val seats: List<Seat>
    val deck: IDeck
    fun updateSeat(seat: Seat)
}