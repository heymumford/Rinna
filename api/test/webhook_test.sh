#!/bin/bash
# Test script for webhook functionality

set -e  # Exit on error

# Create a temp directory for our config and logs
TEST_DIR=$(mktemp -d)
CONFIG_DIR="$TEST_DIR/configs"
mkdir -p "$CONFIG_DIR"

echo "Using test directory: $TEST_DIR"

# Create a test config file
cat > "$CONFIG_DIR/config.yaml" << 'CONFIG'
# Rinna API Server Configuration

project:
  name: "Rinna"
  version: "1.3.0"
  environment: "development"

server:
  port: 9080
  host: "localhost"
  shutdownTimeout: 15

java:
  command: "java"
  host: "localhost"
  port: 8081
  connectTimeout: 5000
  requestTimeout: 30000
  # API endpoints
  endpoints:
    workitems: "/api/workitems"
    health: "/health"
    webhook_secret: "/api/webhooks/secret"

logging:
  level: "debug"  # Use debug for more detailed logs
  format: "text"  # Use text for easier reading
  file: ""

auth:
  tokenSecret: "rinna-development-secret-key"
  tokenExpiry: 1440  # 1 day in minutes
  secretExpiry: 60   # 1 hour in minutes
  devMode: true      # Enable development mode for testing
  allowedOrigins:
    - "http://localhost:3000"
    - "http://localhost:8080"
CONFIG

# Function to clean up resources
cleanup() {
  echo "Cleaning up..."
  if [ -n "$SERVER_PID" ] && ps -p $SERVER_PID > /dev/null; then
    echo "Stopping server (PID: $SERVER_PID)..."
    kill $SERVER_PID
  fi
  echo "Removing test directory: $TEST_DIR"
  rm -rf "$TEST_DIR"
}

# Set up trap to ensure resources are cleaned up
trap cleanup EXIT

echo "Starting test server..."

# Start the server in the background with the test config
./rinnasrv --config "$CONFIG_DIR/config.yaml" --port 9080 > "$TEST_DIR/server.log" 2>&1 &
SERVER_PID=$!

# Wait for the server to start
echo "Waiting for server to start (PID: $SERVER_PID)..."
sleep 3

# Check if server started successfully
if ! ps -p $SERVER_PID > /dev/null; then
  echo "Error: Server failed to start"
  cat "$TEST_DIR/server.log"
  exit 1
fi

# Create a payload and compute signature for the GitHub webhook
PAYLOAD='{"zen": "Test ping", "hook_id": 123456, "hook": {"type": "ping", "id": 123456}}'
SECRET="gh-webhook-secret-1234"  # Dev mode secret
SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$SECRET" | cut -d' ' -f2)

# Test the backward compatible webhook endpoint
echo -e "\n===================="
echo "Testing backward compatible webhook endpoint"
echo "===================="
RESPONSE=$(curl -s -X POST "http://localhost:9080/webhooks/github?project=test-project" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: ping" \
  -H "X-Hub-Signature-256: sha256=$SIGNATURE" \
  -d "$PAYLOAD")
echo "$RESPONSE"

# Test the new API webhook endpoint
echo -e "\n===================="
echo "Testing new API webhook endpoint"
echo "===================="
RESPONSE=$(curl -s -X POST "http://localhost:9080/api/v1/webhooks/github?project=test-project" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: ping" \
  -H "X-Hub-Signature-256: sha256=$SIGNATURE" \
  -d "$PAYLOAD")
echo "$RESPONSE"

# Test missing project key
echo -e "\n===================="
echo "Testing missing project key"
echo "===================="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:9080/api/v1/webhooks/github" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: ping" \
  -H "X-Hub-Signature-256: sha256=$SIGNATURE" \
  -d "$PAYLOAD")
echo "Expected HTTP 400, got: $HTTP_CODE"

# Test invalid signature
echo -e "\n===================="
echo "Testing invalid signature"
echo "===================="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:9080/api/v1/webhooks/github?project=test-project" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: ping" \
  -H "X-Hub-Signature-256: sha256=invalid-signature" \
  -d "$PAYLOAD")
echo "Expected HTTP 401, got: $HTTP_CODE"

# Test missing event type
echo -e "\n===================="
echo "Testing missing event type"
echo "===================="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:9080/api/v1/webhooks/github?project=test-project" \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: sha256=$SIGNATURE" \
  -d "$PAYLOAD")
echo "Expected HTTP 400, got: $HTTP_CODE"

echo -e "\n===================="
echo "All tests completed"
echo "===================="