package org.dogcard.util

import org.dogcard.model.card.Card
import org.dogcard.model.deck.CardZone
import org.dogcard.model.deck.CardZoneType
import org.dogcard.model.deck.ICardZone
import org.dogcard.model.deck.IDeck

// Test double for IDeck. Cards are served in the given order with no shuffling,
// making game session tests fully deterministic.
class FakeDeck(vararg cards: Card, seatCount: Int = 2) : IDeck {

    private val zones: Map<CardZoneType, CardZone> = buildZones(cards.toList(), seatCount)

    override val remaining: Int get() = zones[CardZoneType.DrawPile]!!.toList().size

    override fun zone(type: CardZoneType): ICardZone = zones[type] ?: error("Zone $type not found")

    override fun draw(n: Int, seatIndex: Int): List<Card> {
        val drawZone = zones[CardZoneType.DrawPile]!!
        val handZone = zones[CardZoneType.Hand(seatIndex)]!!
        if (drawZone.toList().size < n) reshuffle()
        val cards = drawZone.toList().take(n)
        drawZone.transferAll(cards, handZone)
        return cards
    }

    // No shuffle — preserves discard order so tests can assert on specific cards.
    override fun reshuffle() {
        val drawZone = zones[CardZoneType.DrawPile]!!
        val discardZone = zones[CardZoneType.DiscardPile]!!
        discardZone.transferAll(discardZone.toList(), drawZone)
    }

    // Convenience accessor for test assertions — avoids casting to FakeDeck in every test.
    val discardPile: List<Card> get() = zones[CardZoneType.DiscardPile]!!.toList()

    fun discard(cards: List<Card>, fromSeatIndex: Int) {
        zone(CardZoneType.Hand(fromSeatIndex)).transferAll(cards, zone(CardZoneType.DiscardPile))
    }

    private fun buildZones(cards: List<Card>, seatCount: Int): Map<CardZoneType, CardZone> {
        val map = mutableMapOf<CardZoneType, CardZone>()
        map[CardZoneType.DrawPile] = CardZone(CardZoneType.DrawPile, cards)
        map[CardZoneType.DiscardPile] = CardZone(CardZoneType.DiscardPile)
        map[CardZoneType.InFlight] = CardZone(CardZoneType.InFlight)
        repeat(seatCount) { i ->
            map[CardZoneType.Hand(i)] = CardZone(CardZoneType.Hand(i))
            map[CardZoneType.Judgment(i)] = CardZone(CardZoneType.Judgment(i))
            map[CardZoneType.Equipment(i)] = CardZone(CardZoneType.Equipment(i))
        }
        return map
    }
}