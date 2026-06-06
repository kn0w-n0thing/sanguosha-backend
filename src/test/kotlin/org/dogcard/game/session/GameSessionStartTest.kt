package org.dogcard.game.session

import org.dogcard.game.factory.GameRoomFactory
import org.dogcard.model.hero.Role
import org.dogcard.model.seat.Allegiance
import org.dogcard.model.turn.GamePhase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class GameSessionStartTest {

    private val factory = GameRoomFactory()

    @Test
    fun `after start, exactly one seat has LORD allegiance`() {
        val setup = factory.create1v1Setup()
        val session = GameSession(setup, random = Random(seed = 0))
        session.start()
        val lordCount = session.seats.count { it.allegiance == Allegiance.RoleBased(Role.LORD) }
        assertEquals(1, lordCount)
    }

    @Test
    fun `after start, exactly one seat has SPY allegiance`() {
        val setup = factory.create1v1Setup()
        val session = GameSession(setup, random = Random(seed = 0))
        session.start()
        val spyCount = session.seats.count { it.allegiance == Allegiance.RoleBased(Role.SPY) }
        assertEquals(1, spyCount)
    }

    @Test
    fun `after start, no seat has Unknown allegiance`() {
        val setup = factory.create1v1Setup()
        val session = GameSession(setup, random = Random(seed = 0))
        session.start()
        assertTrue(session.seats.none { it.allegiance == Allegiance.Unknown })
    }

    @Test
    fun `allegiance assignment is not always the same order`() {
        val runs = 20
        val lordCounts = IntArray(2)
        repeat(runs) { seed ->
            val session = GameSession(factory.create1v1Setup(), random = Random(seed))
            session.start()
            val lordIndex = session.seats.indexOfFirst { it.allegiance == Allegiance.RoleBased(Role.LORD) }
            lordCounts[lordIndex]++
        }
        assertTrue(lordCounts[0] >= 5, "Seat 0 was LORD too rarely: ${lordCounts[0]}/$runs")
        assertTrue(lordCounts[1] >= 5, "Seat 1 was LORD too rarely: ${lordCounts[1]}/$runs")
    }

    @Test
    fun `after start, each seat has 4 cards in hand`() {
        val setup = factory.create1v1Setup()
        val session = GameSession(setup, random = Random(seed = 0))
        session.start()
        session.seats.forEach { seat ->
            assertEquals(4, seat.handCards.size)
        }
    }

    @Test
    fun `after start, deck size decreased by 8`() {
        val setup = factory.create1v1Setup()
        val deckSizeBefore = setup.deck.remaining
        val session = GameSession(setup, random = Random(seed = 0))
        session.start()
        assertEquals(deckSizeBefore - 8, session.deck.remaining)
    }

    @Test
    fun `start enters Begin phase for the SPY seat`() {
        val setup = factory.create1v1Setup()
        val session = GameSession(setup, random = Random(seed = 0))
        session.start()
        val spySeatIndex = session.seats.indexOfFirst { it.allegiance == Allegiance.RoleBased(Role.SPY) }
        assertEquals(spySeatIndex, session.currentSeatIndex)
        assertEquals(GamePhase.Begin, session.currentPhase)
    }

    @Test
    fun `start cannot be called twice`() {
        val setup = factory.create1v1Setup()
        val session = GameSession(setup, random = Random(seed = 0))
        session.start()
        org.junit.jupiter.api.assertThrows<IllegalStateException> {
            session.start()
        }
    }
}