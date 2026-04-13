package org.dogcard.game.factory

import org.dogcard.model.card.CardType
import org.dogcard.model.seat.Allegiance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameRoomFactoryTest {

    private val factory = GameRoomFactory()

    @Test
    fun `factory creates a room with 2 seats`() {
        val setup = factory.create1v1Setup()
        assertEquals(2, setup.room.seats.size)
    }

    @Test
    fun `each seat has 3 blank heroes`() {
        val setup = factory.create1v1Setup()
        setup.room.seats.forEach { seat ->
            assertEquals(3, seat.heroes.size)
        }
    }

    @Test
    fun `each hero has 4 HP`() {
        val setup = factory.create1v1Setup()
        setup.room.seats.forEach { seat ->
            seat.heroes.forEach { hero ->
                assertEquals(4, hero.maxHp.wholes)
            }
        }
    }

    @Test
    fun `seats start at full HP`() {
        val setup = factory.create1v1Setup()
        setup.room.seats.forEach { seat ->
            assertEquals(seat.hp.max, seat.heroes.first().maxHp.wholes)
            assertEquals(seat.hp.max, seat.hp.current)
        }
    }

    @Test
    fun `card pool contains only ATTACK and DODGE`() {
        val allowedTypes = setOf(CardType.ATTACK, CardType.DODGE)
        assertTrue(factory.currentCardPool.isNotEmpty())
        factory.currentCardPool.forEach { card ->
            assertTrue(card.type in allowedTypes, "Unexpected card type: ${card.type}")
        }
    }

    @Test
    fun `seats start in Unknown allegiance before game starts`() {
        val setup = factory.create1v1Setup()
        setup.room.seats.forEach { seat ->
            assertInstanceOf(Allegiance.Unknown::class.java, seat.allegiance)
        }
    }

    @Test
    fun `seat indices match list order`() {
        val setup = factory.create1v1Setup()
        setup.room.seats.forEachIndexed { index, seat ->
            assertEquals(index, seat.seatIndex)
        }
    }

    @Test
    fun `seats start with empty hand`() {
        val setup = factory.create1v1Setup()
        setup.room.seats.forEach { seat ->
            assertTrue(seat.handCards.isEmpty())
        }
    }
}