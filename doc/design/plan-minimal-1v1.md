# Plan — Minimal 1v1 + AI Agent (First Playable Slice)

## Goals

1. Tiny deck — ATTACK + DODGE cards only (subset of `StandardCards`)
2. Blank heroes — 4 HP, no skills
3. Runnable game server (REST + WebSocket)
4. AI agent that can connect and play (Spring AI + Ollama)
5. Game log collection for self-supervised training

No real card skills, no hero draft, no equipment.
Hand limit = current HP count (standard rule).
Draw = 2 cards per turn.

---

## Bug Fix Required First

### `Seat.init` vs `OneVsOneMode.checkWinCondition` contradiction

`Seat.init` has `require(heroes.isNotEmpty())`.
`OneVsOneMode.checkWinCondition` checks `seat.heroes.isEmpty()`.
`OneVsOneMode.onSeatDeath` returns early when `remaining.isEmpty()` — never empties the heroes list.
Result: win condition never triggers for the last hero.

**Fix (two changes):**
- `model/seat/Seat.kt`: remove `require(heroes.isNotEmpty())`; document that empty means eliminated
- `game/mode/OneVsOneMode.kt`: when `remaining.isEmpty()`, still call `room.updateSeat(dead.copy(heroes = emptyList()))` before returning

---

## Phase Flow (GameEngine responsibility)

```
Judge  → auto-advance (no judgments in minimal slice)
Draw   → auto-draw 2, emit CardsDrawn + HandUpdated, auto-advance
Play   → wait for player: PlayAttack | EndPlayPhase
           PlayAttack → set pendingRequest, emit ResponseRequested (target must Respond)
           RespondWithDodge → clear pendingRequest, emit DodgePlayed (play continues)
           Pass → clear pendingRequest, apply damage, emit DamageDealt
                  if hp.isDying → onSeatDeath → checkWinCondition
Discard → auto-discard excess to hand limit (= current HP), auto-advance
End    → tape exhausts → startTurn(nextSeat) → advance into next seat's Judge
```

---

## Files to Create / Modify

### 1. ~~`model/card/MinimalCards.kt`~~ → inline in `GameRoomFactory`

`MinimalCards` is not a domain model — it is a temporary card pool that grows step by step
until it equals `StandardCards`, at which point it is simply replaced.
Keep it private to `GameRoomFactory`:

```kotlin
// Inside GameRoomFactory — grows as card effects are implemented
private val currentCardPool: List<Card> =
    StandardCards.filter { it.type == CardType.ATTACK || it.type == CardType.DODGE }
```

When more card types are implemented, add them here.
When it equals `StandardCards`, replace with `StandardCards` directly and remove the filter.

### 2. ~~`model/hero/BlankHero.kt`~~ → inline in `GameRoomFactory`

`BlankHero` is a bootstrap stand-in, not a domain type.
Keep it private to `GameRoomFactory`:

```kotlin
// Inside GameRoomFactory
private val BLANK_HP = HpValue.of(4)

private fun blankHero(id: HeroId) = Hero(
    heroId   = id,
    gender   = Gender.MALE,
    maxHp    = BLANK_HP,
    skills   = emptyList(),
)
```

When real heroes are implemented, replace via `GameRoomFactory` directly — no model changes needed.

### 3. `model/action/GameAction.kt` (NEW)

```kotlin
sealed class GameAction {
    /** Play an ATTACK from handCards[handIndex] targeting opponent seat. */
    data class PlayAttack(val handIndex: Int, val targetSeatIndex: Int) : GameAction()

    /** Respond to an incoming ATTACK with a DODGE from handCards[handIndex]. */
    data class RespondWithDodge(val handIndex: Int) : GameAction()

    /** Decline to dodge — take the damage. */
    data object Pass : GameAction()

    /** End your Play phase (no more attacks this turn). */
    data object EndPlayPhase : GameAction()
}
```

### 4. `model/action/PendingRequest.kt` (NEW)

