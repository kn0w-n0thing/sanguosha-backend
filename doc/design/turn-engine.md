# Domain Design — Turn Engine

Turn phase state machine, skill hook system, and related types.

---

## GamePhase

Represents one phase within a single seat's turn.
Modelled as a **sealed class** for exhaustive `when` expressions and future extensibility.

```
sealed GamePhase                 — org.dogcard.model.game (to be created)
├── Idle     — waiting between turns
├── Begin    — start-of-turn skills fire here; auto-advances if no skill reacts
├── Judge    — resolve delayed tricks in judgment area
├── Draw     — draw 2 cards (default)
├── Play     — play cards freely
├── Discard  — discard down to hand limit (= current HP)
└── End      — trigger end-of-turn effects; advance to next seat
```

Normal turn flow:
```
Idle → Begin → Judge → Draw → Play → Discard → End → Idle (next seat)
```

| Phase   | Description                                                                                           |
|---------|-------------------------------------------------------------------------------------------------------|
| Idle    | Waiting; between turns                                                                                |
| Begin   | Start-of-turn phase; skills with Judge/ENTER hooks fire here; auto-advances when no skill reacts      |
| Judge   | Resolve each delayed trick in order; flip a card from deck; apply or discard based on suit/number    |
| Draw    | Draw 2 cards (default; some skills modify this)                                                       |
| Play    | Play cards freely until the player ends the phase                                                     |
| Discard | Discard down to max hand size (= current HP) if over limit                                            |
| End     | Trigger end-of-turn effects; advance to next seat                                                     |

**Delayed tricks resolved in Judge:**

| Card          | Judgment condition | Effect if triggered                                           |
|---------------|--------------------|---------------------------------------------------------------|
| 乐不思蜀 Ecstasy  | Flip is not ♥      | Skip Play phase                                               |
| 兵粮寸断 Suppression | Flip is not ♣  | Skip Draw phase                                               |
| 闪电 Lightning | Flip is ♠ 2–9      | Deal 3 thunder damage; otherwise pass to next player          |

> Card effects that interrupt the turn flow (e.g. DUEL, PEACH_REQUEST) are modelled
> as `GameEvent` — they are **not** `GamePhase` subtypes.

---

## PhaseCell

The unit of work on the phase tape. Each cell carries which phase to execute and which seat is active.

```
PhaseCell
├── phase:     GamePhase
└── seatIndex: Int     — which seat is active for this phase
```

A full turn for seat N is initialized as six cells pushed onto the tape:
```
[Begin(N), Judge(N), Draw(N), Play(N), Discard(N), End(N)]
```

`Idle` is not a cell — it is the implicit state when the tape is empty (between turns).

---

## PhaseTiming

Named checkpoints within or around a phase at which hooks may fire.
`ENTER` / `EXIT` are lifecycle boundaries; phase-specific checkpoints sit between them.

```
PhaseTiming
├── ENTER              — before the phase body starts (all phases)
├── EXIT               — after the phase body ends (all phases)
├── BEFORE_JUDGMENT    — within Judge: before each judgment card resolves
│                        (e.g. 司马懿·鬼才, 张角·鬼道 may replace the card here)
├── AFTER_JUDGMENT     — within Judge: after each judgment card resolves
│                        (e.g. 郭嘉·天妒 gains the resolved card here)
└── AFTER_DRAW         — within Draw: after cards have been drawn into hand
```

Checkpoint sequence per phase:
```
Begin:   ENTER → EXIT
Judge:   ENTER → [BEFORE_JUDGMENT → AFTER_JUDGMENT] × N cards → EXIT
Draw:    ENTER → AFTER_DRAW → EXIT
Play:    ENTER → EXIT        (card-level events are GameEvent, not phase hooks)
Discard: ENTER → EXIT
End:     ENTER → EXIT
Idle:    ENTER → EXIT
```

---

## HookPattern

Controls how multiple hooks registered for the same `(phase, timing, seatScope)` interact.

```
HookPattern
├── BROADCAST   — all hooks invoked independently with the same context;
│                 no hook affects what another sees
└── CHAIN       — hooks form an ordered sequence; each receives and may modify
                  a typed payload T before the next hook sees it
```

Chain semantics:
```
initial payload T
  → hook₁(ctx, T)  → T′
  → hook₂(ctx, T′) → T″
  → ...
  → final value used by the engine
```

Chain order: **counter-clockwise from the active seat** (matches physical game rule).

---

