#!/bin/bash
# Validate bash commands for Minecraft AI
# - Only allow commands that execute scripts in bin/
# - Block all other bash commands
#
# Environment variables (set by the mod):
#   MINECRAFT_PLAYER_UUID - Current player's UUID
#   MINECRAFT_IS_OP - "true" if player is an operator

set -e

# Read hook input from stdin
INPUT=$(cat)

# Extract command from the tool input using grep/sed (no jq dependency)
# The input JSON has format: {"tool_input":{"command":"..."}}
COMMAND=$(echo "$INPUT" | grep -o '"command"[[:space:]]*:[[:space:]]*"[^"]*"' | sed 's/"command"[[:space:]]*:[[:space:]]*"//' | sed 's/"$//' | head -1)

if [ -z "$COMMAND" ]; then
    echo "Error: Could not determine command from tool input" >&2
    exit 2
fi

# Get the base directory
BASE_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"

# Allow patterns:
# - bash bin/*.sh
# - ./bin/*.sh
# - bin/*.sh (without bash prefix)
# Also allow the scripts to receive arguments

# Check if the command is calling a bin/ script
if [[ "$COMMAND" =~ ^(bash[[:space:]]+)?(\.\/)?bin\/[a-zA-Z0-9_-]+\.sh([[:space:]]|$) ]]; then
    # Extract the script name
    SCRIPT=$(echo "$COMMAND" | grep -oE 'bin/[a-zA-Z0-9_-]+\.sh' | head -1)

    # Verify the script exists
    if [ -f "$BASE_DIR/$SCRIPT" ]; then
        exit 0
    else
        echo "BLOCKED: Script does not exist: $SCRIPT" >&2
        exit 2
    fi
fi

# Block everything else
echo "BLOCKED: Only bin/*.sh scripts are allowed. Use skills to access Minecraft data." >&2
exit 2
