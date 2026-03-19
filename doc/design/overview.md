# Domain Design — Overview

## Architecture

The domain model is built around the **Strategy pattern** for game modes.
Core models (`Card`, `Hero`, `Seat`, `Deck`, `GamePhase`, `GameRoom`) are shared across all modes.
Mode-specific logic (win conditions, role assignment, allegiance) is encapsulated in `GameMode` implementations.

See sibling files for details:
- [`models.md`](models.md) — data model definitions
- [`turn-engine.md`](turn-engine.md) — turn phase state machine and skill hooks
- [`game-room.md`](game-room.md) — GameMode strategy interface and GameRoom aggregate

---

## Dependency Graph

```
Card
 ├──▶ Hero          (hero carries equipment cards and special area cards)
 │      └──▶ Seat   (seat holds a list of heroes)
 │             └──▶ GameRoom  (room holds a list of seats)
 └──▶ IDeck         (deck manages draw / discard / reveal zones)
        └──▶ GameRoom

GamePhase ──▶ TurnStateMachine ──▶ GameRoom
GameMode  ──▶ GameRoom
```

---

## Design Decisions

| Decision                  | Choice                            | Reason                                                                              |
|---------------------------|-----------------------------------|-------------------------------------------------------------------------------------|
| Mode extensibility        | Strategy pattern (`GameMode`)     | New mode = new class; no changes to core models                                     |
| Player representation     | `Seat` + `Hero` split             | Seat owns distance/turns; Hero owns HP definition/skills/equipment                  |
| Dual-hero support         | `heroes: List<Hero>` on Seat      | Non-empty list; standard modes have one hero, dual-hero (Kingdom) has two           |
| Mode-specific allegiance  | Sealed `Allegiance`               | Replaces nullable role/kingdom; type system prevents cross-mode field leakage       |
| HP representation         | `HpValue` (half-units) + `HpState`| Heroes may have X.5 HP in Kingdom mode; combined integer HP lives on Seat          |
| Team info                 | `TeamBased(teamId: String)`       | Opaque ID for now; typed `Team` added later without structural change               |
| Phase skip mechanism      | `TurnContext.skippedPhases`       | Unified for both delayed tricks and skill-based skips; no special cases             |
| Skill phase observation   | `PhaseHook` (data-driven)         | Engine indexes by `(phase, timing, seatScope)`; only relevant hooks are invoked     |
| Hook invocation patterns  | `BROADCAST` / `CHAIN`             | Broadcast for independent reactions; Chain for interceptable payloads (e.g. 鬼才)   |
| Deck                      | `IDeck` interface + `StandardDeck`| Interface in model layer; implementation in game layer; testable via constructor   |
| Game state                | In-memory                         | No persistence at this stage                                                        |