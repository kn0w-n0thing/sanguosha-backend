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
├── type: CardType           — see CardType enum below
├── suit: Suit               — SPADE, HEART, CLUB, DIAMOND
└── number: Int              — 1 (A) – 13 (K)

```

#### CardType Enum

Grouped by category for clarity. The runtime `category` property is derived from this enum.

**Basic Cards (基本牌)**

| CardType        | CN    | EN             |
|-----------------|-------|----------------|
| `ATTACK`        | 杀    | Attack         |
| `FIRE_ATTACK`   | 火杀  | Fire Attack    |
| `THUNDER_ATTACK`| 雷杀  | Thunder Attack |
| `DODGE`         | 闪    | Dodge          |
| `PEACH`         | 桃    | Peach          |
| `WINE`          | 酒    | Wine           |

**Instant Trick Cards (即时锦囊)**

| CardType                  | CN     | EN                     |
|---------------------------|--------|------------------------|
| `DUEL`                    | 决斗   | Duel                   |
| `SOMETHING_FROM_NOTHING`  | 无中生有 | Something from Nothing |
| `DISMANTLE`               | 过河拆桥 | Dismantle              |
| `STEAL`                   | 顺手牵羊 | Steal                  |
| `BORROW_SWORD`            | 借刀杀人 | Borrow Sword           |
| `NEGATE`                  | 无懈可击 | Negate                 |
| `CHAIN`                   | 铁索连环 | Chain                  |
| `FIRE_RAID`               | 火攻   | Fire Raid              |
| `WAIT_AT_EASE`            | 以逸待劳 | Wait at Ease           |
| `KNOW_THYSELF`            | 知己知彼 | Know Thyself           |
| `ALLY_FAR_ATTACK_NEAR`    | 远交近攻 | Ally Far Attack Near   |
| `HAIL_OF_ARROWS`          | 万箭齐发 | Hail of Arrows         |
| `BARBARIAN_INVASION`      | 南蛮入侵 | Barbarian Invasion     |
| `PEACH_GARDEN_OATH`       | 桃园结义 | Peach Garden Oath      |
| `BOUNTIFUL_HARVEST`       | 五谷丰登 | Bountiful Harvest      |

**Delayed Trick Cards (延时锦囊)**

| CardType      | CN     | EN          |
|---------------|--------|-------------|
| `ECSTASY`     | 乐不思蜀 | Ecstasy     |
| `SUPPRESSION` | 兵粮寸断 | Suppression |
| `LIGHTNING`   | 闪电   | Lightning   |

**Equipment Cards (装备牌) — Weapons**

| CardType                      | CN      | EN                          | Range |
|-------------------------------|---------|-----------------------------|-------|
| `ZHUGE_CROSSBOW`              | 诸葛连弩 | Zhuge Crossbow              | 1     |
| `VERMILION_BIRD_FAN`          | 朱雀羽扇 | Vermilion Bird Fan          | 4     |
| `BOULDER_AXE`                 | 贯石斧   | Boulder Axe                 | 3     |
| `WU_SIX_SWORD`                | 吴六剑   | Wu Six Sword                | 2     |
| `THREE_POINTED_BLADE`         | 三尖两刃刀 | Three-Pointed Blade        | 3     |
| `QILIN_BOW`                   | 麒麟弓   | Qilin Bow                   | 5     |
| `GENDER_SWORDS`               | 雌雄双股剑 | Gender Swords              | 2     |
| `ICE_SWORD`                   | 寒冰剑   | Ice Sword                   | 2     |
| `QINGGANG_SWORD`              | 青釭剑   | Qinggang Sword              | 2     |
| `SNAKE_SPEAR`                 | 丈八蛇矛 | Snake Spear                 | 3     |
| `GREEN_DRAGON_CRESCENT_BLADE` | 青龙偃月刀 | Green Dragon Crescent Blade | 3   |
| `HALBERD`                     | 方天画戟 | Halberd                     | 4     |

**Equipment Cards — Armor**

| CardType           | CN     | EN                |
|--------------------|--------|-------------------|
| `RATTAN_ARMOR`     | 藤甲   | Rattan Armor      |
| `BENEVOLENT_SHIELD`| 仁王盾 | Benevolent Shield |
| `SILVER_LION`      | 白银狮子 | Silver Lion      |
| `EIGHT_TRIGRAMS`   | 八卦阵 | Eight Trigrams    |

**Equipment Cards — Horses**

| CardType           | CN     | EN              | Slot         |
|--------------------|--------|-----------------|--------------|
| `CHITU`            | 赤兔   | Chitu           | Defensive −1 |
| `ZIXING`           | 紫骍   | Zixing          | Defensive −1 |
| `DAYUAN`           | 大宛   | Dayuan          | Defensive −1 |
| `DILU`             | 的卢   | Dilu            | Offensive +1 |
| `JUEYING`          | 绝影   | Jueying         | Offensive +1 |
| `ZHUHUANGFEIDIAN`  | 爪黄飞电 | Zhuhuangfeidian | Offensive +1 |

---

#### CardCategory

Derived from `CardType` — no separate field stored on `Card`.

```
BASIC          — ATTACK, FIRE_ATTACK, THUNDER_ATTACK, DODGE, PEACH, WINE
INSTANT_TRICK  — DUEL, SOMETHING_FROM_NOTHING, DISMANTLE, STEAL, BORROW_SWORD,
                 NEGATE, CHAIN, FIRE_RAID, WAIT_AT_EASE, KNOW_THYSELF,
                 ALLY_FAR_ATTACK_NEAR, HAIL_OF_ARROWS, BARBARIAN_INVASION,
                 PEACH_GARDEN_OATH, BOUNTIFUL_HARVEST
