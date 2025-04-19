#!/bin/bash
#
# notification_system_test.sh - Cross-language test for notification system
#
# This test verifies that the notification system works correctly across
# Java, Go, and Python components, ensuring events can be sent and
# received properly across language boundaries.
#
# Copyright (c) 2025 Eric C. Mumford (@heymumford)
# This file is subject to the terms and conditions defined in
# the LICENSE file, which is part of this source code package.
# (MIT License)

set -eo pipefail

# Source test utilities if available
if [[ -f "./test/common/test_utils.sh" ]]; then
  source "./test/common/test_utils.sh"
fi

# Test variables
API_PORT="${RINNA_TEST_API_PORT:-8085}"
TEST_TEMP_DIR="${RINNA_TEST_TEMP_DIR:-./target/cross-language-tests}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CLI_PATH="$PROJECT_ROOT/bin/rin"
NOTIFICATION_ID=""
PYTHON_RECEIVER="${TEST_TEMP_DIR}/notification_receiver.py"
PYTHON_SENDER="${TEST_TEMP_DIR}/notification_sender.py"

# Test setup
setup() {
  echo "Setting up notification system test..."
  
  # Create temp directory if needed
  mkdir -p "$TEST_TEMP_DIR"
  
  # Ensure CLI exists
  if [[ ! -x "$CLI_PATH" ]]; then
    echo "Error: CLI not found at $CLI_PATH"
    exit 1
  fi
  
  # Check if API is available
  if ! curl -s "http://localhost:$API_PORT/api/health" > /dev/null; then
    echo "Error: API server not running on port $API_PORT"
    exit 1
  fi
  
  # Check Python is available
  if ! command -v python >/dev/null 2>&1; then
    echo "Error: Python not found in PATH"
    exit 1
  fi
  
  # Create Python notification scripts
  create_python_notification_scripts
  
  echo "Setup complete"
}

