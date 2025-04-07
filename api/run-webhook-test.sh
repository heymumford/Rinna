#\!/bin/bash
set -e

# Create a test config in target directory
mkdir -p "$(dirname "$0")/../target/test/webhook-config"
cat > "$(dirname "$0")/../target/test/webhook-config/config.yaml" << 'CONFIG'
project:
  name: "Rinna"
  version: "1.3.0"
  environment: "development"

server:
  port: 9080
  host: "localhost"
  shutdownTimeout: 15

java:
  host: "localhost"
  port: 8081
  connectTimeout: 5000
  requestTimeout: 30000
  endpoints:
    workitems: "/api/workitems"
    health: "/health"

logging:
  level: "debug"
  format: "text"
  file: ""

auth:
  tokenSecret: "rinna-development-secret-key"
  tokenExpiry: 60
  secretExpiry: 60
  devMode: true
  allowedOrigins:
    - "http://localhost:3000"
    - "http://localhost:8080"
CONFIG

# Set environment variable to point to our config dir
export RINNA_PROJECT_CONFIG_DIR="$(dirname "$0")/../target/test/webhook-config"

# Run a simple curl test directly against our handler
echo "Testing webhook validation in isolation..."

# Create output directory in target
mkdir -p "$(dirname "$0")/../target/test/webhook-bin"

# Compile the test binary
go test -c ./internal/handlers -o "$(dirname "$0")/../target/test/webhook-bin/webhook_test"

# Run the test directly
"$(dirname "$0")/../target/test/webhook-bin/webhook_test" -test.v -test.run TestHandleGitHubWebhook_Ping

# No cleanup needed as files are in target directory - they will be cleaned up by maven clean
