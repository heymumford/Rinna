#!/bin/bash
# Start a simplified documentation server

# Set the API directory environment variable
export RINNA_API_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Get port from argument or use default
PORT=${1:-8080}

echo "Starting Rinna API Documentation Server on port $PORT"
echo "Rinna: A clean, compact solution for product, project, development, and quality management!"
echo "API documentation will be available at http://localhost:$PORT/api/docs"

# Ensure the Swagger JSON is up to date
echo "Ensuring Swagger documentation is up to date..."
python3 "$RINNA_API_DIR/bin/sync-swagger.py"

# Create docs directory if it doesn't exist
mkdir -p "$RINNA_API_DIR/cmd/docs-server"

# Install required Go packages
echo "Installing dependencies..."
cd "$RINNA_API_DIR" && go get github.com/russross/blackfriday/v2

# Build and run the documentation server
echo "Building documentation server..."
cd "$RINNA_API_DIR" && go build -o "$RINNA_API_DIR/bin/docs-server" ./cmd/docs-server

# Run the server
echo "Starting server..."
"$RINNA_API_DIR/bin/docs-server" --port "$PORT"