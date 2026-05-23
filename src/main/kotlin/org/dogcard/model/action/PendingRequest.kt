package org.dogcard.model.action

import org.dogcard.model.card.Card

sealed class PendingRequest {
    data class RespondToAttack(
        val attackerSeatIndex: Int,
        val targetSeatIndex: Int,
        val card: Card,
    ) : PendingRequest()
}