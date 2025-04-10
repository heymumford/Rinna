#!/bin/bash
#
# XML Cleanup Scheduler for Rinna
#
# This script tracks build count and runs XML cleanup every N builds.
# It's designed to be called from the main build script.
#
# Usage: ./bin/xml-tools/xml-cleanup-scheduler.sh [--force]
#

# Set script to exit on error
set -e

# Define constants
CLEANUP_FREQUENCY=10
COUNTER_FILE=".rinna-build-tracking/xml-cleanup-counter"
PROJECT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || echo "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)")"
FORCE_CLEANUP=false

# Parse command line arguments
for arg in "$@"; do
    case $arg in
        --force)
            FORCE_CLEANUP=true
            shift
            ;;
        --help)
            echo "Usage: $0 [--force]"
            echo ""
            echo "Options:"
            echo "  --force    Force XML cleanup regardless of counter"
            echo "  --help     Show this help message"
            exit 0
            ;;
    esac
done

# Source common utilities if available
if [ -f "$PROJECT_ROOT/bin/common/rinna_logger.sh" ]; then
    source "$PROJECT_ROOT/bin/common/rinna_logger.sh"
else
    # Simple logging functions if common utilities are not available
    log_info() { echo -e "\033[0;34m[INFO]\033[0m $1"; }
    log_success() { echo -e "\033[0;32m[SUCCESS]\033[0m $1"; }
    log_warning() { echo -e "\033[0;33m[WARNING]\033[0m $1"; }
    log_error() { echo -e "\033[0;31m[ERROR]\033[0m $1"; }
fi

# Create counter file if it doesn't exist
if [ ! -f "$PROJECT_ROOT/$COUNTER_FILE" ]; then
    mkdir -p "$PROJECT_ROOT/.rinna-build-tracking"
    echo "0" > "$PROJECT_ROOT/$COUNTER_FILE"
    log_info "Created build counter file"
fi

# Read current build count
CURRENT_COUNT=$(cat "$PROJECT_ROOT/$COUNTER_FILE")

# Increment counter
NEW_COUNT=$((CURRENT_COUNT + 1))
echo "$NEW_COUNT" > "$PROJECT_ROOT/$COUNTER_FILE"

log_info "Build count: $NEW_COUNT (cleanup every $CLEANUP_FREQUENCY builds)"

# Determine if cleanup should run
if [ "$FORCE_CLEANUP" = true ]; then
    log_info "Forcing XML cleanup..."
    # Run the tag fixer first to handle the n tag issue
    if [ -f "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh" ]; then
        "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh"
    fi
    # Then run the full cleanup
    "$PROJECT_ROOT/bin/xml-tools/xml-cleanup.sh"
    log_success "XML cleanup completed (forced)"
    exit 0
fi

# Run cleanup every X builds
if [ $((NEW_COUNT % CLEANUP_FREQUENCY)) -eq 0 ]; then
    log_info "Running scheduled XML cleanup (build count: $NEW_COUNT)..."
    # Run the tag fixer first to handle the n tag issue
    if [ -f "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh" ]; then
        "$PROJECT_ROOT/bin/xml-tools/pom-n-tag-fixer.sh"
    fi
    # Then run the full cleanup
    "$PROJECT_ROOT/bin/xml-tools/xml-cleanup.sh"
    log_success "Scheduled XML cleanup completed"
else
    log_info "XML cleanup scheduled at build count $((CLEANUP_FREQUENCY - NEW_COUNT % CLEANUP_FREQUENCY)) builds from now"
fi

exit 0