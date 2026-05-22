# Game Events

All events emitted by `GameSession` via the `onEvent` callback.
Consumers: API layer (WebSocket broadcast), GameLogger (JSONL training data), AI agent (local state sync).

---

### `GameStarted`
Emitted once at the start of `GameSession.start()`.

| Field            | Type             | Description                        |
|------------------|------------------|------------------------------------|
| `firstSeatIndex` | `Int`            | Seat index of the SPY (goes first) |
| `seatViews`      | `List<SeatView>` | Public snapshot of all seats       |

### `HandUpdated`
Emitted after any change to a seat's hand cards (initial deal, draw, discard).
Carries the full private hand — delivered only to the owning seat's connection.

| Field       | Type         | Description             |
|-------------|--------------|-------------------------|
| `seatIndex` | `Int`        | Owner of the hand       |
| `cards`     | `List<Card>` | Complete current hand   |

---

### `PhaseChanged`
Emitted each time the active phase or active seat changes.

| Field            | Type        | Description                    |
|------------------|-------------|--------------------------------|
| `seatIndex`      | `Int`       | Seat whose turn it is          |
| `phase`          | `GamePhase` | The new current phase          |

### `CardsDrawn`
Emitted after a seat draws cards during the Draw phase.

| Field       | Type  | Description              |
|-------------|-------|--------------------------|
| `seatIndex` | `Int` | Seat that drew           |
| `count`     | `Int` | Number of cards drawn    |

### `AttackPlayed`
Emitted when a seat plays an ATTACK card targeting another seat.

| Field             | Type   | Description              |
|-------------------|--------|--------------------------|
| `attackerIndex`   | `Int`  | Seat that played ATTACK  |
| `targetIndex`     | `Int`  | Seat being targeted      |

### `ResponseRequested`
Emitted immediately after `AttackPlayed` to open the response window.
Only the target seat may act while this is pending.

| Field           | Type   | Description                        |
|-----------------|--------|------------------------------------|
| `attackerIndex` | `Int`  | Seat that played the ATTACK        |
| `targetIndex`   | `Int`  | Seat that must respond             |

### `DodgePlayed`
Emitted when the target responds with a DODGE card.

| Field       | Type  | Description              |
|-------------|-------|--------------------------|
| `seatIndex` | `Int` | Seat that played DODGE   |

### `DamageDealt`
Emitted when a seat takes damage (target passed or has no DODGE).

| Field       | Type  | Description                  |
|-------------|-------|------------------------------|
| `seatIndex` | `Int` | Seat that took damage        |
| `amount`    | `Int` | HP lost                      |
| `newHp`     | `Int` | Remaining HP after damage    |

### `HeroRotated`
Emitted when a seat's active hero dies and the next hero in the queue enters.

| Field       | Type     | Description                        |
|-------------|----------|------------------------------------|
| `seatIndex` | `Int`    | Seat whose hero rotated            |
| `heroId`    | `HeroId` | The new hero now on field          |
| `newHp`     | `Int`    | Full HP of the new hero            |

### `GameOver`
Emitted when a win condition is met.

| Field               | Type  | Description                  |
|---------------------|-------|------------------------------|
| `winnerSeatIndex`   | `Int` | Seat that won                |