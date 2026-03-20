package org.dogcard.game.room

import org.dogcard.game.turn.TurnEngine
import org.dogcard.model.deck.IDeck
import org.dogcard.model.game.GameMode
import org.dogcard.model.game.IGameRoom
import org.dogcard.model.game.Winner
import org.dogcard.model.seat.Seat

class GameRoom(
    val id: String,
    val deck: IDeck,
    val mode: GameMode,
    val engine: TurnEngine,
    seats: List<Seat>,
) : IGameRoom {

    private val _seats = seats.toMutableList()
    override val seats: List<Seat> get() = _seats

    override fun updateSeat(seat: Seat) {
        _seats[seat.seatIndex] = seat
    }

    /**
     * Assigns allegiances, then starts the first turn (seat 0 = LORD goes first).
     */
    fun start() {
        mode.assignAllegiances(_seats).forEach { updateSeat(it) }
        engine.startTurn(0)
        engine.advance()
    }

    fun checkWinCondition(): Winner? = mode.checkWinCondition(this)
}