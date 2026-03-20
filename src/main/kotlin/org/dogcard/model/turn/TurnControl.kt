package org.dogcard.model.turn

/**
 * Operations that a skill hook may call on the turn engine to modify the remaining phase tape.
 * Passed into every hook action as its first argument.
 */
interface TurnControl {
    fun skip(phase: GamePhase)
    fun insertAfter(cell: PhaseCell)
    fun append(cells: List<PhaseCell>)
}