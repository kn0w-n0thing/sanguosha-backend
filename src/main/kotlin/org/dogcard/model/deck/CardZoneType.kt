package org.dogcard.model.deck

sealed class CardZoneType {
    data class Hand(val seatIndex: Int) : CardZoneType()
    data class Judgment(val seatIndex: Int) : CardZoneType()
    data class Equipment(val seatIndex: Int) : CardZoneType()
    data object DrawPile : CardZoneType()
    data object DiscardPile : CardZoneType()
    data object InFlight : CardZoneType()
}