DELAYED_TRICK  — ECSTASY, SUPPRESSION, LIGHTNING
EQUIPMENT      — all weapon / armor / horse types
```

---

#### DamageType

Determines which armor effects and general skills apply to a hit.

```
NORMAL   — standard Attack; blocked by BENEVOLENT_SHIELD (♠/♣ attacks), RATTAN_ARMOR
FIRE     — Fire Attack, or converted by VERMILION_BIRD_FAN; RATTAN_ARMOR takes +1 extra
THUNDER  — Thunder Attack; bypasses BENEVOLENT_SHIELD
```

#### EquipmentSlot

Each player's `equipmentArea` enforces one card per slot:

```
WEAPON           — sets attack range; grants weapon skill
ARMOR            — passive defensive effect
OFFENSIVE_HORSE  — your attack range +1
DEFENSIVE_HORSE  — others' attack range to you −1
```

> A player may equip at most one card per slot. Playing a new equipment card into an occupied slot discards the old one.

### Seat
```
Seat
├── id: String
├── seatIndex: Int               — position for distance calculation and turn order
├── handCards: List<Card>
├── judgmentArea: List<Card>     — delayed tricks pending resolution (乐不思蜀, 兵粮寸断, 闪电)
├── heroes: List<Hero>           — one hero in standard modes; two in dual-hero mode (non-empty)
└── allegiance: Allegiance       — sealed; GameMode assigns the right subtype at game start
```

### Hero
```
Hero
├── heroId: HeroId
├── gender: Gender               — MALE / FEMALE / NEUTRAL; affects some card/skill targeting
├── hp: Int
├── maxHp: Int
├── skills: List<Skill>          — from hero definition
├── equipmentArea: EquipmentArea
│   ├── weapon: Card?
│   ├── armor: Card?              — e.g. 八卦阵, 藤甲
│   ├── offensiveHorse: Card?     — +1 attack range
│   ├── defensiveHorse: Card?     — -1 attack range to opponents
│   └── treasure: Card?           — e.g. 木牛流马, 玉玺
└── specialArea: List<Card>?      — hero-specific cards (e.g. Zhuge's Qixing)
```

> Hand cards and judgment area belong to the seat and are shared across heroes.

### Gender
```
MALE / FEMALE / NEUTRAL
```

### Allegiance
```
sealed Allegiance
├── RoleBased(role: Role)            — IdentityMode / DoudizhuMode
├── KingdomBased(kingdom: Kingdom)   — KingdomMode
├── TeamBased(teamId: String)        — 3v3Mode; opaque ID for now, typed Team added later
└── None                             — 1v1Mode
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
├── assignAllegiances(seats: List<Seat>)
├── checkWinCondition(room: GameRoom): Winner?
└── onSeatDeath(dead: Seat, room: GameRoom)
```

### GameRoom
```
GameRoom
├── id: String
├── seats: List<Seat>
├── deck: ArrayDeque<Card>    — draw pile
├── discardPile: List<Card>
├── mode: GameMode
├── currentPhase: GamePhase
└── currentSeatIndex: Int
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
 └──▶ Hero
        └──▶ Seat
               └──▶ GameMode
               └──▶ GamePhase
                      └──▶ GameRoom (aggregates all above)
```

---

## Design Decisions

| Decision              | Choice                        | Reason                                                                          |
|-----------------------|-------------------------------|---------------------------------------------------------------------------------|
| Mode extensibility    | Strategy pattern              | New modes = new class, no changes to core models                                |
| Player split          | Seat + Hero                   | Seat owns distance/turns; Hero owns HP/skills/equipment — different lifecycles  |
| Dual-hero support     | `heroes: List<Hero>` on Seat  | Non-empty list; standard modes have one, dual-hero modes have two               |
| Mode-specific identity| Sealed `Allegiance` class     | Replaces nullable role/kingdom; type system prevents cross-mode field leakage   |
| Team info             | `TeamBased(teamId: String)`   | Opaque ID now; typed Team enum added later without structural change             |
| Deck                  | ArrayDeque                    | Efficient draw from front, discard to back                                      |
| Game state            | In-memory                     | No persistence needed at this stage                                             |