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

`OneVsOneMode.onSeatDeath` currently returns the dead seat unchanged. It must rotate the hero
queue (`heroes.drop(1)`) and reset HP to the next hero's `maxHp`. When the last hero falls, it
must return a seat with `heroes = emptyList()`.

`OneVsOneMode.checkWinCondition` currently returns `null`. It must return a `Winner` pointing to
the surviving seat when the opponent's `heroes` list is empty.

An empty `heroes` list is the agreed signal for elimination — `Seat` has no guard against it.

---

## Phase Flow

Each turn for the active seat runs through these phases in order.
`GameSession` auto-handles all phases except Play, which waits for player input.

- **Judge** — auto-advance (no delayed judgments in minimal slice)
- **Draw** — deal 2 cards to the active seat, then auto-advance
- **Play** — wait for the player: `PlayAttack` or `EndPlayPhase`
  - `PlayAttack` opens a response window: the target must `RespondWithDodge` or `Pass`
  - After the response resolves, the active player may act again (but can play at most one ATTACK per turn in this slice)
  - **v1 scope: ATTACK and DODGE only.** Other card types are added incrementally after the AI agent runs successfully (see Card Expansion below)
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

- **`GameEvent`** — sealed class of server-to-client notifications.
  See [`game-events.md`](game-events.md) for full event list and field definitions.

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
- [x] factory creates a room with 2 seats
- [x] each seat has 3 blank heroes
- [x] each hero has 4 HP
- [x] seats start at full HP
- [x] card pool contains only ATTACK and DODGE
- [x] seats start in Unknown allegiance before the game starts
- [x] seat indices match list order
- [x] seats start with empty hand

**`GameSessionStartTest`**
- [x] after start, exactly one seat has LORD allegiance
- [x] after start, exactly one seat has SPY allegiance
- [x] after start, no seat has Unknown allegiance
- [x] allegiance assignment is not always the same order (Random injection, two seeds produce different results)
- [x] after start, each seat has 4 cards in hand
- [x] after start, deck size decreased by 8 (4 cards × 2 seats)
- [x] start enters Judge phase for the SPY seat
- [x] start cannot be called twice

**`GameSessionEventTest`**
- [x] start emits GameStarted as the first event with firstSeatIndex = SPY's seat index
- [x] GameStarted seatViews reflect correct HP and hand count
- [x] start emits HandUpdated for each seat with 4 cards

---

### Step 2 — Deal / Draw cards

**`GameSessionDrawTest`**
- [x] Judge phase auto-advances when judgment area is empty
- [x] Draw phase deals 2 cards to the active seat
- [x] deck size decreases by 2 after draw
- [x] after Draw phase, current phase is Play

**`GameSessionEventTest`** *(continued)*
- [x] CardsDrawn event emitted with correct seatIndex and 2 cards
- [x] CardsDrawn cards are identical to the new cards added to hand
- [x] HandUpdated emitted with hand grown from 4 to 6

---

### Step 3 — Play cards (v1: ATTACK + DODGE only)

**`GameSessionPlayTest`** — attack
- [x] pendingRequest is set after PlayAttack
- [x] PlayAttack with a non-ATTACK card returns an error
- [x] PlayAttack submitted by the non-active seat returns an error
- [x] PlayAttack submitted while pendingRequest is already set returns an error

**`GameSessionPlayTest`** — response window
- [x] RespondWithDodge clears pendingRequest
- [x] RespondWithDodge with a non-DODGE card returns an error
- [x] RespondWithDodge submitted by the wrong seat returns an error
- [x] Pass clears pendingRequest
- [x] RespondWithDodge does not reduce target HP
- [x] Pass reduces target HP by 1 by default
- [x] When pendingRequest is resolved, discard the attack card to the discard pile
- [x] RespondWithDodge discards the dodge card to the discard pile

**`GameSessionPlayTest`** — end turn
- [x] EndPlayPhase while pendingRequest is set returns an error
- [x] EndPlayPhase from the non-active seat returns an error
- [x] EndPlayPhase advances phase to Discard
- [x] Discard phase auto-discards excess cards down to hand limit (= current HP by default)
- [x] The discarded card is moved to the discard pile.
- [x] End phase starts the next seat's turn at Judge phase

