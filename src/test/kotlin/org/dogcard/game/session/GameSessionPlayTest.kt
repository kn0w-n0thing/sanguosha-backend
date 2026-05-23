package org.dogcard.game.session

import org.dogcard.game.factory.GameRoomFactory
import org.dogcard.model.action.GameAction
import org.dogcard.model.card.Card
import org.dogcard.model.card.CardType
import org.dogcard.model.card.Suit
import org.dogcard.util.FakeDeck
import org.dogcard.util.firstOfType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class GameSessionPlayTest {

    private val factory = GameRoomFactory()

    private fun attack(number: Int) = Card(CardType.ATTACK, Suit.SPADE, number)
    private fun dodge(number: Int) = Card(CardType.DODGE, Suit.HEART, number)

    private fun sessionAtPlayPhase(): GameSession {
        val deck = FakeDeck(
            attack(1), attack(2), dodge(1), dodge(2),  // seat[0] initial hand
            attack(3), attack(4), dodge(3), dodge(4),  // seat[1] initial hand
            attack(5), dodge(5),                        // draw-phase cards
        )
        val session = GameSession(factory.create1v1Setup(deck), random = Random(seed = 0))
        session.start()
        session.advancePhase() // Judge → Draw
        session.advancePhase() // Draw → Play
        return session
    }

    @Test
    fun `pendingRequest is set after PlayAttack`() {
        val session = sessionAtPlayPhase()
        val activeSeatIndex = session.currentSeatIndex!!
        val targetSeatIndex = 1 - activeSeatIndex
        val attackCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.ATTACK)
        session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = attackCard, targetSeatIndex = targetSeatIndex)
        )
        assertNotNull(session.pendingRequest)
    }

    @Test
    fun `RespondWithDodge clears pendingRequest`() {
        val session = sessionAtPlayPhase()
        val activeSeatIndex = session.currentSeatIndex!!
        val targetSeatIndex = 1 - activeSeatIndex
        val attackCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.ATTACK)
        session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = attackCard, targetSeatIndex = targetSeatIndex)
        )
        val dodgeCard = session.seats[targetSeatIndex].handCards.firstOfType(CardType.DODGE)
        session.submitAction(targetSeatIndex, GameAction.RespondWithDodge(card = dodgeCard))
        assertNull(session.pendingRequest)
    }

    @Test
    fun `When pendingRequest is resolved, discard the attack card to the discard pile`() {
        val session = sessionAtPlayPhase()
        val activeSeatIndex = session.currentSeatIndex!!
        val targetSeatIndex = 1 - activeSeatIndex
        val attackCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.ATTACK)
        session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = attackCard, targetSeatIndex = targetSeatIndex)
        )
        session.submitAction(targetSeatIndex, GameAction.Pass)
        assertTrue((session.deck as FakeDeck).discardPile.contains(attackCard))
    }

    @Test
    fun `RespondWithDodge discards the dodge card to the discard pile`() {
        val session = sessionAtPlayPhase()
        val activeSeatIndex = session.currentSeatIndex!!
        val targetSeatIndex = 1 - activeSeatIndex
        val attackCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.ATTACK)
        session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = attackCard, targetSeatIndex = targetSeatIndex)
        )
        val dodgeCard = session.seats[targetSeatIndex].handCards.firstOfType(CardType.DODGE)
        session.submitAction(targetSeatIndex, GameAction.RespondWithDodge(card = dodgeCard))
        assertTrue((session.deck as FakeDeck).discardPile.contains(dodgeCard))
    }

    @Test
    fun `RespondWithDodge does not reduce target HP`() {
        val session = sessionAtPlayPhase()
        val activeSeatIndex = session.currentSeatIndex!!
        val targetSeatIndex = 1 - activeSeatIndex
        val hpBefore = session.seats[targetSeatIndex].hp.current
        val attackCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.ATTACK)
        session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = attackCard, targetSeatIndex = targetSeatIndex)
        )
        val dodgeCard = session.seats[targetSeatIndex].handCards.firstOfType(CardType.DODGE)
        session.submitAction(targetSeatIndex, GameAction.RespondWithDodge(card = dodgeCard))
        assertEquals(hpBefore, session.seats[targetSeatIndex].hp.current)
    }

    @Test
    fun `Pass reduces target HP by 1 by default`() {
        val session = sessionAtPlayPhase()
        val activeSeatIndex = session.currentSeatIndex!!
        val targetSeatIndex = 1 - activeSeatIndex
        val hpBefore = session.seats[targetSeatIndex].hp.current
        val attackCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.ATTACK)
        session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = attackCard, targetSeatIndex = targetSeatIndex)
        )
        session.submitAction(targetSeatIndex, GameAction.Pass)
        assertEquals(hpBefore - 1, session.seats[targetSeatIndex].hp.current)
    }

    @Test
    fun `Pass clears pendingRequest`() {
        val session = sessionAtPlayPhase()
        val activeSeatIndex = session.currentSeatIndex!!
        val targetSeatIndex = 1 - activeSeatIndex
        val attackCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.ATTACK)
        session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = attackCard, targetSeatIndex = targetSeatIndex)
        )
        session.submitAction(targetSeatIndex, GameAction.Pass)
        assertNull(session.pendingRequest)
    }

    @Test
    fun `RespondWithDodge submitted by the wrong seat returns an error`() {
        val session = sessionAtPlayPhase()
        val activeSeatIndex = session.currentSeatIndex!!
        val targetSeatIndex = 1 - activeSeatIndex
        val attackCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.ATTACK)
        session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = attackCard, targetSeatIndex = targetSeatIndex)
        )
        val dodgeCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.DODGE)
        val result = session.submitAction(activeSeatIndex, GameAction.RespondWithDodge(card = dodgeCard))
        assertTrue(result.isFailure)
    }

    @Test
    fun `RespondWithDodge with a non-DODGE card returns an error`() {
        val session = sessionAtPlayPhase()
        val activeSeatIndex = session.currentSeatIndex!!
        val targetSeatIndex = 1 - activeSeatIndex
        val attackCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.ATTACK)
        session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = attackCard, targetSeatIndex = targetSeatIndex)
        )
        val nonDodgeCard = session.seats[targetSeatIndex].handCards.firstOfType(CardType.ATTACK)
        val result = session.submitAction(targetSeatIndex, GameAction.RespondWithDodge(card = nonDodgeCard))
        assertTrue(result.isFailure)
    }

    @Test
    fun `PlayAttack submitted while pendingRequest is already set returns an error`() {
        val session = sessionAtPlayPhase()
        val activeSeatIndex = session.currentSeatIndex!!
        val targetSeatIndex = 1 - activeSeatIndex
        val attackCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.ATTACK)
        session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = attackCard, targetSeatIndex = targetSeatIndex)
        )
        val secondAttack = session.seats[activeSeatIndex].handCards.firstOfType(CardType.ATTACK)
        val result = session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = secondAttack, targetSeatIndex = targetSeatIndex)
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `PlayAttack submitted by the non-active seat returns an error`() {
        val session = sessionAtPlayPhase()
        val activeSeatIndex = session.currentSeatIndex!!
        val nonActiveSeatIndex = 1 - activeSeatIndex
        val attackCard = session.seats[nonActiveSeatIndex].handCards.firstOfType(CardType.ATTACK)
        val result = session.submitAction(
            nonActiveSeatIndex,
            GameAction.PlayAttack(card = attackCard, targetSeatIndex = activeSeatIndex)
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `PlayAttack with a non-ATTACK card returns an error`() {
        val session = sessionAtPlayPhase()
        val activeSeatIndex = session.currentSeatIndex!!
        val targetSeatIndex = 1 - activeSeatIndex
        val dodgeCard = session.seats[activeSeatIndex].handCards.firstOfType(CardType.DODGE)
        val result = session.submitAction(
            activeSeatIndex,
            GameAction.PlayAttack(card = dodgeCard, targetSeatIndex = targetSeatIndex)
        )
        assertTrue(result.isFailure)
    }
}