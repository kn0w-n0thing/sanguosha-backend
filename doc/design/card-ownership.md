# Card Ownership

## Problem

`Card` is a plain data class — it can be copied freely. Nothing prevents the same card
instance from appearing in two zones at once (e.g. still in a hand after being played), or
from disappearing entirely (discarded without being tracked). Both bugs are silent.

## Design Goal

Every card instance belongs to **exactly one zone** at any point in time.
Moving a card = an atomic remove-from-source + add-to-dest.
Kotlin has no compile-time ownership, so this is enforced at runtime.

Players do not manipulate zones directly. They call semantic methods on the deck; the deck executes the underlying zone transfers.

---

## Finalized Design

### Zone identity — `CardZoneType`

A sealed class enumerates every zone in the game. Seat-scoped zones (`Hand`, `Judgment`, `Equipment`) carry a `seatIndex`.

### Zone abstraction — `ICardZone`

The public interface exposes only read and transfer operations. `add` / `remove` are not
exposed — callers cannot insert or delete cards without going through a transfer.

### Player action facade — `PlayerAction`

A thin facade created by `IDeck.playerAction(seatIndex)`. Holds `IDeck` + `seatIndex`;
provides the readable action surface for a seat. `GameSession` never calls zone transfers
directly for these flows — it calls `PlayerAction` methods instead.

`respond(pendingCard)` returns a `ResponseBuilder`. `pendingCard` is the in-flight card
being responded to — it may be an ATTACK, DUEL, AOE, or any other card that requires a
response. See the class graph for `with(card)` / `pass()` semantics.

Hand zone (`Seat.handZone: ICardZone`) is a **read-only view** — it exposes what cards
are in hand but has no action methods.

### Concrete zone — `CardZone` (package `model/deck`)

Backed by a `MutableList` (ordered). Only `DrawPile` zones accept initial cards (enforced
by constructor). All other zones must start empty.

### Deck owns all zones — `IDeck`

The deck implementation creates and holds every zone for the session.

`draw(n, seatIndex)` encapsulates: ensure availability (reshuffle if needed) → transfer
top N cards from `DrawPile` → `Hand(seatIndex)` → return the transferred cards (for event emission).

### Seat holds a hand zone reference

`Seat.handZone: ICardZone` is a read-only reference to the `Hand(seatIndex)` zone owned
by the deck — for reading hand cards only. `GameSession` performs card transfers directly
via `deck.zone()`.

### `GameSession` gets `inFlightZone` from the deck

`inFlightZone` is `deck.zone(CardZoneType.InFlight)` — still conceptually on `GameSession`
but backed by the deck's zone registry.

---

## Component Graph

The deck creates and owns all zones. Seats and `GameSession` hold references.

```
┌──────────────────────────────────────────────────────────────┐
│                         GameSession                           │
│                                                               │
│  inFlightZone: ICardZone  ◄── deck.zone(InFlight)            │
│                                                               │
│  ┌───────────────────────┐  ┌─────────────────────────────┐  │
│  │        Seat 0         │  │           Seat 1            │  │
│  │  handZone: ICardZone  │  │  handZone: ICardZone        │  │
│  │  (read-only view)     │  │  (read-only view)           │  │
│  └───────────────────────┘  └─────────────────────────────┘  │
│                                                               │
│  ┌───────────────────────────────────────────────────────┐   │
│  │                        IDeck                          │   │
│  │  DrawPile    ── ordered, private                      │   │
│  │  DiscardPile ── exposed via zone(DiscardPile)          │   │
│  │  InFlight    ── exposed via zone(InFlight)            │   │
│  │  Hand(i)     ── one per seat (ICardZone)              │   │
│  │  Judgment(i), Equipment(i) ── future                  │   │
│  └───────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

## Card flows

Players call semantic methods; the deck executes the zone transfers.

```
draw phase:    deck.draw(n, seatIndex)
play attack:   deck.zone(Hand(i)).transfer(card, deck.zone(InFlight))
respond dodge: deck.zone(Hand(i)).transfer(dodge, deck.zone(DiscardPile))
               deck.zone(InFlight).transfer(pending, deck.zone(DiscardPile))
pass:          deck.zone(InFlight).transfer(pending, deck.zone(DiscardPile))
discard phase: deck.zone(Hand(i)).transferAll(cards, deck.zone(DiscardPile))
reshuffle:     deck.reshuffle()
```

---

## Class Graph

See [`graph/card-ownership.puml`](graph/card-ownership.puml).

---

## TODO List

Items are listed in implementation order. Tests follow immediately after the file they cover.
Interfaces and pure sealed classes have no tests.

---

- [x] `model/deck/CardZoneType.kt` — new; sealed class with all zone types *(no tests)*

---

- [x] `model/card/ICardZone.kt` → `model/deck/ICardZone.kt` — move + package rename only *(no tests)*

---

- [x] `model/deck/CardZoneTest.kt` — write tests first; `model/deck/CardZone.kt` does not exist yet (RED)
  - [x] `toList returns empty list for a new zone`
  - [x] `toList returns cards passed to the DrawPile zone constructor`
  - [x] `contains returns true for a card passed to the DrawPile zone constructor`
  - [x] `contains returns false for a card not in the zone`
  - [x] `constructor throws if a non-DrawPile zone is initialized with cards`
  - [x] `transfer moves card from source to destination` *(also verifies removal from source)*
  - [x] `transfer throws if card is not in source zone`
  - [x] `transferAll moves all listed cards to destination`
---

- [x] `model/deck/IDeck.kt` — replace pile-based API with `zone()`, `draw(n, seatIndex)`, `peek`, `reshuffle` *(no tests)*

---

- [x] `game/deck/StandardDeck.kt` — own all zones via `Map<CardZoneType, CardZone>`; accept `seatCount`
- [x] `game/deck/StandardDeckTest.kt` *(rewrite)*
  - [x] `remaining reflects the number of cards in the draw zone`
  - [x] `draw moves top N cards from draw zone to the target seat's hand zone and returns them`
  - [x] `draw reshuffles automatically when draw zone has fewer cards than requested`
  - [x] `draw takes all remaining draw pile cards then draws the rest from reshuffled discard`
  - [x] `reshuffle randomizes the discard pile order`
  - [x] `all zones are empty except the draw pile after construction`

---

- [x] `util/FakeDeck.kt` — same zone-map pattern; `discardPile` delegates to `discardZone.toList()`
  *(no dedicated test; correctness verified by `GameSessionPlayTest`)*

---

- [x] `model/seat/Seat.kt` — `handZone: ICardZone`; update import to `model/deck` *(no new tests)*
- [x] `game/factory/GameRoomFactory.kt` — create deck first; seats receive `deck.zone(Hand(i))` *(no new tests)*

---

- [x] `game/session/GameSession.kt` — use `deck.zone()` and `deck.discardZone` for all zone transfers; use `deck.draw(n, seatIndex)`; remove old pile-based calls
- [x] `GameSessionPlayTest.kt` *(update)* — swap `FakeDeck` → `FakeDeckNew` (keeps `.discardPile` accessor)
- [x] `GameSessionEventTest.kt` — swap `FakeDeck` → `FakeDeckNew`

---

- [x] Replace `model/card/CardZone.kt` — all callers now use `model/deck/CardZone`; tests pass

### Deleted after moves complete
- [x] `model/card/ICardZone.kt` — deleted as part of move
- [x] `model/card/CardZone.kt` — deleted
- [x] `model/card/CardZoneTest.kt` — deleted