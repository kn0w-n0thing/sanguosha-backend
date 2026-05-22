package org.dogcard.game.session

import org.dogcard.game.factory.GameRoomFactory
import org.dogcard.model.action.GameEvent
import org.dogcard.model.hero.Role
import org.dogcard.model.seat.Allegiance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import kotlin.random.Random

class GameSessionEventTest {

    private val factory = GameRoomFactory()

    @Test
    fun `GameStarted seatViews reflect correct HP and hand count`() {
        val events = mutableListOf<GameEvent>()
        val session = GameSession(factory.create1v1Setup(), random = Random(seed = 0), onEvent = { events += it })
        session.start()
        val gameStarted = events.filterIsInstance<GameEvent.GameStarted>().first()
        gameStarted.seatViews.forEach { view ->
            assertEquals(4, view.hp.current)
            assertEquals(4, view.hp.max)
            assertEquals(4, view.handCount)
        }
    }

    @Test
    fun `start emits GameStarted as the first event with firstSeatIndex = SPY's seat index`() {
        val events = mutableListOf<GameEvent>()
        val session = GameSession(factory.create1v1Setup(), random = Random(seed = 0), onEvent = { events += it })
        session.start()
        val spySeatIndex = session.seats.indexOfFirst { it.allegiance == Allegiance.RoleBased(Role.SPY) }
        val first = events.first()
        assertInstanceOf(GameEvent.GameStarted::class.java, first)
        assertEquals(spySeatIndex, (first as GameEvent.GameStarted).firstSeatIndex)
    }

    @Test
    fun `start emits HandUpdated for each seat with 4 cards`() {
        val events = mutableListOf<GameEvent>()
        val session = GameSession(factory.create1v1Setup(), random = Random(seed = 0), onEvent = { events += it })
        session.start()
        val handUpdates = events.filterIsInstance<GameEvent.HandUpdated>()
        assertEquals(2, handUpdates.size)
        handUpdates.forEach { event ->
            assertEquals(4, event.cards.size)
        }
    }
}