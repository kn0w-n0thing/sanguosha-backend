package org.dogcard.game.session

import org.dogcard.game.factory.GameSetup
import org.dogcard.model.action.GameAction
import org.dogcard.model.action.GameEvent
import org.dogcard.model.action.PendingRequest
import org.dogcard.model.action.SeatView
import org.dogcard.model.card.CardType
import org.dogcard.model.deck.CardZoneType
import org.dogcard.model.deck.ICardZone
import org.dogcard.model.deck.IDeck
import org.dogcard.model.hero.Role
import org.dogcard.model.seat.Allegiance
import org.dogcard.model.seat.Seat
import org.dogcard.model.turn.GamePhase
import kotlin.random.Random

private const val INITIAL_HAND_SIZE = 4
private const val DRAW_COUNT = 2

private const val ERR_GAME_ALREADY_STARTED = "Game has already started"
private const val ERR_NOT_ACTIVE_SEAT_ATTACK = "Only the active seat can play an attack"
private const val ERR_RESPONSE_ALREADY_PENDING = "A response is already pending"
private const val ERR_PLAY_ATTACK_REQUIRES_ATTACK_CARD = "PlayAttack requires an ATTACK card"
private const val ERR_NO_PENDING_ATTACK = "No pending attack to respond to"
private const val ERR_NOT_TARGET_SEAT = "Only the target seat can respond to an attack"
private const val ERR_RESPOND_REQUIRES_DODGE_CARD = "RespondWithDodge requires a DODGE card"
private const val ERR_NOT_ACTIVE_SEAT_END = "Only the active seat can end the play phase"
private const val ERR_CANNOT_END_WHILE_PENDING = "Cannot end play phase while a response is pending"
private const val ERR_NOT_ACTIVE_SEAT_DISCARD = "Only the active seat can discard"

class GameSession(
    private val setup: GameSetup,
    private val random: Random = Random,
    private val onEvent: (GameEvent) -> Unit = {},
) {
    private val _seats: List<Seat> = setup.room.seats
    val seats: List<Seat> get() = _seats
    val inFlightZone: ICardZone get() = setup.deck.zone(CardZoneType.InFlight)
    private val discardZone: ICardZone get() = setup.deck.zone(CardZoneType.DiscardPile)
    val deck: IDeck get() = setup.deck
    var currentSeatIndex: Int? = null
        private set
    var currentPhase: GamePhase? = null
        private set
    var pendingRequest: PendingRequest? = null
        private set

    fun start() {
        check(currentPhase == null) { ERR_GAME_ALREADY_STARTED }
        setup.room.mode.assignAllegiances(_seats, random)
        _seats.forEach { seat ->
            setup.deck.draw(INITIAL_HAND_SIZE, seat.seatIndex)
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
                    return Result.failure(IllegalArgumentException(ERR_NOT_ACTIVE_SEAT_ATTACK))
                if (pendingRequest != null)
                    return Result.failure(IllegalArgumentException(ERR_RESPONSE_ALREADY_PENDING))
                if (action.card.type != CardType.ATTACK)
                    return Result.failure(IllegalArgumentException(ERR_PLAY_ATTACK_REQUIRES_ATTACK_CARD))
                _seats[seatIndex].handZone.transfer(action.card, inFlightZone)
                pendingRequest = PendingRequest.RespondToAttack(
                    attackerSeatIndex = seatIndex,
                    targetSeatIndex = action.targetSeatIndex,
                )
                onEvent(
                    GameEvent.AttackPlayed(
                        attackerSeatIndex = seatIndex,
                        targetSeatIndex = action.targetSeatIndex,
                        card = action.card,
                    )
                )
                onEvent(
                    GameEvent.ResponseRequested(
                        attackerSeatIndex = seatIndex,
                        targetSeatIndex = action.targetSeatIndex,
                    )
                )
                Result.success(Unit)
            }

            is GameAction.RespondWithDodge -> {
                val request = pendingRequest as? PendingRequest.RespondToAttack
                    ?: return Result.failure(IllegalArgumentException(ERR_NO_PENDING_ATTACK))
                if (seatIndex != request.targetSeatIndex)
                    return Result.failure(IllegalArgumentException(ERR_NOT_TARGET_SEAT))
                if (action.card.type != CardType.DODGE)
                    return Result.failure(IllegalArgumentException(ERR_RESPOND_REQUIRES_DODGE_CARD))
                _seats[seatIndex].handZone.transfer(action.card, discardZone)
                onEvent(GameEvent.DodgePlayed(defenderSeatIndex = seatIndex, card = action.card))
                pendingRequest = null
                Result.success(Unit)
            }

            is GameAction.Pass -> {
                val request = pendingRequest as? PendingRequest.RespondToAttack
                    ?: return Result.failure(IllegalArgumentException(ERR_NO_PENDING_ATTACK))
                val target = _seats[request.targetSeatIndex]
                target.hp = target.hp.copy(current = target.hp.current - 1)
                val card = inFlightZone.toList().single()
                check(card.type == CardType.ATTACK) { "Expected ATTACK card in flight, got ${card.type}" }
                inFlightZone.transfer(card, discardZone)
                onEvent(
                    GameEvent.DamageDealt(
                        targetSeatIndex = request.targetSeatIndex,
                        amount = 1,
                        newHp = _seats[request.targetSeatIndex].hp.current,
                    )
                )
                pendingRequest = null
                Result.success(Unit)
            }

            is GameAction.EndPlayPhase -> {
                if (seatIndex != currentSeatIndex)
                    return Result.failure(IllegalArgumentException(ERR_NOT_ACTIVE_SEAT_END))
                if (pendingRequest != null)
                    return Result.failure(IllegalArgumentException(ERR_CANNOT_END_WHILE_PENDING))
                currentPhase = GamePhase.Discard
                Result.success(Unit)
            }

            is GameAction.Discard -> {
                if (seatIndex != currentSeatIndex)
                    return Result.failure(IllegalArgumentException(ERR_NOT_ACTIVE_SEAT_DISCARD))
                _seats[seatIndex].handZone.transferAll(action.cards, discardZone)
                currentPhase = GamePhase.End
                Result.success(Unit)
            }

        }
    }

    fun advancePhase() {
        when (currentPhase) {
            GamePhase.Judge -> advanceJudgePhase()
            GamePhase.Draw  -> advanceDrawPhase()
            GamePhase.End -> advanceEndPhase()
            else            -> {}
        }
    }

    private fun advanceJudgePhase() {
        currentPhase = GamePhase.Draw
    }

    private fun advanceEndPhase() {
        currentSeatIndex = (currentSeatIndex!! + 1) % _seats.size
        currentPhase = GamePhase.Judge
    }

    private fun advanceDrawPhase() {
        val seatIndex = currentSeatIndex!!
        val seat = _seats[seatIndex]
        val drawn = setup.deck.draw(DRAW_COUNT, seatIndex)
        currentPhase = GamePhase.Play
        onEvent(GameEvent.CardsDrawn(seatIndex = seatIndex, cards = drawn))
        onEvent(GameEvent.HandUpdated(seatIndex = seatIndex, cards = seat.handCards))
    }
}