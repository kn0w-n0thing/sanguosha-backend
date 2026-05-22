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
| Local Dev (Ryzen AI 9 HX PRO 370 + Radeon 890M iGPU, 32 GB shared RAM) | Ollama | Qwen2.5-14B or DeepSeek-R1-Distill-14B (Q4, ~8–9 GB shared RAM) |
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

### TDD Workflow
- One test at a time: write ONE failing test → RED → implement the minimum to pass → GREEN → next test
- A compilation failure counts as RED — no need to run the project to confirm failure
- Write only enough production code to make the current test pass — no speculative code
- The "show solution before changing code" rule applies to production code, not test files; tests are written first as the specification

## TODOS

Each iteration delivers a playable end-to-end slice: game server + AI agent + model training + CLI frontend.
Start with 1v1 (simplest mode), then iterate to broader modes.
The frontend is a CLI client (not a UI) — connects to the game server via WebSocket and renders game state as text.

---

### Iteration 0 — Foundation

#### Game Server
- [x] Set up Spring Boot and WebSocket
- [x] Define core domain models (Card, Hero, Seat, Deck, GamePhase, GameMode, GameRoom, OneVsOneMode)
- [x] Set up MockK and testing infrastructure

#### CLI Frontend
- [ ] Scaffold project, WebSocket client, basic lobby commands

---

### Iteration 1 — 1v1 Mode (first playable slice)

Detailed design: see `doc/design/plan-minimal-1v1.md`.

#### Game Server
- [ ] Fix OneVsOneMode (hero rotation, win condition)
- [ ] Action protocol models (GameAction, GameEvent, PendingRequest, SeatView)
- [ ] GameEngine — phase driver + action processor
- [x] GameRoomFactory — minimal 1v1 room
- [ ] GameLogger — JSONL game log for model training
- [ ] Unit tests for game logic
- [ ] Card pool expansion (PEACH → tricks → equipment → full deck)
- [ ] Hero draft and skills

#### WebSocket / REST API
- [ ] REST endpoints (create room, join, start, submit action, get state)
- [ ] WebSocket broadcast
- [ ] Integration test: full game via scripted MockChatModel

#### AI Agent
- [ ] Scaffold ai-agent/ (Spring Boot + Spring AI + Ollama)
- [ ] Game client (WebSocket + REST), @Tool functions, decision loop
- [ ] Unit tests (MockChatModel) and behavioral tests (local-only, Ollama)

#### Model Training
- [ ] Scaffold model-training/ (PyTorch project)
- [ ] Parse game logs and train mini transformer on 1v1 games

#### CLI Frontend
- [ ] 1v1 board rendering and card play commands
- [ ] Real-time state sync and game result output

---

### Iteration 2 — Identity Mode

#### Game Server
- [ ] Implement IdentityMode (role assignment, reveal on death, kill reward/penalty)
- [ ] Scale to 5–10 players

#### AI Agent
- [ ] Extend for identity mode (hidden role reasoning)

#### Model Training
- [ ] Add RAG pipeline (card rules and game knowledge)
- [ ] Train on identity mode game logs

#### CLI Frontend
- [ ] Role display, multi-player lobby, death reveal output

---

### Iteration 3 — Kingdom Mode

#### Game Server
- [ ] Implement KingdomMode (dual-hero, faction markers, 鏖战 trigger)

#### AI Agent
- [ ] Extend for kingdom mode (dual-hero skills, public allegiance)

#### Model Training
- [ ] Fine-tune with LoRA/QLoRA
- [ ] Train on kingdom mode game logs

#### CLI Frontend
- [ ] Dual-hero display, faction and marker output

---

### Iteration 4 — Hardening & Extraction

#### Game Server
- [ ] Implement ThreeVsThreeMode and DoudizhuMode
- [ ] Full scenario tests

#### AI Agent
- [ ] Self-play reinforcement learning
- [ ] Serve trained model via Ollama
- [ ] Extract to separate repo

#### Model Training
- [ ] Extract to separate repo

#### CLI Frontend
- [ ] 3v3 and Doudizhu mode, spectator mode, polish