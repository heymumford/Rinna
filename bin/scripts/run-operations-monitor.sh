#!/bin/bash

# Script to run the Rinna Operations Monitor Demo
# This demonstrates the PUI integration with the CLI services

# Activate the Java environment first
if [ -f ./activate-java.sh ]; then
  source ./activate-java.sh
fi

# Build the project if needed
if [ ! -d "./target/classes" ]; then
  echo "Building project..."
  mvn compile
fi

# Run the operations monitor demo
echo "Starting Rinna Operations Monitor..."
java -cp target/classes org.rinna.pui.examples.OperationsMonitorDemo