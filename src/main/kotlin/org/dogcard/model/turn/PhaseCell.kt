package org.dogcard.model.turn

data class PhaseCell(val phase: GamePhase, val seatIndex: Int)

private val TURN_SEQUENCE = listOf(
    GamePhase.Judge, GamePhase.Draw, GamePhase.Play, GamePhase.Discard, GamePhase.End,
)

fun turnFor(seatIndex: Int): List<PhaseCell> = TURN_SEQUENCE.map { PhaseCell(it, seatIndex) }