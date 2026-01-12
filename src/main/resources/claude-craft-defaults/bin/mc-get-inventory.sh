#!/bin/bash
# Get player inventory from state file
# Reads from players/{uuid}/state.json written by the Minecraft mod
# Outputs the full state - Claude can extract inventory fields

set -e

BASE_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
PLAYER_UUID="${MINECRAFT_PLAYER_UUID:-unknown}"
STATE_FILE="$BASE_DIR/players/$PLAYER_UUID/state.json"

if [ ! -f "$STATE_FILE" ]; then
    echo "Error: State file not found at $STATE_FILE" >&2
    echo "Make sure you are in-game." >&2
    exit 1
fi

# Output the state file - Claude will extract inventory data
cat "$STATE_FILE"
