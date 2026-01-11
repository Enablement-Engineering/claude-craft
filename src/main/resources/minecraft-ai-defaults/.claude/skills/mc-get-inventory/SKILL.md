---
name: mc-get-inventory
description: Get the player's current inventory contents, items, hotbar, armor, and offhand in Minecraft. Use when asked about what items the player has, their inventory, or equipment.
allowed-tools: Read
---

# Get Player Inventory

Read the player's state file to get their current inventory.

## Instructions

Use the Read tool to read the state file at:
```
players/${MINECRAFT_PLAYER_UUID}/state.json
```

The state file contains the player's current inventory.

## Inventory Fields

From the state.json file, the `inventory` object contains:
- `hotbar` - Array of 9 items (slots 0-8, currently selected slot)
- `main` - Array of 27 items (main inventory)
- `armor` - Object with `head`, `chest`, `legs`, `feet`
- `offhand` - Single item slot

Each item has:
- `id` - Item ID like "minecraft:diamond_pickaxe"
- `count` - Stack size
- `slot` - Slot index

Empty slots are `null`.

## Response Format

Summarize the inventory naturally, focusing on what the player asked about:
- For "what do I have?" → List notable items
- For "do I have diamonds?" → Check for specific item
- For "what armor am I wearing?" → Report armor slots

## Examples

- "What's in my inventory?" → Read state.json, summarize items
- "Do I have any food?" → Read state.json, look for food items
- "What armor am I wearing?" → Read state.json, report armor slots
- "How many torches do I have?" → Read state.json, count torches
