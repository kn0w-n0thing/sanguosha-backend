package org.dogcard.game.session

import org.dogcard.game.factory.GameSetup
import org.dogcard.model.action.GameAction
import org.dogcard.model.action.GameEvent
import org.dogcard.model.action.PendingRequest
import org.dogcard.model.action.SeatView
import org.dogcard.model.card.CardType
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
    var pendingRequest: PendingRequest? = null
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

    fun submitAction(seatIndex: Int, action: GameAction): Result<Unit> {
        return when (action) {
            is GameAction.PlayAttack -> {
                if (seatIndex != currentSeatIndex)
                    return Result.failure(IllegalArgumentException("Only the active seat can play an attack"))
                if (pendingRequest != null)
                    return Result.failure(IllegalArgumentException("A response is already pending"))
                if (action.card.type != CardType.ATTACK)
                    return Result.failure(IllegalArgumentException("PlayAttack requires an ATTACK card"))
                pendingRequest = PendingRequest.RespondToAttack(
                    attackerSeatIndex = seatIndex,
                    targetSeatIndex = action.targetSeatIndex,
                    card = action.card,
                )
                Result.success(Unit)
            }

            is GameAction.RespondWithDodge -> {
                val request = pendingRequest as? PendingRequest.RespondToAttack
                    ?: return Result.failure(IllegalArgumentException("No pending attack to respond to"))
                if (seatIndex != request.targetSeatIndex)
                    return Result.failure(IllegalArgumentException("Only the target seat can respond to an attack"))
                if (action.card.type != CardType.DODGE)
                    return Result.failure(IllegalArgumentException("RespondWithDodge requires a DODGE card"))
                setup.deck.discard(listOf(action.card))
                pendingRequest = null
                Result.success(Unit)
            }

            is GameAction.Pass -> {
                val request = pendingRequest as? PendingRequest.RespondToAttack
                    ?: return Result.failure(IllegalArgumentException("No pending attack to respond to"))
                val target = _seats[request.targetSeatIndex]
                _seats = _seats.mapIndexed { i, seat ->
                    if (i == request.targetSeatIndex) seat.copy(hp = target.hp.copy(current = target.hp.current - 1))
                    else seat
                }
                setup.deck.discard(listOf(request.card))
                pendingRequest = null
                Result.success(Unit)
            }

            else -> Result.success(Unit)
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