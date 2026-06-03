package org.dogcard.game.session

import org.dogcard.game.factory.GameRoomFactory
import org.dogcard.model.action.GameAction
import org.dogcard.model.action.GameEvent
import org.dogcard.model.card.Card
import org.dogcard.model.card.CardType
import org.dogcard.model.card.Suit
import org.dogcard.model.hero.Role
import org.dogcard.model.seat.Allegiance
import org.dogcard.util.FakeDeck
import org.dogcard.util.firstOfType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class GameSessionEventTest {

    private val factory = GameRoomFactory()

    private fun attack(number: Int) = Card(CardType.ATTACK, Suit.SPADE, number, id = number)
    private fun dodge(number: Int) = Card(CardType.DODGE, Suit.HEART, number, id = 100 + number)

    private fun sessionAtPlayPhaseWithEvents(): Pair<GameSession, MutableList<GameEvent>> {
        val events = mutableListOf<GameEvent>()
        val deck = FakeDeck(
            attack(1), attack(2), dodge(1), dodge(2),
            attack(3), attack(4), dodge(3), dodge(4),
            attack(5), dodge(5),
        )
        val session = GameSession(factory.create1v1Setup(deck), random = Random(seed = 0), onEvent = { events += it })
        session.start()
        session.advancePhase() // Judge → Draw
        session.advancePhase() // Draw → Play
        events.clear()
        return session to events
    }

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
    fun `HandUpdated emitted with hand grown from 4 to 6`() {
        val events = mutableListOf<GameEvent>()
        val session = GameSession(factory.create1v1Setup(), random = Random(seed = 0), onEvent = { events += it })
        session.start()
        val activeSeatIndex = session.currentSeatIndex!!
        events.clear()
        session.advancePhase() // Judge → Draw
        session.advancePhase() // Draw → Play, should emit HandUpdated
        val handUpdated = events.filterIsInstance<GameEvent.HandUpdated>()
            .first { it.seatIndex == activeSeatIndex }
        assertEquals(6, handUpdated.cards.size)
    }

    @Test
    fun `CardsDrawn event emitted with correct seatIndex and 2 cards`() {
        val events = mutableListOf<GameEvent>()
        val session = GameSession(factory.create1v1Setup(), random = Random(seed = 0), onEvent = { events += it })
        session.start()
        val activeSeatIndex = session.currentSeatIndex!!
        session.advancePhase() // Judge → Draw
        session.advancePhase() // Draw → Play, emits CardsDrawn
        val cardsDrawn = events.filterIsInstance<GameEvent.CardsDrawn>().first()
        assertEquals(activeSeatIndex, cardsDrawn.seatIndex)
        assertEquals(2, cardsDrawn.cards.size)
    }

    @Test
    fun `CardsDrawn cards are identical to the new cards added to hand`() {
        val events = mutableListOf<GameEvent>()
        val session = GameSession(factory.create1v1Setup(), random = Random(seed = 0), onEvent = { events += it })
        session.start()
        val activeSeatIndex = session.currentSeatIndex!!
        val handBefore = session.seats[activeSeatIndex].handCards.toList()
        events.clear()
        session.advancePhase() // Judge → Draw
        session.advancePhase() // Draw → Play
        val cardsDrawn = events.filterIsInstance<GameEvent.CardsDrawn>().first()
        val handUpdated = events.filterIsInstance<GameEvent.HandUpdated>().first { it.seatIndex == activeSeatIndex }
        val newCards = handUpdated.cards.drop(handBefore.size)
        assertEquals(cardsDrawn.cards, newCards)
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
    fun `PlayAttack emits AttackPlayed and ResponseRequested`() {
        val (session, events) = sessionAtPlayPhaseWithEvents()
        val activeSeatIndex = session.currentSeatIndex!!
        val targetSeatIndex = 1 - activeSeatIndex
        val attackCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.ATTACK)
        session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = attackCard, targetSeatIndex = targetSeatIndex)
        )
        val attackPlayed = events.filterIsInstance<GameEvent.AttackPlayed>().firstOrNull()
        val responseRequested = events.filterIsInstance<GameEvent.ResponseRequested>().firstOrNull()
        assertNotNull(attackPlayed)
        assertNotNull(responseRequested)
        assertEquals(activeSeatIndex, attackPlayed!!.attackerSeatIndex)
        assertEquals(targetSeatIndex, attackPlayed.targetSeatIndex)
        assertEquals(attackCard, attackPlayed.card)
        assertEquals(activeSeatIndex, responseRequested!!.attackerSeatIndex)
        assertEquals(targetSeatIndex, responseRequested.targetSeatIndex)
    }

    @Test
    fun `RespondWithDodge emits DodgePlayed`() {
        val (session, events) = sessionAtPlayPhaseWithEvents()
        val activeSeatIndex = session.currentSeatIndex!!
        val targetSeatIndex = 1 - activeSeatIndex
        val attackCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.ATTACK)
        session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = attackCard, targetSeatIndex = targetSeatIndex)
        )
        val dodgeCard = session.seats[targetSeatIndex].handCards.firstOfType(CardType.DODGE)
        events.clear()
        session.submitAction(targetSeatIndex, GameAction.RespondWithDodge(card = dodgeCard))
        val dodgePlayed = events.filterIsInstance<GameEvent.DodgePlayed>().firstOrNull()
        assertNotNull(dodgePlayed)
        assertEquals(targetSeatIndex, dodgePlayed!!.defenderSeatIndex)
        assertEquals(dodgeCard, dodgePlayed.card)
    }

    @Test
    fun `Pass emits DamageDealt with amount=1 and correct newHp`() {
        val (session, events) = sessionAtPlayPhaseWithEvents()
        val activeSeatIndex = session.currentSeatIndex!!
        val targetSeatIndex = 1 - activeSeatIndex
        val hpBefore = session.seats[targetSeatIndex].hp.current
        val attackCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.ATTACK)
        session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = attackCard, targetSeatIndex = targetSeatIndex)
        )
        events.clear()
        session.submitAction(targetSeatIndex, GameAction.Pass)
        val damageDealt = events.filterIsInstance<GameEvent.DamageDealt>().firstOrNull()
        assertNotNull(damageDealt)
        assertEquals(targetSeatIndex, damageDealt!!.targetSeatIndex)
        assertEquals(1, damageDealt.amount)
        assertEquals(hpBefore - 1, damageDealt.newHp)
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