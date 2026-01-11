---
name: mc-get-nearby
description: Get nearby blocks and entities around the player in Minecraft. Use when asked about surroundings, what's nearby, mobs, or blocks around the player.
allowed-tools: Read
---

# Get Nearby Blocks and Entities

Read the player's state file to get information about their surroundings.

## Instructions

Use the Read tool to read the state file at:
```
players/${MINECRAFT_PLAYER_UUID}/state.json
```

The state file contains information about nearby blocks and entities.

## Nearby Fields

From the state.json file:

### `nearbyBlocks`
Array of notable blocks near the player:
- `id` - Block ID like "minecraft:diamond_ore"
- `x`, `y`, `z` - Block coordinates
- `distance` - Distance from player

### `nearbyEntities`
Array of entities near the player:
- `type` - Entity type like "minecraft:zombie"
- `name` - Display name (for named mobs/players)
- `x`, `y`, `z` - Entity position
- `distance` - Distance from player
- `health` - Current health (if applicable)

## Response Format

Summarize surroundings based on what the player asked:
- For "what's around me?" → List notable blocks and entities
- For "any mobs nearby?" → Focus on hostile/neutral mobs
- For "is there any iron ore?" → Check for specific blocks

## Examples

- "What's around me?" → Read state.json, describe surroundings
- "Any enemies nearby?" → Read state.json, report hostile mobs
- "Is there a village nearby?" → Read state.json, look for villagers
- "What ores are close?" → Read state.json, list ore blocks
