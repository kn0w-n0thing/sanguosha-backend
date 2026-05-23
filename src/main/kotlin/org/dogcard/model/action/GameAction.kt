package org.dogcard.model.action

import org.dogcard.model.card.Card

sealed class GameAction {
    data class PlayAttack(val card: Card, val targetSeatIndex: Int) : GameAction()
    data class RespondWithDodge(val card: Card) : GameAction()
    data object Pass : GameAction()
    data object EndPlayPhase : GameAction()
    data class Discard(val cards: List<Card>) : GameAction()
}