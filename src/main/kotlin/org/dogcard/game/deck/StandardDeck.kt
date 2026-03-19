package org.dogcard.game.deck

import org.dogcard.model.card.Card
import org.dogcard.model.deck.IDeck

/**
 * Standard implementation of [IDeck].
 *
 * The primary constructor takes cards **in the given order** with no shuffling applied,
 * which makes the deck fully deterministic and easy to test.
 * Use [StandardDeck.shuffled] for normal game use.
 *
 * When the draw pile runs out mid-operation, the discard pile is automatically
 * reshuffled back into the draw pile before proceeding.
 *
 * @param cards Cards to place into the draw pile, top-first.
 */
class StandardDeck(cards: List<Card>) : IDeck {

    private val drawPile:     ArrayDeque<Card> = ArrayDeque(cards)
    private val discardPile:  MutableList<Card> = mutableListOf()
    private val revealedZone: MutableList<Card> = mutableListOf()

    override val remaining: Int get() = drawPile.size

    private fun ensureAvailable(n: Int) {
        if (remaining < n) {
            reshuffle()
            check(remaining >= n) {
                "Not enough cards even after reshuffle: need $n, have $remaining"
            }
        }
    }

    // -------------------------------------------------------------------------
    // Draw pile operations
    // -------------------------------------------------------------------------

    override fun draw(n: Int): List<Card> {
        require(n >= 0) { "n must be non-negative, got $n" }
        ensureAvailable(n)
        return List(n) { drawPile.removeFirst() }
    }

    override fun flip(): Card {
        ensureAvailable(1)
        return drawPile.removeFirst()
    }

    override fun peek(n: Int): List<Card> {
        require(n >= 0) { "n must be non-negative, got $n" }
        ensureAvailable(n)
        return drawPile.take(n)
    }

    override fun putOnTop(cards: List<Card>) {
        cards.asReversed().forEach { drawPile.addFirst(it) }
    }

    override fun putOnBottom(cards: List<Card>) {
        cards.forEach { drawPile.addLast(it) }
    }

    override fun search(predicate: (Card) -> Boolean): Card? {
        val index = drawPile.indexOfFirst(predicate)
        if (index == -1) return null
        return drawPile.removeAt(index)
    }

    // -------------------------------------------------------------------------
    // Discard pile operations
    // -------------------------------------------------------------------------

    override fun discard(cards: List<Card>) {
        discardPile.addAll(cards)
    }

    override fun takeFromDiscard(card: Card): Card {
        check(discardPile.remove(card)) { "Card $card not found in discard pile" }
        return card
    }

    override fun reshuffle() {
        drawPile.addAll(discardPile.shuffled())
        discardPile.clear()
    }

    // -------------------------------------------------------------------------
    // Revealed zone operations
    // -------------------------------------------------------------------------

    override fun reveal(n: Int) {
        require(n >= 0) { "n must be non-negative, got $n" }
        ensureAvailable(n)
        repeat(n) { revealedZone.add(drawPile.removeFirst()) }
    }

    override fun takeRevealed(card: Card): Card {
        check(revealedZone.remove(card)) { "Card $card not found in revealed zone" }
        return card
    }

    override fun discardRevealed() {
        discardPile.addAll(revealedZone)
        revealedZone.clear()
    }

    companion object {
        /** Create a [StandardDeck] with cards shuffled into random order for normal game use. */
        fun shuffled(cards: List<Card>) = StandardDeck(cards.shuffled())
    }
}