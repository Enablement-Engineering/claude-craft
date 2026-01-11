# Minecraft AI

An AI-powered assistant for Minecraft using Claude Code in headless mode. Chat with Claude directly in-game, save notes and memories, and get help with your Minecraft adventures.

## Features

- **In-game AI Chat** - Talk to Claude directly from Minecraft
- **Persistent Memory** - Claude remembers your conversations and notes
- **Game State Awareness** - Claude can see your coordinates, inventory, and surroundings
- **Scratch Pad** - A shared notepad between you and Claude
- **Pinnable Overlays** - Dock chat and notes to your screen
- **Per-player Files** - Each player has their own private AI context

## Tech Stack

| Component | Version |
|-----------|---------|
| Minecraft | 1.21.1 |
| Mod Loader | NeoForge |
| UI Library | LDLib2 2.1.5+ |
| AI Backend | Claude Code (headless) |
| Java | 21 |

## Prerequisites

- **Java 21** (required)
  ```bash
  # macOS with Homebrew
  brew install openjdk@21

  # Or use SDKMAN
  sdk install java 21.0.5-zulu
  ```

- **Claude Code CLI** installed and authenticated
  ```bash
  # Verify Claude Code is working
  claude -p "Hello" --output-format json
  ```

## Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/minecraft-ai.git
   cd minecraft-ai
   ```

2. **Set Java 21**
   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 21)
   ```

3. **Build the mod**
   ```bash
   ./gradlew build
   ```

4. **Install the mod**
   - Copy `build/libs/minecraftai-*.jar` to your Minecraft mods folder
   - Also install LDLib2 from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ldlib) or [Modrinth](https://modrinth.com/mod/ldlib)

5. **Run Minecraft with NeoForge 1.21.1**

## Development

```bash
# Run client with mod loaded (for testing)
./gradlew runClient

# Run server with mod loaded
./gradlew runServer

# Clean build
./gradlew clean build
```

## Architecture

The mod runs Claude Code as a subprocess on the server:

```
Minecraft Server
├── Mod (NeoForge + LDLib2)
│   ├── Spawns Claude Code subprocess per chat
│   ├── Streams responses to client
│   └── Updates game state files
└── plugins/minecraft-ai/
    ├── .claude/           # Skills and hooks
    ├── CLAUDE.md          # Server context
    └── players/{uuid}/    # Per-player files
        ├── CLAUDE.md      # Player context
        ├── scratch.md     # Shared notepad
        ├── state.json     # Game state
        └── notes/         # Saved notes
```

## Security

- Claude can only write `.md` and `.json` files
- Non-OP players can only write to their own directory
- OPs can write anywhere in the plugin folder
- Bash commands restricted to approved scripts in `bin/`

## Documentation

- [PLAN.md](./PLAN.md) - Full implementation plan
- [CLAUDE.md](./CLAUDE.md) - Claude Code guidance
- [LDLib2 Docs](https://low-drag-mc.github.io/LowDragMC-Doc/ldlib2/) - UI framework

## License

MIT
