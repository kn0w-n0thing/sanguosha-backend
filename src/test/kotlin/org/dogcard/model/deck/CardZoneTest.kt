package org.dogcard.model.deck

import org.dogcard.model.card.Card
import org.dogcard.model.card.CardType
import org.dogcard.model.card.Suit
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CardZoneTest {

    private fun card(id: Int) = Card(CardType.ATTACK, Suit.SPADE, id, id = id)

    @Test
    fun `toList returns empty list for a new zone`() {
        val zone = CardZone(CardZoneType.DrawPile)
        assertTrue(zone.toList().isEmpty())
    }

    @Test
    fun `toList returns cards passed to the DrawPile zone constructor`() {
        val cards = listOf(card(1), card(2))
        val zone = CardZone(CardZoneType.DrawPile, cards)
        assertEquals(cards, zone.toList())
    }

    @Test
    fun `contains returns true for a card passed to the DrawPile zone constructor`() {
        val c = card(1)
        val zone = CardZone(CardZoneType.DrawPile, listOf(c))
        assertTrue(zone.contains(c))
    }

    @Test
    fun `contains returns false for a card not in the zone`() {
        val zone = CardZone(CardZoneType.DrawPile, listOf(card(1)))
        assertFalse(zone.contains(card(2)))
    }

    @Test
    fun `constructor throws if a non-DrawPile zone is initialized with cards`() {
        assertThrows<IllegalStateException> {
            CardZone(CardZoneType.Hand(0), listOf(card(1)))
        }
    }

    @Test
    fun `transfer moves card from source to destination`() {
        val source = CardZone(CardZoneType.DrawPile, listOf(card(1)))
        val dest = CardZone(CardZoneType.DiscardPile)
        source.transfer(card(1), dest)
        assertFalse(source.contains(card(1)))
        assertTrue(dest.contains(card(1)))
    }

    @Test
    fun `transfer throws if card is not in source zone`() {
        val source = CardZone(CardZoneType.DrawPile)
        val dest = CardZone(CardZoneType.DiscardPile)
        assertThrows<IllegalStateException> { source.transfer(card(1), dest) }
    }

    @Test
    fun `transferAll moves all listed cards to destination`() {
        val cards = listOf(card(1), card(2), card(3))
        val source = CardZone(CardZoneType.DrawPile, cards)
        val dest = CardZone(CardZoneType.DiscardPile)
        source.transferAll(cards, dest)
        assertTrue(source.toList().isEmpty())
        assertEquals(cards.toSet(), dest.toList().toSet())
    }
}