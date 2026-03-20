# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
This project is the backend of the sanguosha game, providing the server-side logic, API endpoints, AI agent, and model training pipeline. All components live in one repo and will be extracted to separate repos when their interfaces stabilize.

## Project Architecture
### Code Structure
- `doc/`            - Documentation for the project
- `src/`            - Spring Boot 4 game server (Kotlin)
- `cli-client/`     - CLI frontend (Kotlin) — connects via WebSocket, renders game state as text
- `ai-agent/`       - Spring AI agent module (Kotlin) — extracted to separate repo when stable
- `model-training/` - Mini model training pipeline (Python + PyTorch) — extracted to separate repo when stable

### Data Flow
```
Game Server → generates game logs
                    ↓
            model-training/ (PyTorch)
                    ↓
            trained model → served via Ollama
                    ↓
            AI Agent (Spring AI) → plays as a player client
                    ↓
            Game Server
```

### Technologies
#### Game Server
- Spring Boot 4 + Kotlin
- Spring WebSocket (real-time game state)
- No JPA (in-memory game state to start)
- JUnit 5 + MockK (unit and integration testing)
- Spring AI MockChatModel (AI layer testing without real LLM)

#### AI Agent
- Spring AI (integration layer, abstracts LLM provider per environment)

#### Model Training
- Python + PyTorch (mini transformer trained from scratch on game logs)
- Hugging Face Transformers (model architecture)
- Hugging Face Datasets (training data management)
- Weights & Biases (training metrics visualization)

### AI Agent Design
- AI agent is a player client, communicating via the same API as human players
- Game actions are exposed as Spring AI @Tool functions
- Spring AI ChatClient abstracts the LLM provider — swapped via config only, no code change

#### Environments
| Environment                            | Runtime               | Model                                                  |
|----------------------------------------|-----------------------|--------------------------------------------------------|
| Local Dev (i7-8700 + GTX 1070 Ti 8GB) | Ollama                | Qwen2.5-7B or DeepSeek-R1-Distill-7B (Q4, ~4GB VRAM) |
| Production (Aliyun / AWS, no GPU)      | Claude API (Anthropic)| claude-haiku-4-5 (cost-effective)                      |

#### AI Learning Progression
- Stage 1: Game server + AI agent using off-the-shelf model via Ollama
- Stage 2: Train mini transformer (~100M–350M params) on game logs from scratch
- Stage 3: Add RAG (card rules and game knowledge retrieval)
- Stage 4: Fine-tune with LoRA/QLoRA on GTX 1070 Ti
- Stage 5: Self-play reinforcement learning
- Stage 6: Serve the trained model via Ollama → plug back into agent

#### Notes
- AI agent and model training modules will be extracted to separate repos when interfaces stabilize

### Testing Strategy

#### Game Server Tests
| Type        | What                                                            | Tool                                    |
|-------------|-----------------------------------------------------------------|-----------------------------------------|
| Unit        | Game logic — state machine, card effects, legal move validation | JUnit 5 + MockK                         |
| Integration | REST API and WebSocket endpoints                                | Spring Boot Test + WebSocketStompClient |
| Scenario    | Full game flow (deal cards → turns → win condition)             | JUnit 5, in-memory                      |

#### AI Agent Tests
| Type       | What                                                           | Tool                                       |
|------------|----------------------------------------------------------------|--------------------------------------------|
| Unit       | Agent parses game state correctly, calls right @Tool functions | JUnit 5 + MockK + Spring AI MockChatModel  |
| Behavioral | Agent only makes legal moves against real game state           | Ollama locally (tagged @Tag("local-only")) |
| Simulation | AI vs AI full game — completes without errors                  | Ollama locally (tagged @Tag("local-only")) |

#### Full Integration Tests (Game Server + AI Agent)
| Approach               | How                                                                             | CI  |
|------------------------|---------------------------------------------------------------------------------|-----|
| Scripted MockChatModel | MockChatModel returns pre-scripted valid game actions (deterministic AI player) | Yes |
| Ollama (real behavior) | Real LLM plays against game server, tagged `@Tag("local-only")`                 | No  |

#### Rules
- No real LLM calls in CI — all CI tests use MockChatModel
- Scripted MockChatModel acts as a deterministic AI player — verifies full game server + agent wiring
- Ollama-based tests tagged `@Tag("local-only")`, excluded from CI pipeline
- AI agent tests mock the game server — agent repo stays independently testable after extraction
- Game server tests never depend on AI — clean separation

## Project Instructions for Claude

### Coding Standards
- Never use magic numbers
- Show me the whole solution and todos before changing code
- Never build the project because I will do it myself

## TODOS

Each iteration delivers a playable end-to-end slice: game server + AI agent + model training + CLI frontend.
Start with 1v1 (simplest mode), then iterate to broader modes.
The frontend is a CLI client (not a UI) — connects to the game server via WebSocket and renders game state as text.

---

### Iteration 0 — Foundation

#### Game Server
- [x] Set up Spring Boot in build.gradle.kts
- [x] Set up WebSocket support
- [x] Define core domain models
  - [x] Card + CardType / CardCategory / DamageType / EquipmentSlot enums
  - [x] Hero (heroId, gender, maxHp, skills, equipmentArea, specialArea)
  - [x] Seat (seatIndex, handCards, judgmentArea, heroes, hp, allegiance)
  - [x] Deck (drawPile, discardPile, revealedCards + all operations)
  - [x] GamePhase + phase tape model (PhaseCell, TurnEngine, TurnControl, PhaseHook)
  - [x] GameMode strategy interface + IGameRoom
  - [x] GameRoom (seats, deck, mode, engine)
  - [x] OneVsOneMode (role assignment, hero rotation on death)
