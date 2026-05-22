package org.dogcard.model.action

import org.dogcard.model.card.Card

sealed class GameEvent {
    data class GameStarted(val firstSeatIndex: Int, val seatViews: List<SeatView>) : GameEvent()
    data class HandUpdated(val seatIndex: Int, val cards: List<Card>) : GameEvent()
    data class CardsDrawn(val seatIndex: Int, val cards: List<Card>) : GameEvent()
}