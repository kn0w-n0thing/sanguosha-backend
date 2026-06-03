package org.dogcard.model.deck

import org.dogcard.model.card.Card

class CardZone(val type: CardZoneType, initial: List<Card> = emptyList()) : ICardZone {
    init {
        check(type == CardZoneType.DrawPile || initial.isEmpty()) {
            "Only DrawPile zones can be initialized with cards; $type must be empty"
        }
    }
    private val cards: MutableList<Card> = initial.toMutableList()

    override fun contains(card: Card): Boolean = card in cards
    override fun toList(): List<Card> = cards.toList()

    override fun transfer(card: Card, to: ICardZone): Card {
        check(cards.remove(card)) { "Card $card not found in zone $type" }
        (to as CardZone).cards.add(card)
        return card
    }
    override fun transferAll(batch: List<Card>, to: ICardZone) {
        batch.toList().forEach { transfer(it, to) }
    }
}