# Create Python notification scripts
create_python_notification_scripts() {
  # Create notification receiver
  cat > "$PYTHON_RECEIVER" <<EOF
#!/usr/bin/env python3
"""
Notification receiver for cross-language testing.

This script listens for notifications using the API and logs received notifications.
"""

import os
import sys
import json
import time
import requests
import threading
from datetime import datetime

# Configuration from environment
API_PORT = os.environ.get("RINNA_TEST_API_PORT", "8085")
API_URL = f"http://localhost:{API_PORT}/api"
TEST_TEMP_DIR = os.environ.get("RINNA_TEST_TEMP_DIR", "./target/cross-language-tests")
LOG_FILE = os.path.join(TEST_TEMP_DIR, "notification_receiver.log")
RECEIVED_FILE = os.path.join(TEST_TEMP_DIR, "received_notifications.json")

# Global variables
should_exit = False
received_notifications = []

def log_message(message):
    """Log a message to the log file."""
    timestamp = datetime.now().isoformat()
    with open(LOG_FILE, 'a') as f:
        f.write(f"[{timestamp}] {message}\n")
    print(f"[{timestamp}] {message}")

def save_received_notifications():
    """Save received notifications to file."""
    with open(RECEIVED_FILE, 'w') as f:
        json.dump(received_notifications, f, indent=2)
    log_message(f"Saved {len(received_notifications)} notifications to {RECEIVED_FILE}")

def poll_notifications():
    """Poll the notification API endpoint."""
    global received_notifications
    
    log_message("Starting notification polling...")
    
    while not should_exit:
        try:
            response = requests.get(f"{API_URL}/notifications?unread=true")
            
            if response.status_code == 200:
                notifications = response.json()
                
                if notifications:
                    log_message(f"Received {len(notifications)} notifications")
                    
                    for notification in notifications:
                        notification_id = notification.get('id')
                        if notification_id:
                            log_message(f"Processing notification: {notification_id}")
                            
                            # Add timestamp when received
                            notification['received_at'] = datetime.now().isoformat()
                            notification['received_by'] = 'python-receiver'
                            
                            # Add to received list
                            received_notifications.append(notification)
                            
                            # Mark as read
                            mark_response = requests.post(
                                f"{API_URL}/notifications/{notification_id}/read"
                            )
                            
                            if mark_response.status_code == 200:
                                log_message(f"Marked notification {notification_id} as read")
                            else:
                                log_message(f"Failed to mark notification as read: {mark_response.status_code}")
            
            # Save current state
            save_received_notifications()
            
        except Exception as e:
            log_message(f"Error polling notifications: {str(e)}")
        
        # Sleep before next poll
        time.sleep(2)

def main():
    """Main function for the notification receiver."""
    global should_exit
    
    # Initialize log file
    with open(LOG_FILE, 'w') as f:
        f.write(f"[{datetime.now().isoformat()}] Notification receiver started\n")
    
    # Initialize received file
    with open(RECEIVED_FILE, 'w') as f:
        json.dump([], f)
    
    # Start polling thread
    polling_thread = threading.Thread(target=poll_notifications)
    polling_thread.daemon = True
    polling_thread.start()
    
    log_message("Notification receiver is running. Press Ctrl+C to stop.")
    
    try:
        # Run for specified duration or until manually interrupted
        if len(sys.argv) > 1 and sys.argv[1].isdigit():
            duration = int(sys.argv[1])
            log_message(f"Will run for {duration} seconds")
            time.sleep(duration)
        else:
            # Run until interrupted
            while True:
                time.sleep(1)
    except KeyboardInterrupt:
        log_message("Received interrupt, shutting down...")
    finally:
        should_exit = True
        polling_thread.join(timeout=3)
        save_received_notifications()
        log_message("Notification receiver stopped")

if __name__ == "__main__":
    main()
EOF

  # Create notification sender
  cat > "$PYTHON_SENDER" <<EOF
#!/usr/bin/env python3
"""
Notification sender for cross-language testing.

This script sends notifications through the API for testing cross-language notification.
"""

import os
import sys
import json
import uuid
import argparse
import requests
from datetime import datetime

# Configuration from environment
API_PORT = os.environ.get("RINNA_TEST_API_PORT", "8085")
API_URL = f"http://localhost:{API_PORT}/api"
TEST_TEMP_DIR = os.environ.get("RINNA_TEST_TEMP_DIR", "./target/cross-language-tests")

def send_notification(type_name, title, message, user=None, priority=None):
    """Send a notification via the API."""
    # Generate a unique notification ID with timestamp
    notification_id = f"test-{str(uuid.uuid4())[:8]}"
    
    # Prepare notification data
    notification = {
        "id": notification_id,
        "type": type_name,
        "title": title,
        "message": message,
        "createdAt": datetime.now().isoformat(),
        "metadata": {
            "source": "python-sender",
            "test": True,
            "timestamp": datetime.now().isoformat()
        }
    }
    
    # Add optional fields
    if user:
        notification["user"] = user
    
    if priority:
        notification["priority"] = priority
    
    try:
        # Send notification
        response = requests.post(
            f"{API_URL}/notifications",
            json=notification,
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 201:
            print(f"Notification sent successfully: {notification_id}")
            result = response.json()
            print(json.dumps(result, indent=2))
            return result.get("id", notification_id)
        else:
            print(f"Failed to send notification: {response.status_code}")
            print(response.text)
            return None
    except Exception as e:
        print(f"Error sending notification: {str(e)}")
        return None

def main():
    """Main function to process arguments and send notifications."""
    parser = argparse.ArgumentParser(description="Send notifications for cross-language testing")
    parser.add_argument("--type", "-t", default="TEST", help="Notification type")
    parser.add_argument("--title", default="Test Notification", help="Notification title")
    parser.add_argument("--message", "-m", required=True, help="Notification message")
    parser.add_argument("--user", "-u", help="Target user (optional)")
    parser.add_argument("--priority", "-p", choices=["LOW", "MEDIUM", "HIGH"], default="MEDIUM", 
                        help="Notification priority")
    
    args = parser.parse_args()
    
    # Send notification
    notification_id = send_notification(
        args.type, 
        args.title, 
        args.message, 
        args.user, 
        args.priority
    )
    
    if notification_id:
        print(notification_id)
        sys.exit(0)
    else:
        sys.exit(1)

if __name__ == "__main__":
    main()
EOF

  # Make the scripts executable
  chmod +x "$PYTHON_RECEIVER" "$PYTHON_SENDER"
}

# Test teardown
teardown() {
  echo "Tearing down notification system test..."
  
  # Clean up any created notifications
  if [[ -n "$NOTIFICATION_ID" ]]; then
    curl -s -X DELETE "http://localhost:$API_PORT/api/notifications/$NOTIFICATION_ID" > /dev/null || true
  fi
  
  echo "Teardown complete"
}

# Test Java CLI notification creation
test_java_cli_notification_creation() {
  echo "Testing Java CLI notification creation..."
  
  # Create a notification using the CLI
  local notification_output
  notification_output=$("$CLI_PATH" notify create --title="Test from Java" --message="Test notification from Java CLI" --type=CLI_TEST --output=json)
  
  # Check if notification was created successfully
  if [[ $? -ne 0 ]]; then
    echo "Error: Failed to create notification via Java CLI"
    echo "CLI output: $notification_output"
    return 1
  fi
  
  # Extract notification ID
  NOTIFICATION_ID=$(echo "$notification_output" | grep -o '"id":\s*"[^"]*"' | cut -d'"' -f4)
  
  if [[ -z "$NOTIFICATION_ID" ]]; then
    echo "Error: Failed to extract notification ID from output"
    echo "CLI output: $notification_output"
    return 1
  fi
  
  echo "Created notification with ID: $NOTIFICATION_ID"
  return 0
}

# Test Go API notification retrieval
test_go_api_notification_retrieval() {
  echo "Testing Go API notification retrieval..."
  
  if [[ -z "$NOTIFICATION_ID" ]]; then
    echo "Error: No notification ID available for retrieval"
    return 1
  fi
  
  # Retrieve notification from API
  local response
  response=$(curl -s "http://localhost:$API_PORT/api/notifications/$NOTIFICATION_ID")
  
  # Check if notification exists with correct properties
  if echo "$response" | grep -q "\"id\":\"$NOTIFICATION_ID\"" && \
     echo "$response" | grep -q "\"title\":\"Test from Java\""; then
    echo "Notification retrieved successfully from Go API"
    return 0
  else
    echo "Error: Notification not found or has incorrect properties"
    echo "API response: $response"
    return 1
  fi
}

# Test Python notification listening
test_python_notification_listening() {
  echo "Testing Python notification listening..."
  
  # Start the Python notification receiver in background
  python "$PYTHON_RECEIVER" 10 > "${TEST_TEMP_DIR}/receiver_output.log" 2>&1 &
  local receiver_pid=$!
  
  # Wait for receiver to initialize
  sleep 3
  
  # Create a test notification using the Python sender
  local python_notification_id
  python_notification_id=$(python "$PYTHON_SENDER" --title="Test from Python" --message="This is a test notification from Python")
  
  if [[ -z "$python_notification_id" ]]; then
    echo "Error: Failed to create notification from Python"
    kill $receiver_pid 2>/dev/null || true
    return 1
  fi
  
  echo "Created notification with ID: $python_notification_id from Python"
  
  # Wait for notification to be processed
  sleep 5
  
  # Kill the receiver process
  kill $receiver_pid 2>/dev/null || true
  
  # Check if notification was received
  if [[ -f "${TEST_TEMP_DIR}/received_notifications.json" ]]; then
    if grep -q "$python_notification_id" "${TEST_TEMP_DIR}/received_notifications.json"; then
      echo "Python successfully received notification"
      return 0
    else
      echo "Error: Python did not receive the notification"
      cat "${TEST_TEMP_DIR}/received_notifications.json"
      return 1
    fi
  else
    echo "Error: Received notifications file not found"
    return 1
  fi
}

# Test Java CLI notification listing
test_java_cli_notification_listing() {
  echo "Testing Java CLI notification listing..."
  
  # List notifications using CLI
  local notifications
  notifications=$("$CLI_PATH" notify list --output=json)
  
  # Check if listing succeeded
  if [[ $? -ne 0 ]]; then
    echo "Error: Failed to list notifications via Java CLI"
    echo "CLI output: $notifications"
    return 1
  fi
  
  # Check if our notifications are in the list
  if echo "$notifications" | grep -q "\"title\":\"Test from Java\"" && \
     echo "$notifications" | grep -q "\"title\":\"Test from Python\""; then
    echo "Both notifications found in Java CLI listing"
    return 0
  else
    echo "Error: Not all notifications found in Java CLI listing"
    echo "CLI output: $notifications"
    return 1
  fi
}

# Test notification marking as read via Go API
test_go_api_notification_marking() {
  echo "Testing notification marking as read via Go API..."
  
  if [[ -z "$NOTIFICATION_ID" ]]; then
    echo "Error: No notification ID available for marking"
    return 1
  fi
  
  # Mark notification as read
  local response
  response=$(curl -s -X POST "http://localhost:$API_PORT/api/notifications/$NOTIFICATION_ID/read")
  
  # Check if operation succeeded
  if [[ $? -eq 0 ]]; then
    echo "Notification marked as read via Go API"
    
    # Verify notification is marked as read
    local check_response
    check_response=$(curl -s "http://localhost:$API_PORT/api/notifications/$NOTIFICATION_ID")
    
    if echo "$check_response" | grep -q "\"read\":true"; then
      echo "Notification confirmed as read"
      return 0
    else
      echo "Error: Notification not marked as read"
      echo "API response: $check_response"
      return 1
    fi
  else
    echo "Error: Failed to mark notification as read"
    echo "API response: $response"
    return 1
  fi
}

# Test notification clearing via Java CLI
test_java_cli_notification_clearing() {
  echo "Testing notification clearing via Java CLI..."
  
  # Clear read notifications
  local clear_output
  clear_output=$("$CLI_PATH" notify clear --read-only)
  
  # Check if clearing succeeded
  if [[ $? -ne 0 ]]; then
    echo "Error: Failed to clear notifications via Java CLI"
    echo "CLI output: $clear_output"
    return 1
  fi
  
  # Verify the read notification is no longer in the list
  local notifications
  notifications=$("$CLI_PATH" notify list --output=json)
  
  if echo "$notifications" | grep -q "\"id\":\"$NOTIFICATION_ID\""; then
    echo "Error: Cleared notification still appears in listing"
    echo "CLI output: $notifications"
    return 1
  else
    echo "Notification successfully cleared via Java CLI"
    return 0
  fi
}

# Run the complete test
run_test() {
  local success=true
  
  setup
  
  # Run test steps, aborting on first failure
  if ! test_java_cli_notification_creation; then
    echo "Test failed at step: test_java_cli_notification_creation"
    success=false
  elif ! test_go_api_notification_retrieval; then
    echo "Test failed at step: test_go_api_notification_retrieval"
    success=false
  elif ! test_python_notification_listening; then
    echo "Test failed at step: test_python_notification_listening"
    success=false
  elif ! test_java_cli_notification_listing; then
    echo "Test failed at step: test_java_cli_notification_listing"
    success=false
  elif ! test_go_api_notification_marking; then
    echo "Test failed at step: test_go_api_notification_marking"
    success=false
  elif ! test_java_cli_notification_clearing; then
    echo "Test failed at step: test_java_cli_notification_clearing"
    success=false
  fi
  
  teardown
  
  if $success; then
    echo "Notification system test completed successfully"
    return 0
  else
    echo "Notification system test failed"
    return 1
  fi
}

# Run the test if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  run_test
fi