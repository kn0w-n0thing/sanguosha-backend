# Domain Design — Game Room

GameMode strategy interface, GameRoom aggregate, and per-mode design notes.
For mode rules, see [`../rules.md`](../rules.md).

---

## GameMode (Strategy Interface)

Encapsulates all mode-specific logic. Swapped at room creation; core models are unchanged.

```
GameMode
├── assignAllegiances(seats: List<Seat>)       — called once at game start
├── checkWinCondition(room: GameRoom): Winner? — called after every seat death and turn end
└── onSeatDeath(dead: Seat, room: GameRoom)    — kill rewards, role reveals, hero swap, etc.
```

---

## GameRoom

Top-level aggregate. Owns all game state for one running session.

```
GameRoom
├── id: String
├── seats: List<Seat>
├── deck: IDeck
├── mode: GameMode
├── turnMachine: TurnStateMachine    — drives the active seat's turn
└── currentSeatIndex: Int            — index into seats; advances counter-clockwise
```

---

## Mode Design Notes

Full rules are in [`../rules.md`](../rules.md). Notes here cover design-level differences only.

### Identity Mode (身份 · 5–10 players)
- `Allegiance.RoleBased(role)` — LORD / LOYALIST / REBEL / SPY
- Lord revealed at game start; others hidden until death
- `onSeatDeath`: reveal role, apply kill reward/penalty, check win condition

### Kingdom Mode (国战 · 4–10 players)
- `Allegiance.Unrevealed` until first general revealed (明置) → `Allegiance.KingdomBased(kingdom)`
- `HpState.max` = combined `HpValue` of both heroes (main + sub general)
- `onSeatDeath`: reveal all generals, discard cards, check win condition

### 1v1 Mode (竞技 · 2 players)
- `Allegiance.RoleBased(LORD)` vs `RoleBased(SPY)`
- `Seat.heroes` is a rotation queue (3 heroes); index 0 is on field
- `onSeatDeath`: discard fallen hero's cards; advance queue; new hero enters with full HP + 4 cards
- Win condition: all 3 opponent heroes eliminated

### 3v3 Mode
- `Allegiance.TeamBased(teamId)` — opaque team ID; typed `Team` added when mode is implemented

### Doudizhu Mode (斗地主 · 3 players)
- `Allegiance.RoleBased(role)` — LANDLORD (地主) vs FARMER (农民) × 2