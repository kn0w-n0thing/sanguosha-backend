package org.dogcard.model.action

sealed class PendingRequest {
    data class RespondToAttack(
        val attackerSeatIndex: Int,
        val targetSeatIndex: Int,
    ) : PendingRequest()
}