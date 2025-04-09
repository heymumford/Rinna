#!/bin/bash

# Script to run the Rinna Work Item List Demo
# This demonstrates the PUI integration with work item filtering and sorting

# Activate the Java environment first
if [ -f ./activate-java.sh ]; then
  source ./activate-java.sh
fi

# Build the project if needed
if [ ! -d "./target/classes" ]; then
  echo "Building project..."
  mvn compile
fi

# Run the work item list demo
echo "Starting Rinna Work Item List Demo..."
java -cp target/classes org.rinna.pui.examples.WorkItemListDemo