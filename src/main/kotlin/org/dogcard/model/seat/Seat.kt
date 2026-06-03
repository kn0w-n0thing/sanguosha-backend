package org.dogcard.model.seat

import org.dogcard.model.card.Card
import org.dogcard.model.deck.ICardZone
import org.dogcard.model.hero.Hero

class Seat(
    val seatIndex: Int,
    val heroes: List<Hero>,
    val handZone: ICardZone,
    var hp: HpState,
    var allegiance: Allegiance = Allegiance.Unknown,
) {
    val handCards: List<Card> get() = handZone.toList()
}