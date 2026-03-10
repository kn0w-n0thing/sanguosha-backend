# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
This project is the backend of the sanguosha game, providing the server-side logic, API endpoints, AI agent, and model training pipeline. All components live in one repo and will be extracted to separate repos when their interfaces stabilize.

## Project Architecture
### Code Structure
- `doc/`            - Documentation for the project
- `src/`            - Spring Boot 4 game server (Kotlin)
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
### Game Server
- [x] Set up Spring Boot in build.gradle.kts
- [x] Set up WebSocket support
- [ ] Define core game domain models
  - [ ] Card (type, suit, number)
  - [ ] Player (HP, maxHp, hand cards, role, general) — depends on Card
  - [ ] GamePhase (turn phase state machine) — depends on Player
  - [ ] GameMode (strategy interface + implementations) — depends on Player
    - [ ] IdentityMode (Lord/Loyalist/Rebel/Spy, hidden roles)
    - [ ] KingdomMode (Wei/Shu/Wu/Qun, public allegiance)
    - [ ] OneVsOneMode
    - [ ] ThreeVsThreeMode
    - [ ] DoudizhuMode (Landlord/Farmer)
  - [ ] GameRoom (roomId, players, deck, mode, current phase) — depends on all above
- [ ] Implement basic game room creation and join API
- [ ] Implement game turn/phase state machine
- [ ] Set up MockK and testing infrastructure
- [ ] Write unit tests for core game logic (state machine, card effects, legal moves)
- [ ] Write integration tests for REST API and WebSocket endpoints

### AI Agent
- [ ] Integrate Spring AI + Ollama as an AI player client
- [ ] Write AI agent unit tests using Spring AI MockChatModel
- [ ] Write behavioral/simulation tests for AI agent (local-only, Ollama)
- [ ] Extract an AI agent module to a separate repo when the interfaces stabilize

### Model Training
- [ ] Set up a model-training/ directory with PyTorch project structure
- [ ] Implement game log export from a game server as training data
- [ ] Train mini transformer (~100M–350M params) from scratch on game logs
- [ ] Add RAG pipeline (card rules and game knowledge)
- [ ] Fine-tune with LoRA/QLoRA on GTX 1070 Ti
- [ ] Implement self-play reinforcement learning
- [ ] Serve a trained model via Ollama and plug into an AI agent
- [ ] Extract a model training module to a separate repo when the interfaces stabilize