- [x] Set up MockK and testing infrastructure (mockk in build.gradle.kts)

#### CLI Frontend
- [ ] Scaffold CLI frontend project (cli-client/)
- [ ] Set up WebSocket client
- [ ] Basic lobby commands (create / join room)

---

### Iteration 1 — 1v1 Mode (first playable slice)

Approach: minimal slice first (blank heroes + ATTACK/DODGE only), then grow incrementally.
Detailed design: see `doc/design/plan-minimal-1v1.md`.

#### Game Server
- [ ] Bug fixes
  - [ ] `Seat.init`: remove `require(heroes.isNotEmpty())` — empty list = eliminated
  - [ ] `OneVsOneMode.onSeatDeath`: update seat with `emptyList()` when last hero falls
- [ ] model/action: `GameAction`, `GameEvent`, `PendingRequest`, `SeatView`
- [ ] `game/engine/GameEngine` — action processor + phase driver + attack→dodge interrupt
- [ ] `game/factory/GameRoomFactory` — builds minimal 1v1 room (blank heroes, growing card pool)
- [ ] `game/log/GameLogger` — JSONL (state, action) log for training data
- [ ] REST API + WebSocket
  - [ ] `POST /rooms`, `POST /rooms/{id}/join`, `POST /rooms/{id}/start`
  - [ ] `POST /rooms/{id}/actions` — submit GameAction
  - [ ] `WS /ws/game/{roomId}` — broadcast GameEvent
- [ ] Unit tests for GameEngine (pure logic, no Spring)
- [ ] Integration test: full game via scripted MockChatModel (AI vs AI, deterministic)
- Card pool (grows as effects are implemented; lives in `GameRoomFactory.currentCardPool`)
  - [x] ATTACK, DODGE
  - [ ] PEACH
  - [ ] DUEL, BARBARIAN_INVASION, HAIL_OF_ARROWS
  - [ ] Equipment (weapons, armor, horses)
  - [ ] Full standard deck
- Hero roster (blank heroes → real heroes; swap in `GameRoomFactory`)
  - [ ] Hero draft (28-hero pool, alternating picks, 3 per player)
  - [ ] Hero skills (implement per hero; register PhaseHooks into TurnEngine)

#### AI Agent
- [ ] `ai-agent/` submodule scaffold (Spring Boot + Spring AI + Ollama)
- [ ] `GameClientService` — WS + REST client, maintains local game state
- [ ] `GameTools` — `@Tool` functions: playAttack, dodge, pass, endPlayPhase
- [ ] `AiDecisionService` — ChatClient loop: state → prompt → action
- [ ] Unit tests using MockChatModel
- [ ] Behavioral tests: AI only makes legal moves (local-only, Ollama)

#### Model Training
- [ ] Set up `model-training/` directory with PyTorch project structure
- [ ] Parse `GameLogger` JSONL → HuggingFace Dataset
- [ ] Train mini transformer (~100M–350M params) on 1v1 game logs

#### CLI Frontend
- [ ] 1v1 game board rendering (seats, hand cards, hero, HP as text)
- [ ] Card play commands (select card, select target by index)
- [ ] Real-time game state sync via WebSocket
- [ ] Game result output

---

### Iteration 2 — Identity Mode

#### Game Server
- [ ] Implement IdentityMode (Lord / Loyalist / Rebel / Spy, hidden roles)
  - [ ] Role assignment and reveal on death
  - [ ] Kill reward / penalty rules
- [ ] Scale turn state machine to 5–10 players
- [ ] Write unit tests for identity mode logic

#### AI Agent
- [ ] Extend AI agent for identity mode (hidden role reasoning)
- [ ] Write behavioral tests for identity mode (local-only)

#### Model Training
- [ ] Add RAG pipeline (card rules and game knowledge)
- [ ] Collect and train on identity mode game logs

#### CLI Frontend
- [ ] Identity mode role display (hidden / revealed)
- [ ] Multi-player lobby commands (5–10 players)
- [ ] Kill reward / death reveal output

---

### Iteration 3 — Kingdom Mode

#### Game Server
- [ ] Implement KingdomMode (Wei / Shu / Wu / Qun, public allegiance)
  - [ ] Dual-hero selection (main + sub general, faction matching)
  - [ ] Special markers (先驱, 珠联璧合, 阴阳鱼)
  - [ ] 鏖战 mode trigger
- [ ] Write unit tests for kingdom mode logic

#### AI Agent
- [ ] Extend AI agent for kingdom mode (dual-hero skills, public allegiance)
- [ ] Write behavioral tests for kingdom mode (local-only)

#### Model Training
- [ ] Fine-tune with LoRA/QLoRA on GTX 1070 Ti
- [ ] Collect and train on kingdom mode game logs

#### CLI Frontend
- [ ] Dual-hero display (main / sub general, face-up / face-down state)
- [ ] Faction and special marker output
- [ ] 鏖战 mode indicator

---

### Iteration 4 — Hardening & Extraction

#### Game Server
- [ ] Implement ThreeVsThreeMode and DoudizhuMode
- [ ] Write full scenario tests (deal → turns → win condition)

#### AI Agent
- [ ] Self-play reinforcement learning
- [ ] Serve trained model via Ollama and plug into agent
- [ ] Extract AI agent module to a separate repo

#### Model Training
- [ ] Extract model training module to a separate repo

#### CLI Frontend
- [ ] 3v3 and Doudizhu mode commands
- [ ] Spectator mode (read-only output)
- [ ] Polish (consistent text layout, colorized output)