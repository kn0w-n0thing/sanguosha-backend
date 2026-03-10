# Domain Design

## Overview

The domain model is designed around the **Strategy pattern** for game modes.
Core models (`Card`, `Player`, `GamePhase`, `GameRoom`) are shared across all modes.
Mode-specific logic (win conditions, role assignment, team composition) is encapsulated in `GameMode` implementations.

---

## Core Models

### Card
```
Card
├── type: CardType       — ATTACK, DODGE, PEACH, DUEL, ...
├── suit: Suit           — SPADE, HEART, CLUB, DIAMOND
└── number: Int          — 1–13
```

### Player
```
Player
├── id: String
├── general: General              — warrior card; owns identity, HP, skills, and special area
│   ├── id: GeneralId
│   ├── hp: Int
│   ├── maxHp: Int
│   └── specialArea: List<Card>? — general-specific cards (e.g. Zhuge's Qixing); null if unused
├── handCards: List<Card>
├── judgmentArea: List<Card>      — delayed tricks pending resolution (乐不思蜀, 兵粮寸断, 闪电)
├── equipmentArea: Equipment      — named slots for equipped cards
│   ├── weapon: Card?
│   ├── armor: Card?              — e.g. 八卦阵, 藤甲
│   ├── offensiveHorse: Card?     — +1 attack range
│   ├── defensiveHorse: Card?     — -1 attack range to opponents
│   └── treasure: Card?           — e.g. 木牛流马, 玉玺
├── role: Role?                   — assigned by GameMode (nullable, not all modes use roles)
└── kingdom: Kingdom?             — Wei / Shu / Wu / Qun (nullable, only KingdomMode)
```

### GamePhase
Represents the phases within a single player's turn:
```
IDLE → JUDGE → DRAW → PLAY → DISCARD → END
```

| Phase   | Description                                                                                                                          |
|---------|--------------------------------------------------------------------------------------------------------------------------------------|
| IDLE    | Waiting; between turns                                                                                                               |
| JUDGE   | Resolve each delayed trick in the player's judgment area, in order. Flip a card from the deck; apply or discard based on suit/number |
| DRAW    | Draw 2 cards (default)                                                                                                               |
| PLAY    | Play cards freely until the player ends the phase or runs out of legal moves                                                         |
| DISCARD | Discard down to max hand size (= current HP) if over limit                                                                           |
| END     | Trigger end-of-turn effects; advance to next player                                                                                  |

**Delayed tricks resolved in the JUDGE phase:**

| Card               | Judgment condition | Effect if triggered                                                  |
|--------------------|--------------------|----------------------------------------------------------------------|
| 乐不思蜀 (Ecstasy)     | Flip is not ♥      | Skip PLAY phase entirely                                             |
| 兵粮寸断 (Suppression) | Flip is not ♣      | Skip DRAW phase entirely                                             |
| 闪电 (Lightning)     | Flip is ♠ 2–9      | Deal 3 lightning damage to the player; otherwise pass to next player |

Special phases triggered by card effects (e.g. DUEL, PEACH_REQUEST) interrupt the normal turn flow and may occur during PLAY phase.

### GameMode (Strategy Interface)
```
GameMode
├── assignRoles(players: List<Player>)
├── checkWinCondition(room: GameRoom): Winner?
└── onPlayerDeath(dead: Player, room: GameRoom)
```

### GameRoom
```
GameRoom
├── id: String
├── players: List<Player>
├── deck: ArrayDeque<Card>    — draw pile
├── discardPile: List<Card>
├── mode: GameMode
├── currentPhase: GamePhase
└── currentPlayerIndex: Int
```

---

## Game Modes

### Identity Mode (身份 · 5–10 players)
Players are secretly assigned roles at game start. Roles are revealed on death.

| Role          | Count | Win Condition                   |
|---------------|-------|---------------------------------|
| Lord (主公)     | 1     | All Rebels and the Spy are dead |
| Loyalist (忠臣) | 1–3   | Same as Lord                    |
| Rebel (反贼)    | 2–4   | Lord is dead                    |
| Spy (内奸)      | 1     | Last player alive               |

- Lord is revealed at game start; all other roles are hidden
- Loyalist who kills a Rebel draws 3 cards as reward
- Lord who kills a Loyalist discards all hand cards and equipment as penalty

### Kingdom Mode (国战 · 4–10 players)
Players belong to a kingdom. Allegiance is **public**. No hidden roles.

| Kingdom | Characters             |
|---------|------------------------|
| Wei (魏) | Cao Cao, Sima Yi, ...  |
| Shu (蜀) | Liu Bei, Guan Yu, ...  |
| Wu (吴)  | Sun Quan, Zhou Yu, ... |
| Qun (群) | Neutrals               |

- Win condition: last kingdom with living players
- Players of the same kingdom cannot attack each other (default rule)

### 1v1 Mode
- 2 players, each drafts a set of generals from a pool
- Win condition: eliminate the opponent
- No roles, no kingdoms

### 3v3 Mode
- 6 players split into Team A and Team B (3 per team)
- Each team drafts generals
- Win condition: eliminate all members of the opposing team

### Doudizhu Mode (斗地主 · 3 players)
Adapted from the Chinese card game.

| Role          | Count | Win Condition          |
|---------------|-------|------------------------|
| Landlord (地主) | 1     | Eliminate both Farmers |
| Farmer (农民)   | 2     | Eliminate the Landlord |

---

## Dependency Graph

```
Card
 └──▶ Player
        └──▶ GameMode
        └──▶ GamePhase
               └──▶ GameRoom (aggregates all above)
```

---

## Design Decisions

| Decision           | Choice                   | Reason                                                  |
|--------------------|--------------------------|---------------------------------------------------------|
| Mode extensibility | Strategy pattern         | New modes = new class, no changes to core models        |
| Role storage       | Nullable field on Player | Not all modes use roles; avoids unnecessary subclassing |
| Deck               | ArrayDeque               | Efficient draw from front, discard to back              |
| Game state         | In-memory                | No persistence needed at this stage                     |