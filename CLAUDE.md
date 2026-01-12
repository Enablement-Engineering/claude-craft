# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Claude Craft is a NeoForge mod that integrates Claude Code (headless mode) directly into Minecraft. Players can chat with an AI assistant that has access to game state and can save notes, plans, and memories.

**Tech Stack:**
- Minecraft 1.21.1
- NeoForge (mod loader)
- LDLib2 (UI framework)
- Claude Code (AI backend via subprocess)
- Java 21

## Prerequisites

- **Java 21** (required - Java 25 is not yet supported by Gradle 8.x)
  ```bash
  # macOS with Homebrew
  brew install openjdk@21
  export JAVA_HOME=$(/usr/libexec/java_home -v 21)

  # Or use SDKMAN
  sdk install java 21.0.5-zulu
  sdk use java 21.0.5-zulu
  ```

## Common Commands

```bash
# Build the mod (requires Java 21)
./gradlew build
# Output: build/libs/claudecraft-*.jar

# Run Minecraft client with mod loaded
./gradlew runClient

# Run dedicated server with mod loaded
./gradlew runServer

# Clean build artifacts
./gradlew clean

# Refresh dependencies
./gradlew --refresh-dependencies
```

## Keybinds

| Key | Action |
|-----|--------|
| `\` (backslash) | Open AI Chat screen |
| `'` (apostrophe) | Open Scratch Pad screen |
| `-` (minus) | Toggle Chat overlay (pinned HUD) |
| `=` (equals) | Toggle Scratch Pad overlay (pinned HUD) |
| `0` (zero) | Hide all overlays |

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│              Minecraft Server (NeoForge)                │
│  ┌─────────────────┐    ┌───────────────────────────┐  │
│  │  Mod + LDLib2   │───▶│  Claude Code subprocess   │  │
│  │                 │◀───│  --output-format stream   │  │
│  └────────┬────────┘    └───────────────────────────┘  │
│           │                        │                    │
│           ▼                        ▼                    │
│  ┌──────────────┐    ┌─────────────────────────────┐   │
│  │ State Files  │    │ .claude/ (skills, hooks)   │   │
│  │ state.json   │    │ players/{uuid}/ (files)    │   │
│  └──────────────┘    └─────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### Key Components

- `src/main/java/engineering/enablement/claudecraft/ClaudeCraft.java` - Mod entry point
- `src/main/resources/META-INF/neoforge.mods.toml` - Mod metadata
- `plugins/claude-craft/` - Runtime data directory (created by mod)
  - `.claude/` - Claude Code skills and hooks
  - `players/{uuid}/` - Per-player files (CLAUDE.md, scratch.md, notes/)

### Claude Code Integration

The mod invokes Claude Code as a subprocess:
```bash
claude -p "message" \
  --output-format stream-json \
  --allowedTools "Read,Write,Bash(bin/*)"
```

Environment variables passed:
- `MINECRAFT_PLAYER_UUID` - Current player's UUID
- `MINECRAFT_IS_OP` - Whether player is an operator

### Skills (Minecraft Tools)

Located in `plugins/claude-craft/.claude/skills/`:
- `mc-get-position/` - Get player coordinates
- `mc-get-inventory/` - Get inventory contents
- `mc-get-nearby/` - Get nearby blocks/entities
- `mc-get-world-info/` - Get world time, weather, etc.

### Security

Write hook restricts Claude to:
- Only `.md` and `.json` files
- Only player's own directory (unless OP)

## UI System

### LDLib2 Screens

Full-screen interfaces using LDLib2's UI framework:
- **AIChatScreen** - Chat interface with streaming responses, new/history buttons
- **ScratchPadScreen** - Multi-line text editor with save/clear

See: https://low-drag-mc.github.io/LowDragMC-Doc/ldlib2/

### HUD Overlays

Pinnable panels rendered on the HUD during gameplay:
- **Chat Overlay** - Shows recent AI responses, streaming status
- **Scratch Pad Overlay** - Read-only view of scratch pad content

Overlays are managed by `OverlayManager` and rendered via `OverlayRenderer` using NeoForge's `RenderGuiLayerEvent`.

## Project Structure

```
src/main/java/engineering/enablement/claudecraft/
├── ClaudeCraft.java           # Mod entry point
├── claude/                    # Claude Code integration
│   ├── ClaudeProcess.java     # Subprocess wrapper
│   ├── ClaudeProcessTracker.java # Process lifecycle tracking
│   ├── ClaudeSessionReader.java # Session file parsing
│   └── ClaudeStreamEvent.java # Stream JSON parsing
├── data/
│   └── PlayerDataManager.java # Player directory & state management
├── network/                   # Client-server packets
│   ├── ModNetworking.java     # Packet registration
│   ├── Serverbound*Packet.java # Client → Server packets
│   ├── Clientbound*Packet.java # Server → Client packets
│   ├── ChatSessionManager.java # Conversation persistence
│   └── ClientChatHandler.java  # Client-side state
└── ui/
    ├── ModKeybinds.java       # Keybind registration
    ├── ClientEvents.java      # Client tick handler
    ├── AIChatScreen.java      # LDLib2 chat screen
    ├── ScratchPadScreen.java  # LDLib2 scratch pad screen
    └── overlay/
        ├── OverlayManager.java  # Overlay state management
        └── OverlayRenderer.java # HUD rendering

src/main/resources/
├── META-INF/neoforge.mods.toml
├── assets/claudecraft/lang/en_us.json  # Translations
└── claude-craft-defaults/     # Default files copied to plugin dir
    ├── .claude/               # Skills and hooks
    ├── bin/                   # Shell scripts for skills
    └── player-template/       # Template for new players
```
