# Domain Design — Game Room

GameMode strategy interface, GameRoom aggregate, GameSession orchestrator, and per-mode design notes.
For mode rules, see [`../rules.md`](../rules.md).

---

## Component Graph

```
                        ┌──────────────────────────────────────────────┐
                        │                 GameSession                  │
                        │                                              │
                        │   owns ──► IDeck         (card operations)  │
                        │   owns ──► TurnEngine    (phase tape)        │
                        │   owns ──► GameRoom                         │
                        │              │                               │
                        │              ├── mode: GameMode              │
                        │              └── seats: List<Seat>           │
                        └──────────────────────────────────────────────┘
```

**Ownership rules:**
- `GameSession` owns everything active: deck, turn engine, and the room
- `GameRoom` is pure data — id, mode strategy, and the seat list
- `TurnEngine` lives in `GameSession`, not `GameRoom` — phase driving is a runtime concern, not a data concern
- `GameMode` is a pure strategy — receives only `List<Seat>`, returns updated values, no side effects

---

## GameMode (Strategy Interface)

Encapsulates all mode-specific logic. Swapped at room creation; no other code changes.

```
GameMode
├── assignAllegiances(seats): List<Seat>   — called once at game start
├── checkWinCondition(seats): Winner?      — called after every death and turn end
├── onSeatDeath(dead): Seat                — returns updated seat; no deck access
└── heroEntryDrawCount: Int                — cards drawn when next hero enters (default 0)
```

`onSeatDeath` returns the updated `Seat` with an empty hand.
`GameSession` reads `heroEntryDrawCount` and draws the entry cards itself.
This keeps `GameMode` a pure function on `Seat` data — no session or deck dependency.

---

## GameRoom

Pure data aggregate. Holds mode strategy and seat list. No id, no runtime components.
The session id lives in `GameSession`, assigned by the API layer when a session is created.

```
GameRoom
├── mode: GameMode
└── seats: List<Seat>    — mutable; updated via updateSeat(seat)
```

---

## TurnEngine

The phase tape machine. Owns the mutable sequence of `PhaseCell`s that defines what happens next.

```
TurnEngine
├── tape: ArrayDeque<PhaseCell>   — remaining work; skills reshape this via skip/insertAfter/append
├── current: PhaseCell?           — the phase currently executing
├── startTurn(seatIndex)          — loads the standard phase sequence for a seat's turn
└── advance()                     — fires EXIT on current, pops next cell, fires ENTER
```

`currentSeatIndex` and `currentPhase` are derived from `current` at any point in time.

---

## GameSession

Orchestrates a full game session. The single entry point for all external interactions.

```
GameSession
├── id: String              — assigned by the API layer when the session is created
├── room: GameRoom
├── deck: IDeck
├── engine: TurnEngine
└── onEvent: (GameEvent) -> Unit    — emits events to callers (API layer, AI agent, logger)
```

Exposes derived state so callers never reach into internals:
```
currentPhase: GamePhase?      — engine.current?.phase
currentSeatIndex: Int?        — engine.current?.seatIndex
```

Responsibilities:
- Drives phase transitions via `engine.advance()` and `engine.startTurn()`
- Processes `GameAction` from players (human or AI — same interface)
- Owns all deck operations (draw, discard)
- Applies `onSeatDeath` result and draws entry cards
- Emits `GameEvent` for every observable state change

**Factory:** `GameRoomFactory.createMinimal1v1` returns a `GameSetup(room, deck, engine)`
so the three components can be constructed and wired independently in tests.

---

## Mode Design Notes

Full rules are in [`../rules.md`](../rules.md). Notes here cover design-level differences only.

### 1v1 Mode (竞技 · 2 players)
- `Allegiance.RoleBased(LORD)` vs `RoleBased(SPY)`
- `Seat.heroes` is a rotation queue (3 heroes); index 0 is on field
- `heroEntryDrawCount = 4`
- `onSeatDeath`: advance queue; new hero enters with full HP; `GameSession` deals 4 entry cards
- Win condition: opponent's hero queue is empty

### Identity Mode (身份 · 5–10 players)
- `Allegiance.RoleBased(role)` — LORD / LOYALIST / REBEL / SPY
- Lord revealed at game start; others hidden until death
- `heroEntryDrawCount = 0` (no hero rotation)
- `onSeatDeath`: reveal role, apply kill reward/penalty, check win condition

### Kingdom Mode (国战 · 4–10 players)
- `Allegiance.Unrevealed` until first general revealed (明置) → `Allegiance.KingdomBased(kingdom)`
- `HpState.max` = combined `HpValue` of both heroes (main + sub general)
- `heroEntryDrawCount = 0`
- `onSeatDeath`: reveal all generals, discard cards, check win condition

### 3v3 Mode
- `Allegiance.TeamBased(teamId)` — opaque team ID; typed `Team` added when mode is implemented

### Doudizhu Mode (斗地主 · 3 players)
- `Allegiance.RoleBased(role)` — LANDLORD (地主) vs FARMER (农民) × 2