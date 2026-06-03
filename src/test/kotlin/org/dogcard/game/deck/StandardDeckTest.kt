package org.dogcard.game.deck

import org.dogcard.model.card.Card
import org.dogcard.model.card.CardType
import org.dogcard.model.card.Suit
import org.dogcard.model.deck.CardZoneType
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StandardDeckTest {

    private fun card(id: Int) = Card(CardType.ATTACK, Suit.SPADE, id, id = id)

    @Test
    fun `all zones are empty except the draw pile after construction`() {
        val deck = StandardDeck(listOf(card(1), card(2)), seatCount = 2)
        assertTrue(deck.zone(CardZoneType.DiscardPile).toList().isEmpty())
        assertTrue(deck.zone(CardZoneType.InFlight).toList().isEmpty())
        assertTrue(deck.zone(CardZoneType.Hand(0)).toList().isEmpty())
        assertTrue(deck.zone(CardZoneType.Hand(1)).toList().isEmpty())
        assertEquals(2, deck.remaining)
    }

    @Test
    fun `remaining reflects the number of cards in the draw zone`() {
        val deck = StandardDeck(listOf(card(1), card(2), card(3)), seatCount = 2)
        assertEquals(3, deck.remaining)
    }

    @Test
    fun `reshuffle randomizes the discard pile order`() {
        val cards = (1..10).map { card(it) }
        val deck = StandardDeck(cards, seatCount = 2, random = Random(seed = 42))
        deck.draw(10, seatIndex = 0)
        deck.discard((1..10).map { card(it) }, fromSeatIndex = 0)
        deck.reshuffle()
        val orderAfterReshuffle = deck.zone(CardZoneType.DrawPile).toList()
        assertNotEquals(cards, orderAfterReshuffle)
    }

    @Test
    fun `draw takes all remaining draw pile cards then draws the rest from reshuffled discard`() {
        val deck = StandardDeck(listOf(card(1), card(2), card(3), card(4), card(5)), seatCount = 2)
        deck.draw(3, seatIndex = 0)
        deck.discard(listOf(card(1), card(2), card(3)), fromSeatIndex = 0)
        // draw zone: [card(4), card(5)], discard: [card(1), card(2), card(3)]
        val drawn = deck.draw(4, seatIndex = 1)
        assertEquals(4, drawn.size)
        // card(4) and card(5) must be drawn first from the draw pile
        assertTrue(drawn.containsAll(listOf(card(4), card(5))))
        // 1 card should remain in draw after drawing 4 total (2 from draw + 2 from reshuffle, leaving 1)
        assertEquals(1, deck.remaining)
    }

    @Test
    fun `draw reshuffles automatically when draw zone has fewer cards than requested`() {
        val deck = StandardDeck(listOf(card(1), card(2), card(3)), seatCount = 2)
        deck.draw(3, seatIndex = 0)
        deck.discard(listOf(card(1), card(2)), fromSeatIndex = 0)
        val drawn = deck.draw(2, seatIndex = 1)
        assertEquals(2, drawn.size)
        assertEquals(2, deck.zone(CardZoneType.Hand(1)).toList().size)
    }

    @Test
    fun `draw moves top N cards from draw zone to the target seat's hand zone and returns them`() {
        val deck = StandardDeck(listOf(card(1), card(2), card(3)), seatCount = 2)
        val drawn = deck.draw(2, seatIndex = 0)
        assertEquals(setOf(card(1), card(2)), drawn.toSet())
        assertEquals(setOf(card(1), card(2)), deck.zone(CardZoneType.Hand(0)).toList().toSet())
        assertEquals(1, deck.remaining)
    }
}