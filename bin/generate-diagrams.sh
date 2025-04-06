#!/usr/bin/env bash
# Script for generating C4 diagrams asynchronously during the build process
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
#
# Usage:
#   ./bin/generate-diagrams.sh [--async] [--format png|svg|pdf] [--type context|container|component|code|all]
#
# Options:
#   --async      Generate diagrams asynchronously (default: false)
#   --format     Output format for diagrams (default: svg)
#   --type       Type of diagrams to generate (default: all)
#   --output-dir Output directory for diagrams (default: docs/diagrams)
#   --clean      Clean existing diagrams before generating new ones
#   --help       Show this help message

set -e

# Determine project root
PROJECT_ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$PROJECT_ROOT"

# Default options
ASYNC=false
FORMAT="svg"
TYPE="all"
OUTPUT_DIR="docs/diagrams"
CLEAN=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --async)
      ASYNC=true
      shift
      ;;
    --format)
      FORMAT="$2"
      shift 2
      ;;
    --type)
      TYPE="$2"
      shift 2
      ;;
    --output-dir)
      OUTPUT_DIR="$2"
      shift 2
      ;;
    --clean)
      CLEAN=true
      shift
      ;;
    --help)
      echo "Usage: ./bin/generate-diagrams.sh [--async] [--format png|svg|pdf] [--type context|container|component|code|clean|all] [--output-dir path] [--clean]"
      echo ""
      echo "Options:"
      echo "  --async      Generate diagrams asynchronously (default: false)"
      echo "  --format     Output format for diagrams (default: svg)"
      echo "  --type       Type of diagrams to generate:"
      echo "               - context:   System context diagram"
      echo "               - container: Container diagram showing major components"
      echo "               - component: Component diagram showing system structure"
      echo "               - code:      Code diagram showing key classes"
      echo "               - clean:     Clean Architecture diagram showing layers"
      echo "               - all:       Generate all diagram types (default)"
      echo "  --output-dir Output directory for diagrams (default: docs/diagrams)"
      echo "  --clean      Clean existing diagrams before generating new ones"
      echo "  --help       Show this help message"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Use --help to see available options"
      exit 1
      ;;
  esac
