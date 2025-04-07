#\!/bin/bash

# Create a temp directory for our config in target folder
mkdir -p "$(dirname "$0")/../target/test/webhook-integration/configs"

# Create a test config file
cat > "$(dirname "$0")/../target/test/webhook-integration/configs/config.yaml" << 'CONFIG'
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

logging:
  level: "info"
  format: "json"
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

# Start the server in the background with the test config
./rinnasrv --config "$(dirname "$0")/../target/test/webhook-integration/configs/config.yaml" --port 9080 &
SERVER_PID=$\!

# Wait for the server to start
echo "Starting server with PID $SERVER_PID..."
sleep 3

# Create a payload and compute signature
PAYLOAD='{"zen": "Test ping", "hook_id": 123456, "hook": {"type": "ping", "id": 123456}}'
SECRET="gh-webhook-secret-1234"  # Dev mode secret
SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$SECRET" | cut -d' ' -f2)

# Send a ping request to the webhook endpoint
echo "Sending test ping request..."
curl -v -X POST "http://localhost:9080/webhooks/github?project=test-project" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: ping" \
  -H "X-Hub-Signature-256: sha256=$SIGNATURE" \
  -d "$PAYLOAD"

# Let the response finish
sleep 1

# Kill the server
echo -e "\nStopping server with PID $SERVER_PID..."
kill $SERVER_PID

# No cleanup needed as files are in target directory - they will be cleaned up by maven clean
