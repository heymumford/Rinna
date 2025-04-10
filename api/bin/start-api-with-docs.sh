#!/bin/bash
# Start the API server with documentation features enabled

# Set the API directory environment variable
export RINNA_API_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export RINNA_LOG_LEVEL=DEBUG

# Get port from argument or use default
PORT=${1:-8080}

echo "Starting Rinna API server on port $PORT with documentation enabled"
echo "API documentation will be available at http://localhost:$PORT/api/docs"

# Ensure the Swagger JSON is up to date
echo "Ensuring Swagger documentation is up to date..."
python3 "$RINNA_API_DIR/bin/sync-swagger.py"

# Start the API server
"$RINNA_API_DIR/cmd/rinnasrv/rinnasrv" --port "$PORT"