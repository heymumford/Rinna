#!/bin/bash
#
# generate-swagger-simple.sh - Generate Swagger/OpenAPI Documentation
#
# This is a simplified version of the script that should work without dependencies

# Set up directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# Define API directory
API_DIR="$PROJECT_ROOT/api"
SWAGGER_FILE="$API_DIR/swagger.yaml"
OUTPUT_DIR="$API_DIR/docs"

# Ensure output directory exists
mkdir -p "$OUTPUT_DIR"

# Check if swagger file exists
if [[ ! -f "$SWAGGER_FILE" ]]; then
  echo "ERROR: Swagger file not found: $SWAGGER_FILE"
  exit 1
fi

echo "== Swagger/OpenAPI Documentation Generator =="
echo "→ Validating Swagger file: $SWAGGER_FILE"

# Basic YAML validation
if command -v python3 &> /dev/null; then
  if python3 -c "import yaml; yaml.safe_load(open('$SWAGGER_FILE'))" 2>/dev/null; then
    echo "✓ Basic YAML validation passed"
  else
    echo "✗ YAML validation failed - invalid YAML format"
    exit 1
  fi
else
  echo "⚠️ No YAML validator found. Skipping validation."
fi

# Copy YAML file to docs directory
echo "→ Generating YAML documentation"
cp "$SWAGGER_FILE" "$OUTPUT_DIR/swagger.yaml"
echo "✓ Generated YAML documentation: $OUTPUT_DIR/swagger.yaml"

# Generate JSON if Python is available
if command -v python3 &> /dev/null; then
  echo "→ Converting Swagger to JSON format"
  python3 -c "import yaml, json, sys; json.dump(yaml.safe_load(open('$SWAGGER_FILE')), open('$OUTPUT_DIR/swagger.json', 'w'), indent=2)" 2>/dev/null
  if [[ $? -eq 0 ]]; then
    echo "✓ Generated JSON documentation: $OUTPUT_DIR/swagger.json"
  else
    echo "✗ Failed to convert to JSON format"
  fi
else
  echo "⚠️ Python3 required for JSON conversion. Skipping."
fi

echo "✅ Swagger documentation process completed"
echo "Documentation files generated in: $OUTPUT_DIR"

# Set execute permissions
chmod +x "$0"