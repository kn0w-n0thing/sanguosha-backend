package org.dogcard.util

import org.dogcard.model.card.Card
import org.dogcard.model.deck.IDeck

class FakeDeck(vararg cards: Card) : IDeck {
    private val queue = ArrayDeque(cards.toList())
    override val remaining: Int get() = queue.size
    override fun draw(n: Int): List<Card> = (1..n).map { queue.removeFirst() }
    override fun flip(): Card = queue.removeFirst()
    override fun peek(n: Int): List<Card> = queue.take(n)
    override fun putOnTop(cards: List<Card>) = cards.reversed().forEach { queue.addFirst(it) }
    override fun putOnBottom(cards: List<Card>) = cards.forEach { queue.addLast(it) }
    override fun search(predicate: (Card) -> Boolean): Card? =
        queue.firstOrNull(predicate)?.also { queue.remove(it) }

    val discardPile: MutableList<Card> = mutableListOf()
    override fun discard(cards: List<Card>) {
        discardPile.addAll(cards)
    }

    override fun takeFromDiscard(card: Card): Card = throw UnsupportedOperationException()
    override fun reshuffle() {}
    override fun reveal(n: Int) {}
    override fun takeRevealed(card: Card): Card = throw UnsupportedOperationException()
    override fun discardRevealed() {}
}