**`GameSessionEventTest`** *(continued)*
- [x] PlayAttack emits AttackPlayed and ResponseRequested
- [x] RespondWithDodge emits DodgePlayed
- [x] Pass emits DamageDealt with amount=1 and correct newHp

**Refactoring**
- [ ] Use error message variables instead of raw string
- [ ] Use ownership and borrowing to make sure the cards will not be duplicated or missing
---

### Step 4 — Game over

**`GameSessionDeathTest`**
- [ ] rotated hero enters with full HP
- [ ] GameSession draws heroEntryDrawCount entry cards after rotation
- [ ] when HP drops to 0 and no remaining heroes, seat heroes becomes empty
- [ ] checkWinCondition identifies the correct winner after elimination
- [ ] isOver is true after game ends
- [ ] submitAction after game over returns an error

**`GameSessionEventTest`** *(continued)*
- [ ] HeroRotated is emitted when HP drops to 0 and remaining heroes exist
- [ ] HandUpdated is emitted after hero rotation
- [ ] GameOver is emitted with the correct winnerSeatIndex

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

### Card Expansion (after AI agent runs successfully)

Each card is added in its own increment: implement the effect, add its test, expand the card pool.
The AI agent does not need to be paused — it simply gains new legal actions as each card lands.

- [ ] **PEACH** — heal 1 HP (only playable during Play phase, only when not at full HP)
- [ ] **Weapons** — equip slot; each weapon adds a unique attack rule (e.g. range, extra effect)
- [ ] **Armor** — equip slot; each armor adds a passive defensive rule
- [ ] **Defensive horses** — +1 distance to self (harder to be targeted)
- [ ] **Offensive horses** — -1 distance to self (easier to reach targets)
Instant tricks — AOE:
- [ ] **BARBARIAN_INVASION (南蛮入侵)** — all other players must play ATTACK or take 1 damage
- [ ] **HAIL_OF_ARROWS (万箭齐发)** — all other players must play DODGE or take 1 damage
- [ ] **PEACH_GARDEN_OATH (桃园结义)** — every living player recovers 1 HP

Instant tricks — targeting:
- [ ] **DUEL (决斗)** — both players alternate playing ATTACK until one cannot; loser takes 1 damage
- [ ] **BORROW_SWORD (借刀杀人)** — force weapon-equipped target to Attack another; refuse → lose weapon
- [ ] **DISMANTLE (过河拆桥)** — discard one card from target's hand, equipment, or judgment area
- [ ] **STEAL (顺手牵羊)** — take one card from an adjacent player into your hand

Instant tricks — self / draw:
- [ ] **SOMETHING_FROM_NOTHING (无中生有)** — draw 2 cards
- [ ] **BOUNTIFUL_HARVEST (五谷丰登)** — reveal N cards from deck; each player picks one in turn order

Instant tricks — response:
- [ ] **NEGATE (无懈可击)** — cancel the effect of one trick card on one target; can chain-counter

Delayed tricks (resolved during Judge phase):
- [ ] **ECSTASY (乐不思蜀)** — skip Play phase if judgment is not ♥
- [ ] **LIGHTNING (闪电)** — deal 3 thunder damage if judgment is ♠2–9; otherwise pass to next player

- [ ] Full standard deck (replace `currentCardPool` filter with `StandardCards` directly)

---

## File Map

### Modify

| File                        | Change                                                                                                          |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------|
| `game/mode/OneVsOneMode.kt` | `onSeatDeath`: rotate hero queue; empty list when last hero falls. `checkWinCondition`: return winner when any seat's heroes empty |

### Create (game server)

| File                              | Status | Purpose                                   |
|-----------------------------------|--------|-------------------------------------------|
| `model/action/GameAction.kt`      | [x]    | Player intent protocol                    |
| `model/action/PendingRequest.kt`  | [x]    | Mid-turn interrupt state                  |
| `model/action/GameEvent.kt`       | [x]    | Server-to-client events                   |
| `model/action/SeatView.kt`        | [x]    | Public seat snapshot                      |
| `model/game/GameMode.kt`          | [x]    | Pure strategy interface                   |
| `game/room/GameRoom.kt`           | [x]    | Pure data aggregate (no deck, no engine)  |
| `game/session/GameSession.kt`     | [x]    | Game orchestrator (partial)               |
| `game/factory/GameRoomFactory.kt` | [x]    | Minimal 1v1 setup builder                 |
| `game/log/GameLogger.kt`          | [ ]    | JSONL game log for training               |
| `api/RoomStore.kt`                | [ ]    | In-memory room registry                   |
| `api/RoomController.kt`           | [ ]    | REST endpoints                            |
| `api/GameWebSocketHandler.kt`     | [ ]    | WS event broadcast                        |

