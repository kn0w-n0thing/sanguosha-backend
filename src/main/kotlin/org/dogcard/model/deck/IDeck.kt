package org.dogcard.model.deck

import org.dogcard.model.card.Card

/**
 * Domain interface for the game card deck.
 *
 * Manages three distinct card zones:
 * - **Draw pile** — the face-down stack players draw from.
 * - **Discard pile** — cards that have been used or discarded.
 * - **Revealed zone** — cards temporarily exposed by an effect (e.g. 五谷丰登).
 */
interface IDeck {

    /** Number of cards remaining in the draw pile. */
    val remaining: Int

    // -------------------------------------------------------------------------
    // Draw pile operations
    // -------------------------------------------------------------------------

    /**
     * Draw [n] cards from the top of the draw pile.
     * Caller must ensure [remaining] >= [n] (reshuffle beforehand if needed).
     */
    fun draw(n: Int): List<Card>

    /**
     * Flip one card from the top of the draw pile for a judgment (判定).
     * The card is publicly revealed; the caller is responsible for discarding it afterward.
     */
    fun flip(): Card

    /**
     * View the top [n] cards without removing them (e.g. 郭嘉·遗计, 诸葛亮·观星).
     * Returns a snapshot; the draw pile is unchanged.
     */
    fun peek(n: Int): List<Card>

    /**
     * Place [cards] onto the **top** of the draw pile, in list order
     * (e.g. after a peek-and-reorder effect).
     */
    fun putOnTop(cards: List<Card>)

    /**
     * Place [cards] onto the **bottom** of the draw pile, in list order
     * (e.g. after a peek-and-reorder effect).
     */
    fun putOnBottom(cards: List<Card>)

    /**
     * Find and remove the first card in the draw pile that satisfies [predicate]
     * (e.g. 卞夫人·挽危 searching for a specific card).
     * Returns the card, or `null` if none matches.
     */
    fun search(predicate: (Card) -> Boolean): Card?

    // -------------------------------------------------------------------------
    // Discard pile operations
    // -------------------------------------------------------------------------

    /** Move [cards] to the discard pile. */
    fun discard(cards: List<Card>)

    /**
     * Retrieve a specific [card] from the discard pile and return it
     * (e.g. 张昭张纮·固政 fetching a card from the discard pile).
     * Throws [NoSuchElementException] if the card is not in the discard pile.
     */
    fun takeFromDiscard(card: Card): Card

    /** Shuffle the entire discard pile back into the draw pile. */
    fun reshuffle()

    // -------------------------------------------------------------------------
    // Revealed zone operations
    // -------------------------------------------------------------------------

    /**
     * Move the top [n] cards from the draw pile into the revealed zone
     * (e.g. 五谷丰登 exposing cards for players to choose from).
     */
    fun reveal(n: Int)

    /**
     * Remove and return one [card] from the revealed zone
     * (a player picks their card during the effect resolution).
     * Throws [NoSuchElementException] if the card is not in the revealed zone.
     */
    fun takeRevealed(card: Card): Card

    /** Discard all remaining cards in the revealed zone (effect cleanup). */
    fun discardRevealed()
}