#!/bin/bash
#
# sync-versions.sh - Wrapper script to synchronize versions across project components
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)
#

set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Ensure version tools are executable
chmod +x "${SCRIPT_DIR}/version-tools/version-validator.sh"
chmod +x "${SCRIPT_DIR}/version-tools/version-sync.sh"

# Run the version synchronizer with all arguments passed to this script
"${SCRIPT_DIR}/version-tools/version-sync.sh" "$@"