package org.dogcard.game.deck

import org.dogcard.model.card.Card
import org.dogcard.model.card.CardType
import org.dogcard.model.card.Suit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StandardDeckTest {

    // Five distinct cards in a predictable order: c1 is on top, c5 on bottom.
    private val c1 = Card(CardType.ATTACK,               Suit.HEART,   1)
    private val c2 = Card(CardType.DODGE,                Suit.SPADE,   2)
    private val c3 = Card(CardType.PEACH,                Suit.CLUB,    3)
    private val c4 = Card(CardType.DUEL,                 Suit.DIAMOND,  4)
    private val c5 = Card(CardType.SOMETHING_FROM_NOTHING, Suit.HEART, 5)

    // Extra cards used in individual tests (not in the default deck).
    private val extra1 = Card(CardType.DISMANTLE, Suit.SPADE,   6)
    private val extra2 = Card(CardType.STEAL,     Suit.DIAMOND,  7)

    private lateinit var deck: StandardDeck

    @BeforeEach
    fun setUp() {
        deck = StandardDeck(listOf(c1, c2, c3, c4, c5))
    }

    // ── remaining ────────────────────────────────────────────────────────────

    @Test
    fun `remaining reflects current draw pile size`() {
        assertEquals(5, deck.remaining)
        deck.draw(2)
        assertEquals(3, deck.remaining)
    }

    // ── draw ─────────────────────────────────────────────────────────────────

    @Test
    fun `draw returns top n cards in order`() {
        assertEquals(listOf(c1, c2, c3), deck.draw(3))
        assertEquals(2, deck.remaining)
    }

    @Test
    fun `draw auto-reshuffles when draw pile exhausted mid-draw`() {
        val drawn = deck.draw(5)          // exhaust draw pile
        deck.discard(drawn)               // put all into discard
        val redrawn = deck.draw(5)        // triggers auto-reshuffle
        assertEquals(5, redrawn.size)
        assertEquals(0, deck.remaining)
    }

    @Test
    fun `draw throws when not enough cards even after reshuffle`() {
        deck.draw(5)                      // exhaust; discard pile still empty
        assertThrows<IllegalStateException> { deck.draw(1) }
    }

    // ── flip ─────────────────────────────────────────────────────────────────

    @Test
    fun `flip removes and returns top card`() {
        assertEquals(c1, deck.flip())
        assertEquals(4, deck.remaining)
    }

    @Test
    fun `flip auto-reshuffles when draw pile empty`() {
        deck.draw(5)
        deck.discard(listOf(c1))
        val flipped = deck.flip()
        assertEquals(c1, flipped)
        assertEquals(0, deck.remaining)
    }

    // ── peek ─────────────────────────────────────────────────────────────────

    @Test
    fun `peek returns top n cards without removing them`() {
        val peeked = deck.peek(3)
        assertEquals(listOf(c1, c2, c3), peeked)
        assertEquals(5, deck.remaining)   // draw pile unchanged
    }

    @Test
    fun `peek does not consume the draw pile`() {
        deck.peek(5)
        assertEquals(listOf(c1, c2, c3, c4, c5), deck.draw(5))
    }

    // ── putOnTop ─────────────────────────────────────────────────────────────

    @Test
    fun `putOnTop places cards on top preserving list order`() {
        deck.putOnTop(listOf(extra1, extra2))
        assertEquals(listOf(extra1, extra2, c1, c2, c3), deck.draw(5))
    }

    @Test
    fun `putOnTop with single card places it on top`() {
        deck.putOnTop(listOf(extra1))
        assertEquals(extra1, deck.flip())
    }

    // ── putOnBottom ──────────────────────────────────────────────────────────

    @Test
    fun `putOnBottom places cards at bottom preserving list order`() {
        deck.putOnBottom(listOf(extra1, extra2))
        val all = deck.draw(7)
        assertEquals(listOf(extra1, extra2), all.takeLast(2))
    }

    // ── search ───────────────────────────────────────────────────────────────

    @Test
    fun `search finds and removes first matching card`() {
        val found = deck.search { it.type == CardType.PEACH }
        assertEquals(c3, found)
        assertEquals(4, deck.remaining)
    }

    @Test
    fun `search returns null when no card matches`() {
        assertNull(deck.search { it.type == CardType.LIGHTNING })
        assertEquals(5, deck.remaining)   // draw pile unchanged
    }

    // ── discard and takeFromDiscard ───────────────────────────────────────────

    @Test
    fun `discard and takeFromDiscard roundtrip`() {
        deck.draw(1)                      // remove c1 from draw pile
        deck.discard(listOf(c1))
        assertEquals(c1, deck.takeFromDiscard(c1))
    }

    @Test
    fun `takeFromDiscard throws when card not in discard pile`() {
        assertThrows<IllegalStateException> { deck.takeFromDiscard(c1) }
    }

    // ── reshuffle ────────────────────────────────────────────────────────────

    @Test
    fun `reshuffle merges discard pile into draw pile`() {
        val drawn = deck.draw(3)
        deck.discard(drawn)
        assertEquals(2, deck.remaining)
        deck.reshuffle()
        assertEquals(5, deck.remaining)
    }

    @Test
    fun `reshuffle clears the discard pile`() {
        deck.discard(listOf(c1))
        deck.reshuffle()
        assertThrows<IllegalStateException> { deck.takeFromDiscard(c1) }
    }

    // ── reveal / takeRevealed / discardRevealed ───────────────────────────────

    @Test
    fun `reveal moves top n cards out of draw pile`() {
        deck.reveal(3)
        assertEquals(2, deck.remaining)
    }

    @Test
    fun `takeRevealed removes a specific card from the revealed zone`() {
        deck.reveal(3)                    // c1, c2, c3 revealed
        val taken = deck.takeRevealed(c2)
        assertEquals(c2, taken)
        assertEquals(2, deck.remaining)   // draw pile unchanged
    }

    @Test
    fun `takeRevealed throws when card not in revealed zone`() {
        deck.reveal(2)                    // c1, c2 revealed; c3 still in draw pile
        assertThrows<IllegalStateException> { deck.takeRevealed(c3) }
    }

    @Test
    fun `discardRevealed moves all revealed cards to discard pile`() {
        deck.reveal(3)                    // c1, c2, c3 revealed
        deck.discardRevealed()
        assertEquals(2, deck.remaining)
        // verify they landed in the discard pile
        assertEquals(c1, deck.takeFromDiscard(c1))
        assertEquals(c2, deck.takeFromDiscard(c2))
        assertEquals(c3, deck.takeFromDiscard(c3))
    }

    @Test
    fun `discardRevealed clears the revealed zone`() {
        deck.reveal(2)                    // c1, c2 revealed
        deck.discardRevealed()
        // c1 is now in discard, not revealed — takeRevealed should fail
        assertThrows<IllegalStateException> { deck.takeRevealed(c1) }
    }
}