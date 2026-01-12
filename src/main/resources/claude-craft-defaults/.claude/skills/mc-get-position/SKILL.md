---
name: mc-get-position
description: Get the player's current position, coordinates, location, dimension, and facing direction in Minecraft. Use when asked about where the player is, their coords, or location.
allowed-tools: Read
---

# Get Player Position

Read the player's state file to get their current position.

## Instructions

The player's UUID is available in the MINECRAFT_PLAYER_UUID environment variable.

Use the Read tool to read the state file. The path is:
`players/<player-uuid>/state.json`

Replace `<player-uuid>` with the value from the MINECRAFT_PLAYER_UUID environment variable.

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
