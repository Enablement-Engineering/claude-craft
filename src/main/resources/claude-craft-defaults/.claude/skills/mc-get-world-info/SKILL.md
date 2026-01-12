---
name: mc-get-world-info
description: Get world information like time, weather, difficulty, and server details in Minecraft. Use when asked about time of day, weather conditions, or world settings.
allowed-tools: Glob,Read
---

# Get World Information

Read the player's state file to get world information.

## Instructions

1. Use Glob with pattern `players/*/state.json` to find the state file
2. Read the state.json file that Glob finds

## World Fields

From the state.json file, the `world` object contains:
- `time` - Current game time (0-24000, where 0=dawn, 6000=noon, 12000=dusk, 18000=midnight)
- `dayTime` - Human-readable time period ("day", "night", "dawn", "dusk")
- `weather` - Current weather ("clear", "rain", "thunder")
- `difficulty` - Game difficulty ("peaceful", "easy", "normal", "hard")
- `gameMode` - Player's game mode ("survival", "creative", "adventure", "spectator")
- `day` - Current day number

## Response Format

Summarize world info based on what the player asked:
- For "what time is it?" → Report time of day naturally
- For "is it raining?" → Report weather
- For "what day is it?" → Report day number

## Time Reference

- 0-999: Dawn (6:00 AM)
- 1000-5999: Morning
- 6000: Noon (12:00 PM)
- 6001-11999: Afternoon
- 12000-12999: Dusk (6:00 PM)
- 13000-17999: Evening
- 18000: Midnight (12:00 AM)
- 18001-22999: Night
- 23000-23999: Pre-dawn

## Examples

- "What time is it?" → Read state.json, report time naturally
- "Is it safe to go out?" → Read state.json, check time and weather
- "What's the weather like?" → Read state.json, report weather
- "What difficulty are we on?" → Read state.json, report difficulty
