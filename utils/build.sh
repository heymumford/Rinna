#!/usr/bin/env bash
#
# build.sh - Main entry point for Rinna build system
#
# This script is a simple wrapper around the unified build orchestrator
# in util/build-orchestrator.sh. It passes all arguments to the orchestrator.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# 
# This source code is licensed under the MIT License
# found in the LICENSE file in the root directory of this source tree.
#

# Determine script and project directories
SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(dirname "$SCRIPT_PATH")"
ORCHESTRATOR="$SCRIPT_DIR/util/build-orchestrator.sh"

# Check if the orchestrator exists
if [[ ! -f "$ORCHESTRATOR" ]]; then
  echo "ERROR: Build orchestrator not found at $ORCHESTRATOR"
  exit 1
fi

# Forward all arguments to the orchestrator
exec "$ORCHESTRATOR" "$@"