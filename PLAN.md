# Minecraft AI Refactor Plan

## Overview

Refactor from PydanticAI + FastAPI backend to Claude Code headless mode running as a Minecraft server plugin. Uses LDLib2 for modern UI.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Minecraft Server (NeoForge 1.21)         │
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │  Mod + LDLib2   │───▶│  Claude Code (subprocess)       │ │
│  │                 │◀───│  --output-format stream-json    │ │
│  └────────┬────────┘    └─────────────────────────────────┘ │
│           │                          │                       │
│           ▼                          ▼                       │
│  ┌──────────────────┐    ┌─────────────────────────────────┐│
│  │ State Files      │    │ .claude/ (skills, hooks)        ││
│  │ players/{uuid}/  │    │ players/{uuid}/ (user files)    ││
│  │ state.json       │    │ sessions/ (conversation hist)   ││
│  └──────────────────┘    └─────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
           │ packets
           ▼
┌─────────────────────────────────────────────────────────────┐
│                    Minecraft Client                          │
│  LDLib2 UI: Chat, Scratch Pad, Pinnable Overlay, Settings   │
└─────────────────────────────────────────────────────────────┘
```

## Directory Structure

```
plugins/minecraft-ai/
├── .claude/
│   ├── settings.json              # Hooks configuration
│   ├── CLAUDE.md                  # Instructions for Claude about this system
│   ├── hooks/
│   │   └── validate_write.sh      # Restrict writes: user folder, .md/.json only
│   └── skills/
│       ├── mc-get-position/
│       │   └── SKILL.md
│       ├── mc-get-inventory/
│       │   └── SKILL.md
│       ├── mc-get-nearby/
│       │   └── SKILL.md
│       └── mc-get-world-info/
│           └── SKILL.md
├── CLAUDE.md                      # Server-wide context
├── config.json                    # Plugin settings
├── bin/
│   ├── mc-position.sh             # Read player state
│   ├── mc-inventory.sh
│   ├── mc-nearby.sh
│   └── mc-world-info.sh
└── players/
    └── {uuid}/
        ├── CLAUDE.md              # Player-specific context
        ├── scratch.md             # Scratch pad (agent-visible)
        ├── state.json             # Current game state (written by mod)
        ├── notes/                 # User's saved notes
        └── sessions/
            ├── current.txt        # Current session ID
            └── history/           # Previous session metadata
                └── {session-id}.json
```

## Security Model

### Allowed Tools
- `Read` - Read any file in plugin directory
- `Write` - Restricted by hook (see below)
- `Bash(bin/*)` - Only scripts in bin/ directory

### Write Hook Restrictions
1. **File extensions**: Only `.md` and `.json` allowed
2. **Directory access**:
   - Regular players: Only `players/{their-uuid}/` directory
   - OPs/Admins: Anywhere in `plugins/minecraft-ai/`
3. **Environment variables** passed to hook:
   - `MINECRAFT_PLAYER_UUID`
   - `MINECRAFT_IS_OP` (from server OP list)

## Skills

| Skill | Description | Script |
|-------|-------------|--------|
| `mc-get-position` | Player coords, dimension, facing | `bin/mc-position.sh` |
| `mc-get-inventory` | Inventory contents | `bin/mc-inventory.sh` |
| `mc-get-nearby` | Blocks/entities in radius | `bin/mc-nearby.sh` |
| `mc-get-world-info` | Time, weather, difficulty | `bin/mc-world-info.sh` |

Scripts read from `players/{uuid}/state.json` which is updated by the mod.

## State File Format

`players/{uuid}/state.json` (written by mod, read by scripts):
```json
{
  "position": { "x": 100, "y": 64, "z": -200 },
  "dimension": "minecraft:overworld",
  "rotation": { "yaw": 90.0, "pitch": 0.0 },
  "health": 20.0,
  "hunger": 18,
  "gamemode": "survival",
  "inventory": [
    { "slot": 0, "item": "minecraft:diamond_pickaxe", "count": 1 }
  ],
  "nearby_blocks": [...],
  "nearby_entities": [...],
  "world": {
    "time": 6000,
    "weather": "clear",
    "difficulty": "normal"
  },
  "updated_at": "2025-01-10T12:00:00Z"
}
```

## Session Management

### New Conversation
1. Generate new session ID
2. Save to `players/{uuid}/sessions/current.txt`
3. Archive previous session metadata to `history/`

### Continue Conversation
1. Read session ID from `current.txt`
2. Pass `--resume {session_id}` to Claude

### Previous Conversations (UI)
1. List `sessions/history/*.json`
2. Each contains: `{ session_id, started_at, summary, message_count }`
3. User can select to resume any previous session

## UI Components (LDLib2)

### 1. Chat Interface
- Message input field
- Scrollable message history
- Streaming text display (real-time)
- "New Conversation" button
- "Previous Conversations" dropdown/list

### 2. Scratch Pad
- Displays `scratch.md` content
- Editable by player
- Synced to server on change
- Claude can read/write this file

### 3. Pinnable Overlay
- Dock chat, notes, or files to screen edges
- Draggable, resizable panels
- Persist positions across sessions

### 4. Settings Screen
- Toggle AI features
- Configure keybinds
- View/clear conversation history

### 5. File Browser
- Browse `notes/` directory
- Create/edit/delete notes
- Pin notes to overlay

---

## Implementation Phases

### Phase 1: Foundation
- [ ] Delete Python backend (src/, tests/, pyproject.toml, etc.)
- [ ] Create new NeoForge 1.21 mod project structure
- [ ] Add LDLib2 dependency to build.gradle
- [ ] Basic mod entry point and registration

### Phase 2: Claude Code Integration
- [ ] Create `.claude/` directory structure
- [ ] Implement `settings.json` with write hook
- [ ] Create `validate_write.sh` hook script
- [ ] Create skills: position, inventory, nearby, world-info
- [ ] Create `bin/` scripts that read state.json
- [ ] Implement player directory creation on join
- [ ] Implement state.json writer (updates on player tick)
- [ ] Build ClaudeProcess wrapper class:
  - Subprocess spawning with environment vars
  - Stream JSON parsing
  - Session ID management

### Phase 3: Networking
- [ ] Define packets: ChatRequest, ChatStreamChunk, ChatComplete
- [ ] Server->Client: stream chunks, conversation list
- [ ] Client->Server: send message, new conversation, resume session

### Phase 4: LDLib2 UI
- [ ] Chat screen with streaming display
- [ ] New/Previous conversation controls
- [ ] Scratch pad viewer/editor
- [ ] Settings screen
- [ ] Keybind registration

### Phase 5: Overlay System
- [ ] Pinnable panel base class
- [ ] Chat overlay panel
- [ ] Notes overlay panel
- [ ] Position persistence

### Phase 6: Polish
- [ ] File browser for notes/
- [ ] Conversation search
- [ ] Error handling and timeouts
- [ ] Rate limiting
- [ ] Documentation

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Minecraft | 1.21.1 |
| Mod Loader | NeoForge |
| UI Library | LDLib2 2.1.5+ |
| AI Backend | Claude Code (headless subprocess) |
| Data Format | JSON (state), Markdown (notes/context) |
| IPC | File-based (state.json) |

## Commands

### In-Game
- `/ai` - Open chat UI
- `/ai scratch` - Open scratch pad
- `/ai settings` - Open settings

### Keybinds (configurable)
- `K` - Toggle chat overlay
- `L` - Toggle scratch pad overlay
