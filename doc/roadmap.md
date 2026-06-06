# Roadmap

Each iteration delivers a playable end-to-end slice: game server + AI agent + model training + CLI frontend.
Start with 1v1 (simplest mode), then iterate to broader modes.
The frontend is a CLI client (not a UI) — connects to the game server via WebSocket and renders game state as text.

---

## Iteration 0 — Foundation

### Game Server
- [x] Set up Spring Boot and WebSocket
- [x] Define core domain models (Card, Hero, Seat, Deck, GamePhase, GameMode, GameRoom, OneVsOneMode)
- [x] Set up MockK and testing infrastructure

### CLI Frontend
- [ ] Scaffold project, WebSocket client, basic lobby commands

---

## Iteration 1 — 1v1 Mode (first playable slice)

Detailed design: see `doc/design/plan-minimal-1v1.md`.

### Game Server
- [ ] Fix OneVsOneMode (hero rotation, win condition)
- [ ] Action protocol models (GameAction, GameEvent, PendingRequest, SeatView)
- [ ] GameEngine — phase driver + action processor
- [x] GameRoomFactory — minimal 1v1 room
- [ ] GameLogger — JSONL game log for model training
- [ ] Unit tests for game logic
- [ ] Card pool expansion (PEACH → tricks → equipment → full deck)
- [ ] Hero draft and skills

### WebSocket / REST API
- [ ] REST endpoints (create room, join, start, submit action, get state)
- [ ] WebSocket broadcast
- [ ] Integration test: full game via scripted MockChatModel

### AI Agent
- [ ] Scaffold ai-agent/ (Spring Boot + Spring AI + Ollama)
- [ ] Game client (WebSocket + REST), @Tool functions, decision loop
- [ ] Unit tests (MockChatModel) and behavioral tests (local-only, Ollama)

### Model Training
- [ ] Scaffold model-training/ (PyTorch project)
- [ ] Parse game logs and train mini transformer on 1v1 games

### CLI Frontend
- [ ] 1v1 board rendering and card play commands
- [ ] Real-time state sync and game result output

---

## Iteration 2 — Identity Mode

### Game Server
- [ ] Implement IdentityMode (role assignment, reveal on death, kill reward/penalty)
- [ ] Scale to 5–10 players

### AI Agent
- [ ] Extend for identity mode (hidden role reasoning)

### Model Training
- [ ] Add RAG pipeline (card rules and game knowledge)
- [ ] Train on identity mode game logs

### CLI Frontend
- [ ] Role display, multi-player lobby, death reveal output

---

## Iteration 3 — Kingdom Mode

### Game Server
- [ ] Implement KingdomMode (dual-hero, faction markers, 鏖战 trigger)

### AI Agent
- [ ] Extend for kingdom mode (dual-hero skills, public allegiance)

### Model Training
- [ ] Fine-tune with LoRA/QLoRA
- [ ] Train on kingdom mode game logs

### CLI Frontend
- [ ] Dual-hero display, faction and marker output

---

## Iteration 4 — Hardening & Extraction

### Game Server
- [ ] Implement ThreeVsThreeMode and DoudizhuMode
- [ ] Full scenario tests

### AI Agent
- [ ] Self-play reinforcement learning
- [ ] Serve trained model via Ollama
- [ ] Extract to separate repo

### Model Training
- [ ] Extract to separate repo

### CLI Frontend
- [ ] 3v3 and Doudizhu mode, spectator mode, polish