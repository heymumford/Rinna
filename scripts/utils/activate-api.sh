#!/usr/bin/env bash
# API environment setup for Rinna
#
# Set API environment variables
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export RINNA_API_DIR="$SCRIPT_DIR/api"
export PATH="$RINNA_API_DIR/bin:$PATH"
export GO111MODULE=on

# Print status
echo "Rinna API environment activated"
echo "API directory: $RINNA_API_DIR"