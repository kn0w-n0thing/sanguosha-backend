package org.dogcard.model.seat

import org.dogcard.model.card.Card
import org.dogcard.model.hero.Hero

data class Seat(
    val seatIndex: Int,
    val heroes: List<Hero>,
    val handCards: List<Card>,
    val hp: HpState,
    val allegiance: Allegiance = Allegiance.Unknown,
)