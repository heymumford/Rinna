#!/bin/bash
# Build and verify the API documentation
set -e

# Set the API directory environment variable
export RINNA_API_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RINNA_API_DIR"

echo "=== Rinna API Documentation Builder ==="
echo "Rinna: A clean, compact solution for product, project, development, and quality management!"
echo "Building comprehensive API documentation for Rinna API v1.6.6"

# Step 1: Ensure the swagger.yaml is valid
echo -e "\n--- Step 1: Validating Swagger YAML ---"
if [ ! -f "$RINNA_API_DIR/swagger.yaml" ]; then
  echo "Error: swagger.yaml not found at $RINNA_API_DIR/swagger.yaml"
  exit 1
fi

# Install dependencies
echo -e "\n--- Step 2: Installing dependencies ---"
command -v python3 >/dev/null 2>&1 || { echo "Python 3 is required but not installed. Aborting."; exit 1; }
pip3 install pyyaml >/dev/null 2>&1 || { echo "Failed to install PyYAML. Please install manually."; exit 1; }

# Step 3: Build the documentation
echo -e "\n--- Step 3: Synchronizing documentation formats ---"
python3 "$RINNA_API_DIR/bin/sync-swagger.py" --validate
if [ $? -ne 0 ]; then
  echo "Error: Swagger YAML validation failed"
  exit 1
fi

# Convert YAML to JSON
echo "Converting YAML to JSON"
python3 "$RINNA_API_DIR/bin/sync-swagger.py"
if [ $? -ne 0 ]; then
  echo "Error: Failed to convert Swagger YAML to JSON"
  exit 1
fi

# Step 4: Ensure Swagger UI is available
echo -e "\n--- Step 4: Verifying Swagger UI files ---"
mkdir -p "$RINNA_API_DIR/docs/swagger-ui"

if [ ! -f "$RINNA_API_DIR/docs/swagger-ui/index.html" ]; then
  echo "Error: Swagger UI index.html not found at $RINNA_API_DIR/docs/swagger-ui/index.html"
  exit 1
fi

if [ ! -f "$RINNA_API_DIR/docs/swagger-ui/oauth2-redirect.html" ]; then
  echo "Error: Swagger UI oauth2-redirect.html not found at $RINNA_API_DIR/docs/swagger-ui/oauth2-redirect.html"
  exit 1
fi

# Step 5: Ensure example and security documentation is available
echo -e "\n--- Step 5: Verifying documentation files ---"
if [ ! -f "$RINNA_API_DIR/docs/examples.md" ]; then
  echo "Error: API examples documentation not found at $RINNA_API_DIR/docs/examples.md"
  exit 1
fi

if [ ! -f "$RINNA_API_DIR/docs/security-guide.md" ]; then
  echo "Error: API security guide not found at $RINNA_API_DIR/docs/security-guide.md"
  exit 1
fi

# Step 6: Build the documentation server
echo -e "\n--- Step 6: Building the documentation server ---"
echo "Installing Go dependencies"
cd "$RINNA_API_DIR" && go get github.com/russross/blackfriday/v2

echo "Building documentation server"
cd "$RINNA_API_DIR" && go build -o "$RINNA_API_DIR/bin/docs-server" ./cmd/docs-server
if [ $? -ne 0 ]; then
  echo "Error: Failed to build documentation server"
  exit 1
fi

echo -e "\n--- Documentation Build Completed Successfully ---"
echo -e "Run the documentation server with:\n./bin/start-docs-server.sh\n"

echo -e "Documentation available at:\nhttp://localhost:8080/api/docs"
echo -e "Swagger UI available at:\nhttp://localhost:8080/api/docs/swagger-ui/"
echo -e "API Examples available at:\nhttp://localhost:8080/api/docs/examples"
echo -e "Security Guide available at:\nhttp://localhost:8080/api/docs/security-guide"