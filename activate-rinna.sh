#!/usr/bin/env bash
#
# activate-rinna.sh - Master activation script for Rinna environment
#
# Save current directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export RINNA_DIR="$SCRIPT_DIR"
export RINNA_VERSION="1.7.0"

# Function to load component if available
load_component() {
  local script="$1"
  local name="$2"

  if [[ -f "$script" ]]; then
    echo "Loading $name environment..."
    source "$script"
    return 0
  fi
  return 1
}

# Clear any existing Rinna environment
# This prevents stacking environments if sourced multiple times
if [[ -n "$RINNA_ENV_ACTIVE" ]]; then
  echo "Refreshing Rinna environment..."
  # If we had a Python venv activated, deactivate it
  if [[ -n "$VIRTUAL_ENV" ]]; then
    deactivate 2>/dev/null || true
  fi
fi

# Load component configurations
load_component "$RINNA_DIR/activate-java.sh" "Java"
load_component "$RINNA_DIR/activate-go.sh" "Go"

# Python environment - prefer venv but fall back to system
if [[ -f "$RINNA_DIR/activate-python.sh" ]]; then
  load_component "$RINNA_DIR/activate-python.sh" "Python virtual environment"
else
  load_component "$RINNA_DIR/activate-system-python.sh" "System Python"
fi

# Mark environment as active
export RINNA_ENV_ACTIVE="true"

# Add bin directory to path
export PATH="$RINNA_DIR/bin:$PATH"

# Display environment information
echo ""
echo "Rinna environment activated (version $RINNA_VERSION)"
echo "Run 'deactivate-rinna' to exit this environment"

# Define deactivation function
deactivate-rinna() {
  if [[ -n "$VIRTUAL_ENV" ]]; then
    deactivate
  fi

  # Restore PATH (remove our bin directory)
  export PATH="${PATH//$RINNA_DIR\/bin:/}"

  # Unset variables
  unset RINNA_ENV_ACTIVE
  unset RINNA_DIR
  unset deactivate-rinna

  echo "Rinna environment deactivated"
}