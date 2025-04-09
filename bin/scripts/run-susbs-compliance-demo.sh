#!/bin/bash

# Script to run the SUSBS compliance demo for Rinna PUI
# This demonstrates how PUI components integrate with shell environments per SUSBS standards

# Set up environment
source activate-java.sh 2>/dev/null || echo "No Java environment activation found, using system Java"

# Ensure we're in the project directory
cd "$(dirname "$0")" || exit 1

# Compile the demo
echo "Compiling SUSBS compliance demo..."
mvn compile -q

# Run the demo
echo "Starting SUSBS compliance demo..."
echo "This demo shows how PUI components integrate with shell operations."
echo "You can enter commands, use direct shell commands with ! prefix,"
echo "escape to shell, and generate shell scripts from UI operations."
echo "Press 'q' to exit the demo when running."
echo ""
echo "Press Enter to continue..."
read -r

# Use Java to run the demo
java -cp target/classes org.rinna.pui.examples.SUSBSComplianceDemo

# Exit with the status of the Java command
exit $?