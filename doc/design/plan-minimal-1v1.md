# Plan — Minimal 1v1 + AI Agent (First Playable Slice)

## Goals

1. Tiny deck — ATTACK + DODGE cards only, growing step by step toward the full standard deck
2. Blank heroes — 4 HP, no skills, 3 per player (rotation queue)
3. Runnable game server (REST + WebSocket)
4. AI agent that can connect and play (Spring AI + Ollama)
5. Game log collection for self-supervised training

No hero skills, no hero draft, no equipment in this slice.
Hand limit = current HP count (standard rule). Draw = 2 cards per turn.

---

## Bug Fix Required First

`Seat.init` has `require(heroes.isNotEmpty())`, but `OneVsOneMode.checkWinCondition` checks
`seat.heroes.isEmpty()` to detect elimination. These contradict each other.

**Fix:** remove the `require` from `Seat.init`; document that an empty heroes list means eliminated.
`OneVsOneMode.onSeatDeath` must update the seat with an empty heroes list when the last hero falls,
so `checkWinCondition` can detect it.

---

## Phase Flow

Each turn for the active seat runs through these phases in order.
`GameSession` auto-handles all phases except Play, which waits for player input.

- **Judge** — auto-advance (no delayed judgments in minimal slice)
- **Draw** — deal 2 cards to the active seat, then auto-advance
- **Play** — wait for the player: `PlayAttack` or `EndPlayPhase`
  - `PlayAttack` opens a response window: the target must `RespondWithDodge` or `Pass`
  - After the response resolves, the active player may act again (but can play at most one ATTACK per turn in minimal slice)
- **Discard** — auto-discard excess cards down to hand limit (= current HP), then auto-advance
- **End** — advance turn to the next seat; their Judge phase begins

---

## Key Classes

### Model layer (`model/`)

- **`GameAction`** — sealed class representing a player's intent:
  `PlayAttack`, `RespondWithDodge`, `Pass`, `EndPlayPhase`

- **`PendingRequest`** — sealed class representing a mid-turn interrupt waiting for a specific player.
  Currently one subtype: `RespondToAttack` (attacker, target, attack card).
  While set, only the target may submit actions.

- **`GameEvent`** — sealed class of server-to-client notifications:
  `GameStarted`, `PhaseChanged`, `CardsDrawn`, `HandUpdated`, `AttackPlayed`,
  `ResponseRequested`, `DodgePlayed`, `DamageDealt`, `HeroRotated`, `GameOver`

- **`SeatView`** — public snapshot of one seat (HP, hand count, hero id).
  `cards` field is non-null only for the owning seat's private view.

- **`GameMode`** — strategy interface; pure functions on `List<Seat>` and `Seat`:
  - `assignAllegiances` — called once at game start
  - `checkWinCondition` — called after every death
  - `onSeatDeath` — returns the updated seat; `GameSession` draws entry cards
  - `heroEntryDrawCount` — number of cards drawn when next hero enters

### Game layer (`game/`)

- **`GameRoom`** — pure data aggregate: room id, mode strategy, and seat list.
  Provides `updateSeat` to replace a seat by index. No deck, no turn engine.

- **`TurnEngine`** — owns the mutable phase tape (`ArrayDeque<PhaseCell>`).
  `startTurn(seatIndex)` loads the standard phase sequence for one seat's turn.
  `advance()` fires EXIT on the current phase, pops the next cell, fires ENTER.
  Skills call `skip`, `insertAfter`, or `append` to reshape the remaining tape.

- **`GameSession`** — the game orchestrator. Owns `GameRoom`, `IDeck`, and `TurnEngine`.
  All external interactions (player actions, event subscriptions) go through `GameSession`.
  Exposes `currentPhase` and `currentSeatIndex` derived from `TurnEngine.current`.

- **`GameRoomFactory`** — builds a minimal 1v1 `GameSetup(room, deck, engine)`.
  The card pool (`currentCardPool`) starts with ATTACK + DODGE and grows as
  card effects are implemented. Blank heroes replace real ones until hero draft is added.

- **`GameLogger`** — records every `(state, action)` pair to a JSONL file for training data.

### API layer (`api/`)

- **`RoomStore`** — in-memory map of `roomId → GameSession`
- **`RoomController`** — REST endpoints: create room, join, start, submit action, get state
- **`GameWebSocketHandler`** — broadcasts `GameEvent` to all WebSocket subscribers in the room;
  delivers `HandUpdated` only to the owning seat's session

---

## Data Flow (AI agent integration)

```
Human / AI agent
      │
      │  POST /rooms/{id}/actions  (GameAction as JSON)
      ▼
RoomController → GameSession.submitAction(actorSeatIndex, action)
      │
      │  (GameEvent) → onEvent callback
      ▼
GameWebSocketHandler → broadcast to all WS subscribers
      │
      ├──► Human client (CLI)
      ├──► AI agent (GameClientService maintains local state)
      └──► GameLogger (writes JSONL for training)
```

The AI agent is just another player client — it submits actions via the same REST endpoint
and receives events over the same WebSocket. `GameSession` does not know or care whether
the actor is human or AI.

---

## Test Cases (TDD Checklist)

One test at a time. Write test → confirm it fails → implement → confirm it passes → check it off.
Unit tests (Steps 1–4) are pure Kotlin, no Spring context. Step 5 uses Spring Boot Test.

---

### Step 1 — Start a game room

**`GameRoomFactoryTest`**
- [ ] factory creates a room with 2 seats
- [ ] each seat has 3 blank heroes
- [ ] each hero has 4 HP
- [ ] seats start at full HP
- [ ] card pool contains only ATTACK and DODGE
- [ ] seats start in Unknown allegiance before game start
- [ ] seat indices match list order
- [ ] seats start with empty hand
- [ ] factory rejects fewer than 2 players
- [ ] factory rejects more than 2 players

