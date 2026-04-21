package org.dogcard.game.mode

import org.dogcard.model.hero.Role
import org.dogcard.model.game.GameMode
import org.dogcard.model.game.Winner
import org.dogcard.model.seat.Allegiance
import org.dogcard.model.seat.Seat

class OneVsOneMode : GameMode {
    override val heroEntryDrawCount: Int = 4

    override fun assignAllegiances(seats: List<Seat>, random: kotlin.random.Random): List<Seat> {
        val lordIndex = random.nextInt(seats.size)
        return seats.mapIndexed { index, seat ->
            val role = if (index == lordIndex) Role.LORD else Role.SPY
            seat.copy(allegiance = Allegiance.RoleBased(role))
        }
    }

    override fun checkWinCondition(seats: List<Seat>): Winner? = null

    override fun onSeatDeath(dead: Seat, seats: List<Seat>): Seat = dead
}