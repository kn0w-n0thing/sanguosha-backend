package org.dogcard.model.turn

/**
 * Index key used to look up hooks registered for a specific (phase, timing, seatScope) combination.
 */
data class HookKey(
    val phase: GamePhase,
    val timing: PhaseTiming,
    val seatScope: SeatScope,
)

/**
 * Declared by a Skill to react to a specific phase lifecycle checkpoint.
 *
 * - [Broadcast]: fired for all matching hooks independently; no hook affects what another sees.
 * - [Chain]: hooks form an ordered sequence (counter-clockwise from active seat);
 *   each receives and may modify a typed payload before the next hook sees it.
 */
sealed class PhaseHook {
    abstract val phase: GamePhase
    abstract val timing: PhaseTiming
    abstract val seatScope: SeatScope
    abstract val pattern: HookPattern

    val key: HookKey get() = HookKey(phase, timing, seatScope)

    class Broadcast(
        override val phase: GamePhase,
        override val timing: PhaseTiming,
        override val seatScope: SeatScope,
        val action: (TurnControl) -> Unit,
    ) : PhaseHook() {
        override val pattern: HookPattern = HookPattern.BROADCAST
    }

    class Chain<T : Any>(
        override val phase: GamePhase,
        override val timing: PhaseTiming,
        override val seatScope: SeatScope,
        val action: (TurnControl, T) -> T,
    ) : PhaseHook() {
        override val pattern: HookPattern = HookPattern.CHAIN
    }
}