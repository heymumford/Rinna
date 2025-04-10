#!/bin/bash
# Test that the swagger documentation is working

# Set the API directory
API_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PORT=9090  # Use a non-standard port for testing

# Function to clean up
function cleanup {
  echo "Stopping server..."
  kill $PID
  exit
}

# Trap Ctrl+C to clean up
trap cleanup SIGINT SIGTERM

# Ensure docs are up to date
echo "Syncing Swagger documentation..."
python3 "$API_DIR/bin/sync-swagger.py"

# Start server in background
echo "Starting documentation server on port $PORT..."
echo "Rinna: A clean, compact solution for product, project, development, and quality management!"
"$API_DIR/bin/docs-server" --port $PORT &
PID=$!

# Wait for server to start
sleep 2

# Test endpoints
echo "Testing API documentation endpoints..."

# Test Swagger UI redirect
echo -n "  /api/docs -> "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/api/docs)
if [ "$STATUS" == "302" ]; then
  echo "OK (HTTP 302)"
else
  echo "FAIL (HTTP $STATUS)"
  cleanup
fi

# Test Swagger UI
echo -n "  /api/docs/swagger-ui/ -> "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/api/docs/swagger-ui/)
if [ "$STATUS" == "200" ]; then
  echo "OK (HTTP 200)"
else
  echo "FAIL (HTTP $STATUS)"
  cleanup
fi

# Test Swagger JSON
echo -n "  /api/docs/swagger.json -> "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/api/docs/swagger.json)
if [ "$STATUS" == "200" ]; then
  echo "OK (HTTP 200)"
else
  echo "FAIL (HTTP $STATUS)"
  cleanup
fi

# Test Swagger YAML
echo -n "  /api/docs/swagger.yaml -> "
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/api/docs/swagger.yaml)
if [ "$STATUS" == "200" ]; then
  echo "OK (HTTP 200)"
else
  echo "FAIL (HTTP $STATUS)"
  cleanup
fi

echo "All endpoints tested successfully!"
cleanup