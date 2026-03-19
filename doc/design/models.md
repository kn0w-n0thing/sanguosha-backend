# Domain Design — Models

Data model definitions for all core domain types.

---

## Card Models

### Card
```
Card                             — org.dogcard.model.card
├── type: CardType
├── suit: Suit                   — SPADE / HEART / CLUB / DIAMOND
└── number: Int                  — 1 (A) – 13 (K)
```

Derived properties (no separate field stored):
```
category:      CardCategory      — derived from type
equipmentType: EquipmentType?    — null if not equipment
weaponRange:   Int?              — null if not a weapon
damageType:    DamageType?       — null if not an attack card
```

### CardCategory
```
BASIC          — ATTACK, FIRE_ATTACK, THUNDER_ATTACK, DODGE, PEACH, WINE
INSTANT_TRICK  — DUEL, SOMETHING_FROM_NOTHING, DISMANTLE, STEAL, BORROW_SWORD,
                 NEGATE, CHAIN, FIRE_RAID, WAIT_AT_EASE, KNOW_THYSELF,
                 ALLY_FAR_ATTACK_NEAR, HAIL_OF_ARROWS, BARBARIAN_INVASION,
                 PEACH_GARDEN_OATH, BOUNTIFUL_HARVEST
DELAYED_TRICK  — ECSTASY, SUPPRESSION, LIGHTNING
EQUIPMENT      — all weapon / armor / horse types
```

### DamageType
```
NORMAL   — standard Attack; blocked by BENEVOLENT_SHIELD (♠/♣), RATTAN_ARMOR
FIRE     — Fire Attack or converted by VERMILION_BIRD_FAN; RATTAN_ARMOR takes +1
THUNDER  — Thunder Attack; bypasses BENEVOLENT_SHIELD
```

### EquipmentSlot
```
WEAPON           — sets attack range; grants weapon skill
ARMOR            — passive defensive effect
OFFENSIVE_HORSE  — your attack range +1
DEFENSIVE_HORSE  — others' attack range to you −1
```

> A hero may equip at most one card per slot.
> Playing a new equipment card into an occupied slot discards the old one.

---

## Hero Models

### Hero
```
Hero                             — org.dogcard.model.hero
├── heroId: HeroId               — @JvmInline value class wrapping String
├── gender: Gender               — MALE / FEMALE / NEUTRAL; affects card/skill targeting
├── maxHp: HpValue               — base HP from the card definition; may be X.5 in Kingdom mode
│                                  Hero does not track current HP — that belongs to Seat.hp
├── skills: List<Skill>          — skills granted by this hero card
├── equipmentArea: EquipmentArea
│   ├── weapon: Card?
│   ├── armor: Card?
│   ├── offensiveHorse: Card?    — +1 attack range
│   ├── defensiveHorse: Card?    — -1 attack range to opponents
│   └── treasure: Card?          — e.g. 木牛流马, 玉玺
└── specialArea: List<Card>?     — hero-specific staging cards (e.g. Zhuge Liang's 七星)
```

> Hand cards and judgment area belong to the Seat and are shared across heroes.

### HpValue
```
HpValue                          — org.dogcard.model.hero; @JvmInline value class
├── halves: Int                  — internal storage in half-units (e.g. 5 → 2.5 HP)
├── wholes: Int                  — floor(halves / 2); used as integer HP for HpState
└── hasHalf: Boolean             — true if a leftover half-fish remains
```

> Construction: `HpValue.of(3)` → 3 HP · `HpValue.ofHalf(5)` → 2.5 HP
>
> Kingdom mode: sum two heroes' HpValues with `+`.
> `wholes` becomes `HpState.max`; if `hasHalf` is true the leftover becomes an 阴阳鱼 marker on the seat.

### Gender
```
Gender                           — org.dogcard.model.hero
MALE / FEMALE / NEUTRAL
```

### Skill
```
Skill                            — org.dogcard.model.hero (interface, marker)
└── name: String
```

> Concrete skill implementations (active, passive, locked, limited) extend this interface.
> Each skill declares its `PhaseHook`s — see [`turn-engine.md`](turn-engine.md).

### Kingdom
```
Kingdom                          — org.dogcard.model.hero
WEI / SHU / WU / QUN
```

> Intrinsic attribute of the hero card itself; always known. Used in all modes.
> The face-down state in Kingdom mode is `Allegiance.Unrevealed` on the Seat, not here.

### Role
```
Role                             — org.dogcard.model.hero
LORD / LOYALIST / REBEL / SPY
```

> Identity mode uses all four. 1v1 mode uses LORD and SPY only.

---

## Seat Models

### Seat
```
Seat                             — org.dogcard.model.seat
├── id: String
├── seatIndex: Int               — zero-based; used for distance calculation and turn order
├── handCards: List<Card>
├── judgmentArea: List<Card>     — delayed tricks pending resolution (乐不思蜀, 兵粮寸断, 闪电)
├── heroes: List<Hero>           — non-empty; one in identity/1v1, two in Kingdom mode (国战)
│                                  In 1v1 the list is a rotation queue; index 0 is on field
├── hp: HpState                  — live HP; effective max computed by GameMode at seat-assembly
└── allegiance: Allegiance       — sealed; GameMode assigns the right subtype at game start
```

### HpState
```
HpState                          — org.dogcard.model.seat
├── current: Int                 — current HP; 0 means dying (濒死)
├── max: Int                     — effective HP cap (whole integer; fractions resolved before construction)
│
├── isDying: Boolean             — current == 0
├── isFull:  Boolean             — current == max
└── lost:    Int                 — max − current; 已损失体力值, used in skill conditions
```

> `HpState.full(HpValue)` constructs a full-health state from a hero's base `HpValue`.

### Allegiance
```
sealed Allegiance                — org.dogcard.model.seat
├── RoleBased(role: Role)        — Identity mode (Lord/Loyalist/Rebel/Spy) / 1v1 (Lord/Spy)
├── KingdomBased(kingdom: Kingdom) — Kingdom mode; set when first general is revealed (明置)
├── TeamBased(teamId: String)    — 3v3 mode; opaque ID, typed Team added later
└── Unrevealed                   — Kingdom mode only; generals still face-down (暗置)
```

---

## Deck

### IDeck
```
IDeck                            — org.dogcard.model.deck (interface)
│
│   — draw pile
├── draw(n: Int): List<Card>             — draws n cards; auto-reshuffles if needed
├── flip(): Card                         — draw 1 for judgment; caller discards afterward
├── peek(n: Int): List<Card>            — view top n without removing
├── putOnTop(cards: List<Card>)         — place onto top in list order
├── putOnBottom(cards: List<Card>)      — place onto bottom in list order
├── search(predicate: (Card) → Boolean): Card?
│                                        — find and remove first match (e.g. 卞夫人·挽危)
│   — discard pile
├── discard(cards: List<Card>)           — move to discard pile
├── takeFromDiscard(card: Card): Card    — retrieve specific card (e.g. 张昭张纮·固政)
├── reshuffle()                          — shuffle discard pile back into draw pile
│
│   — revealed zone
├── reveal(n: Int)                       — move top n cards to revealed zone (e.g. 五谷丰登)
├── takeRevealed(card: Card): Card       — player picks one card from revealed zone
├── discardRevealed()                    — discard all remaining revealed cards (cleanup)
│
└── remaining: Int                       — cards left in draw pile
```

> Implementation: `org.dogcard.game.deck.StandardDeck`
> Card set: `org.dogcard.model.card.StandardCards` (108-card 标准版+EX deck)