package org.dogcard.game.mode

import org.dogcard.model.game.GameMode
import org.dogcard.model.game.Winner
import org.dogcard.model.seat.Seat

class OneVsOneMode : GameMode {
    override val heroEntryDrawCount: Int = 4

    override fun assignAllegiances(seats: List<Seat>): List<Seat> = seats

    override fun checkWinCondition(seats: List<Seat>): Winner? = null

    override fun onSeatDeath(dead: Seat, seats: List<Seat>): Seat = dead
}