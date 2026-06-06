package org.dogcard.model.turn

sealed class GamePhase {
    object Idle : GamePhase()
    object Begin : GamePhase()
    object Judge : GamePhase()
    object Draw : GamePhase()
    object Play : GamePhase()
    object Discard : GamePhase()
    object End : GamePhase()
}