### Create (ai-agent submodule)

| File                        | Status | Purpose                                      |
|-----------------------------|--------|----------------------------------------------|
| `ai-agent/build.gradle.kts` | [ ]    | Submodule build (Spring AI + Ollama)         |
| `AiAgentApplication.kt`     | [ ]    | Spring Boot entry point                      |
| `GameClientService.kt`      | [ ]    | WS + REST client; maintains local game state |
| `GameTools.kt`              | [ ]    | Spring AI `@Tool` functions                  |
| `AiDecisionService.kt`      | [ ]    | ChatClient loop: state → prompt → action     |

---

## WebSocket / REST API

### Implementation
- [ ] `api/RoomStore.kt` — in-memory `roomId → GameSession` map
- [ ] `api/RoomController.kt`
  - [ ] `POST /rooms` — create room, return `{ roomId }`
  - [ ] `POST /rooms/{id}/join` — assign next free seat, return `{ seatIndex }`
  - [ ] `POST /rooms/{id}/start` — start game, return 200
  - [ ] `POST /rooms/{id}/actions` — submit `GameAction` as JSON, return 200 or 400 with error
  - [ ] `GET /rooms/{id}/state` — return `GameStateView`
- [ ] `api/GameWebSocketHandler.kt`
  - [ ] Subscribe at `WS /ws/game/{roomId}`
  - [ ] Broadcast all `GameEvent`s to every subscriber in the room
  - [ ] Deliver `HandUpdated` only to the owning seat's connection

Tests: see Step 5.

---

## CLI Frontend

### Implementation
- [ ] Scaffold `cli-client/` Kotlin project (`build.gradle.kts`, main entry point)
- [ ] REST client — call `POST /rooms`, `/join`, `/start`, `/actions`
- [ ] WebSocket client — subscribe to `/ws/game/{roomId}`, parse `GameEvent` JSON
- [ ] 1v1 board renderer — display seats, current hero name, HP, hand card count as text
- [ ] Hand display — show own cards with index (`[0] ATTACK ♠7  [1] DODGE ♥3`)
- [ ] Input commands — `attack <seat>`, `dodge`, `pass`, `end` → post `GameAction` JSON
- [ ] Real-time update — re-render board on each received `GameEvent`
- [ ] Game result — print winner on `GameOver`

---

## AI Agent

### Implementation
- [ ] Scaffold `ai-agent/` Kotlin project (Spring Boot + Spring AI + Ollama in `build.gradle.kts`)
- [ ] `GameClientService` — WebSocket listener + REST caller; maintains a local `GameStateView`
- [ ] `GameTools` — Spring AI `@Tool` functions: `playAttack(targetSeatIndex)`, `respondWithDodge()`, `pass()`, `endPlayPhase()`
- [ ] `AiDecisionService` — ChatClient loop: serialize `GameStateView` → prompt LLM → parse tool call → submit action

### Tests
- [ ] `AiDecisionServiceTest` (MockChatModel): scripted response → verify correct `GameAction` posted to REST
- [ ] `GameToolsTest` (MockK): each `@Tool` function sends correct JSON to REST endpoint
- [ ] `AiBehaviorTest` (`@Tag("local-only")`, Ollama): AI submits only legal moves across 10 live games

---

## Model Training

### Implementation
- [ ] Scaffold `model-training/` Python project (`pyproject.toml` or `requirements.txt`)
- [ ] `parse_game_log.py` — read `GameLogger` JSONL, build `(state_tokens, action_label)` HuggingFace Dataset
- [ ] `model.py` — mini transformer architecture (~100M–350M params, decoder-only)
- [ ] `train.py` — cross-entropy training loop on next-action prediction; log to Weights & Biases
- [ ] `evaluate.py` — top-k accuracy on held-out 1v1 game logs