done

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Clean existing diagrams if requested
if [[ "$CLEAN" == "true" ]]; then
  echo "Cleaning existing diagrams in $OUTPUT_DIR..."
  rm -f "$OUTPUT_DIR"/*.png "$OUTPUT_DIR"/*.svg "$OUTPUT_DIR"/*.pdf
fi

# Create a lock file to prevent concurrent generation
LOCK_FILE="${OUTPUT_DIR}/.diagram-generation.lock"

# Function to generate diagrams
function generate_diagrams() {
  # Check for Python environment
  if [[ -f "${PROJECT_ROOT}/activate-python.sh" ]]; then
    source "${PROJECT_ROOT}/activate-python.sh"
  fi

  # Check for diagram generator script
  if [[ ! -f "${PROJECT_ROOT}/bin/c4_diagrams.py" ]]; then
    echo "Error: C4 diagram generator script not found at ${PROJECT_ROOT}/bin/c4_diagrams.py"
    return 1
  fi

  # Create timestamp file to indicate generation in progress
  TIMESTAMP=$(date +%Y%m%d%H%M%S)
  touch "${OUTPUT_DIR}/.generating-${TIMESTAMP}"

  # Acquire lock
  if ! mkdir "$LOCK_FILE" 2>/dev/null; then
    echo "Warning: Another diagram generation process is already running. Skipping."
    return 0
  fi

  # Generate diagrams
  echo "Generating $TYPE C4 diagrams in $FORMAT format..."
  python "${PROJECT_ROOT}/bin/c4_diagrams.py" --type "$TYPE" --output "$FORMAT" --dir "$OUTPUT_DIR"
  
  # Create markdown documentation that includes the diagrams
  generate_diagram_documentation
  
  # Release lock
  rmdir "$LOCK_FILE"
  
  # Remove timestamp file
  rm -f "${OUTPUT_DIR}/.generating-${TIMESTAMP}"
  
  # Create a marker file to indicate when diagrams were last generated
  date > "${OUTPUT_DIR}/.last-generated"
  
  echo "Diagram generation completed successfully."
}

# Function to generate markdown documentation for diagrams
function generate_diagram_documentation() {
  DOC_FILE="${OUTPUT_DIR}/README.md"
  
  echo "# Rinna C4 Architecture Diagrams" > "$DOC_FILE"
  echo "" >> "$DOC_FILE"
  echo "Last generated: $(date)" >> "$DOC_FILE"
  echo "" >> "$DOC_FILE"
  echo "This directory contains automatically generated C4 model diagrams for the Rinna project." >> "$DOC_FILE"
  echo "" >> "$DOC_FILE"
  
  # Context diagram
  if [[ -f "${OUTPUT_DIR}/rinna_context_diagram.${FORMAT}" ]]; then
    echo "## Context Diagram" >> "$DOC_FILE"
    echo "" >> "$DOC_FILE"
    echo "The context diagram shows the high-level system context and external dependencies:" >> "$DOC_FILE"
    echo "" >> "$DOC_FILE"
    echo "![Context Diagram](./rinna_context_diagram.${FORMAT})" >> "$DOC_FILE"
    echo "" >> "$DOC_FILE"
  fi
  
  # Container diagram
  if [[ -f "${OUTPUT_DIR}/rinna_container_diagram.${FORMAT}" ]]; then
    echo "## Container Diagram" >> "$DOC_FILE"
    echo "" >> "$DOC_FILE"
    echo "The container diagram shows the major components and their relationships:" >> "$DOC_FILE"
    echo "" >> "$DOC_FILE"
    echo "![Container Diagram](./rinna_container_diagram.${FORMAT})" >> "$DOC_FILE"
    echo "" >> "$DOC_FILE"
  fi
  
  # Component diagram
  if [[ -f "${OUTPUT_DIR}/rinna_component_diagram.${FORMAT}" ]]; then
    echo "## Component Diagram" >> "$DOC_FILE"
    echo "" >> "$DOC_FILE"
    echo "The component diagram shows the internal components of the system:" >> "$DOC_FILE"
    echo "" >> "$DOC_FILE"
    echo "![Component Diagram](./rinna_component_diagram.${FORMAT})" >> "$DOC_FILE"
    echo "" >> "$DOC_FILE"
  fi
  
  # Code diagram
  if [[ -f "${OUTPUT_DIR}/rinna_code_diagram.${FORMAT}" ]]; then
    echo "## Code Diagram" >> "$DOC_FILE"
    echo "" >> "$DOC_FILE"
    echo "The code diagram shows the key classes and their relationships:" >> "$DOC_FILE"
    echo "" >> "$DOC_FILE"
    echo "![Code Diagram](./rinna_code_diagram.${FORMAT})" >> "$DOC_FILE"
    echo "" >> "$DOC_FILE"
  fi
  
  echo "## Diagram Generation" >> "$DOC_FILE"
  echo "" >> "$DOC_FILE"
  echo "These diagrams are automatically generated during the build process. To manually regenerate them, run:" >> "$DOC_FILE"
  echo "" >> "$DOC_FILE"
  echo '```' >> "$DOC_FILE"
  echo './bin/generate-diagrams.sh' >> "$DOC_FILE"
  echo '```' >> "$DOC_FILE"
  echo "" >> "$DOC_FILE"
  echo "For more options, run:" >> "$DOC_FILE"
  echo "" >> "$DOC_FILE"
  echo '```' >> "$DOC_FILE"
  echo './bin/generate-diagrams.sh --help' >> "$DOC_FILE"
  echo '```' >> "$DOC_FILE"
  
  echo "Generated diagram documentation: $DOC_FILE"
}

# Check if we should run asynchronously
if [[ "$ASYNC" == "true" ]]; then
  # Run the diagram generation in the background
  echo "Starting asynchronous diagram generation..."
  (generate_diagrams > "${OUTPUT_DIR}/diagram-generation.log" 2>&1 &)
  echo "Diagram generation running in background. Check ${OUTPUT_DIR}/diagram-generation.log for progress."
else
  # Run synchronously
  generate_diagrams
fi