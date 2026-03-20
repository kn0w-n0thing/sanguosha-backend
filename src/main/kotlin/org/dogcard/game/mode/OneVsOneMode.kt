package org.dogcard.game.mode

import org.dogcard.model.game.GameMode
import org.dogcard.model.game.IGameRoom
import org.dogcard.model.game.Winner
import org.dogcard.model.hero.HpValue
import org.dogcard.model.seat.Allegiance
import org.dogcard.model.seat.HpState
import org.dogcard.model.seat.Seat
import org.dogcard.model.hero.Role

private const val HERO_ENTRY_DRAW = 4

class OneVsOneMode : GameMode {

    override fun assignAllegiances(seats: List<Seat>): List<Seat> {
        require(seats.size == 2) { "1v1 mode requires exactly 2 seats, got ${seats.size}" }
        return listOf(
            seats[0].copy(allegiance = Allegiance.RoleBased(Role.LORD)),
            seats[1].copy(allegiance = Allegiance.RoleBased(Role.SPY)),
        )
    }

    override fun checkWinCondition(room: IGameRoom): Winner? {
        for (seat in room.seats) {
            if (seat.heroes.isEmpty()) {
                return Winner((seat.seatIndex + 1) % 2)
            }
        }
        return null
    }

    /**
     * Advances the fallen seat's hero queue.
     * Discards hand and judgment cards, restores full HP for the new hero, draws [HERO_ENTRY_DRAW] cards.
     * If no heroes remain the game is already over — [checkWinCondition] handles that.
     */
    override fun onSeatDeath(dead: Seat, room: IGameRoom) {
        val remaining = dead.heroes.drop(1)
        if (remaining.isEmpty()) return

        val newHero = remaining.first()
        val entryHp = HpState.full(newHero.maxHp)
        val entryCards = room.deck.draw(HERO_ENTRY_DRAW)

        room.updateSeat(
            dead.copy(
                heroes = remaining,
                hp = entryHp,
                handCards = entryCards,
                judgmentArea = emptyList(),
            )
        )
    }
}