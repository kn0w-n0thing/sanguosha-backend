package org.dogcard.model.deck

import org.dogcard.model.card.Card

interface IDeck {
    val remaining: Int
    fun zone(type: CardZoneType): ICardZone
    fun draw(n: Int, seatIndex: Int): List<Card>
    fun reshuffle()
}