## PhaseHook

Declared by a `Skill` to react to a specific `(phase, timing, seatScope)` combination.
The engine indexes hooks by that triple and invokes only relevant ones.

```
PhaseHook
├── phase:     GamePhase           — which phase to observe
├── timing:    PhaseTiming         — which checkpoint within that phase
├── seatScope: SeatScope           — SELF (fires only when cell.seatIndex == hook owner's seat)
│                                    ANY  (fires for every cell, regardless of seat)
├── pattern:   HookPattern         — BROADCAST or CHAIN
└── action:    (GameContext, T?) → T?
               — T is Unit for BROADCAST; a domain type (e.g. Card) for CHAIN
```

```
SeatScope — SELF / ANY
```

技能阶段钩子汇总：

| 技能 | Phase / Timing / Scope | Pattern | 效果（原文） |
|------|------------------------|---------|-------------|
| 诸葛亮·观星 | Begin / ENTER / SELF | BROADCAST | 准备阶段开始时，你可以观看牌堆顶的X张牌，然后以任意顺序置于牌堆顶或牌堆底（X为存活角色数，且至多为5）。 |
| 甄姬·洛神 | Begin / ENTER / SELF | BROADCAST | 准备阶段开始时，你可以判定；若结果为黑色，你获得此判定牌并可以重复此流程。 |
| 孙坚·英魂 | Begin / ENTER / SELF | BROADCAST | 准备阶段，若你已受伤，你可以选择一名其他角色并选择一项：1.令其摸X张牌，然后弃置一张牌；2.令其摸一张牌，然后弃置X张牌。（X为你已损失体力值） |
| 甘夫人·神智 | Begin / ENTER / SELF | BROADCAST | 准备阶段，你可以弃置所有手牌，若你以此法弃置的手牌数不小于你的体力值，你回复1点体力。 |
| 马岱·潜袭 | Begin / ENTER / SELF | BROADCAST | 准备阶段，你可以判定，然后选择距离为1的一名角色，其本回合不能使用或打出与判定结果颜色相同的手牌。 |
| 于禁·节钺 | Begin / ENTER / SELF | BROADCAST | 准备阶段，你可以交给不是魏势力的一名角色一张手牌，然后令其执行一次"军令"。若其执行，你摸一张牌；若其不执行，你本回合摸牌阶段多摸三张牌。 |
| 孙策·魂殇 | Begin / ENTER / SELF | BROADCAST | 副将技，锁定技，此武将牌减少半个阴阳鱼。准备阶段，若你的体力值不大于1，你本回合获得"英姿""英魂"。 |
| 袁术·妄尊 | Begin / ENTER / ANY | BROADCAST | 主公技。主公的准备阶段，你可以摸一张牌；然后若主公的手牌上限大于0，本回合其手牌上限-1。 |
| 司马懿·鬼才 | Judge / BEFORE_JUDGMENT / ANY | CHAIN | 在一张判定牌生效前，你可以打出一张手牌代替之。（payload: Card） |
| 张角·鬼道 | Judge / BEFORE_JUDGMENT / ANY | CHAIN | 当一名角色的判定牌生效前，你可以打出一张黑色牌替换之。（payload: Card；在鬼才结果基础上生效） |
| 郭嘉·天妒 | Judge / AFTER_JUDGMENT / SELF | BROADCAST | 当你的判定牌生效后，你可以获得此牌。 |
| 陆抗·筑围 | Judge / AFTER_JUDGMENT / ANY | BROADCAST | 当你的判定牌生效后，若此牌为【杀】或伤害锦囊牌，你可以获得之，然后你可以令当前回合角色本回合手牌上限+1、使用【杀】的限制次数+1。 |
| 张辽·突袭 | Draw / ENTER / SELF | BROADCAST | 摸牌阶段，你可以放弃摸牌，改为获得至多两名其他角色的各一张手牌。 |
| 许褚·裸衣 | Draw / ENTER / SELF | BROADCAST | 摸牌阶段，你可以少摸一张牌；若如此做，你本回合使用【杀】或【决斗】造成的伤害+1。 |
| 孟获·再起 | Draw / ENTER / SELF | BROADCAST | 摸牌阶段，你可以改为亮出牌堆顶X张牌（X为你已损失体力值），回复与其中红桃牌数等量的体力，弃置这些红桃牌并获得剩余牌。 |
| 颜良文丑·双雄 | Draw / ENTER / SELF | BROADCAST | 摸牌阶段，你可以改为进行一次判定，你获得判定牌且本回合可以将一张与之颜色不同的手牌当【决斗】使用。 |
| 董卓·横征 | Draw / ENTER / SELF | BROADCAST | 摸牌阶段，若你的体力值为1或你没有手牌，你可以改为获得每名其他角色区域里的一张牌。 |
| 李典·恂恂 | Draw / ENTER / SELF | BROADCAST | 摸牌阶段开始时，你可以观看牌堆顶的四张牌，然后将其中两张牌置于牌堆顶，将剩余牌置于牌堆底。 |
| 夏侯渊·神速 | Begin / ENTER / SELF | BROADCAST | 你可以做出如下选择：1.跳过判定阶段和摸牌阶段；2.跳过出牌阶段并弃置一张装备牌；3.跳过弃牌阶段并翻面。你每选择一项，视为你使用一张无距离限制的【杀】。（调用 engine.skip()） |
| 张郃·巧变 | Begin / ENTER / SELF | BROADCAST | 你可以弃置一张手牌并跳过一个阶段（准备阶段和结束阶段除外），若为：摸牌阶段，你可以获得至多两名角色各一张手牌；出牌阶段，你可以移动场上一张牌。（调用 engine.skip()） |
| 周瑜·英姿 | Draw / AFTER_DRAW / SELF | BROADCAST | 摸牌阶段，你可以额外摸一张牌。 |
| 鲁肃·好施 | Draw / AFTER_DRAW / SELF | BROADCAST | 摸牌阶段，你可以多摸两张牌，然后若你的手牌数大于5，你将一半的手牌（向下取整）交给手牌最少的一名其他角色。 |
| 刘禅·放权 | Play / ENTER / SELF | BROADCAST | 你可以跳过出牌阶段，然后本回合结束时，你可以弃置一张手牌并令一名其他角色执行一个额外的回合。（调用 engine.skip(Play)；End/EXIT 调用 engine.append(extraTurn)） |
| 纪灵·双刃 | Play / ENTER / SELF | BROADCAST | 出牌阶段开始时，你可以拼点：若你赢，你视为对被拼点者或与其势力相同的另一名角色使用一张【杀】；若你没赢，你结束出牌阶段。 |
| 崔琰毛玠·征辟 | Play / ENTER / SELF | BROADCAST | 出牌阶段开始时，你可以选择一项：1.选择一名未确定势力的角色，若其本阶段明置了武将牌，本阶段结束时你获得其一张手牌和一张装备区里的牌；2.交给一名有明置武将牌的其他角色一张基本牌，令其交给你一张非基本牌或两张基本牌。 |
| 吕范·调度 | Play / ENTER / SELF | BROADCAST | 出牌阶段开始时，你可以获得与你势力相同的一名角色装备区里的一张牌，然后可以将此牌交给另一名角色。 |
| 何太后·鸩毒 | Play / ENTER / ANY | BROADCAST | 每名角色出牌阶段开始时，你可以弃置一张手牌，然后其视为使用一张【酒】，若其不是你，你对其造成1点伤害。 |
| 董卓·暴凌 | Play / EXIT / SELF | BROADCAST | 主将技，锁定技，出牌阶段结束时，若你有副将，则你移除副将，然后增加3点体力上限，回复3点体力，并获得"崩坏"。 |
| 吕范·典财 | Play / EXIT / ANY | BROADCAST | 其他角色出牌阶段结束时，若你此阶段失去了至少X张牌（X为你的体力值），你可以将手牌摸至体力上限，然后你可以变更副将。 |
| 吕蒙·克己（身份） | Discard / ENTER / SELF | BROADCAST | 若你于出牌阶段未使用或打出过【杀】，你可以跳过弃牌阶段。（调用 engine.skip(Discard)） |
| 吕蒙·克己（国战） | Discard / ENTER / SELF | BROADCAST | 锁定技，弃牌阶段开始时，若你未于出牌阶段使用过颜色不同的牌或出牌阶段被跳过，你本回合手牌上限+4。 |
| 蒋琬费祎·生息 | Discard / ENTER / SELF | BROADCAST | 弃牌阶段开始时，若你本回合未造成伤害，你可以摸两张牌。 |
| 卞夫人·约俭 | Discard / ENTER / ANY | BROADCAST | 锁定技，与你势力相同角色的弃牌阶段开始时，若其本回合处于明置状态时未使用牌指定过其他势力的角色为目标，其本回合手牌上限等于其体力上限。 |
| 张昭张纮·固政 | Discard / EXIT / ANY | BROADCAST | 其他角色弃牌阶段结束时，你可以令其获得一张此阶段因弃置进入弃牌堆的牌，然后你可以获得剩余牌。 |
| 貂蝉·闭月 | End / ENTER / SELF | BROADCAST | 结束阶段，你可以摸一张牌。 |
| 曹仁·据守（身份） | End / ENTER / SELF | BROADCAST | 结束阶段，你可以翻面并摸四张牌，然后弃置一张手牌，若以此法弃置的是装备牌，则你改为使用之。 |
| 曹仁·据守（国战） | End / ENTER / SELF | BROADCAST | 结束阶段，你可以摸X张牌（X为已亮明势力数），并选择一项：1.弃置手中一张非装备牌；2.使用一张装备牌。若X大于2，你翻面。 |
| 曹洪·护援 | End / ENTER / SELF | BROADCAST | 结束阶段，你可以将一张装备牌置入一名角色的装备区，然后你可以弃置其距离为1的一名角色的一张牌。 |
| 陈武董袭·奋命 | End / ENTER / SELF | BROADCAST | 结束阶段，若你处于连环状态，你可以弃置所有处于连环状态的角色各一张牌。 |
| 吕蒙·谋断 | End / ENTER / SELF | BROADCAST | 结束阶段，若你于出牌阶段使用过四种花色或三种类型的牌，你可以移动场上一张牌。 |
| 乐进·骁果 | End / ENTER / ANY | BROADCAST | 其他角色的结束阶段，你可以弃置一张基本牌，令其选择一项：1.弃置一张装备牌，你摸一张牌；2.你对其造成1点伤害。 |
| 周泰·奋激 | End / ENTER / ANY | BROADCAST | 每名角色的结束阶段，若其没有手牌，你可以令其摸两张牌，然后你失去1点体力。 |

