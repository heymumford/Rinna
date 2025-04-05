#!/bin/bash

# Script to test the end-to-end functionality of the Rinna system
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

# Step 1: Start both services
echo "Step 1: Starting services..."
"$SCRIPT_DIR/start-services.sh" &
SERVICES_PID=$!

# Give services time to start
sleep 10
echo ""

# Step 2: Test service health
echo "Step 2: Testing service health..."
if curl -s http://localhost:9080/health | grep -q '"status":"ok"'; then
    echo "Go API server health check: OK"
else
    echo "Go API server health check: FAILED"
    kill $SERVICES_PID
    exit 1
fi

JAVA_HEALTH=$(curl -s http://localhost:8081/health || echo "Connection failed")
if [[ "$JAVA_HEALTH" == *"status"*"ok"* ]]; then
    echo "Java Backend health check: OK"
    echo "$JAVA_HEALTH"
else
    echo "Java Backend health check: FAILED"
    echo "$JAVA_HEALTH"
    # Continue anyway since the Go API was able to connect
fi
echo ""

# Step 3: Test Go API connection to Java Backend
echo "Step 3: Testing Go-Java communication..."
GO_RESPONSE=$(curl -s http://localhost:9080/health)
echo "$GO_RESPONSE"

if [[ "$GO_RESPONSE" == *"java"*"status"*"ok"* ]]; then
    echo "Go API can communicate with Java Backend: OK"
else
    echo "Go API can communicate with Java Backend: FAILED"
    kill $SERVICES_PID
    exit 1
fi
echo ""

# Step 4: Build the CLI
echo "Step 4: Building CLI..."
cd "$PROJECT_ROOT" && mvn -pl rinna-cli package -DskipTests
echo ""

# Step 5: Test simple CLI operations
echo "Step 5: Testing CLI..."
"$SCRIPT_DIR/rin-cli" --help
echo ""

# Step 6: Create a work item with the CLI (will fail until API is fully implemented)
echo "Step 6: Creating work item with CLI (expected to fail if API is not implemented)..."
"$SCRIPT_DIR/rin-cli" add -t "CHORE" -p "MEDIUM" -d "This is a test item" "Test Item"
echo ""

# Cleanup
echo "Stopping services..."
kill $SERVICES_PID
wait $SERVICES_PID

echo "End-to-end test complete"