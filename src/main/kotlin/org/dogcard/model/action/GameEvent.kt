package org.dogcard.model.action

import org.dogcard.model.card.Card

sealed class GameEvent {
    data class GameStarted(val firstSeatIndex: Int, val seatViews: List<SeatView>) : GameEvent()
    data class HandUpdated(val seatIndex: Int, val cards: List<Card>) : GameEvent()
    data class CardsDrawn(val seatIndex: Int, val cards: List<Card>) : GameEvent()
    data class AttackPlayed(val attackerSeatIndex: Int, val targetSeatIndex: Int, val card: Card) : GameEvent()
    data class ResponseRequested(val attackerSeatIndex: Int, val targetSeatIndex: Int) : GameEvent()
    data class DodgePlayed(val defenderSeatIndex: Int, val card: Card) : GameEvent()
    data class DamageDealt(val targetSeatIndex: Int, val amount: Int, val newHp: Int) : GameEvent()
}