package org.dogcard.game.session

import org.dogcard.game.factory.GameSetup
import org.dogcard.model.action.GameEvent
import org.dogcard.model.action.SeatView
import org.dogcard.model.deck.IDeck
import org.dogcard.model.hero.Role
import org.dogcard.model.seat.Allegiance
import org.dogcard.model.seat.Seat
import org.dogcard.model.turn.GamePhase
import kotlin.random.Random

private const val INITIAL_HAND_SIZE = 4
private const val DRAW_COUNT = 2

class GameSession(
    private val setup: GameSetup,
    private val random: Random = Random,
    private val onEvent: (GameEvent) -> Unit = {},
) {
    private var _seats: List<Seat> = setup.room.seats
    val seats: List<Seat> get() = _seats
    val deck: IDeck get() = setup.deck
    var currentSeatIndex: Int? = null
        private set
    var currentPhase: GamePhase? = null
        private set

    fun start() {
        check(currentPhase == null) { "Game has already started" }
        _seats = setup.room.mode.assignAllegiances(_seats, random)
        _seats = _seats.map { seat ->
            seat.copy(handCards = setup.deck.draw(INITIAL_HAND_SIZE))
        }
        val spyIndex = _seats.indexOfFirst { it.allegiance == Allegiance.RoleBased(Role.SPY) }
        currentSeatIndex = spyIndex
        currentPhase = GamePhase.Judge
        onEvent(GameEvent.GameStarted(
            firstSeatIndex = spyIndex,
            seatViews = _seats.map { seat ->
                SeatView(seatIndex = seat.seatIndex, hp = seat.hp, handCount = seat.handCards.size)
            },
        ))
        _seats.forEach { seat ->
            onEvent(GameEvent.HandUpdated(seatIndex = seat.seatIndex, cards = seat.handCards))
        }
    }

    fun advancePhase() {
        when (currentPhase) {
            GamePhase.Judge -> advanceJudgePhase()
            GamePhase.Draw  -> advanceDrawPhase()
            else            -> {}
        }
    }

    private fun advanceJudgePhase() {
        currentPhase = GamePhase.Draw
    }

    private fun advanceDrawPhase() {
        val seatIndex = currentSeatIndex!!
        val drawn = setup.deck.draw(DRAW_COUNT)
        val updatedSeat = _seats[seatIndex].copy(handCards = _seats[seatIndex].handCards + drawn)
        _seats = _seats.mapIndexed { i, seat -> if (i == seatIndex) updatedSeat else seat }
        currentPhase = GamePhase.Play
        onEvent(GameEvent.CardsDrawn(seatIndex = seatIndex, cards = drawn))
        onEvent(GameEvent.HandUpdated(seatIndex = seatIndex, cards = updatedSeat.handCards))
    }
}