> 鬼才 和 鬼道 同在 Judge / BEFORE_JUDGMENT / ANY 触发，均为 CHAIN。
> 优先级：以当前回合角色为起点，逆时针方向依次结算。

---

## TurnEngine

Drives the game forward by consuming a mutable phase tape. Owned by `GameRoom`.

```
TurnEngine
├── tape:    ArrayDeque<PhaseCell>   — remaining cells to execute
├── current: PhaseCell?              — cell currently executing (null between turns)
│
├── startTurn(seatIndex: Int)        — push [Begin, Judge, Draw, Play, Discard, End](seatIndex) onto tape
├── advance()                        — pop next cell from tape and execute it
├── skip(phase: GamePhase)           — remove all cells of given phase from tape
│                                      (called by skills and delayed-trick effects)
├── insertAfter(cell: PhaseCell)     — inject a cell immediately after current
│                                      (e.g. re-judgment after 鬼才 replaces card)
├── append(cells: List<PhaseCell>)   — push cells to end of tape
│                                      (e.g. 奉迎 extra turn, 放权 grants extra turn)
└── fireCheckpoint(timing, payload?) — invoke hooks for (current.phase, timing, current.seatIndex)
                                       BROADCAST: call all; CHAIN: thread payload through ordered seats
```

`advance()` lifecycle:
```
1. fireCheckpoint(EXIT)              — hooks react on phase exit; may call skip/append
2. current = tape.removeFirst()      — pop next cell (if tape is empty, game is between turns)
3. fireCheckpoint(ENTER)             — hooks react on phase enter; may call skip/insertAfter/append
4. execute phase body
```

Phase-internal checkpoint (Judge resolving one delayed trick):
```
1. fireCheckpoint(BEFORE_JUDGMENT, payload=card)   — CHAIN: card may be replaced (鬼才, 鬼道)
2. engine applies the judgment effect; delayed-trick outcome may call skip(Draw) or skip(Play)
3. fireCheckpoint(AFTER_JUDGMENT,  payload=card)   — BROADCAST: skills react to result (天妒, 筑围)
```

Tape state at key moments:
```
Turn start (seat 2):   tape = [Begin(2), Judge(2), Draw(2), Play(2), Discard(2), End(2), ...]
After 神速 skips Draw: tape = [Play(2), Discard(2), End(2), ...]   ← Draw(2) removed
After 奉迎 extra turn: tape = [..., Begin(2), Judge(2), Draw(2), Play(2), Discard(2), End(2)]  ← appended
```