```kotlin
/** Interrupt state between two players during a turn. */
sealed class PendingRequest {
    /**
     * Target must respond with [RespondWithDodge] or [Pass].
     * Only [targetSeatIndex] may act while this is set.
     */
    data class RespondToAttack(
        val attackerSeatIndex: Int,
        val targetSeatIndex: Int,
        val attackCard: Card,
    ) : PendingRequest()
}
```

### 5. `model/action/GameEvent.kt` (NEW)

Server-to-client events. All events are broadcast to all players in the room.
`HandUpdated` carries the full hand and is delivered only to the owning seat (filtering is the API layer's job).

```kotlin
sealed class GameEvent {
    data class GameStarted(
        val seats: List<SeatView>,
        val firstSeatIndex: Int,
    ) : GameEvent()

    data class PhaseChanged(
        val seatIndex: Int,
        val phase: GamePhase,
    ) : GameEvent()

    /** Count only — players learn their own draws via HandUpdated. */
    data class CardsDrawn(val seatIndex: Int, val count: Int) : GameEvent()

    /** Private: full hand after draw or discard. API layer sends only to owner. */
    data class HandUpdated(val seatIndex: Int, val cards: List<Card>) : GameEvent()

    data class AttackPlayed(
        val attackerSeatIndex: Int,
        val card: Card,
        val targetSeatIndex: Int,
    ) : GameEvent()

    /** Target must reply with RespondWithDodge or Pass. */
    data class ResponseRequested(
        val targetSeatIndex: Int,
        val attackCard: Card,
    ) : GameEvent()

    data class DodgePlayed(val seatIndex: Int, val card: Card) : GameEvent()

    data class DamageDealt(
        val targetSeatIndex: Int,
        val amount: Int,
        val newHp: Int,
    ) : GameEvent()

    data class HeroRotated(
        val seatIndex: Int,
        val heroId: String,
        val newMaxHp: Int,
        val newHp: Int,
    ) : GameEvent()

    data class GameOver(val winnerSeatIndex: Int) : GameEvent()
}
```

`SeatView` (public state snapshot — opaque hand size for opponents):

```kotlin
data class SeatView(
    val seatIndex: Int,
    val heroId: String,
    val hp: Int,
    val maxHp: Int,
    val handCount: Int,          // always visible
    val cards: List<Card>?,      // non-null only for the seat's own view
)
```

### 6. `game/engine/GameEngine.kt` (NEW)

Wraps `GameRoom`. Processes `GameAction`, drives `TurnEngine`, emits `GameEvent`.

```kotlin
class GameEngine(
    private val room: GameRoom,
    private val onEvent: (GameEvent) -> Unit,
) {
    var pendingRequest: PendingRequest? = null
        private set

    private var winner: Winner? = null

    val isOver: Boolean get() = winner != null

    /** Start the game. Assigns allegiances, deals opening hands, enters first phase. */
    fun start()

    /**
     * Submit an action.
     * Returns an error string if the action is illegal, null on success.
     */
    fun submitAction(actorSeatIndex: Int, action: GameAction): String?

    private fun enterCurrentPhase()
    private fun applyDamage(targetSeatIndex: Int, amount: Int)
    private fun advanceToNextPhase()
}
```

**Validation rules inside `submitAction`:**
- If `pendingRequest != null`: only `targetSeatIndex` may act; only `RespondWithDodge` / `Pass` are legal
- If `pendingRequest == null`: only the current active seat may act; only `PlayAttack` / `EndPlayPhase` are legal in Play phase
- Actions submitted in any other phase are rejected (Draw/Discard/End/Judge are auto-handled)
- `PlayAttack.handIndex` must be in bounds and point to an ATTACK card
- `RespondWithDodge.handIndex` must be in bounds and point to a DODGE card

### 7. `game/factory/GameRoomFactory.kt` (NEW)

Builds a ready-to-use `GameRoom` for minimal 1v1.

```kotlin
object GameRoomFactory {

    private val BLANK_HERO_IDS = listOf(HeroId("blank_0"), HeroId("blank_1"), HeroId("blank_2"))
    private const val OPENING_HAND_SIZE = 4

    /**
     * Create a 1v1 GameRoom with minimal deck and blank heroes.
     * Each seat gets a rotation queue of 3 blank heroes (as per 1v1 rules).
     */
    fun createMinimal1v1(roomId: String, playerIds: List<String>): GameRoom {
        require(playerIds.size == 2)
        val deck = StandardDeck.shuffled(MinimalCards)
        val mode = OneVsOneMode()
        val engine = TurnEngine(seatCount = 2)

        val seats = playerIds.mapIndexed { index, playerId ->
            val heroes = BLANK_HERO_IDS.map { blankHero(it) }
            Seat(
                id           = playerId,
                seatIndex    = index,
                handCards    = emptyList(),        // dealt by GameEngine.start()
                judgmentArea = emptyList(),
                heroes       = heroes,
                hp           = HpState.full(heroes.first().maxHp),
                allegiance   = Allegiance.Unknown, // assigned by mode.assignAllegiances in start()
            )
        }
        return GameRoom(id = roomId, deck = deck, mode = mode, engine = engine, seats = seats)
    }
}
```

Note: `Allegiance.Unknown` is a placeholder — `mode.assignAllegiances` replaces it in `GameRoom.start()`.
The opening hand draw happens in `GameEngine.start()` after `room.start()`.

### 8. `model/seat/Allegiance.kt` (MODIFY)

Add `Unknown` subtype as a pre-start placeholder:

```kotlin
sealed class Allegiance {
    object Unknown : Allegiance()          // pre-start placeholder
    data class RoleBased(val role: Role) : Allegiance()
    // ... existing subtypes
}
```

### 9. `game/log/GameLogger.kt` (NEW)

Records every `(state, action)` pair as JSONL for training data.

```kotlin
class GameLogger(private val writer: BufferedWriter) {

    data class LogEntry(
        val roomId: String,
        val turn: Int,
        val seatIndex: Int,
        val phase: String,
        val state: GameStateSnapshot,
        val action: GameAction,
    )

    fun log(entry: LogEntry) {
        writer.write(Json.encodeToString(entry))
        writer.newLine()
    }
}
```

`GameStateSnapshot` — compact representation for training:
- `seats`: list of `{hp, maxHp, handCount, heroId}`
- `myHand`: list of card type strings (attacker/responder's own hand)
- `currentSeat`: index
- `phase`: phase name

Log file: `logs/game-<roomId>-<timestamp>.jsonl`

---

## API Layer

### `api/RoomStore.kt` (NEW)

```kotlin
@Component
class RoomStore {
    private val rooms = ConcurrentHashMap<String, GameEngine>()

    fun create(engine: GameEngine): String   // returns roomId
    fun get(roomId: String): GameEngine?
    fun all(): List<GameEngine>
}
```

### `api/RoomController.kt` (NEW)

```
POST /rooms                     — create room; returns {roomId}
POST /rooms/{id}/join           — body: {playerId}; returns seat index
POST /rooms/{id}/start          — starts game if 2 players joined
POST /rooms/{id}/actions        — body: {playerId, action}; returns 400 on illegal action
GET  /rooms/{id}/state          — returns current GameStateSnapshot (for reconnect)
```

### `api/GameWebSocketHandler.kt` (NEW)

WebSocket endpoint: `/ws/game/{roomId}`

- On connect: register session under roomId
- On `GameEvent`:
  - Broadcast all non-private events to all sessions in room
  - Send `HandUpdated` only to the session matching `seatIndex`
- On disconnect: unregister session

Uses Spring `WebSocketHandler` (not STOMP — simpler for CLI client).

### `api/dto/` (NEW)

```
ActionRequest.kt     — {playerId: String, action: GameAction}
RoomResponse.kt      — {roomId: String}
JoinResponse.kt      — {seatIndex: Int}
GameStateView.kt     — {seats: List<SeatView>, currentSeatIndex, currentPhase, pendingRequest?}
```

---

## AI Agent (`ai-agent/` submodule)

### Build

`ai-agent/build.gradle.kts` — depends on `spring-ai-starter-model-ollama`, `spring-boot-starter-websocket`, `spring-boot-starter-web`.

`settings.gradle.kts` — add `include("ai-agent")`.

### Files

```
ai-agent/src/main/kotlin/org/dogcard/agent/
  AiAgentApplication.kt       — Spring Boot entry point
  GameClientService.kt        — WS + REST client; maintains local GameStateView
  GameTools.kt                — @Tool functions: playAttack, dodge, pass, endPlayPhase
  AiDecisionService.kt        — ChatClient loop: state → prompt → LLM → action
  AgentConfig.kt              — Spring AI config (ChatClient bean, model selection)
```

### Prompt design

```
You are playing Sanguosha 1v1. It is your turn.

State:
  Your HP: {hp}/{maxHp}
  Your hand: {cards}
  Opponent HP: {hp}/{maxHp}, hand count: {n}

Legal actions:
  {actions}

Reply with exactly one action name and parameters.
```

For response window (opponent defending):
```
You are defending. An ATTACK ({card}) was played against you.
Your hand: {cards}
Legal actions: DODGE (hand indices {indices}), PASS
```

### Self-supervised learning data flow

```
GameEngine → GameLogger → logs/game-*.jsonl
                                    ↓
                          model-training/
                          parse_logs.py   → HuggingFace Dataset
                          train.py        → mini transformer
                          serve via Ollama ← trained .gguf
```

---

## File Summary

### Modify
| File | Change |
|------|--------|
| `model/seat/Seat.kt` | Remove `require(heroes.isNotEmpty())` |
| `model/seat/Allegiance.kt` | Add `Unknown` subtype |
| `game/mode/OneVsOneMode.kt` | Update seat even when `remaining.isEmpty()` |
| `settings.gradle.kts` | `include("ai-agent")` |

### Create (game server)
| File | Purpose |
|------|---------|
| `model/action/GameAction.kt` | Player intent protocol |
| `model/action/PendingRequest.kt` | Mid-turn interrupt state |
| `model/action/GameEvent.kt` | Server-to-client events + SeatView |
| `game/engine/GameEngine.kt` | Action processor + phase driver |
| `game/factory/GameRoomFactory.kt` | Minimal 1v1 room builder |
| `game/log/GameLogger.kt` | JSONL game log for training |
| `api/RoomStore.kt` | In-memory room registry |
| `api/RoomController.kt` | REST endpoints |
| `api/GameWebSocketHandler.kt` | WS event broadcast |
| `api/dto/ActionRequest.kt` | REST request DTO |
| `api/dto/RoomResponse.kt` | REST response DTOs |
| `api/dto/GameStateView.kt` | State snapshot DTO |

### Create (ai-agent submodule)
| File | Purpose |
|------|---------|
| `ai-agent/build.gradle.kts` | Submodule build |
| `ai-agent/.../AiAgentApplication.kt` | Spring Boot entry |
| `ai-agent/.../GameClientService.kt` | WS + REST client |
| `ai-agent/.../GameTools.kt` | Spring AI @Tool functions |
| `ai-agent/.../AiDecisionService.kt` | ChatClient loop |
| `ai-agent/.../AgentConfig.kt` | Spring AI config |

---

## Server-Side TDD Flow

Each step: **write tests first, then implement**.
All unit tests are pure Kotlin (no Spring context) — fast and isolated.
The AI agent integration test appears at the end of Step 5 as the final acceptance gate.

---

### Step 1 — Start a game room

**What:** create a room, assign allegiances, deal opening hands, enter first phase.

**Tests (`GameRoomFactoryTest`, `GameEngineStartTest`):**
- `GameRoomFactory` creates 2 seats, each with 3 blank heroes (4 HP each)
- Card pool contains only ATTACK and DODGE
- `GameEngine.start()` assigns seat 0 = LORD, seat 1 = SPY
- `GameEngine.start()` deals 4 cards to each seat
- Emits `GameStarted` with correct `firstSeatIndex = 0`
- Emits two `HandUpdated` events (one per seat)
- After start, `engine.current.phase == GamePhase.Judge` for seat 0

**Files to create (after tests pass):**
- bug fix: `model/seat/Seat.kt`, `game/mode/OneVsOneMode.kt`
- `model/seat/Allegiance.kt` — add `Unknown` subtype
- `model/action/GameAction.kt`, `model/action/GameEvent.kt`, `model/action/PendingRequest.kt`
- `game/factory/GameRoomFactory.kt`
- `game/engine/GameEngine.kt` (just `start()` and event emission)

---

### Step 2 — Deal / Draw cards

**What:** auto-handle Judge and Draw phases; auto-advance to Play.

**Tests (`GameEngineDrawTest`):**
- Judge phase auto-advances when there are no judgments in the judgment area
- Draw phase: active seat draws 2 cards; deck size decreases by 2
- `CardsDrawn(seatIndex=0, count=2)` event emitted
- `HandUpdated(seatIndex=0, cards=[...])` event emitted (hand grows from 4 to 6)
- After Draw phase, current phase = Play for seat 0

---

### Step 3 — Play cards

**What:** the Play phase interaction loop — attack, response, and end-turn.

**Tests (`GameEnginePlayTest`):**

*Attack:*
- `PlayAttack(handIndex, target=1)` with an ATTACK card →
  emits `AttackPlayed` + `ResponseRequested`; `pendingRequest` is set
- `PlayAttack` with a DODGE card → returns error (not an ATTACK)
- `PlayAttack` from seat 1 while seat 0 is active → returns error (not your turn)
- `PlayAttack` submitted while `pendingRequest != null` → returns error (resolve first)

*Response window — only target may act:*
- `RespondWithDodge(handIndex)` from seat 1 → emits `DodgePlayed`; `pendingRequest` cleared
- `RespondWithDodge` with a non-DODGE card → returns error
- `RespondWithDodge` submitted by seat 0 (wrong player) → returns error
- `Pass` from seat 1 → emits `DamageDealt(targetSeatIndex=1, amount=1, newHp=3)`; HP decremented

*End turn:*
- `EndPlayPhase` while `pendingRequest != null` → returns error (must resolve first)
- `EndPlayPhase` from non-active seat → returns error
- `EndPlayPhase` → current phase advances to Discard
- Discard phase: auto-discards excess cards down to hand limit (= current HP); advances to End
- End phase: tape exhausts; `startTurn(1)` begins; current phase = Judge for seat 1

---

### Step 4 — Game over

**What:** hero rotation on death; win condition; game-over event.

**Tests (`GameEngineDeathTest`):**
- Damage reduces HP to 0, seat has remaining heroes →
  `HeroRotated` emitted; new hero has full HP; seat draws 4 cards; `HandUpdated` emitted
- Damage reduces HP to 0, no remaining heroes →
  seat's `heroes` becomes `emptyList()` (eliminated)
- `checkWinCondition` returns `Winner(0)` when seat 1 is eliminated
- `GameOver(winnerSeatIndex=0)` event emitted
- `isOver == true` after game ends
- `submitAction` after game over → returns error ("game is already over")

---

### Step 5 — REST + WebSocket + AI agent integration

**What:** expose `GameEngine` over HTTP/WS; verify AI agent can drive a full game.

**Tests (`RoomControllerTest`, `GameWebSocketTest` — Spring Boot Test):**
- `POST /rooms` → 200, body `{roomId}`
- `POST /rooms/{id}/join` (×2 players) → 200, body `{seatIndex}`
- `POST /rooms/{id}/start` → 200; `GameStarted` broadcast over WS
- `POST /rooms/{id}/actions` with valid action → 200
- `POST /rooms/{id}/actions` with illegal action → 400 + error message
- `GET /rooms/{id}/state` → 200 + `GameStateView`
- WS client receives `GameOver` when game finishes

**AI agent seam test (`AiAgentSeamTest` — scripted `MockChatModel`):**
- Two `MockChatModel` instances produce valid pre-scripted action sequences
- Full game completes (start → turns → GameOver) without any illegal action errors
- `GameLogger` JSONL file is non-empty after the game
- This test is the acceptance gate for the full server + AI wiring

**Files to create:**
- `game/log/GameLogger.kt`
- `api/RoomStore.kt`, `api/RoomController.kt`, `api/GameWebSocketHandler.kt`
- `api/dto/ActionRequest.kt`, `api/dto/RoomResponse.kt`, `api/dto/GameStateView.kt`