#!/bin/bash
# Validate write operations for Claude Craft
# - Only allow .md and .json files
# - Non-OPs can only write to their own player directory
# - OPs can write anywhere in the plugin folder
#
# Environment variables (set by the mod):
#   MINECRAFT_PLAYER_UUID - Current player's UUID
#   MINECRAFT_IS_OP - "true" if player is an operator

set -e

# Read hook input from stdin
INPUT=$(cat)

# Extract file path from the tool input using grep/sed (no jq dependency)
# Try file_path first, then filePath
FILE_PATH=$(echo "$INPUT" | grep -o '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | sed 's/"file_path"[[:space:]]*:[[:space:]]*"//' | sed 's/"$//' | head -1)
if [ -z "$FILE_PATH" ]; then
    FILE_PATH=$(echo "$INPUT" | grep -o '"filePath"[[:space:]]*:[[:space:]]*"[^"]*"' | sed 's/"filePath"[[:space:]]*:[[:space:]]*"//' | sed 's/"$//' | head -1)
fi

if [ -z "$FILE_PATH" ]; then
    echo "Error: Could not determine file path from tool input" >&2
    exit 2
fi

# Get the base directory (CLAUDE_PROJECT_DIR should be set to plugins/claude-craft)
BASE_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
PLAYER_UUID="${MINECRAFT_PLAYER_UUID:-unknown}"
IS_OP="${MINECRAFT_IS_OP:-false}"

# Resolve to absolute path if relative
if [[ "$FILE_PATH" != /* ]]; then
    FILE_PATH="$BASE_DIR/$FILE_PATH"
fi

# Normalize the path (remove . and ..)
FILE_PATH=$(cd "$(dirname "$FILE_PATH")" 2>/dev/null && pwd)/$(basename "$FILE_PATH") 2>/dev/null || FILE_PATH="$FILE_PATH"

# Check 1: File extension must be .md or .json
if [[ ! "$FILE_PATH" =~ \.(md|json)$ ]]; then
    echo "BLOCKED: Only .md and .json files are allowed. Got: $(basename "$FILE_PATH")" >&2
    exit 2
fi

# Check 2: File must be within the plugin directory
if [[ "$FILE_PATH" != "$BASE_DIR"/* ]]; then
    echo "BLOCKED: Cannot write outside plugin directory" >&2
    exit 2
fi

# Check 3: Non-OPs can only write to their own player directory
if [ "$IS_OP" != "true" ]; then
    PLAYER_DIR="$BASE_DIR/players/$PLAYER_UUID"
    if [[ "$FILE_PATH" != "$PLAYER_DIR"/* ]]; then
        echo "BLOCKED: You can only write to your own player directory: players/$PLAYER_UUID/" >&2
        exit 2
    fi
fi

# All checks passed
exit 0
