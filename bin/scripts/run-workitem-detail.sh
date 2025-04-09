#!/bin/bash

# Script to run the Rinna Work Item Detail Demo with Miller columns navigation
# This demonstrates the PUI integration with hierarchical work item navigation

# Activate the Java environment first
if [ -f ./activate-java.sh ]; then
  source ./activate-java.sh
fi

# Build the project if needed
if [ ! -d "./target/classes" ]; then
  echo "Building project..."
  mvn compile
fi

# Run the work item detail demo
echo "Starting Rinna Work Item Detail Demo with Miller columns..."
java -cp target/classes org.rinna.pui.examples.WorkItemDetailDemo