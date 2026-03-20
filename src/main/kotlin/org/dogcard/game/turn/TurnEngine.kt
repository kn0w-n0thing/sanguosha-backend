package org.dogcard.game.turn

import org.dogcard.model.turn.*

/**
 * Drives the game forward by consuming a mutable phase tape.
 * Owned by GameRoom.
 *
 * Hooks registered via [register] are invoked at the appropriate checkpoints during [advance].
 * Hook actions may call [skip], [insertAfter], or [append] to reshape the remaining tape.
 */
class TurnEngine(private val seatCount: Int) : TurnControl {

    private val tape = ArrayDeque<PhaseCell>()

    var current: PhaseCell? = null
        private set

    private val broadcastRegistry =
        mutableMapOf<HookKey, MutableList<IndexedHook<PhaseHook.Broadcast>>>()
    private val chainRegistry =
        mutableMapOf<HookKey, MutableList<IndexedHook<PhaseHook.Chain<*>>>>()

    private data class IndexedHook<H>(val seatIndex: Int, val hook: H)

    // ── Registration ────────────────────────────────────────────────────────

    fun register(ownerSeatIndex: Int, hook: PhaseHook) {
        when (hook) {
            is PhaseHook.Broadcast -> broadcastRegistry
                .getOrPut(hook.key) { mutableListOf() }
                .add(IndexedHook(ownerSeatIndex, hook))

            is PhaseHook.Chain<*> -> chainRegistry
                .getOrPut(hook.key) { mutableListOf() }
                .add(IndexedHook(ownerSeatIndex, hook))
        }
    }

    // ── Tape operations (TurnControl) ────────────────────────────────────────

    /** Initialise the tape for seat [seatIndex]'s turn. */
    fun startTurn(seatIndex: Int) {
        tape.addAll(turnFor(seatIndex))
    }

    /**
     * Fire EXIT on the current cell, pop the next cell from the tape,
     * then fire ENTER on it.
     */
    fun advance() {
        current?.let { fireBroadcast(PhaseTiming.EXIT) }
        current = tape.removeFirstOrNull()
        current?.let { fireBroadcast(PhaseTiming.ENTER) }
    }

    /** Remove all remaining cells whose phase matches [phase]. */
    override fun skip(phase: GamePhase) {
        tape.removeAll { it.phase == phase }
    }

    /** Inject [cell] as the very next cell to execute after the current one. */
    override fun insertAfter(cell: PhaseCell) {
        tape.addFirst(cell)
    }

    /** Append [cells] to the end of the tape (e.g. extra turns). */
    override fun append(cells: List<PhaseCell>) {
        tape.addAll(cells)
    }

    // ── Checkpoint firing ────────────────────────────────────────────────────

    /**
     * Invoke all BROADCAST hooks registered for (current.phase, [timing]).
     * SELF-scope hooks fire first (owner == active seat), then ANY-scope hooks
     * in counter-clockwise seat order.
     */
    fun fireBroadcast(timing: PhaseTiming) {
        val cell = current ?: return
        resolvedBroadcasts(cell, timing).forEach { it.action(this) }
    }

    /**
     * Thread [initial] through all CHAIN hooks registered for (current.phase, [timing], ANY)
     * in counter-clockwise seat order, returning the final transformed value.
     *
     * The unchecked cast is safe: all Chain hooks on the same [HookKey] must carry the
     * same payload type T, enforced by the skill that registers them.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> fireChain(timing: PhaseTiming, initial: T): T {
        val cell = current ?: return initial
        val key = HookKey(cell.phase, timing, SeatScope.ANY)
        val hooks = (chainRegistry[key] ?: return initial)
            .sortedByCounterClockwise(cell.seatIndex)
            .map { it.hook as PhaseHook.Chain<T> }
        return hooks.fold(initial) { payload, hook -> hook.action(this, payload) }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun resolvedBroadcasts(cell: PhaseCell, timing: PhaseTiming): List<PhaseHook.Broadcast> {
        val selfKey = HookKey(cell.phase, timing, SeatScope.SELF)
        val anyKey = HookKey(cell.phase, timing, SeatScope.ANY)

        val selfHooks = (broadcastRegistry[selfKey] ?: emptyList())
            .filter { it.seatIndex == cell.seatIndex }
            .map { it.hook }

        val anyHooks = (broadcastRegistry[anyKey] ?: emptyList())
            .sortedByCounterClockwise(cell.seatIndex)
            .map { it.hook }

        return selfHooks + anyHooks
    }

    /**
     * Sort hooks counter-clockwise from [activeSeat].
     * Order: (activeSeat-1), (activeSeat-2), ..., (activeSeat+1), activeSeat.
     */
    private fun <H> List<IndexedHook<H>>.sortedByCounterClockwise(activeSeat: Int): List<IndexedHook<H>> =
        sortedBy { (activeSeat - it.seatIndex - 1 + seatCount) % seatCount }
}