**`GameSessionStartTest`**
- [ ] start assigns seat 0 as LORD
- [ ] start assigns seat 1 as SPY
- [ ] start deals 4 cards to each seat
- [ ] start emits GameStarted as first event with firstSeatIndex=0
- [ ] GameStarted seatViews reflect correct HP and hand count
- [ ] start emits HandUpdated for each seat with 4 cards
- [ ] start enters Judge phase for seat 0
- [ ] start cannot be called twice

---

### Step 2 — Deal / Draw cards

**`GameSessionDrawTest`**
- [ ] Judge phase auto-advances when judgment area is empty
- [ ] Draw phase deals 2 cards to the active seat
- [ ] deck size decreases by 2 after draw
- [ ] CardsDrawn event emitted with correct seatIndex and count
- [ ] HandUpdated emitted with hand grown from 4 to 6
- [ ] after Draw phase, current phase is Play

---

### Step 3 — Play cards

**`GameSessionPlayTest`** — attack
- [ ] PlayAttack with ATTACK card emits AttackPlayed and ResponseRequested
- [ ] pendingRequest is set after PlayAttack
- [ ] PlayAttack with a non-ATTACK card returns an error
- [ ] PlayAttack submitted by the non-active seat returns an error
- [ ] PlayAttack submitted while pendingRequest is already set returns an error

**`GameSessionPlayTest`** — response window
- [ ] RespondWithDodge from the target emits DodgePlayed and clears pendingRequest
- [ ] RespondWithDodge with a non-DODGE card returns an error
- [ ] RespondWithDodge submitted by the wrong seat returns an error
- [ ] Pass from the target emits DamageDealt with amount=1 and correct newHp
- [ ] Pass clears pendingRequest

**`GameSessionPlayTest`** — end turn
- [ ] EndPlayPhase while pendingRequest is set returns an error
- [ ] EndPlayPhase from the non-active seat returns an error
- [ ] EndPlayPhase advances phase to Discard
- [ ] Discard phase auto-discards excess cards down to hand limit (= current HP)
- [ ] End phase starts the next seat's turn at Judge phase

---

### Step 4 — Game over

**`GameSessionDeathTest`**
- [ ] when HP drops to 0 and remaining heroes exist, HeroRotated is emitted
- [ ] rotated hero enters with full HP
- [ ] GameSession draws heroEntryDrawCount entry cards after rotation
- [ ] HandUpdated is emitted after hero rotation
- [ ] when HP drops to 0 and no remaining heroes, seat heroes becomes empty
- [ ] checkWinCondition identifies the correct winner after elimination
- [ ] GameOver is emitted with the correct winnerSeatIndex
- [ ] isOver is true after game ends
- [ ] submitAction after game over returns an error

---

### Step 5 — REST + WebSocket + AI agent seam test

**`RoomControllerTest`** (Spring Boot Test)
- [ ] POST /rooms returns 200 with roomId
- [ ] POST /rooms/{id}/join returns 200 with seatIndex
- [ ] POST /rooms/{id}/start returns 200
- [ ] POST /rooms/{id}/actions with valid action returns 200
- [ ] POST /rooms/{id}/actions with illegal action returns 400 with error message
- [ ] GET /rooms/{id}/state returns 200 with current GameStateView

**`GameWebSocketTest`** (Spring Boot Test)
- [ ] WebSocket subscriber receives GameStarted after game starts
- [ ] WebSocket subscriber receives GameOver when game ends

**`GameSessionSeamTest`** (scripted MockChatModel — acceptance gate)
- [ ] full AI vs AI game completes from start to GameOver without any illegal action errors
- [ ] GameLogger produces a non-empty JSONL file after the game

---

## File Map

### Modify

| File                          | Change                                                              |
|-------------------------------|---------------------------------------------------------------------|
| `model/seat/Seat.kt`          | Remove `require(heroes.isNotEmpty())`                               |
| `model/seat/Allegiance.kt`    | Add `Unknown` subtype                                               |
| `game/mode/OneVsOneMode.kt`   | `onSeatDeath` returns `Seat`; empty heroes list on last death       |

### Create (game server)

| File                              | Purpose                                  |
|-----------------------------------|------------------------------------------|
| `model/action/GameAction.kt`      | Player intent protocol                   |
| `model/action/PendingRequest.kt`  | Mid-turn interrupt state                 |
| `model/action/GameEvent.kt`       | Server-to-client events                  |
| `model/action/SeatView.kt`        | Public seat snapshot                     |
| `model/game/GameMode.kt`          | Pure strategy interface                  |
| `game/room/GameRoom.kt`           | Pure data aggregate (no deck, no engine) |
| `game/session/GameSession.kt`     | Game orchestrator                        |
| `game/factory/GameRoomFactory.kt` | Minimal 1v1 setup builder                |
| `game/log/GameLogger.kt`          | JSONL game log for training              |
| `api/RoomStore.kt`                | In-memory room registry                  |
| `api/RoomController.kt`           | REST endpoints                           |
| `api/GameWebSocketHandler.kt`     | WS event broadcast                       |

### Create (ai-agent submodule)

| File                        | Purpose                                       |
|-----------------------------|-----------------------------------------------|
| `ai-agent/build.gradle.kts` | Submodule build (Spring AI + Ollama)          |
| `AiAgentApplication.kt`     | Spring Boot entry point                       |
| `GameClientService.kt`      | WS + REST client; maintains local game state  |
| `GameTools.kt`              | Spring AI `@Tool` functions                   |
| `AiDecisionService.kt`      | ChatClient loop: state → prompt → action      |