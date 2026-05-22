package org.dogcard.game.session

import org.dogcard.game.factory.GameRoomFactory
import org.dogcard.model.turn.GamePhase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class GameSessionDrawTest {

    private val factory = GameRoomFactory()

    @Test
    fun `Judge phase auto-advances when judgment area is empty`() {
        val session = GameSession(factory.create1v1Setup(), random = Random(seed = 0))
        session.start()
        assertEquals(GamePhase.Judge, session.currentPhase)
        session.advancePhase()
        assertEquals(GamePhase.Draw, session.currentPhase)
    }

    @Test
    fun `Draw phase deals 2 cards to the active seat`() {
        val session = GameSession(factory.create1v1Setup(), random = Random(seed = 0))
        session.start()
        val activeSeatIndex = session.currentSeatIndex!!
        val handSizeBefore = session.seats[activeSeatIndex].handCards.size
        session.advancePhase() // Judge → Draw
        session.advancePhase() // Draw → Play, deals cards
        assertEquals(handSizeBefore + 2, session.seats[activeSeatIndex].handCards.size)
    }

    @Test
    fun `after Draw phase, current phase is Play`() {
        val session = GameSession(factory.create1v1Setup(), random = Random(seed = 0))
        session.start()
        session.advancePhase() // Judge → Draw
        session.advancePhase() // Draw → Play
        assertEquals(GamePhase.Play, session.currentPhase)
    }

    @Test
    fun `deck size decreases by 2 after draw`() {
        val session = GameSession(factory.create1v1Setup(), random = Random(seed = 0))
        session.start()
        val deckSizeBefore = session.deck.remaining
        session.advancePhase() // Judge → Draw
        session.advancePhase() // Draw → Play, draws from deck
        assertEquals(deckSizeBefore - 2, session.deck.remaining)
    }
}