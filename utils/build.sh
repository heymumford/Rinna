#!/usr/bin/env bash
#
# build.sh - Main entry point for Rinna build system
#
# This script is a lightweight wrapper around the build orchestrator
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# 
# This source code is licensed under the MIT License
# found in the LICENSE file in the root directory of this source tree.
#

# Determine script directory using a POSIX-compatible approach
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ORCHESTRATOR="$SCRIPT_DIR/build-orchestrator.sh"

# Forward to the orchestrator
if [[ -x "$ORCHESTRATOR" ]]; then
  exec "$ORCHESTRATOR" "$@"
else
  echo "ERROR: Build orchestrator not executable at $ORCHESTRATOR"
  exit 1
fi