#!/bin/bash

# Script to start both Java and Go services
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

# Start the Java server in the background
echo "Starting Java server..."
"$SCRIPT_DIR/start-java-server.sh" 8081 &
JAVA_PID=$!

# Give the Java server a moment to start
sleep 5
echo "Java server started with PID $JAVA_PID"

# Start the Go API server in the background
echo "Starting Go API server..."
cd "$PROJECT_ROOT/api" && ./rinnasrv -config configs/config.yaml &
GO_PID=$!

# Give the Go server a moment to start
sleep 2
echo "Go API server started with PID $GO_PID"

# Register a cleanup handler
cleanup() {
    echo "Stopping services..."
    kill $GO_PID
    kill $JAVA_PID
    wait $GO_PID
    wait $JAVA_PID
    echo "Services stopped"
}

# Register the cleanup handler on script exit
trap cleanup EXIT

# Check if both servers are running
echo "Checking server status..."
cd "$PROJECT_ROOT/api" && ./test_health
echo ""
echo "Both services are now running."
echo "Press Ctrl+C to stop both services."

# Wait for user to hit Ctrl+C
wait