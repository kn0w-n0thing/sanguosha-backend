# Domain Design — Turn Engine

Turn phase state machine, skill hook system, and related types.

---

## GamePhase

Represents one phase within a single seat's turn.
Modelled as a **sealed class** for exhaustive `when` expressions and future extensibility.

```
sealed GamePhase                 — org.dogcard.model.game (to be created)
├── Idle     — waiting between turns
├── Judge    — resolve delayed tricks in judgment area
├── Draw     — draw 2 cards (default)
├── Play     — play cards freely
├── Discard  — discard down to hand limit (= current HP)
└── End      — trigger end-of-turn effects; advance to next seat
```

Normal turn flow:
```
Idle → Judge → Draw → Play → Discard → End → Idle (next seat)
```

| Phase   | Description                                                                                           |
|---------|-------------------------------------------------------------------------------------------------------|
| Idle    | Waiting; between turns                                                                                |
| Judge   | Resolve each delayed trick in order; flip a card from deck; apply or discard based on suit/number    |
| Draw    | Draw 2 cards (default; some skills modify this)                                                       |
| Play    | Play cards freely until the player ends the phase                                                     |
| Discard | Discard down to max hand size (= current HP) if over limit                                            |
| End     | Trigger end-of-turn effects; advance to next seat                                                     |

**Delayed tricks resolved in Judge:**

| Card          | Judgment condition | Effect if triggered                                           |
|---------------|--------------------|---------------------------------------------------------------|
| 乐不思蜀 Ecstasy  | Flip is not ♥      | Skip Play phase                                               |
| 兵粮寸断 Suppression | Flip is not ♣  | Skip Draw phase                                               |
| 闪电 Lightning | Flip is ♠ 2–9      | Deal 3 thunder damage; otherwise pass to next player          |

> Card effects that interrupt the turn flow (e.g. DUEL, PEACH_REQUEST) are modelled
> as `GameEvent` — they are **not** `GamePhase` subtypes.

---

## TurnContext

Accumulated state that modifies the current turn's phase flow.
Written by delayed tricks and skill hooks during the turn; consumed by `TurnStateMachine.advance()`.

```
TurnContext
└── skippedPhases: Set<GamePhase>   — phases to bypass this turn
```

Advance rule:
```
GamePhase.advance(ctx) → next phase in [Idle → Judge → Draw → Play → Discard → End]
                          that is NOT in ctx.skippedPhases; wraps back to Idle at End
```

Both delayed tricks and skill hooks write to the same `skippedPhases` — no special cases.

---

## PhaseTiming

Named checkpoints within or around a phase at which hooks may fire.
`ENTER` / `EXIT` are lifecycle boundaries; phase-specific checkpoints sit between them.

```
PhaseTiming
├── ENTER              — before the phase body starts (all phases)
├── EXIT               — after the phase body ends (all phases)
├── BEFORE_JUDGMENT    — within Judge: before each judgment card resolves
│                        (e.g. 司马懿·鬼才, 张角·鬼道 may replace the card here)
└── AFTER_DRAW         — within Draw: after cards have been drawn into hand
```

Checkpoint sequence per phase:
```
Judge:   ENTER → [BEFORE_JUDGMENT → AFTER_JUDGMENT] × N cards → EXIT
Draw:    ENTER → AFTER_DRAW → EXIT
Play:    ENTER → EXIT        (card-level events are GameEvent, not phase hooks)
Discard: ENTER → EXIT
End:     ENTER → EXIT
Idle:    ENTER → EXIT
```

---

## HookPattern

Controls how multiple hooks registered for the same `(phase, timing, seatScope)` interact.

```
HookPattern
├── BROADCAST   — all hooks invoked independently with the same context;
│                 no hook affects what another sees
└── CHAIN       — hooks form an ordered sequence; each receives and may modify
                  a typed payload T before the next hook sees it
```

Chain semantics:
```
initial payload T
  → hook₁(ctx, T)  → T′
  → hook₂(ctx, T′) → T″
  → ...
  → final value used by the engine
```

Chain order: **counter-clockwise from the active seat** (matches physical game rule).

---

## PhaseHook

Declared by a `Skill` to react to a specific `(phase, timing, seatScope)` combination.
The engine indexes hooks by that triple and invokes only relevant ones.

```
PhaseHook
├── phase:     GamePhase           — which phase to observe
├── timing:    PhaseTiming         — which checkpoint within that phase
├── seatScope: SeatScope           — SELF (active seat only) / ANY (all seats)
├── pattern:   HookPattern         — BROADCAST or CHAIN
└── action:    (GameContext, T?) → T?
               — T is Unit for BROADCAST; a domain type (e.g. Card) for CHAIN
```

```
SeatScope — SELF / ANY
```

Examples:
| Skill              | Phase / Timing / Scope          | Pattern   | Effect                                                  |
|--------------------|---------------------------------|-----------|---------------------------------------------------------|
| 张辽·突袭             | Play / ENTER / SELF             | BROADCAST | May attack without distance limit this turn             |
| 司马懿·鬼才            | Judge / BEFORE_JUDGMENT / ANY   | CHAIN     | May replace the judgment card (payload: Card)           |
| 张角·鬼道             | Judge / BEFORE_JUDGMENT / ANY   | CHAIN     | May replace with a black card; sees 鬼才's result first  |
| 诸葛亮·空城            | End / EXIT / SELF               | BROADCAST | Discard all equipment; gain immunity until next turn    |
| (phase-skip skill) | Discard / ENTER / SELF          | BROADCAST | Writes `Discard` into `TurnContext.skippedPhases`       |

> 鬼才 and 鬼道 are both CHAIN on the same checkpoint.
> Priority order: counter-clockwise from the active seat.

---

## TurnStateMachine

Stateful wrapper owned by `GameRoom`. Drives one seat's turn from `Idle` back to `Idle`.

```
TurnStateMachine
├── currentPhase: GamePhase          — current phase of the active seat
├── context: TurnContext             — accumulated skip flags for this turn
│
├── advance()                        — exit current phase → compute next → enter next
├── fireCheckpoint(timing, payload?) — invoke hooks for (currentPhase, timing, seatScope)
│                                      BROADCAST: call all; CHAIN: thread payload through
└── reset()                          — clear context; called at the start of each new turn
```

`advance()` lifecycle:
```
1. fireCheckpoint(EXIT)               — hooks react on phase exit
2. next = currentPhase.advance(ctx)
3. fireCheckpoint(ENTER) on next      — hooks may write to ctx (e.g. add skipped phases)
4. currentPhase = next
```

Phase-internal checkpoint (Judge resolving one delayed trick):
```
1. fireCheckpoint(BEFORE_JUDGMENT, payload=card)   — CHAIN: card may be replaced
2. engine applies the final card's judgment effect
3. fireCheckpoint(AFTER_JUDGMENT,  payload=card)   — BROADCAST: skills react to result
```