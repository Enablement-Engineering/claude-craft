---
name: mc-get-position
description: Get the player's current position, coordinates, location, dimension, and facing direction in Minecraft. Use when asked about where the player is, their coords, or location.
allowed-tools: Read
---

# Get Player Position

Read the player's state file to get their current position.

## Instructions

Use the Read tool to read the state file at:
```
players/${MINECRAFT_PLAYER_UUID}/state.json
```

The state file contains the player's current game state including position.

## Position Fields

From the state.json file, extract:
- `position.x`, `position.y`, `position.z` - Block coordinates
- `dimension` - Current dimension (minecraft:overworld, minecraft:the_nether, minecraft:the_end)
- `rotation.yaw` - Horizontal rotation (0-360, 0=south, 90=west)
- `rotation.pitch` - Vertical rotation (-90=up, 90=down)
- `biome` - Current biome

## Response Format

After reading the file, summarize the position naturally, e.g.:
"You're at coordinates X: -141, Y: 72, Z: 443 in the Overworld (stony_shore biome), facing southwest."

## Examples

- "Where am I?" → Read state.json, report coordinates and dimension
- "What are my coordinates?" → Read state.json, report x/y/z
- "What dimension am I in?" → Read state.json, report dimension
- "Save my current location" → Read state.json, then write to notes/
